package de.zettsystems.h3comsim.battle.domain;

import java.util.Objects;
import java.util.Set;

/**
 * Spezialfähigkeit eines Helden (Manual, <em>Individual Heroes</em>: „Unique Ability"). Jeder
 * Held hat genau eine. Bis auf {@link Other} wirken alle Arten im Kampf.
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
 *   <li>{@link Ballista} — „increases the Attack and Defense skill of any Ballista under his
 *       command"; SoD: +5 % je angefangene fünf Level (Ballista ist Stufe 5).</li>
 *   <li>{@link CommonSpell} — „common spell specialty" (heroes.thelazy.net, <em>Hero
 *       specialty</em>): der Effekt des Zaubers × (1 + 0,03 × Heldenlevel / Stufe des Ziels),
 *       für Schadenszauber und Cure, Animate Dead, Resurrection; bei Ciele fest +50 % auf Magic
 *       Arrow.</li>
 *   <li>{@link UniqueSpell} — „unique spell specialty": feste Zusatzpunkte je Stufe des Ziels
 *       (Haste, Prayer, Weakness, Precision, Stone Skin, Bloodlust, Slayer), +2 auf Disrupting
 *       Ray, Fortune immer +3 Glück, Bless zusätzlich 3 % × Level / Stufe Schaden.</li>
 *   <li>{@link Other} — übrige Zauber-Spezialitäten (Hypnotize, Fire Wall) und Ressourcen, ohne
 *       Wirkung.</li>
 * </ul>
 *
 * <p>Wer sich auf einen Zauber spezialisiert, beginnt mit ihm im Zauberbuch („All heroes who
 * specialize in a spell start with it"), siehe {@link #startingSpells()}.
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

    /** Attack-Aufschlag für die Ballista dieses Helden. */
    default int ballistaAttackBonus(int heroLevel) {
        return 0;
    }

    /** Aufschlag in Prozent auf Schaden oder Heilung dieses Zaubers für diese Kreatur. */
    default int spellEffectPercent(Spell spell, int heroLevel, Unit target) {
        return 0;
    }

    /**
     * Zusatzpunkte auf einen liegenden Zauber für diese Kreatur (bei Bless: Prozent Schaden, bei
     * Fortune: das Glück, das der Zauber mindestens gibt).
     */
    default int spellEffectBonus(Spell spell, int heroLevel, Unit target) {
        return 0;
    }

    /** Zauber, mit denen ein Held dieser Spezialität beginnt. */
    default Set<Spell> startingSpells() {
        return Set.of();
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

    }

    /** +5 % des Grundwerts je angefangene Spanne von {@code tier} Leveln, aufgerundet. */
    static int percentOf(int base, int heroLevel, int tier) {
        int steps = Math.ceilDiv(heroLevel, tier);
        return Math.ceilDiv(base * steps * 5, 100);
    }

    /** Fertigkeits-Spezialist: +5 % je Level auf den Effekt der Fertigkeit. */
    record Skill(SecondarySkill skill) implements HeroSpecialty {
        public Skill {
            Objects.requireNonNull(skill, "skill");
        }

        @Override
        public boolean effectiveInCombat() {
            return skill == SecondarySkill.OFFENSE || skill == SecondarySkill.ARCHERY
                    || skill == SecondarySkill.ARMORER || skill == SecondarySkill.FIRST_AID;
        }

        @Override
        public int skillFactorPerMille(SecondarySkill other, int heroLevel) {
            return other == skill ? 1000 + 50 * heroLevel : 1000;
        }
    }

    /**
     * Ballista-Spezialist (SoD, heroes.thelazy.net, z. B. Torosar): Attack und Defense der
     * Ballista +5 % je angefangene fünf Level. Weil die Ballista hier nicht angegriffen wird,
     * wirkt nur der Attack-Teil.
     */
    record Ballista() implements HeroSpecialty {
        private static final int BALLISTA_LEVEL = 5;

        @Override
        public boolean effectiveInCombat() {
            return true;
        }

        @Override
        public int ballistaAttackBonus(int heroLevel) {
            return percentOf(WarMachine.BALLISTA.attack(), heroLevel, BALLISTA_LEVEL);
        }
    }

    /**
     * Gewöhnliche Zauber-Spezialität (SoD): Effekt × (1 + 0,03 × Level / Stufe des Ziels), also
     * {@code 3 × Level / Stufe} Prozent, abgerundet.
     *
     * @param fixedPercent fester Aufschlag statt der Level-Formel (Ciele), sonst 0
     */
    record CommonSpell(Spell spell, int fixedPercent) implements HeroSpecialty {
        public CommonSpell {
            Objects.requireNonNull(spell, "spell");
        }

        @Override
        public boolean effectiveInCombat() {
            return true;
        }

        @Override
        public int spellEffectPercent(Spell cast, int heroLevel, Unit target) {
            if (cast != spell) {
                return 0;
            }
            return fixedPercent > 0 ? fixedPercent : 3 * heroLevel / target.tier();
        }

        @Override
        public Set<Spell> startingSpells() {
            return Set.of(spell);
        }
    }

    /** Besondere Zauber-Spezialität (SoD-Werte von heroes.thelazy.net, unabhängig vom Level außer Bless). */
    record UniqueSpell(Spell spell) implements HeroSpecialty {
        public UniqueSpell {
            Objects.requireNonNull(spell, "spell");
        }

        @Override
        public boolean effectiveInCombat() {
            return true;
        }

        @Override
        public int spellEffectBonus(Spell cast, int heroLevel, Unit target) {
            if (cast != spell) {
                return 0;
            }
            int tier = target.tier();
            return switch (spell) {
                // +3 für Stufe 1–2, +2 für 3–4, +1 für 5–6.
                case HASTE, PRAYER, WEAKNESS, PRECISION, STONE_SKIN, BLOODLUST -> tier <= 6 ? 3 - (tier - 1) / 2 : 0;
                // +4 für Stufe 1 bis +1 für Stufe 4.
                case SLAYER -> Math.max(0, 5 - tier);
                case DISRUPTING_RAY -> 2;
                case FORTUNE -> 3;
                case BLESS -> 3 * heroLevel / tier;
                default -> 0;
            };
        }

        @Override
        public Set<Spell> startingSpells() {
            return Set.of(spell);
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
