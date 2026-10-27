package de.zettsystems.h3comsim.matrix.application;

import de.zettsystems.h3comsim.matrix.values.MatrixJobStatus;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

/** Die asynchronen Läufe einer Matrix-Art mit Fortschritt, Ergebnis oder Fehler. */
@Slf4j
final class JobRegistry<R> {

    /** Stand eines Laufs; {@code report} nur bei COMPLETED, {@code error} nur bei FAILED. */
    record View<R>(MatrixJobStatus status, int completed, int total, @Nullable R report, @Nullable String error) {
    }

    private final MatrixJobRunner runner;
    private final ConcurrentMap<String, JobState<R>> jobs = new ConcurrentHashMap<>();

    JobRegistry(MatrixJobRunner runner) {
        this.runner = runner;
    }

    /** Reiht den Lauf ein und liefert die neue Job-ID. */
    String start(int total, Function<ProgressListener, R> work) {
        String jobId = UUID.randomUUID().toString();
        JobState<R> state = new JobState<>(total);
        jobs.put(jobId, state);
        runner.execute(() -> run(jobId, work, state));
        return jobId;
    }

    Optional<View<R>> get(String jobId) {
        JobState<R> state = jobs.get(jobId);
        return state == null ? Optional.empty() : Optional.of(state.view());
    }

    private static <R> void run(String jobId, Function<ProgressListener, R> work, JobState<R> state) {
        try {
            // Benannter statt unbenannter Parameter: Mit "_" macht OpenRewrite daraus
            // state::recordProgress, das bei zwei Parametern nicht kompiliert.
            state.complete(work.apply((completed, total) -> state.recordProgress(completed)));
        } catch (RuntimeException ex) {
            String message = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
            state.fail(message);
            LOG.warn("Matrix run failed: jobId={} reason={}", jobId, message, ex);
        }
    }

    private static final class JobState<R> {
        private final int total;
        private final AtomicInteger completed = new AtomicInteger();
        private volatile MatrixJobStatus status = MatrixJobStatus.RUNNING;
        private final AtomicReference<@Nullable R> report = new AtomicReference<>();
        private final AtomicReference<@Nullable String> error = new AtomicReference<>();

        JobState(int total) {
            this.total = total;
        }

        void recordProgress(int newValue) {
            // Updates kommen aus mehreren Threads — wir wollen Monotonie für die UI.
            completed.accumulateAndGet(newValue, Math::max);
        }

        void complete(R result) {
            this.completed.set(total);
            this.report.set(result);
            this.status = MatrixJobStatus.COMPLETED;
        }

        void fail(String message) {
            this.error.set(message);
            this.status = MatrixJobStatus.FAILED;
        }

        View<R> view() {
            return new View<>(status, completed.get(), total, report.get(), error.get());
        }
    }
}
