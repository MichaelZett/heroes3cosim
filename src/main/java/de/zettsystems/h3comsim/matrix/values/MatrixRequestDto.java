package de.zettsystems.h3comsim.matrix.values;

import de.zettsystems.h3comsim.battle.domain.Faction;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.jspecify.annotations.Nullable;

import java.util.Set;

/**
 * Request-DTO für {@code POST /api/experiments/matrix}. Alle Felder sind optional —
 * werden Defaults eingesetzt, wenn nicht gesetzt.
 */
@Schema(description = "Konfiguration eines Matrix-Laufs. Alle Felder sind optional; nicht gesetzte Felder fallen auf Defaults.")
public record MatrixRequestDto(
        @Schema(description = "Skalierungsfaktor pro Stack. Bei EQUAL_COUNT direkt die Stack-Größe; bei den anderen Modi ein Multiplikator.",
                example = "20", minimum = "1", maximum = "200", defaultValue = "20", nullable = true)
        @Nullable @Min(1) @Max(200) Integer unitCount,

        @Schema(description = "Unit-Namen, die NICHT am Lauf teilnehmen sollen (Case-sensitive, vgl. /api/units).",
                example = "[\"Imp\", \"Familiar\"]", nullable = true)
        @Nullable Set<String> excludeUnits,

        @Schema(description = "Faktionen, deren Einheiten komplett ausgeschlossen werden.",
                example = "[\"NEUTRAL\"]", nullable = true)
        @Nullable Set<Faction> excludeFactions,

        @Schema(description = "Tiers (1..7), die vollständig ausgeschlossen werden.",
                example = "[1, 2]", nullable = true)
        @Nullable Set<Integer> excludeTiers,

        @Schema(description = "Wie wird die Stack-Größe pro Seite bestimmt? Default `EQUAL_COUNT`.",
                defaultValue = "EQUAL_COUNT", nullable = true)
        @Nullable StackSizingMode mode,

        @Schema(description = "Anzahl Seeds pro Match-up (mit getauschten Rollen — Faktor 2 in den Sims).",
                example = "20", minimum = "1", maximum = "100", defaultValue = "20", nullable = true)
        @Nullable @Min(1) @Max(100) Integer seedsPerMatchup
) {
    public MatrixRequest toApplication() {
        return new MatrixRequest(
                unitCount != null ? unitCount : 20,
                excludeUnits != null ? excludeUnits : Set.of(),
                excludeFactions != null ? excludeFactions : Set.of(),
                excludeTiers != null ? excludeTiers : Set.of(),
                mode != null ? mode : StackSizingMode.EQUAL_COUNT,
                seedsPerMatchup != null ? seedsPerMatchup : 20);
    }
}
