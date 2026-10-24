package de.zettsystems.h3comsim.battle.domain;

import java.util.Objects;

/**
 * Ein Zauber, der auf einem Stack liegt.
 *
 * @param level      Ausbaustufe, mit der er gewirkt wurde — sie bestimmt die Stärke
 * @param roundsLeft verbleibende Runden; „1 Round/Power" im Manual, bei Kreaturen fest
 * @param amount     Disrupting Ray: Summe der Defense-Abzüge; sonst die Zusatzpunkte aus der
 *                   Spezialität des Zaubernden ({@link HeroSpecialty#spellEffectBonus}), meist 0
 */
public record ActiveSpell(Spell spell, SkillLevel level, int roundsLeft, int amount) {
    public ActiveSpell {
        Objects.requireNonNull(spell, "spell");
        Objects.requireNonNull(level, "level");
    }

    public ActiveSpell(Spell spell, SkillLevel level, int roundsLeft) {
        this(spell, level, roundsLeft, 0);
    }

    /** Wert je Ausbaustufe, wie ihn das Manual für Basic, Advanced und Expert nennt. */
    int byLevel(int basic, int advanced, int expert) {
        return switch (level) {
            case NONE, BASIC -> basic;
            case ADVANCED -> advanced;
            case EXPERT -> expert;
        };
    }

    ActiveSpell tick() {
        return new ActiveSpell(spell, level, roundsLeft - 1, amount);
    }
}
