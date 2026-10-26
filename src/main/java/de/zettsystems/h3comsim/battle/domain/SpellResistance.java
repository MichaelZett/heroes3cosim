package de.zettsystems.h3comsim.battle.domain;

import de.zettsystems.h3comsim.battle.domain.events.Side;

import java.util.EnumSet;
import java.util.Set;
import java.util.random.RandomGenerator;

/**
 * Ob ein Zauber auf einem Stack wirkt: erst Immunitäten, dann bei feindlichen Zaubern die
 * Magieresistenz.
 *
 * <p>Immunitäten (gegen nützliche wie feindliche Zauber):
 * <ul>
 *   <li>Black Dragon und Magic Elemental gegen alle Zauber, Gold Dragon gegen Stufe 1–4, Red
 *       Dragon gegen Stufe 1–3 (Manual S. 91 und 107, AB-Manual).</li>
 *   <li>Efreet, Efreet Sultan, Fire und Energy Elemental, Firebird und Phoenix gegen die Schule
 *       des Feuers (Manual S. 98, AB-Manual).</li>
 *   <li>Gegen Gedankenzauber: Giants und Titans (Manual S. 115), Elementare (AB-Manual), dazu
 *       nach SoD-Regel Untote und nicht lebende Kreaturen (Golems, Gargoyles).</li>
 *   <li>Troglodytes gegen Blind (Manual S. 89).</li>
 *   <li>Kriegsmaschinen gegen alles außer den Schadenszaubern ohne Implosion, Death Ripple und
 *       Destroy Undead und den Wirkungen, die heroes.thelazy.net (<em>War Machine</em>) als
 *       wirksam listet.</li>
 * </ul>
 *
 * <p>Magieresistenz gilt nur gegen feindliche Zauber: Dwarves 20 %, Battle Dwarves 40 %
 * (Manual S. 105), dazu die Aura eines benachbarten Unicorns (+20 %, nicht für das Unicorn
 * selbst, mehrere Auren zählen einmal). Zwei Quellen verrechnen sich als zwei unabhängige
 * Chancen. Ohne Resistenz wird nicht gewürfelt.
 */
final class SpellResistance {

    private static final int AURA_PERCENT = 20;

    /** Was Kriegsmaschinen trifft (heroes.thelazy.net, <em>War Machine</em>). */
    private static final Set<Spell> WAR_MACHINE_SPELLS = EnumSet.of(
            Spell.MAGIC_ARROW, Spell.LIGHTNING_BOLT, Spell.ICE_BOLT, Spell.FIREBALL, Spell.INFERNO,
            Spell.METEOR_SHOWER, Spell.FROST_RING, Spell.CHAIN_LIGHTNING, Spell.ARMAGEDDON,
            Spell.CURE, Spell.ANTI_MAGIC, Spell.DISPEL, Spell.FORTUNE, Spell.MISFORTUNE, Spell.BLESS, Spell.CURSE,
            Spell.SLAYER, Spell.PRAYER, Spell.PRECISION, Spell.SHIELD, Spell.STONE_SKIN, Spell.AIR_SHIELD,
            Spell.MAGIC_MIRROR, Spell.PROTECTION_FROM_AIR, Spell.PROTECTION_FROM_EARTH, Spell.PROTECTION_FROM_FIRE,
            Spell.PROTECTION_FROM_WATER, Spell.FIRE_SHIELD);

    private SpellResistance() {
    }

    /**
     * Wie {@link #isImmune(Unit, Spell)}, dazu Anti-Magic auf dem Stack (Manual S. 70): Basic lässt
     * nur noch Zauber der Stufen 4 und 5 durch, Advanced nur Stufe 5, Expert gar keine — und die
     * Immunitäten aus den Artefakten des Helden ({@link Equipment#grantsImmunity}).
     */
    static boolean isImmune(Stack target, Spell spell) {
        return isImmune(target, spell, false);
    }

    /**
     * Wie {@link #isImmune(Stack, Spell)} für einen Zauber dieser Seite: Orb of Vulnerability
     * hebt die natürlichen Immunitäten auf ({@link BattleSetup#negatesImmunities}), nicht aber die
     * aus Artefakten, die der Untoten, Elementare und nicht lebenden Kreaturen gegen
     * Gedankenzauber und die der Kriegsmaschinen.
     */
    static boolean isImmune(Stack target, Spell spell, Side caster, BattleSetup setup) {
        return isImmune(target, spell, setup.negatesImmunities(caster, target.side()));
    }

    private static boolean isImmune(Stack target, Spell spell, boolean negated) {
        ActiveSpell antiMagic = target.activeSpell(Spell.ANTI_MAGIC);
        if (antiMagic != null && spell.level() <= antiMagic.byLevel(3, 4, 5)) {
            return true;
        }
        Hero hero = target.commander();
        if (hero != null && hero.artifacts().grantsImmunity(spell)) {
            return true;
        }
        Unit unit = target.unit();
        if (negated) {
            return WarMachine.isMachine(unit) ? !WAR_MACHINE_SPELLS.contains(spell)
                    : (spell.mind() && (unit.isUndead() || !unit.isLiving()));
        }
        return isImmune(unit, spell);
    }

    static boolean isImmune(Unit unit, Spell spell) {
        if (WarMachine.isMachine(unit)) {
            return !WAR_MACHINE_SPELLS.contains(spell);
        }
        if (unit.hasSpeciality(UnitSpeciality.IMMUNE_TO_SPELLS)) {
            return true;
        }
        if (unit.hasSpeciality(UnitSpeciality.IMMUNE_TO_SPELLS_BELOW_5) && spell.level() < 5) {
            return true;
        }
        if (unit.hasSpeciality(UnitSpeciality.IMMUNE_TO_SPELLS_BELOW_4) && spell.level() < 4) {
            return true;
        }
        if (spell == Spell.BLIND && unit.hasSpeciality(UnitSpeciality.IMMUNE_TO_BLIND)) {
            return true;
        }
        if (spell.school() == MagicSchool.FIRE && unit.hasSpeciality(UnitSpeciality.IMMUNE_TO_FIRE_SPELLS)) {
            return true;
        }
        return spell.mind() && isImmuneToMind(unit);
    }

    static boolean isImmuneToMind(Unit unit) {
        return unit.isUndead()
                || unit.hasSpeciality(UnitSpeciality.NON_LIVING)
                || unit.hasSpeciality(UnitSpeciality.ELEMENTAL_RACE)
                || unit.hasSpeciality(UnitSpeciality.IMMUNE_TO_MIND_SPELLS);
    }

    /**
     * Chance in Prozent, einen feindlichen Zauber abzuwehren: eigene Resistenz, Unicorn-Aura und
     * die Resistance-Fertigkeit des eigenen Helden samt Artefakten, als unabhängige Chancen
     * verrechnet. Mit Orb of Vulnerability keine (heroes.thelazy.net: auch Resistance und die
     * Artefakte zählen dann nicht).
     */
    static int resistancePercent(Stack target, Side caster, BattleSetup setup) {
        if (setup.negatesImmunities(caster, target.side())) {
            return 0;
        }
        Hero hero = target.commander();
        int skill = hero == null ? 0 : hero.resistancePercent();
        return combine(creatureResistancePercent(target, setup), skill);
    }

    private static int combine(int a, int b) {
        return 100 - (100 - a) * (100 - b) / 100;
    }

    private static int creatureResistancePercent(Stack target, BattleSetup setup) {
        int own = 0;
        if (target.hasSpeciality(UnitSpeciality.MAGIC_RESISTANCE_40)) {
            own = 40;
        } else if (target.hasSpeciality(UnitSpeciality.MAGIC_RESISTANCE_20)) {
            own = 20;
        }
        int aura = hasAuraNeighbour(target, setup) ? AURA_PERCENT : 0;
        return combine(own, aura);
    }

    // ally != target: die Aura wirkt nicht auf das Unicorn selbst (Identitätsvergleich).
    @SuppressWarnings("ReferenceEquality")
    private static boolean hasAuraNeighbour(Stack target, BattleSetup setup) {
        for (Stack ally : setup.stacksOf(target.side())) {
            if (ally != target && ally.isAlive()
                    && ally.hasSpeciality(UnitSpeciality.MAGIC_RESISTANCE_AURA)
                    && ally.distanceTo(target) == 1) {
                return true;
            }
        }
        return false;
    }

    /**
     * Wirkt der Zauber? Immunität schließt ihn aus; ein feindlicher Zauber kann zusätzlich an der
     * Resistenz scheitern.
     */
    static boolean takesEffect(Spell spell, Side caster, Stack target, BattleSetup setup, RandomGenerator rng) {
        if (isImmune(target, spell, caster, setup)) {
            return false;
        }
        if (spell.beneficial()) {
            return true;
        }
        int resistance = resistancePercent(target, caster, setup);
        return resistance == 0 || rng.nextInt(100) >= resistance;
    }
}
