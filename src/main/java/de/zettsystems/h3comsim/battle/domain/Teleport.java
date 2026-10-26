package de.zettsystems.h3comsim.battle.domain;

import org.jspecify.annotations.Nullable;

import java.util.Comparator;
import java.util.List;

/**
 * Teleport (heroes.thelazy.net, <em>Teleport</em>): ein eigener Stack springt auf ein freies Feld.
 * Basic: nicht über Mauern oder Graben; Advanced: über den Graben, nicht über Mauern; Expert:
 * überallhin. Als einziger Zauber wird er mit der Schule billiger: 15/12/6/3 Mana ohne, mit
 * Basic, Advanced und Expert Water Magic.
 *
 * <p>Der Held teleportiert nur den Stack, der gerade am Zug ist, und nur einen Bodentruppen-Stack,
 * der diese Runde keinen Gegner erreicht: er landet neben dem gefährlichsten Gegner, den er von
 * dort aus angreifen darf, und schlägt sofort zu. Flieger brauchen den Zauber nicht. Ein
 * Landefeld, das gegnerische Nahkämpfer mit mindestens der halben Lebenskraft des Stacks an
 * Schaden erreichen, scheidet aus: ohne diese Regel verlor auf freiem Feld die Seite mit Teleport
 * 0.456 (−3.3 σ, 1440 Kämpfe), mit ihr 0.496; in der Belagerung (Castle, Angreifer doppelt so
 * stark, Expert Water Magic) gewinnt der Angreifer 0.908 statt 0.722.
 */
final class Teleport {

    private Teleport() {
    }

    /** Mana nach der Stufe der Water Magic des Helden, nicht nach der Wirkungsstufe. */
    static int cost(SkillLevel waterMagic) {
        return switch (waterMagic) {
            case NONE -> 15;
            case BASIC -> 12;
            case ADVANCED -> 6;
            case EXPERT -> 3;
        };
    }

    /** Wo {@code mover} landet, oder {@code null}, wenn der Zauber ihm nichts bringt. */
    static @Nullable Hex destination(Stack mover, SkillLevel level, BattleSetup setup) {
        if (!worthTeleporting(mover, setup)) {
            return null;
        }
        Battlefield field = setup.battlefieldFor(mover.side());
        return setup.stacksOf(HeroSpellcasting.other(mover.side())).stream()
                .filter(Stack::isAlive)
                .sorted(Comparator.comparingInt(Teleport::dangerScore).reversed()
                        .thenComparingInt(mover::distanceTo))
                .flatMap(enemy -> setup.attackHeads(mover, enemy).stream())
                .filter(head -> mover.hexesAt(head).stream()
                        .allMatch(h -> field.isPassable(h) && !inMoat(h, setup) && allowed(level, mover, h, setup)))
                .filter(head -> !exposed(mover, head, setup))
                .findFirst().orElse(null);
    }

    /**
     * Was der Sprung bringt: die Runden, die der Stack sonst bis zum Gegner bräuchte (höchstens
     * {@link HeroSpellcasting#EXPECTED_ROUNDS}, hinter einer Mauer ohne Bresche genau so viele),
     * je Runde ein {@link HeroSpellcasting#ROUND_SHARE} seines Werts.
     */
    static double value(Stack mover, BattleSetup setup) {
        int steps = stepsToNearestEnemy(mover, setup);
        int rounds = steps == Integer.MAX_VALUE ? HeroSpellcasting.EXPECTED_ROUNDS
                : Math.min(HeroSpellcasting.EXPECTED_ROUNDS, (steps - 1) / Math.max(1, mover.getSpeed()));
        return HeroSpellcasting.stackValue(mover) * HeroSpellcasting.ROUND_SHARE * rounds;
    }

    private static boolean worthTeleporting(Stack mover, BattleSetup setup) {
        return mover.isAlive() && !mover.isWarMachine() && !mover.isBound() && !mover.canShoot()
                && mover.unit().movement() == Movement.GROUND
                && !SpellResistance.isImmune(mover, Spell.TELEPORT)
                && stepsToNearestEnemy(mover, setup) > mover.getSpeed();
    }

    /** Wie viele Schritte der Stack zu Fuß bis neben den nächsten Gegner braucht; unerreichbar = MAX. */
    private static int stepsToNearestEnemy(Stack mover, BattleSetup setup) {
        Battlefield field = setup.battlefieldFor(mover.side());
        int best = Integer.MAX_VALUE;
        for (Stack enemy : setup.stacksOf(HeroSpellcasting.other(mover.side()))) {
            if (!enemy.isAlive()) {
                continue;
            }
            if (mover.distanceTo(enemy) == 1) {
                return 0;
            }
            for (Hex head : setup.attackHeads(mover, enemy)) {
                List<Hex> path = field.pathFor(mover, head);
                if (!path.isEmpty()) {
                    best = Math.min(best, path.size());
                }
            }
        }
        return best;
    }

    /**
     * Darf der Stack auf dieser Stufe von seinem Feld nach {@code to}? Ohne Belagerung immer;
     * sonst Basic nur innerhalb seines Bereichs (vor dem Graben, im Graben, hinter den Mauern),
     * Advanced auch über den Graben, Expert auch über die Mauern.
     */
    static boolean allowed(SkillLevel level, Stack mover, Hex to, BattleSetup setup) {
        Siege siege = setup.siege();
        if (siege == null || level == SkillLevel.EXPERT) {
            return true;
        }
        Zone from = zone(mover.position(), siege);
        Zone target = zone(to, siege);
        if (level == SkillLevel.ADVANCED) {
            return (from == Zone.INSIDE) == (target == Zone.INSIDE);
        }
        return from == target;
    }

    private enum Zone { BEFORE_MOAT, MOAT, INSIDE }

    private static Zone zone(Hex hex, Siege siege) {
        if (!SiegeLayout.isOutside(hex)) {
            return Zone.INSIDE;
        }
        int moatQ = siege.moat().stream().filter(m -> m.r() == hex.r()).mapToInt(Hex::q).min()
                .orElse(SiegeLayout.GATE_BRIDGE.r() == hex.r() && !siege.moat().isEmpty()
                        ? SiegeLayout.GATE_BRIDGE.q() : Integer.MAX_VALUE);
        return hex.q() >= moatQ ? Zone.MOAT : Zone.BEFORE_MOAT;
    }

    private static boolean inMoat(Hex hex, BattleSetup setup) {
        Siege siege = setup.siege();
        return siege != null && siege.isMoat(hex);
    }

    /** Wie Greedys ungedeckter Anlauf: Nahkämpfer, die das Feld erreichen, gegen die halbe Lebenskraft. */
    private static boolean exposed(Stack mover, Hex head, BattleSetup setup) {
        int incoming = 0;
        for (Stack enemy : setup.stacksOf(HeroSpellcasting.other(mover.side()))) {
            if (enemy.isAlive() && !enemy.canShoot() && enemy.distanceTo(head) <= enemy.getSpeed() + 1) {
                incoming += dangerScore(enemy);
            }
        }
        return 2 * incoming >= HeroSpellcasting.totalHealth(mover);
    }

    private static int dangerScore(Stack s) {
        return (s.unit().minDamage() + s.unit().maxDamage()) / 2 * s.getCount();
    }
}
