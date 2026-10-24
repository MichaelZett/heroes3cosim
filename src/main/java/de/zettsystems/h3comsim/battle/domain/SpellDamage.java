package de.zettsystems.h3comsim.battle.domain;

/**
 * Schaden der Heldenzauber (Manual, Spell Reference S. 64–79) und seine Modifikatoren.
 *
 * <p>Reihenfolge: Grundschaden nach Formel aus Spell Power und Stufe, dann Sorcery und
 * Spezialität des Zaubernden, dann beim Ziel Protection from … (−30/50 %), Golems (Stone halber,
 * Iron ein Viertel des Schadens, Manual S. 113) und die Elementar-Eigenheiten aus dem AB-Manual:
 * Air Elementals nehmen doppelten Schaden von Lightning Bolt und Chain Lightning und keinen von
 * Meteor Shower; Earth Elementals keinen von Lightning Bolt und Chain Lightning, doppelten von
 * Meteor Shower; Fire Elementals doppelten von Ice Bolt und Frost Ring; Water Elementals keinen
 * von Ice Bolt und Frost Ring, doppelten von Fireball und Inferno. Immunität ({@link
 * SpellResistance}) macht den Schaden zu 0.
 */
final class SpellDamage {

    private SpellDamage() {
    }

    /** Grundschaden laut Manual für Spell Power {@code power} und die Stufe des Zaubers. */
    static int base(Spell spell, SkillLevel level, int power) {
        return switch (spell) {
            case MAGIC_ARROW -> power * 10 + byLevel(level, 10, 20, 30);
            case LIGHTNING_BOLT -> power * 25 + byLevel(level, 10, 20, 50);
            case ICE_BOLT -> power * 20 + byLevel(level, 10, 20, 50);
            case DEATH_RIPPLE -> power * 5 + byLevel(level, 10, 20, 30);
            case DESTROY_UNDEAD -> power * 10 + byLevel(level, 10, 20, 50);
            case FIREBALL, FROST_RING -> power * 10 + byLevel(level, 15, 30, 60);
            case CHAIN_LIGHTNING -> power * 40 + byLevel(level, 25, 50, 100);
            case METEOR_SHOWER -> power * 25 + byLevel(level, 25, 50, 100);
            case INFERNO -> power * 10 + byLevel(level, 20, 40, 80);
            case ARMAGEDDON -> power * 50 + byLevel(level, 30, 60, 120);
            case IMPLOSION -> power * 75 + byLevel(level, 100, 200, 300);
            default -> 0;
        };
    }

    static int byLevel(SkillLevel level, int basic, int advanced, int expert) {
        return switch (level) {
            case NONE, BASIC -> basic;
            case ADVANCED -> advanced;
            case EXPERT -> expert;
        };
    }

    /** Schaden auf ein Ziel, ausgehend vom (bei Chain Lightning schon halbierten) Grundschaden. */
    static int toTarget(Spell spell, int baseDamage, Hero caster, Stack target) {
        if (SpellResistance.isImmune(target, spell)) {
            return 0;
        }
        long damage = baseDamage;
        damage = damage * (100 + caster.sorceryPercent()) / 100;
        damage = damage * (100 + caster.specialty().spellDamagePercent(spell, caster.level(), target.unit())) / 100;
        damage = damage * target.spellDamagePercent(spell.school()) / 100;
        if (target.hasSpeciality(UnitSpeciality.SPELL_DAMAGE_HALVED)) {
            damage = damage / 2;
        } else if (target.hasSpeciality(UnitSpeciality.SPELL_DAMAGE_QUARTERED)) {
            damage = damage / 4;
        }
        return (int) (damage * elementalPercent(spell, target.unit()) / 100);
    }

    private static int elementalPercent(Spell spell, Unit unit) {
        if (HeroSpecialty.sameCreature(UnitCatalog.AIR_ELEMENTAL, unit)) {
            return switch (spell) {
                case LIGHTNING_BOLT, CHAIN_LIGHTNING -> 200;
                case METEOR_SHOWER -> 0;
                default -> 100;
            };
        }
        if (HeroSpecialty.sameCreature(UnitCatalog.EARTH_ELEMENTAL, unit)) {
            return switch (spell) {
                case LIGHTNING_BOLT, CHAIN_LIGHTNING -> 0;
                case METEOR_SHOWER -> 200;
                default -> 100;
            };
        }
        if (HeroSpecialty.sameCreature(UnitCatalog.FIRE_ELEMENTAL, unit)) {
            return spell == Spell.ICE_BOLT || spell == Spell.FROST_RING ? 200 : 100;
        }
        if (HeroSpecialty.sameCreature(UnitCatalog.WATER_ELEMENTAL, unit)) {
            return switch (spell) {
                case ICE_BOLT, FROST_RING -> 0;
                case FIREBALL, INFERNO -> 200;
                default -> 100;
            };
        }
        return 100;
    }
}
