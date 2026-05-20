package de.zettsystems.h3comsim.battle.domain;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Bewegungsart einer Einheit. `GROUND`-Einheiten respektieren Obstacles, `FLYING`-Einheiten überspringen sie.",
        enumAsRef = true)
public enum Movement {
    GROUND,
    FLYING
}
