package de.zettsystems.h3comsim.battle.domain.events;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Reine (q, r)-Hex-Koordinate für Event-Snapshots — entkoppelt vom domain.Hex-Record.
 */
@Schema(description = "Axial-Hex-Koordinate (q, r) auf dem Battlefield. Wird in Move-Pfaden, Obstacle-Listen und Stack-Positionen verwendet.")
public record HexCoord(
        @Schema(description = "Axial-Hex-Koordinate q (Spalte)", example = "3")
        int q,

        @Schema(description = "Axial-Hex-Koordinate r (Reihe)", example = "5")
        int r
) {
}
