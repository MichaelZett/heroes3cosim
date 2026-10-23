package de.zettsystems.h3comsim.battle.domain;

import java.util.Objects;

/**
 * Spezialfähigkeit eines Helden (Manual, <em>Individual Heroes</em>: „Unique Ability"). Jeder
 * Held hat genau eine. Im Kampf wirken drei Arten, alle übrigen werden nur geführt.
 *
 * <ul>
 *   <li>{@link Creature} — Kreatur-Spezialist. Das Manual formuliert es als „increases the Attack
 *       and Defense skills of any X … for each level he attains after the Nth level", N ist die
 *       Stufe der Kreatur. Abgebildet ist die SoD-Fassung (heroes.thelazy.net, z. B. Fiona):
 *       +1 Speed und +5 % Attack und Defense „for every N levels (rounded up)".</li>
 *   <li>{@link Skill} — „receives a 5% per level bonus to his X skill": der Effekt der Fertigkeit
 *       wird mit (1 + 0,05 × Level) multipliziert. Kampfwirksam nur für Offense, Archery und
 *       Armorer; ohne die Fertigkeit selbst bringt die Spezialität nichts.</li>
 *   <li>{@link FixedCreature} — die Planeswalker aus Armageddon's Blade: feste Boni auf Attack,
 *       Defense und Schaden ihrer Elementare, unabhängig vom Level.</li>
 *   <li>{@link Other} — Zauber, Ressourcen, Ballista. Ohne Zaubersystem, Abenteuerkarte und
 *       Kriegsmaschinen ohne Wirkung.</li>
 * </ul>
 *
 * <p>Eine Spezialität gilt für Basis- und Ausbaustufe der Kreatur; erkannt wird sie an Fraktion
 * und Stufe, die beide teilen.
 */
public sealed interface HeroSpecialty {

    /** Wirkt die Spezialität im Kampf? Für die Anzeige. */
    boolean effectiveInCombat();

    default int attackBonus(Unit unit, int heroLevel) {
        return 0;
    }

    default int defenseBonus(Unit unit, int heroLevel) {
        return 0;
    }

    default int speedBonus(Unit unit) {
        return 0;
    }

    default int damageBonus(Unit unit) {
        return 0;
    }

    /** Faktor in Promille auf den Effekt der Fertigkeit; 1000 = unverändert. */
    default int skillFactorPerMille(SecondarySkill skill, int heroLevel) {
        return 1000;
    }

    static boolean sameCreature(Unit base, Unit unit) {
        return base.faction() == unit.faction() && base.tier() == unit.tier();
    }

    /**
     * Kreatur-Spezialist (SoD): +1 Speed, Attack und Defense je +5 % des Grundwerts für jede
     * angefangene Spanne von {@code Stufe der Kreatur} Leveln, aufgerundet. Ein Held auf Level 1
     * bringt also schon eine Stufe mit — bei Swordsmen (Attack 10) +1.
     *
     * @param creature Basis- oder Ausbaustufe; maßgeblich sind Fraktion und Stufe
     */
    record Creature(Unit creature) implements HeroSpecialty {
        public Creature {
            Objects.requireNonNull(creature, "creature");
        }

        @Override
        public boolean effectiveInCombat() {
            return true;
        }

        @Override
        public int attackBonus(Unit unit, int heroLevel) {
            return sameCreature(creature, unit) ? percentOf(unit.attack(), heroLevel, unit.tier()) : 0;
        }

        @Override
        public int defenseBonus(Unit unit, int heroLevel) {
            return sameCreature(creature, unit) ? percentOf(unit.defense(), heroLevel, unit.tier()) : 0;
        }

        @Override
        public int speedBonus(Unit unit) {
            return sameCreature(creature, unit) ? 1 : 0;
        }

        private static int percentOf(int base, int heroLevel, int tier) {
            int steps = Math.ceilDiv(heroLevel, tier);
            return Math.ceilDiv(base * steps * 5, 100);
        }
    }

    /** Fertigkeits-Spezialist: +5 % je Level auf den Effekt der Fertigkeit. */
    record Skill(SecondarySkill skill) implements HeroSpecialty {
        public Skill {
            Objects.requireNonNull(skill, "skill");
        }

        @Override
        public boolean effectiveInCombat() {
            return skill == SecondarySkill.OFFENSE || skill == SecondarySkill.ARCHERY
                    || skill == SecondarySkill.ARMORER;
        }

        @Override
        public int skillFactorPerMille(SecondarySkill other, int heroLevel) {
            return other == skill ? 1000 + 50 * heroLevel : 1000;
        }
    }

    /** Planeswalker: feste Boni für die Elementare einer Stufe (Basis- und Ausbaustufe). */
    record FixedCreature(Unit creature, int attack, int defense, int damage) implements HeroSpecialty {
        public FixedCreature {
            Objects.requireNonNull(creature, "creature");
        }

        @Override
        public boolean effectiveInCombat() {
            return true;
        }

        @Override
        public int attackBonus(Unit unit, int heroLevel) {
            return sameCreature(creature, unit) ? attack : 0;
        }

        @Override
        public int defenseBonus(Unit unit, int heroLevel) {
            return sameCreature(creature, unit) ? defense : 0;
        }

        @Override
        public int damageBonus(Unit unit) {
            return sameCreature(creature, unit) ? damage : 0;
        }
    }

    /** Spezialität ohne Kampfwirkung, mit kurzer Beschreibung aus dem Manual. */
    record Other(String description) implements HeroSpecialty {
        /** Spezialität ohne jede Wirkung, für Testhelden. */
        public static final Other NONE = new Other("none");

        public Other {
            Objects.requireNonNull(description, "description");
        }

        @Override
        public boolean effectiveInCombat() {
            return false;
        }
    }
}
