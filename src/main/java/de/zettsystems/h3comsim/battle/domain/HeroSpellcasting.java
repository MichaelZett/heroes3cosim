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
 * Runden. Schutz vor einer Schule und Anti-Magic zählen den Schaden, den der gegnerische Held
 * dem Stack mit seinen Zaubern und seinem Mana höchstens zufügen kann, gewichtet mit dem Anteil
 * des Stacks am eigenen Armeewert; Dispel zählt die aufgehobenen Wirkungen.
 *
 * <p>Mana: Jeder Wert wird mit der Zahl der Würfe multipliziert, die das Mana in den restlichen
 * Runden (bis {@link #EXPECTED_BATTLE_ROUNDS}) für diesen Zauber hergibt. Bei knappem Mana gewinnt
 * so zweimal Bless gegen einmal Lightning Bolt, bei reichlich Mana zählt nur der Wert. Magi und
 * Arch Magi senken die Kosten der eigenen Zauber um 2, Pegasi und Silver Pegasi heben die des
 * Gegners um 2 (Manual, Tower und Rampart), mindestens 1.
 */
final class HeroSpellcasting {

    /** Anteil seines Werts, den ein Stack je Runde an Schaden umsetzt — grob ein Fünftel. */
    static final double ROUND_SHARE = 0.2;
    /** Runden, die eine Wirkung höchstens zählt; ein Kampf ist selten länger entschieden. */
    static final int EXPECTED_ROUNDS = 4;
    /** Runden, für die ein Held sein Mana einteilt. */
    static final int EXPECTED_BATTLE_ROUNDS = 5;
    private static final int COST_MODIFIER = 2;

    private HeroSpellcasting() {
    }

    /**
     * Ein Zauber und wen er trifft. {@code target} ist das gewählte Ziel bei Einzel- und
     * Kettenzaubern, bei Flächen- und Massenzaubern {@code null}; {@code affected} ist die
     * Reihenfolge der Treffer (bei Chain Lightning die Sprungfolge).
     */
    record Cast(Spell spell, SkillLevel level, @Nullable Stack target, List<Stack> affected) {
    }

    /** Wie {@link #decide(Hero, Side, int, BattleSetup, int)} in der ersten Runde. */
    static @Nullable Cast decide(@Nullable Hero hero, Side side, int mana, BattleSetup setup) {
        return decide(hero, side, mana, setup, 0);
    }

    /**
     * Der beste bezahlbare Zauber für diese Seite, oder {@code null}, wenn keiner etwas bringt.
     * Ohne Helden, ohne Zauberbuch oder ohne Mana immer {@code null}.
     *
     * @param round Runde ab 0; bestimmt, für wie viele Runden das Mana noch reichen soll
     */
    static @Nullable Cast decide(@Nullable Hero hero, Side side, int mana, BattleSetup setup, int round) {
        if (hero == null || hero.spells().isEmpty()) {
            return null;
        }
        int roundsLeft = Math.max(1, EXPECTED_BATTLE_ROUNDS - round);
        Cast best = null;
        double bestValue = 0;
        for (Spell spell : hero.spells()) {
            int cost = cost(spell, side, setup);
            if (cost > mana) {
                continue;
            }
            int casts = Math.min(roundsLeft, mana / cost);
            for (Cast cast : candidates(hero, side, spell, setup)) {
                double value = value(hero, side, cast, setup) * casts;
                if (value > bestValue) {
                    best = cast;
                    bestValue = value;
                }
            }
        }
        return best;
    }

    /** Mana-Kosten für diese Seite: Magi im eigenen Heer −2, Pegasi beim Gegner +2, mindestens 1. */
    static int cost(Spell spell, Side side, BattleSetup setup) {
        int cost = spell.cost();
        if (anyAlive(setup.stacksOf(side), UnitSpeciality.SPELL_COST_REDUCTION)) {
            cost -= COST_MODIFIER;
        }
        if (anyAlive(setup.stacksOf(other(side)), UnitSpeciality.SPELL_COST_INCREASE)) {
            cost += COST_MODIFIER;
        }
        return Math.max(1, cost);
    }

    private static boolean anyAlive(List<Stack> stacks, UnitSpeciality speciality) {
        return stacks.stream().anyMatch(s -> s.isAlive() && s.hasSpeciality(speciality));
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
            case DISPEL -> {
                // Basic nur auf eigene Stacks, Advanced auf jeden, Expert auf alle (Manual S. 76).
                if (level == SkillLevel.EXPERT) {
                    casts.add(new Cast(spell, level, null, setup.aliveStacks()));
                } else {
                    List<Stack> targets = level == SkillLevel.BASIC ? own : setup.aliveStacks();
                    targets.forEach(t -> casts.add(new Cast(spell, level, t, List.of(t))));
                }
            }
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

    /** Heilung des Helden auf dieses Ziel, mit dem Aufschlag seiner Spezialität. */
    static int healing(Hero hero, Spell spell, SkillLevel level, Stack target) {
        return healing(spell, level, hero.spellPower())
                * (100 + hero.specialty().spellEffectPercent(spell, hero.level(), target.unit())) / 100;
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
            double chance = successChance(spell, s, side, setup);
            if (chance == 0) {
                continue;
            }
            if (spell == Spell.DISPEL) {
                total += chance * dispelValue(s, side, setup);
                continue;
            }
            double value = switch (spell.kind()) {
                case DAMAGE -> damageValue(hero, spell, baseDamage(cast, hero.spellPower(), i), s);
                case INSTANT -> instantValue(hero, spell, cast.level(), s);
                case EFFECT -> effectValue(hero, spell, cast.level(), s, setup);
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

    /** Wie {@link #successChance(Spell, Stack, BattleSetup)}; Dispel auf eigene Stacks würfelt nicht. */
    static double successChance(Spell spell, Stack target, Side casterSide, BattleSetup setup) {
        if (spell == Spell.DISPEL && target.side() == casterSide) {
            return SpellResistance.isImmune(target, spell) ? 0 : 1;
        }
        return successChance(spell, target, setup);
    }

    private static double damageValue(Hero caster, Spell spell, int baseDamage, Stack s) {
        return Math.min(SpellDamage.toTarget(spell, baseDamage, caster, s), totalHealth(s)) * costPerHealth(s);
    }

    /**
     * Dispel hebt alle Zauber auf: auf einem eigenen Stack zählen die negativen als Gewinn und die
     * nützlichen als Verlust, auf einem gegnerischen umgekehrt.
     */
    private static double dispelValue(Stack s, Side casterSide, BattleSetup setup) {
        double net = 0;
        for (ActiveSpell active : s.activeSpells()) {
            double v = lastingValue(active, s, setup);
            net += active.spell().beneficial() ? v : -v;
        }
        return s.side() == casterSide ? -net : net;
    }

    private static double instantValue(Hero hero, Spell spell, SkillLevel level, Stack s) {
        int amount = healing(hero, spell, level, s);
        return switch (spell) {
            case CURE -> {
                double negatives = 0;
                for (ActiveSpell active : s.activeSpells()) {
                    if (!active.spell().beneficial()) {
                        negatives += lastingValue(active, s, null);
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

    private static double effectValue(Hero hero, Spell spell, SkillLevel level, Stack s, BattleSetup setup) {
        if (spell != Spell.DISRUPTING_RAY && s.hasSpell(spell)) {
            return 0;
        }
        int power = hero.spellPower();
        int rounds = Math.min(spell == Spell.DISRUPTING_RAY ? EXPECTED_ROUNDS : power, EXPECTED_ROUNDS);
        if (spell == Spell.ANTI_MAGIC || protectedSchool(spell) != null) {
            return protectionValue(spell, level, s, setup) * rounds;
        }
        int bonus = hero.specialty().spellEffectBonus(spell, hero.level(), s.unit());
        double gain = gainPercent(spell, level, bonus, s, setup);
        return gain <= 0 ? 0 : gain / 100 * ROUND_SHARE * stackValue(s) * rounds;
    }

    /** Wert eines liegenden Zaubers für seine restlichen Runden. */
    private static double lastingValue(ActiveSpell active, Stack s, @Nullable BattleSetup setup) {
        Spell spell = active.spell();
        int bonus = spell == Spell.DISRUPTING_RAY ? active.amount() - active.byLevel(3, 4, 5) : active.amount();
        return Math.max(0, gainPercent(spell, active.level(), bonus, s, setup)) / 100 * ROUND_SHARE
                * stackValue(s) * Math.min(active.roundsLeft(), EXPECTED_ROUNDS);
    }

    private static @Nullable MagicSchool protectedSchool(Spell spell) {
        return switch (spell) {
            case PROTECTION_FROM_AIR -> MagicSchool.AIR;
            case PROTECTION_FROM_EARTH -> MagicSchool.EARTH;
            case PROTECTION_FROM_FIRE -> MagicSchool.FIRE;
            case PROTECTION_FROM_WATER -> MagicSchool.WATER;
            default -> null;
        };
    }

    /**
     * Was Schutz vor einer Schule oder Anti-Magic je Runde abwendet: der schlimmste Zauber, den der
     * gegnerische Held bezahlen kann, gewichtet mit dem Anteil des Stacks am eigenen Armeewert —
     * so wahrscheinlich trifft es gerade ihn.
     */
    private static double protectionValue(Spell spell, SkillLevel level, Stack s, BattleSetup setup) {
        MagicSchool school = protectedSchool(spell);
        double averted;
        if (school != null) {
            int percent = SpellDamage.byLevel(level, 30, 50, 50);
            averted = threat(s, e -> e.kind() == Spell.Kind.DAMAGE && e.school() == school, setup) * percent / 100;
        } else {
            int maxLevel = SpellDamage.byLevel(level, 3, 4, 5);
            averted = threat(s, e -> e.level() <= maxLevel, setup);
        }
        return averted * ownShare(s, setup);
    }

    /**
     * Der größte Schaden in Gold, den der gegnerische Held diesem Stack mit einem der erfassten
     * feindlichen Zauber zufügen kann, solange sein Mana reicht; 0 ohne gegnerischen Helden.
     */
    static double threat(Stack s, Predicate<Spell> which, BattleSetup setup) {
        Side enemy = other(s.side());
        Hero foe = setup.heroOf(enemy);
        if (foe == null) {
            return 0;
        }
        int mana = setup.manaOf(enemy);
        double worst = 0;
        for (Spell spell : foe.spells()) {
            if (spell.beneficial() || !which.test(spell) || cost(spell, enemy, setup) > mana) {
                continue;
            }
            SkillLevel level = foe.castLevel(spell);
            double harm = switch (spell.kind()) {
                case DAMAGE -> damageValue(foe, spell, SpellDamage.base(spell, level, foe.spellPower()), s);
                case EFFECT -> Math.max(0, gainPercent(spell, level,
                        foe.specialty().spellEffectBonus(spell, foe.level(), s.unit()), s, setup))
                        / 100 * ROUND_SHARE * stackValue(s);
                case INSTANT -> 0;
            };
            worst = Math.max(worst, harm * successChance(spell, s, setup));
        }
        return worst;
    }

    private static double ownShare(Stack s, BattleSetup setup) {
        double all = 0;
        for (Stack ally : setup.stacksOf(s.side())) {
            all += ally.isAlive() ? stackValue(ally) : 0;
        }
        return all == 0 ? 0 : stackValue(s) / all;
    }

    /**
     * Geschätzte Wirkung je Runde in Prozent: wie viel mehr Schaden der Stack macht bzw. wie viel
     * weniger er nimmt. Grundlage sind die Werte der Spell Reference und die Faustregel 5 % je
     * Punkt Attack, 2,5 % je Punkt Defense (Manual S. 33). 0, wo der Zauber nichts bewirkt.
     */
    static double gainPercent(Spell spell, SkillLevel level, Stack s, @Nullable BattleSetup setup) {
        return gainPercent(spell, level, 0, s, setup);
    }

    /**
     * Wie {@link #gainPercent(Spell, SkillLevel, Stack, BattleSetup)}, mit den Zusatzpunkten einer
     * Spezialität ({@link HeroSpecialty#spellEffectBonus}).
     */
    static double gainPercent(Spell spell, SkillLevel level, int bonus, Stack s, @Nullable BattleSetup setup) {
        Unit unit = s.unit();
        boolean shooter = s.canShoot();
        double spread = unit.minDamage() < unit.maxDamage()
                ? 100.0 * (unit.maxDamage() - unit.minDamage()) / (unit.maxDamage() + unit.minDamage()) : 0;
        int attackPoints = SpellDamage.byLevel(level, 3, 6, 6) + bonus;
        return switch (spell) {
            case BLESS -> spread + bonus;
            case CURSE -> spread;
            case BLOODLUST -> shooter ? 0 : 5.0 * attackPoints;
            case PRECISION -> shooter ? 5.0 * attackPoints : 0;
            case WEAKNESS -> 5.0 * attackPoints;
            case SHIELD -> SpellDamage.byLevel(level, 15, 30, 30);
            case HASTE -> 5.0 * (SpellDamage.byLevel(level, 3, 5, 5) + bonus);
            case STONE_SKIN -> 2.5 * attackPoints;
            case AIR_SHIELD -> SpellDamage.byLevel(level, 25, 50, 50) * shooterShare(s, setup);
            case PRAYER -> 10.0 * (SpellDamage.byLevel(level, 2, 4, 4) + bonus);
            case FORTUNE -> 4.0 * Math.min(ArmyLuck.MAX - s.getLuck(), Math.max(SpellDamage.byLevel(level, 1, 2, 2), bonus));
            case MISFORTUNE -> 4.0 * Math.min(s.getLuck() - ArmyLuck.MIN, SpellDamage.byLevel(level, 1, 2, 2));
            case MIRTH -> !unit.hasFixedZeroMorale() && s.getMorale() < ArmyMorale.MAX
                    ? SpellDamage.byLevel(level, 4, 8, 8) : 0;
            case SORROW -> !unit.hasFixedZeroMorale() && s.getMorale() > ArmyMorale.MIN
                    ? SpellDamage.byLevel(level, 4, 8, 8) : 0;
            case COUNTERSTRIKE -> s.hasSpeciality(UnitSpeciality.COUNTERSTRIKE_UNLIMITED) || shooter
                    ? 0 : SpellDamage.byLevel(level, 10, 20, 20);
            case FIRE_SHIELD -> shooter ? 0 : 10;
            case SLAYER -> 5.0 * (8 + bonus) * slayerShare(s, level, setup);
            case SLOW -> shooter ? 5 : SpellDamage.byLevel(level, 30, 50, 50);
            case BLIND -> s.isAbleToAct() ? 70 : 0;
            case DISRUPTING_RAY -> 5.0 * (SpellDamage.byLevel(level, 3, 4, 5) + bonus);
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
