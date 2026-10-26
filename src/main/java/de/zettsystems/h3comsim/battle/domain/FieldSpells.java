package de.zettsystems.h3comsim.battle.domain;

import de.zettsystems.h3comsim.battle.domain.events.Side;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Wahl der Feldzauber (heroes.thelazy.net; Formen wie VCMI {@code config/spells/other.json}):
 *
 * <ul>
 *   <li>Fire Wall und Force Field: das Zielfeld und sein oberer Nachbar zur Gegenseite hin, ab
 *       Advanced ein drittes Feld darüber; nur auf freie Felder. Gesetzt werden sie auf den Weg
 *       eines gegnerischen Bodenstacks zu seinem nächsten Ziel: Fire Wall für den Schaden beim
 *       Durchgehen, Force Field für die Runden, die der Umweg kostet.</li>
 *   <li>Quicksand und Land Mine: 4/6/8 zufällige freie Felder; bewertet nach der Chance, dass ein
 *       gegnerischer Bodenstack auf seinem Weg hineintritt. Land Mine geht nicht, wenn ein Gegner auf
 *       seinem Heimatgelände kämpft und nicht immun ist — er sieht die Minen.</li>
 *   <li>Remove Obstacle: ein Hindernis (Basic), auch Fire Wall (Advanced), auch Force Field,
 *       Quicksand und Minen (Expert); bewertet nach den Runden, die eigene Bodentruppen dadurch
 *       früher am Feind sind. Große Hindernisse an fester Stelle (Klippen) bleiben stehen.</li>
 * </ul>
 */
final class FieldSpells {

    /** Ein gegnerischer Stack geht mit dieser Chance wirklich durch die Feuerwand. */
    private static final double PASS_CHANCE = 0.7;
    private static final int UNREACHABLE = Integer.MAX_VALUE;

    private FieldSpells() {
    }

    static boolean handles(Spell spell) {
        return spell == Spell.FIRE_WALL || spell == Spell.FORCE_FIELD || spell == Spell.QUICKSAND
                || spell == Spell.LAND_MINE || spell == Spell.REMOVE_OBSTACLE;
    }

    /** Die Felder einer Wand an {@code center}, gezaubert von {@code caster}. */
    static List<Hex> wall(Hex center, Side caster, SkillLevel level) {
        Hex second = caster == Side.ATTACKER ? topRight(center) : topLeft(center);
        List<Hex> hexes = new ArrayList<>(List.of(center, second));
        if (level == SkillLevel.ADVANCED || level == SkillLevel.EXPERT) {
            hexes.add(caster == Side.ATTACKER ? topLeft(second) : topRight(second));
        }
        return hexes;
    }

    /** 4/6/8 Felder für Quicksand und Land Mine. */
    static int patches(SkillLevel level) {
        return SpellDamage.byLevel(level, 4, 6, 8);
    }

    static List<HeroSpellcasting.Cast> candidates(Side side, Spell spell, SkillLevel level, BattleSetup setup) {
        return switch (spell) {
            case FIRE_WALL, FORCE_FIELD -> walls(side, spell, level, setup);
            case QUICKSAND, LAND_MINE -> scattered(side, spell, level, setup);
            case REMOVE_OBSTACLE -> removable(level, side, setup).stream()
                    .map(group -> new HeroSpellcasting.Cast(spell, level, null, List.of(), group)).toList();
            default -> throw new IllegalArgumentException(spell + " is no field spell");
        };
    }

    /** Wände auf den ersten Feldern des Wegs jedes gegnerischen Bodenstacks. */
    private static List<HeroSpellcasting.Cast> walls(Side side, Spell spell, SkillLevel level, BattleSetup setup) {
        List<HeroSpellcasting.Cast> casts = new ArrayList<>();
        Set<List<Hex>> seen = new HashSet<>();
        for (Stack enemy : walkers(HeroSpellcasting.other(side), setup)) {
            List<Hex> path = pathToNearestFoe(enemy, setup.battlefieldFor(enemy.side()), setup);
            for (int i = 0; i < Math.min(3, path.size() - 1); i++) {
                List<Hex> hexes = wall(path.get(i), side, level);
                if (placeable(hexes, side, setup) && seen.add(hexes)) {
                    casts.add(new HeroSpellcasting.Cast(spell, level, enemy, List.of(enemy), hexes));
                }
            }
        }
        return casts;
    }

    /** Quicksand und Land Mine: ein Wurf über das ganze Feld, bewertet gegen alle gegnerischen Bodenstacks. */
    private static List<HeroSpellcasting.Cast> scattered(Side side, Spell spell, SkillLevel level, BattleSetup setup) {
        Side enemySide = HeroSpellcasting.other(side);
        boolean spotted = spell == Spell.LAND_MINE && setup.stacksOf(enemySide).stream()
                .anyMatch(s -> s.onNativeTerrain() && !SpellResistance.isImmune(s, Spell.LAND_MINE));
        List<Stack> walkers = walkers(enemySide, setup);
        if (spotted || walkers.isEmpty() || freeHexes(setup).size() < patches(level)) {
            return List.of();
        }
        return List.of(new HeroSpellcasting.Cast(spell, level, null, walkers));
    }


    static double value(Hero hero, Side side, HeroSpellcasting.Cast cast, BattleSetup setup) {
        Spell spell = cast.spell();
        int power = hero.spellPower();
        return switch (spell) {
            case FIRE_WALL -> {
                Stack enemy = cast.affected().getFirst();
                int damage = SpellDamage.toTarget(spell, SpellDamage.base(spell, cast.level(), power), hero, enemy);
                yield PASS_CHANCE * Math.min(damage, HeroSpellcasting.totalHealth(enemy))
                        * HeroSpellcasting.costPerHealth(enemy);
            }
            case FORCE_FIELD -> {
                Stack enemy = cast.affected().getFirst();
                Battlefield field = setup.battlefieldFor(enemy.side());
                int before = roundsToFoe(enemy, field, setup);
                Set<Hex> blocked = new HashSet<>(field.obstacles());
                blocked.addAll(cast.hexes());
                int after = roundsToFoe(enemy, field.withObstacles(blocked), setup);
                yield before == UNREACHABLE ? 0 : Math.min(delay(before, after), HeroSpellcasting.EXPECTED_ROUNDS)
                        * HeroSpellcasting.stackValue(enemy) * HeroSpellcasting.ROUND_SHARE;
            }
            case QUICKSAND, LAND_MINE -> {
                double share = (double) patches(cast.level()) / Math.max(1, freeHexes(setup).size());
                double total = 0;
                for (Stack enemy : cast.affected()) {
                    int steps = Math.min(10, pathToNearestFoe(enemy, setup.battlefieldFor(enemy.side()), setup).size());
                    double chance = 1 - Math.pow(1 - share, steps);
                    if (spell == Spell.QUICKSAND) {
                        total += chance * HeroSpellcasting.stackValue(enemy) * HeroSpellcasting.ROUND_SHARE;
                    } else {
                        int damage = SpellDamage.toTarget(spell, SpellDamage.base(spell, cast.level(), power), hero,
                                enemy);
                        total += chance * Math.min(damage, HeroSpellcasting.totalHealth(enemy))
                                * HeroSpellcasting.costPerHealth(enemy);
                    }
                }
                yield total;
            }
            case REMOVE_OBSTACLE -> {
                double total = 0;
                for (Stack own : walkers(side, setup)) {
                    Battlefield field = setup.battlefieldFor(side);
                    int before = roundsToFoe(own, field, setup);
                    Set<Hex> open = new HashSet<>(field.obstacles());
                    cast.hexes().forEach(open::remove);
                    int after = roundsToFoe(own, field.withObstacles(open), setup);
                    total += Math.min(delay(after, before), HeroSpellcasting.EXPECTED_ROUNDS)
                            * HeroSpellcasting.stackValue(own) * HeroSpellcasting.ROUND_SHARE;
                }
                yield total;
            }
            default -> throw new IllegalArgumentException(spell + " is no field spell");
        };
    }

    /** Wie viele Runden später als {@code faster} ein Stack mit {@code slower} ankommt. */
    private static int delay(int faster, int slower) {
        if (faster == UNREACHABLE) {
            return 0;
        }
        return slower == UNREACHABLE ? HeroSpellcasting.EXPECTED_ROUNDS : Math.max(0, slower - faster);
    }

    /** Lebende Bodenstacks einer Seite, die laufen müssen: keine Flieger, keine Schützen, keine Maschinen. */
    static List<Stack> walkers(Side side, BattleSetup setup) {
        return setup.stacksOf(side).stream()
                .filter(s -> s.isAlive() && !s.isWarMachine() && !s.canShoot()
                        && s.unit().movement() == Movement.GROUND)
                .toList();
    }

    /** Runden bis neben den nächsten Gegner auf diesem Feld; {@link #UNREACHABLE}, wenn kein Weg führt. */
    static int roundsToFoe(Stack mover, Battlefield field, BattleSetup setup) {
        List<Stack> foes = setup.opponentsOf(mover);
        if (foes.stream().anyMatch(f -> mover.distanceTo(f) == 1)) {
            return 0;
        }
        List<Hex> path = pathToNearestFoe(mover, field, setup);
        return path.isEmpty() ? UNREACHABLE : Math.ceilDiv(path.size(), Math.max(1, mover.getSpeed()));
    }

    /** Der Weg zum nächsten Gegner, über dessen Landefelder; leer, wenn keiner führt. */
    static List<Hex> pathToNearestFoe(Stack mover, Battlefield field, BattleSetup setup) {
        Stack nearest = setup.opponentsOf(mover).stream()
                .min(Comparator.comparingInt(mover::distanceTo)).orElse(null);
        if (nearest == null) {
            return List.of();
        }
        List<Hex> best = List.of();
        for (Hex head : setup.attackHeads(mover, nearest)) {
            List<Hex> path = field.pathFor(mover, head);
            if (!path.isEmpty() && (best.isEmpty() || path.size() < best.size())) {
                best = path;
            }
        }
        return best;
    }

    /** Alle Felder der Wand liegen im Feld, sind begehbar, unbesetzt und ohne andere Zauber-Felder. */
    static boolean placeable(List<Hex> hexes, Side side, BattleSetup setup) {
        Battlefield field = setup.battlefieldFor(side);
        for (Hex hex : hexes) {
            if (!field.contains(hex) || !field.isPassable(hex) || setup.fieldEffects().occupied(hex)
                    || setup.spellTargets().stream().anyMatch(s -> s.occupies(hex))) {
                return false;
            }
        }
        return true;
    }

    /** Freie Felder für Quicksand und Minen: begehbar, unbesetzt, ohne Zauber-Felder. */
    static List<Hex> freeHexes(BattleSetup setup) {
        Battlefield field = setup.battlefield();
        Set<Hex> standing = setup.standingObstacles();
        Siege siege = setup.siege();
        Set<Hex> walls = siege == null ? Set.of() : siege.blockedFor(Side.ATTACKER);
        List<Hex> free = new ArrayList<>();
        for (Hex hex : field.cells()) {
            if (!standing.contains(hex) && !walls.contains(hex) && !setup.fieldEffects().occupied(hex)
                    && setup.spellTargets().stream().noneMatch(s -> s.occupies(hex))) {
                free.add(hex);
            }
        }
        return free;
    }

    /**
     * Was Remove Obstacle wegnehmen kann: die natürlichen Hindernisse (außer Klippen), ab Advanced
     * Feuerwände, auf Expert alle Zauber-Felder, die die Seite sieht.
     */
    static List<List<Hex>> removable(SkillLevel level, Side side, BattleSetup setup) {
        List<List<Hex>> groups = new ArrayList<>();
        Set<Hex> standing = setup.standingObstacles();
        for (Set<Hex> group : setup.obstacleGroups()) {
            if (standing.containsAll(group)) {
                groups.add(List.copyOf(group));
            }
        }
        boolean fireWalls = level == SkillLevel.ADVANCED || level == SkillLevel.EXPERT;
        boolean all = level == SkillLevel.EXPERT;
        for (FieldEffects.Patch patch : setup.fieldEffects().patches()) {
            boolean visible = patch.owner() == side || patch.revealed() || setup.seesHidden(side)
                    || patch.kind() == FieldEffects.Kind.FIRE_WALL || patch.kind() == FieldEffects.Kind.FORCE_FIELD;
            if (visible && (all || (fireWalls && patch.kind() == FieldEffects.Kind.FIRE_WALL))) {
                groups.add(List.of(patch.hex()));
            }
        }
        return groups;
    }

    static Hex topRight(Hex hex) {
        return new Hex(hex.q() + 1, hex.r() - 1);
    }

    static Hex topLeft(Hex hex) {
        return new Hex(hex.q(), hex.r() - 1);
    }
}
