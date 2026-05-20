package de.zettsystems.h3comsim.battle.domain.events;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Sieger einer Einzelschlacht. DRAW bei beidseitiger Auslöschung oder beim Erreichen des harten Runden-Limits.",
        enumAsRef = true)
public enum Winner {
    ATTACKER,
    DEFENDER,
    DRAW
}
