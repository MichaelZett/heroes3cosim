package de.zettsystems.h3comsim.battle.domain.events;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Zustands-Snapshot eines Stacks nach einem Event (Position, Count, Top-Unit-HP). Wird typischerweise nach Schadens-Events mitgeliefert, damit das Frontend ohne State-Replay den aktuellen Stand kennt.")
public record StackSnapshot(
        @Schema(description = "Seite, zu der dieser Stack gehört")
        Side side,

        @Schema(description = "Slot-Index 0..6 innerhalb der Seite. 0 für Single-Battle und immer für den Stack auf der obersten Spawn-Reihe.", example = "0")
        int slot,

        @Schema(description = "Unit-Name (vgl. /api/units)", example = "Marksman")
        String unitName,

        @Schema(description = "Aktuelle Anzahl Einheiten im Stack", example = "12")
        int count,

        @Schema(description = "HP der obersten (verwundeten) Einheit im Stack", example = "8")
        int topHp,

        @Schema(description = "Axial-Hex-Koordinate q (Spalte)", example = "0")
        int q,

        @Schema(description = "Axial-Hex-Koordinate r (Reihe)", example = "5")
        int r
) {
}
