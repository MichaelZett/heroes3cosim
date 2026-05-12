package de.zettsystems.h3comsim.adapter.web.dto;

import de.zettsystems.h3comsim.application.experiment.MatrixRequest;
import de.zettsystems.h3comsim.domain.Faction;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.jspecify.annotations.Nullable;

import java.util.Set;

/**
 * Request-DTO für {@code POST /api/experiments/matrix}. Alle Felder sind optional —
 * werden Defaults eingesetzt, wenn nicht gesetzt.
 */
public record MatrixRequestDto(
        @Nullable @Min(1) @Max(200) Integer unitCount,
        @Nullable Set<String> excludeUnits,
        @Nullable Set<Faction> excludeFactions,
        @Nullable Set<Integer> excludeTiers,
        @Nullable @Min(1) @Max(100) Integer seedsPerMatchup
) {
    public MatrixRequest toApplication() {
        return new MatrixRequest(
                unitCount != null ? unitCount : 20,
                excludeUnits != null ? excludeUnits : Set.of(),
                excludeFactions != null ? excludeFactions : Set.of(),
                excludeTiers != null ? excludeTiers : Set.of(),
                seedsPerMatchup != null ? seedsPerMatchup : 20);
    }
}
