package de.zettsystems.h3comsim.battle.domain;

import de.zettsystems.h3comsim.battle.domain.events.Side;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Zauber eines Helden: was er trifft und welchen er wählt.
 *
 * <p>Ein Held zaubert „once per round" (Manual S. 45), und das kostet keinen seiner Stacks die
 * Aktion. Wirkungsbereich nach der Spell Reference (S. 64–79): Fireball und Meteor Shower treffen
 * das Zielfeld und seine Nachbarn, Inferno alles im Umkreis von zwei Feldern, Frost Ring nur die
 * Nachbarn; Chain Lightning springt zum jeweils nächsten Stack und trifft vier (Basic) bzw. fünf
 * Stacks mit jeweils halbem Schaden; Death Ripple trifft alle außer Untoten, Destroy Undead alle
 * Untoten, Armageddon alle. Flächenzauber unterscheiden nicht nach Freund und Feind. Auf Expert
 * wirken die Massenzauber ({@link Spell#massAtExpert()}) auf alle Stacks der Zielseite.
 *
 * <p>Die Wahl ist eine Solver-Heuristik: Jeder bezahlbare Zauber wird auf jedes sinnvolle Ziel
 * bewertet, der beste mit positivem Wert wird gewirkt. Schaden zählt als erwartete vernichtete
 * Kosten (Gegner minus eigene Verluste, mal Erfolgschance gegen Magieresistenz). Ein liegender
 * Zauber zählt als Anteil am Wert des Stacks: {@link #gainPercent} schätzt, um wie viel Prozent
 * er den Stack je Runde stärkt oder schwächt, {@link #ROUND_SHARE} ist der Anteil seines Werts,
 * den ein Stack je Runde im Kampf umsetzt, und die Dauer zählt bis zu {@link #EXPECTED_ROUNDS}
 * Runden. Schutzzauber gegen Schulen, Anti-Magic und Dispel wählt die Heuristik nicht.
 */
final class HeroSpellcasting {

    /** Anteil seines Werts, den ein Stack je Runde an Schaden umsetzt — grob ein Fünftel. */
    static final double ROUND_SHARE = 0.2;
    /** Runden, die eine Wirkung höchstens zählt; ein Kampf ist selten länger entschieden. */
    static final int EXPECTED_ROUNDS = 4;

    private HeroSpellcasting() {
    }

    /**
     * Ein Zauber und wen er trifft. {@code target} ist das gewählte Ziel bei Einzel- und
     * Kettenzaubern, bei Flächen- und Massenzaubern {@code null}; {@code affected} ist die
     * Reihenfolge der Treffer (bei Chain Lightning die Sprungfolge).
     */
    record Cast(Spell spell, SkillLevel level, @Nullable Stack target, List<Stack> affected) {
    }

    /**
     * Der beste bezahlbare Zauber für diese Seite, oder {@code null}, wenn keiner etwas bringt.
     * Ohne Helden, ohne Zauberbuch oder ohne Mana immer {@code null}.
     */
    static @Nullable Cast decide(@Nullable Hero hero, Side side, int mana, BattleSetup setup) {
        if (hero == null || hero.spells().isEmpty()) {
            return null;
        }
        Cast best = null;
        double bestValue = 0;
        for (Spell spell : hero.spells()) {
            if (spell.cost() > mana) {
                continue;
            }
            for (Cast cast : candidates(hero, side, spell, setup)) {
                double value = value(hero, side, cast, setup);
                if (value > bestValue) {
                    best = cast;
                    bestValue = value;
                }
            }
        }
        return best;
    }

    /** Alle sinnvollen Ziele eines Zaubers. */
    static List<Cast> candidates(Hero hero, Side side, Spell spell, BattleSetup setup) {
        SkillLevel level = hero.castLevel(spell);
        List<Stack> own = alive(setup.stacksOf(side));
        List<Stack> enemies = alive(setup.stacksOf(other(side)));
        List<Cast> casts = new ArrayList<>();
        switch (spell) {
            case FIREBALL, METEOR_SHOWER, INFERNO, FROST_RING -> {
                for (Hex center : centers(setup)) {
                    List<Stack> hit = inArea(spell, center, setup);
                    if (!hit.isEmpty()) {
                        casts.add(new Cast(spell, level, null, hit));
                    }
                }
            }
            case CHAIN_LIGHTNING -> enemies.forEach(e ->
                    casts.add(new Cast(spell, level, e, chain(e, level == SkillLevel.BASIC ? 4 : 5, setup))));
            case DEATH_RIPPLE, DESTROY_UNDEAD, ARMAGEDDON -> {
                List<Stack> hit = setup.aliveStacks().stream()
                        .filter(s -> spell == Spell.ARMAGEDDON
                                || (spell == Spell.DESTROY_UNDEAD) == s.unit().isUndead())
                        .toList();
                if (!hit.isEmpty()) {
                    casts.add(new Cast(spell, level, null, hit));
                }
            }
            default -> {
                List<Stack> targets = spell.beneficial() ? own : enemies;
                if (spell.massAtExpert() && level == SkillLevel.EXPERT) {
                    casts.add(new Cast(spell, level, null, targets));
                } else {
                    targets.forEach(t -> casts.add(new Cast(spell, level, t, List.of(t))));
                }
            }
        }
        return casts;
    }

    /** Mittelpunkte für Flächenzauber: die Felder aller Stacks und deren Nachbarn. */
    private static Set<Hex> centers(BattleSetup setup) {
        Set<Hex> centers = new LinkedHashSet<>();
        for (Stack stack : setup.aliveStacks()) {
            centers.add(stack.position());
            for (Hex n : stack.position().neighbors()) {
                if (setup.battlefield().contains(n)) {
                    centers.add(n);
                }
            }
        }
        return centers;
    }

    static List<Stack> inArea(Spell spell, Hex center, BattleSetup setup) {
        int min = spell == Spell.FROST_RING ? 1 : 0;
        int max = spell == Spell.INFERNO ? 2 : 1;
        return setup.aliveStacks().stream()
                .filter(s -> {
                    int d = s.position().distanceTo(center);
                    return d >= min && d <= max;
                })
                .toList();
    }

    /**
     * Sprungfolge von Chain Lightning: vom Ziel jeweils zum nächsten noch nicht getroffenen Stack,
     * gleich welcher Seite; bei gleichem Abstand der zuerst gelistete.
     */
    // s != current: Identitätsvergleich auf der mutable Stack-Entity.
    @SuppressWarnings("ReferenceEquality")
    static List<Stack> chain(Stack first, int strikes, BattleSetup setup) {
        List<Stack> hit = new ArrayList<>();
        hit.add(first);
        Stack current = first;
        while (hit.size() < strikes) {
            Stack next = null;
            int bestDistance = Integer.MAX_VALUE;
            for (Stack s : setup.aliveStacks()) {
                int d = s.position().distanceTo(current.position());
                if (!hit.contains(s) && d < bestDistance) {
                    next = s;
                    bestDistance = d;
                }
            }
            if (next == null) {
                break;
            }
            hit.add(next);
            current = next;
        }
        return hit;
    }

    /** Grundschaden für den {@code index}-ten Treffer: bei Chain Lightning je Sprung halbiert. */
    static int baseDamage(Cast cast, int power, int index) {
        int base = SpellDamage.base(cast.spell(), cast.level(), power);
        return cast.spell() == Spell.CHAIN_LIGHTNING ? base >> index : base;
    }

    /** Heilung von Cure, Animate Dead und Resurrection (Manual S. 64 ff.). */
    static int healing(Spell spell, SkillLevel level, int power) {
        return switch (spell) {
            case CURE -> power * 5 + SpellDamage.byLevel(level, 10, 20, 30);
            case ANIMATE_DEAD -> power * 50 + SpellDamage.byLevel(level, 30, 60, 160);
            case RESURRECTION -> power * 50 + SpellDamage.byLevel(level, 40, 80, 160);
            default -> 0;
        };
    }

    /** Erwarteter Nutzen in Gold; eigene Verluste zählen negativ. */
    static double value(Hero hero, Side side, Cast cast, BattleSetup setup) {
        Spell spell = cast.spell();
        double total = 0;
        for (int i = 0; i < cast.affected().size(); i++) {
            Stack s = cast.affected().get(i);
            double chance = successChance(spell, s, setup);
            if (chance == 0) {
                continue;
            }
            double value = switch (spell.kind()) {
                case DAMAGE -> Math.min(SpellDamage.toTarget(spell, baseDamage(cast, hero.spellPower(), i), hero, s),
                        totalHealth(s)) * costPerHealth(s);
                case INSTANT -> instantValue(spell, cast.level(), hero.spellPower(), s);
                case EFFECT -> effectValue(spell, cast.level(), hero.spellPower(), s, setup);
            };
            total += (s.side() == side ? -1 : 1) * (spell.beneficial() ? -1 : 1) * chance * value;
        }
        return total;
    }

    /** Chance, dass der Zauber auf dem Stack wirkt: 0 bei Immunität, sonst gegen die Resistenz. */
    static double successChance(Spell spell, Stack target, BattleSetup setup) {
        if (SpellResistance.isImmune(target, spell)) {
            return 0;
        }
        return spell.beneficial() ? 1 : (100 - SpellResistance.resistancePercent(target, setup)) / 100.0;
    }

    private static double instantValue(Spell spell, SkillLevel level, int power, Stack s) {
        int amount = healing(spell, level, power);
        return switch (spell) {
            case CURE -> {
                double negatives = 0;
                for (ActiveSpell active : s.activeSpells()) {
                    if (!active.spell().beneficial()) {
                        negatives += lastingValue(active.spell(), active.level(), active.roundsLeft(), s);
                    }
                }
                yield Math.min(amount, s.topMissingHealth()) * costPerHealth(s) + negatives;
            }
            case ANIMATE_DEAD -> s.unit().isUndead()
                    ? Math.min(amount, s.missingHealth()) * costPerHealth(s) : 0;
            case RESURRECTION -> s.unit().isResurrectable()
                    ? Math.min(amount, s.missingHealth()) * costPerHealth(s) : 0;
            default -> 0;
        };
    }

    private static double effectValue(Spell spell, SkillLevel level, int power, Stack s, BattleSetup setup) {
        if (spell != Spell.DISRUPTING_RAY && s.hasSpell(spell)) {
            return 0;
        }
        double gain = gainPercent(spell, level, s, setup);
        int rounds = spell == Spell.DISRUPTING_RAY ? EXPECTED_ROUNDS : power;
        return gain <= 0 ? 0 : gain / 100 * ROUND_SHARE * stackValue(s) * Math.min(rounds, EXPECTED_ROUNDS);
    }

    private static double lastingValue(Spell spell, SkillLevel level, int roundsLeft, Stack s) {
        return gainPercent(spell, level, s, null) / 100 * ROUND_SHARE * stackValue(s)
                * Math.min(roundsLeft, EXPECTED_ROUNDS);
    }

    /**
     * Geschätzte Wirkung je Runde in Prozent: wie viel mehr Schaden der Stack macht bzw. wie viel
     * weniger er nimmt. Grundlage sind die Werte der Spell Reference und die Faustregel 5 % je
     * Punkt Attack, 2,5 % je Punkt Defense (Manual S. 33). 0, wo der Zauber nichts bewirkt.
     */
    static double gainPercent(Spell spell, SkillLevel level, Stack s, @Nullable BattleSetup setup) {
        Unit unit = s.unit();
        boolean shooter = s.canShoot();
        double spread = unit.minDamage() < unit.maxDamage()
                ? 100.0 * (unit.maxDamage() - unit.minDamage()) / (unit.maxDamage() + unit.minDamage()) : 0;
        return switch (spell) {
            case BLESS, CURSE -> spread;
            case BLOODLUST -> shooter ? 0 : SpellDamage.byLevel(level, 15, 30, 30);
            case PRECISION -> shooter ? SpellDamage.byLevel(level, 15, 30, 30) : 0;
            case WEAKNESS, SHIELD -> SpellDamage.byLevel(level, 15, 30, 30);
            case HASTE -> SpellDamage.byLevel(level, 15, 25, 25);
            case STONE_SKIN -> SpellDamage.byLevel(level, 75, 150, 150) / 10.0;
            case AIR_SHIELD -> SpellDamage.byLevel(level, 25, 50, 50) * shooterShare(s, setup);
            case PRAYER -> SpellDamage.byLevel(level, 20, 40, 40);
            case FORTUNE -> s.getLuck() < ArmyLuck.MAX ? SpellDamage.byLevel(level, 4, 8, 8) : 0;
            case MISFORTUNE -> s.getLuck() > ArmyLuck.MIN ? SpellDamage.byLevel(level, 4, 8, 8) : 0;
            case MIRTH -> !unit.hasFixedZeroMorale() && s.getMorale() < ArmyMorale.MAX
                    ? SpellDamage.byLevel(level, 4, 8, 8) : 0;
            case SORROW -> !unit.hasFixedZeroMorale() && s.getMorale() > ArmyMorale.MIN
                    ? SpellDamage.byLevel(level, 4, 8, 8) : 0;
            case COUNTERSTRIKE -> s.hasSpeciality(UnitSpeciality.COUNTERSTRIKE_UNLIMITED) || shooter
                    ? 0 : SpellDamage.byLevel(level, 10, 20, 20);
            case FIRE_SHIELD -> shooter ? 0 : 10;
            case SLAYER -> 40 * slayerShare(s, level, setup);
            case SLOW -> shooter ? 5 : SpellDamage.byLevel(level, 30, 50, 50);
            case BLIND -> s.isAbleToAct() ? 70 : 0;
            case DISRUPTING_RAY -> SpellDamage.byLevel(level, 15, 20, 25);
            case FORGETFULNESS -> shooter ? SpellDamage.byLevel(level, 50, 80, 80) : 0;
            default -> 0;
        };
    }

    /** Anteil der Gegner (nach Wert), die schießen — wogegen Air Shield schützt. */
    private static double shooterShare(Stack s, @Nullable BattleSetup setup) {
        return enemyShare(s, setup, Stack::canShoot);
    }

    private static double slayerShare(Stack s, SkillLevel level, @Nullable BattleSetup setup) {
        return enemyShare(s, setup, e -> Stack.isSlayerTarget(e.unit(), level));
    }

    private static double enemyShare(Stack s, @Nullable BattleSetup setup, Predicate<Stack> which) {
        if (setup == null) {
            return 0;
        }
        double all = 0;
        double matching = 0;
        for (Stack e : setup.opponentsOf(s)) {
            double v = stackValue(e);
            all += v;
            if (which.test(e)) {
                matching += v;
            }
        }
        return all == 0 ? 0 : matching / all;
    }

    static int totalHealth(Stack s) {
        return s.isAlive() ? (s.getCount() - 1) * s.unit().health() + s.getCurrentHealth() : 0;
    }

    private static double costPerHealth(Stack s) {
        return (double) s.unit().cost() / s.unit().health();
    }

    static double stackValue(Stack s) {
        return totalHealth(s) * costPerHealth(s);
    }

    private static List<Stack> alive(List<Stack> stacks) {
        return stacks.stream().filter(Stack::isAlive).toList();
    }

    static Side other(Side side) {
        return side == Side.ATTACKER ? Side.DEFENDER : Side.ATTACKER;
    }
}
