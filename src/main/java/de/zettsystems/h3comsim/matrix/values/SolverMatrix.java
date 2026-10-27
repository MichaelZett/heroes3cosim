package de.zettsystems.h3comsim.matrix.values;

import de.zettsystems.h3comsim.battle.domain.SolverKind;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/** Die Faction-Matrix eines Solvers: Rangliste, alle Zellen und die Seitenlast im Spiegel. */
@Schema(description = "Faction-Matrix eines Solvers: Rangliste, alle Zellen (Zeile × Spalte, Spiegel eingeschlossen) und die Seitenlast im Spiegel.")
public record SolverMatrix(
        @Schema(description = "Solver beider Seiten")
        SolverKind solver,

        @Schema(description = "Rangliste nach Siegquote, absteigend")
        List<FactionStanding> standings,

        @Schema(description = "Alle Zellen, Zeile für Zeile in der Reihenfolge der Fraktionen")
        List<FactionPairing> pairings,

        @Schema(description = "Mittlerer Abstand der Angreifer-Siegquote im Spiegel von 0.5 (Seitenlast), 0..0.5",
                example = "0.08", minimum = "0", maximum = "0.5")
        double mirrorSideBias,

        @Schema(description = "Rechenzeit dieses Solvers in Millisekunden", example = "21500")
        long elapsedMs
) {
    public SolverMatrix {
        standings = List.copyOf(standings);
        pairings = List.copyOf(pairings);
    }
}
