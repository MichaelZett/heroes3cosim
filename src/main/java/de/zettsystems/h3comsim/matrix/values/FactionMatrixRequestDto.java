package de.zettsystems.h3comsim.matrix.values;

import de.zettsystems.h3comsim.battle.domain.Faction;
import de.zettsystems.h3comsim.battle.domain.MagicTerrain;
import de.zettsystems.h3comsim.battle.domain.SolverKind;
import de.zettsystems.h3comsim.battle.domain.Terrain;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Request-DTO für {@code POST /api/experiments/faction-matrix}. Alle Felder sind optional —
 * werden Defaults eingesetzt, wenn nicht gesetzt.
 */
@Schema(description = "Konfiguration einer Faction-Matrix. Alle Felder sind optional; nicht gesetzte Felder fallen auf Defaults.")
public record FactionMatrixRequestDto(
        @Schema(description = "Teilnehmende Fraktionen (mindestens zwei, ohne NEUTRAL). Default: alle neun Städte.",
                example = "[\"CASTLE\", \"TOWER\", \"INFERNO\"]", nullable = true)
        @Nullable @Size(min = 2) Set<Faction> factions,

        @Schema(description = "Solver, mit denen die Matrix gerechnet wird; jeder führt beide Seiten. Default: beide.",
                example = "[\"STRATEGIC\"]", nullable = true)
        @Nullable @Size(min = 1) Set<SolverKind> solvers,

        @Schema(description = "Seeds je geordneter Paarung; mit beiden Rollen kämpft jedes Fraktionspaar doppelt so oft.",
                example = "10", minimum = "1", maximum = "100", defaultValue = "10", nullable = true)
        @Nullable @Min(1) @Max(100) Integer seedsPerPair,

        @Schema(description = "Wochen Produktion je Armee (Vielfaches der Wochenproduktion aus den Presets).",
                example = "1", minimum = "1", maximum = "20", defaultValue = "1", nullable = true)
        @Nullable @Min(1) @Max(20) Integer weeks,

        @Schema(description = "Mit Helden (Standardheld der Fraktion, wie im Army-Battle) oder ohne. Default: mit.",
                example = "true", defaultValue = "true", nullable = true)
        @Nullable Boolean heroes,

        @Schema(description = "Grundgelände wie im Army-Battle; bei null ein Feld ohne Gelände.",
                example = "GRASS", nullable = true)
        @Nullable Terrain terrain,

        @Schema(description = "Magisches Gelände wie im Army-Battle, sonst null. Nicht auf Wasser.",
                example = "MAGIC_PLAINS", nullable = true)
        @Nullable MagicTerrain magicTerrain
) {
    public FactionMatrixRequest toApplication() {
        return new FactionMatrixRequest(
                factions != null ? factions : towns(),
                solvers != null ? solvers : EnumSet.allOf(SolverKind.class),
                seedsPerPair != null ? seedsPerPair : 10,
                weeks != null ? weeks : 1,
                heroes == null || heroes,
                terrain,
                magicTerrain);
    }

    private static Set<Faction> towns() {
        return Arrays.stream(Faction.values())
                .filter(f -> f != Faction.NEUTRAL)
                .collect(Collectors.toUnmodifiableSet());
    }
}
