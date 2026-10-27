package de.zettsystems.h3comsim.matrix.values;

import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

/** Snapshot eines asynchronen Faction-Matrix-Laufs, analog zu {@link MatrixJobSnapshot}. */
@Schema(description = "Snapshot eines Faction-Matrix-Jobs, als Antwort auf den Start und beim Polling.")
public record FactionMatrixJobSnapshot(
        @Schema(description = "Eindeutige Job-ID, generiert beim Start.",
                example = "1a2b3c4d-5e6f-7890-abcd-ef1234567890")
        String jobId,

        @Schema(description = "Aktueller Status des Jobs.")
        MatrixJobStatus status,

        @Schema(description = "Bereits abgeschlossene Einzel-Simulationen.", example = "640")
        int completed,

        @Schema(description = "Gesamtzahl der Einzel-Simulationen (Fraktionen² × Seeds × Solver).", example = "1620")
        int total,

        @Schema(description = "Ergebnis — nur gesetzt im Status `COMPLETED`.", nullable = true)
        @Nullable FactionMatrixReport report,

        @Schema(description = "Fehlermeldung — nur gesetzt im Status `FAILED`.", nullable = true)
        @Nullable String error
) {
}
