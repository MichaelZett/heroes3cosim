package de.zettsystems.h3comsim.matrix.values;

import org.jspecify.annotations.Nullable;

/**
 * Immutable Snapshot eines asynchronen Matrix-Laufs. {@code completed}/{@code total} zählen
 * Einzelsimulationen (Pair-Seed-Rolle-Triplets), nicht Match-ups. {@code report} ist nur im
 * {@link MatrixJobStatus#COMPLETED}-Zustand gesetzt, {@code error} nur bei {@link MatrixJobStatus#FAILED}.
 */
public record MatrixJobSnapshot(
        String jobId,
        MatrixJobStatus status,
        int completed,
        int total,
        @Nullable MatrixReport report,
        @Nullable String error
) {
}
