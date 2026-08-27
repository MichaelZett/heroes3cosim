package de.zettsystems.h3comsim.battle.domain;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Ausbaustufe einer Sekundärfertigkeit (Manual S. 35): „Each may by held at a basic, advanced,
 * or expert level of ability."
 *
 * <p>{@link #NONE} steht für „Fertigkeit nicht erlernt" und ist die Antwort für jede der 28
 * Fertigkeiten, die ein Held nicht hat — ein Held kann höchstens acht davon lernen.
 */
@Schema(description = "Ausbaustufe einer Sekundärfertigkeit.", enumAsRef = true)
public enum SkillLevel {
    NONE,
    BASIC,
    ADVANCED,
    EXPERT
}
