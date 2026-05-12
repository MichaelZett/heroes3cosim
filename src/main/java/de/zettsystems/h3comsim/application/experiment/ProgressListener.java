package de.zettsystems.h3comsim.application.experiment;

/**
 * Wird vom {@link MatrixExperimentService} bei jedem Fortschritt aufgerufen. Implementierungen
 * müssen thread-safe sein — der Service ruft die Methode parallel aus mehreren Worker-Threads.
 */
@FunctionalInterface
public interface ProgressListener {

    ProgressListener NOOP = (completed, total) -> {
    };

    void onProgress(int completed, int total);
}
