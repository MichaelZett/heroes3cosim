package de.zettsystems.h3comsim.battle.domain;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.random.RandomGenerator;

/**
 * Zaubernde Kreaturen. Ogre Magi wirken Bloodlust, Master Genies einen zufälligen nützlichen
 * Zauber — beide laut heroes.thelazy.net „at advanced level with a spell power of 6", dreimal
 * pro Kampf, und das Zaubern verbraucht ihre Aktion. Die Dauer ist damit 6 Runden („1
 * Round/Power", Manual S. 64 ff.).
 *
 * <p>Wann gezaubert wird, ist eine Solver-Entscheidung: hier nur, wenn der Stack in diesem Zug
 * keinen Gegner angreifen kann — eine Aktion für einen Zauber herzugeben, während ein Angriff
 * möglich wäre, lohnt sich selten.
 */
final class CreatureSpellcasting {

    static final int CASTS_PER_BATTLE = 3;
    static final int SPELL_POWER = 6;
    static final SkillLevel CAST_LEVEL = SkillLevel.ADVANCED;

    /** Was ein Master Genie wirken kann und im Simulator wirkt (heroes.thelazy.net). */
    static final List<Spell> GENIE_SPELLS = List.of(
            Spell.BLESS, Spell.BLOODLUST, Spell.CURE, Spell.HASTE, Spell.SHIELD, Spell.STONE_SKIN,
            Spell.FORTUNE, Spell.PRECISION, Spell.AIR_SHIELD, Spell.MIRTH, Spell.COUNTERSTRIKE,
            Spell.FIRE_SHIELD, Spell.FRENZY, Spell.PRAYER, Spell.SLAYER);

    private CreatureSpellcasting() {
    }

    static int castsFor(Unit unit) {
        return unit.hasSpeciality(UnitSpeciality.CASTS_BLOODLUST)
                || unit.hasSpeciality(UnitSpeciality.CASTS_RANDOM_BENEFICIAL) ? CASTS_PER_BATTLE : 0;
    }

    /** Ein möglicher Zauber auf ein Ziel. */
    record Cast(Spell spell, Stack target) {
    }

    /** Solver-Entscheidung: zaubern statt handeln? {@code null} heißt nein. */
    static @Nullable Action decide(Stack active, Stack opponent, @Nullable BattleSetup setup) {
        if (setup == null || active.spellCastsLeft() == 0 || canAttackNow(active, opponent)) {
            return null;
        }
        if (active.hasSpeciality(UnitSpeciality.CASTS_BLOODLUST)) {
            Stack target = best(usefulTargets(active, Spell.BLOODLUST, setup));
            return target == null ? null : new Action.CastSpell(Spell.BLOODLUST, target);
        }
        if (active.hasSpeciality(UnitSpeciality.CASTS_RANDOM_BENEFICIAL) && !usefulCasts(active, setup).isEmpty()) {
            return new Action.CastRandomSpell();
        }
        return null;
    }

    private static boolean canAttackNow(Stack active, Stack opponent) {
        if (active.canShoot()) {
            return true;
        }
        return active.position().distanceTo(opponent.position()) - 1 <= active.getSpeed();
    }

    /**
     * Zufallsauswahl des Master Genies: unter den Zaubern, die mindestens ein Ziel sinnvoll
     * treffen, gleichverteilt einer; Ziel ist dann der wertvollste passende Stack. Die Genies
     * „will not cast spells that have no effect" und keine, gegen die das Ziel immun ist.
     */
    static @Nullable Cast randomCast(Stack caster, BattleSetup setup, RandomGenerator rng) {
        List<Spell> candidates = new ArrayList<>();
        for (Spell spell : GENIE_SPELLS) {
            if (!usefulTargets(caster, spell, setup).isEmpty()) {
                candidates.add(spell);
            }
        }
        if (candidates.isEmpty()) {
            return null;
        }
        Spell spell = candidates.get(rng.nextInt(candidates.size()));
        Stack target = best(usefulTargets(caster, spell, setup));
        return target == null ? null : new Cast(spell, target);
    }

    static List<Cast> usefulCasts(Stack caster, BattleSetup setup) {
        List<Cast> casts = new ArrayList<>();
        for (Spell spell : GENIE_SPELLS) {
            for (Stack target : usefulTargets(caster, spell, setup)) {
                casts.add(new Cast(spell, target));
            }
        }
        return casts;
    }

    // ally != caster: Frenzy nicht auf den Zaubernden selbst (Identitätsvergleich).
    @SuppressWarnings("ReferenceEquality")
    static List<Stack> usefulTargets(Stack caster, Spell spell, BattleSetup setup) {
        List<Stack> targets = new ArrayList<>();
        for (Stack ally : setup.stacksOf(caster.side())) {
            if (ally.isAlive() && !SpellResistance.isImmune(ally.unit(), spell)
                    && !(spell == Spell.FRENZY && ally == caster)
                    && isUseful(spell, ally, setup)) {
                targets.add(ally);
            }
        }
        return targets;
    }

    private static boolean isUseful(Spell spell, Stack target, BattleSetup setup) {
        if (spell != Spell.CURE && target.hasSpell(spell)) {
            return false;
        }
        boolean shooter = target.unit().attackType() == AttackType.LONG_RANGE;
        return switch (spell) {
            case BLESS -> target.unit().minDamage() < target.unit().maxDamage();
            case BLOODLUST -> !shooter;
            case CURE -> target.topMissingHealth() > 0 || target.hasSpell(Spell.BLIND) || target.hasSpell(Spell.WEAKNESS);
            case PRECISION -> shooter;
            case FORTUNE -> target.getLuck() < ArmyLuck.MAX;
            case MIRTH -> !target.unit().hasFixedZeroMorale() && target.getMorale() < ArmyMorale.MAX;
            case AIR_SHIELD -> setup.opponentsOf(target).stream()
                    .anyMatch(o -> o.isAlive() && o.unit().attackType() == AttackType.LONG_RANGE);
            case COUNTERSTRIKE -> !target.hasSpeciality(UnitSpeciality.COUNTERSTRIKE_UNLIMITED);
            case FRENZY -> !shooter && target.getDefense() > 0;
            case SLAYER -> setup.opponentsOf(target).stream()
                    .anyMatch(o -> o.isAlive() && Stack.isSlayerTarget(o.unit(), CAST_LEVEL));
            case HASTE, SHIELD, STONE_SKIN, FIRE_SHIELD, PRAYER -> true;
            case DISPEL, BLIND, WEAKNESS -> false;
        };
    }

    /** Der wertvollste Stack (Anzahl × Kosten); bei Gleichstand der erste. */
    private static @Nullable Stack best(List<Stack> targets) {
        return targets.stream()
                .max(Comparator.comparingLong(s -> (long) s.getCount() * s.unit().cost()))
                .orElse(null);
    }
}
