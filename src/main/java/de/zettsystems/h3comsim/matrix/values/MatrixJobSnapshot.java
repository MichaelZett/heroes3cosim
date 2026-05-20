package de.zettsystems.h3comsim.matrix.values;

import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

/**
 * Immutable Snapshot eines asynchronen Matrix-Laufs. {@code completed}/{@code total} zählen
 * Einzelsimulationen (Pair-Seed-Rolle-Triplets), nicht Match-ups. {@code report} ist nur im
 * {@link MatrixJobStatus#COMPLETED}-Zustand gesetzt, {@code error} nur bei {@link MatrixJobStatus#FAILED}.
 */
@Schema(description = "Snapshot eines Matrix-Jobs. Wird sowohl als Antwort auf den Job-Start (RUNNING, ohne report) als auch beim Polling (RUNNING/COMPLETED/FAILED) zurückgegeben.")
public record MatrixJobSnapshot(
        @Schema(description = "Eindeutige Job-ID, generiert beim Start.",
                example = "1a2b3c4d-5e6f-7890-abcd-ef1234567890")
        String jobId,

        @Schema(description = "Aktueller Status des Jobs.")
        MatrixJobStatus status,

        @Schema(description = "Bereits abgeschlossene Einzel-Simulationen (nicht Match-ups).",
                example = "1500")
        int completed,

        @Schema(description = "Gesamtzahl der Einzel-Simulationen für diesen Job (Pair × Seeds × 2 Rollen).",
                example = "3200")
        int total,

        @Schema(description = "Aggregierter Report — nur gesetzt im Status `COMPLETED`.",
                nullable = true)
        @Nullable MatrixReport report,

        @Schema(description = "Fehlermeldung — nur gesetzt im Status `FAILED`.",
                nullable = true)
        @Nullable String error
) {
}
