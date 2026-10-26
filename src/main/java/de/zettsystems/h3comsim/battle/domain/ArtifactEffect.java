package de.zettsystems.h3comsim.battle.domain;

import java.util.Objects;
import java.util.Set;

/**
 * Eine Kampfwirkung eines Artefakts, über die Primärwerte hinaus. Die Werte stammen aus VCMI
 * ({@code config/artifacts.json}), geprüft gegen heroes.thelazy.net (SoD).
 *
 * <p>Die meisten Wirkungen gelten für die Armee des Helden. Ausnahmen wirken auf beide Seiten:
 * {@link BlockMagic}, {@link NegateLuck}, {@link NegateMorale} und {@link NegateImmunities}.
 */
public sealed interface ArtifactEffect {

    /** Glück der Armee, wird mit den übrigen Quellen auf 0..3 gekappt. */
    record Luck(int value) implements ArtifactEffect {
    }

    /** Moral der Armee, wird mit den übrigen Quellen auf −3..+3 gekappt. */
    record Morale(int value) implements ArtifactEffect {
    }

    /** Zusätzliche Runden für jeden Zauber des Helden mit Dauer. */
    record SpellDuration(int rounds) implements ArtifactEffect {
    }

    /** Magieresistenz in Prozent, addiert sich zur Resistance-Fertigkeit. */
    record MagicResistance(int percent) implements ArtifactEffect {
    }

    /** Prozent auf den Archery-Bonus — nur, wenn der Held Archery hat. */
    record Archery(int percent) implements ArtifactEffect {
    }

    /** Speed jeder Kreatur der Armee. */
    record Speed(int value) implements ArtifactEffect {
    }

    /** Lebenspunkte jeder Kreatur der Armee. */
    record Health(int value) implements ArtifactEffect {
    }

    /**
     * Elixir of Life: lebende Kreaturen bekommen 25 % ihrer Grund-Lebenspunkte dazu (abgerundet)
     * und regenerieren zu Beginn ihres Zugs.
     */
    record LivingVitality(int percent) implements ArtifactEffect {
    }

    /** Orb: Prozent auf den Schaden der Zauber einer Schule. */
    record SpellDamage(MagicSchool school, int percent) implements ArtifactEffect {
        public SpellDamage {
            Objects.requireNonNull(school, "school");
        }
    }

    /** Tome: alle Kampfzauber einer Schule, ohne Rücksicht auf Wisdom. */
    record SchoolSpells(MagicSchool school) implements ArtifactEffect {
        public SchoolSpells {
            Objects.requireNonNull(school, "school");
        }
    }

    /** Spellbinder's Hat: alle Zauber einer Stufe, ohne Rücksicht auf Wisdom. */
    record LevelSpells(int level) implements ArtifactEffect {
    }

    /** Ein Zauber, den der Held mit dem Artefakt kennt, wenigstens auf dieser Stufe. */
    record GrantsSpell(Spell spell, SkillLevel level) implements ArtifactEffect {
        public GrantsSpell {
            Objects.requireNonNull(spell, "spell");
            Objects.requireNonNull(level, "level");
        }
    }

    /** Die Armee ist gegen diese Zauber immun, auch gegen die eigenen. */
    record SpellImmunity(Set<Spell> spells) implements ArtifactEffect {
        public SpellImmunity {
            spells = Set.copyOf(spells);
        }
    }

    /** Die Armee ist gegen alle Zauber bis zu dieser Stufe immun. */
    record LevelImmunity(int maxLevel) implements ArtifactEffect {
    }

    /** Die Armee ist gegen Gedankenzauber immun (Badge of Courage, in SoD). */
    record MindImmunity() implements ArtifactEffect {
    }

    /**
     * Kein Held darf Zauber über dieser Stufe wirken (Recanter's Cloak: 2). Bei 0 (Orb of
     * Inhibition) zaubern auch die Kreaturen nicht mehr, außer den Faerie Dragons.
     */
    record BlockMagic(int maxLevel) implements ArtifactEffect {
    }

    /** Orb of Vulnerability: natürliche Immunitäten und Magieresistenz zählen nicht. */
    record NegateImmunities() implements ArtifactEffect {
    }

    /** Hourglass of the Evil Hour: kein positives Glück auf beiden Seiten. */
    record NegateLuck() implements ArtifactEffect {
    }

    /** Spirit of Oppression: keine positive Moral auf beiden Seiten. */
    record NegateMorale() implements ArtifactEffect {
    }

    /** Schützen der Armee schießen ohne Entfernungs- und Hindernis-Strafe. */
    record NoShootingPenalty() implements ArtifactEffect {
    }

    /** Schützen der Armee schießen auch, wenn ein Feind neben ihnen steht. */
    record FreeShooting() implements ArtifactEffect {
    }

    /** Attack und Defense für jeden Drachen der Armee. */
    record DragonBonus(int value) implements ArtifactEffect {
    }

    /**
     * Wird zu Beginn der Schlacht auf Expert gewirkt, auf alle eigenen oder alle feindlichen
     * Stacks, für die genannten Runden.
     */
    record OpeningSpell(Spell spell, int rounds) implements ArtifactEffect {
        public OpeningSpell {
            Objects.requireNonNull(spell, "spell");
        }
    }

    /** Angelic Alliance: gute und neutrale Fraktionen zählen für die Moral als eine. */
    record AlignmentMix() implements ArtifactEffect {
    }
}
