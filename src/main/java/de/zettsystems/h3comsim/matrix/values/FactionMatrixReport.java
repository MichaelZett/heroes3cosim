package de.zettsystems.h3comsim.matrix.values;

import de.zettsystems.h3comsim.battle.domain.Faction;
import de.zettsystems.h3comsim.battle.domain.MagicTerrain;
import de.zettsystems.h3comsim.battle.domain.Terrain;
import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

import java.util.List;

@Schema(description = "Ergebnis einer Faction-Matrix: die Einstellungen des Laufs und je Solver eine Matrix.")
public record FactionMatrixReport(
        @Schema(description = "Teilnehmende Fraktionen in Matrix-Reihenfolge")
        List<Faction> factions,

        @Schema(description = "Seeds je geordneter Paarung", example = "10")
        int seedsPerPair,

        @Schema(description = "Wochen Produktion je Armee", example = "1")
        int weeks,

        @Schema(description = "Mit Standardhelden oder ohne Helden")
        boolean heroes,

        @Schema(description = "Grundgelände oder null", nullable = true)
        @Nullable Terrain terrain,

        @Schema(description = "Magisches Gelände oder null", nullable = true)
        @Nullable MagicTerrain magicTerrain,

        @Schema(description = "Je gewähltem Solver eine Matrix, Greedy vor Strategic")
        List<SolverMatrix> matrices,

        @Schema(description = "Wall-clock-Dauer des Laufs in Millisekunden", example = "43000")
        long elapsedMs
) {
    public FactionMatrixReport {
        factions = List.copyOf(factions);
        matrices = List.copyOf(matrices);
    }
}
