package de.zettsystems.h3comsim.application.experiment;

import de.zettsystems.h3comsim.domain.UnitCatalog;
import jakarta.annotation.PreDestroy;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class DefaultMatrixJobService implements MatrixJobService {

    private final MatrixExperimentService experiment;
    private final ConcurrentMap<String, JobState> jobs = new ConcurrentHashMap<>();
    // Ein Job pro Zeit: der Experiment-Service parallelisiert intern, wir wollen nicht
    // mehrere parallele Matrix-Läufe um dieselben Worker konkurrieren lassen.
    private final ExecutorService jobRunner = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "matrix-job");
        t.setDaemon(true);
        return t;
    });

    public DefaultMatrixJobService(MatrixExperimentService experiment) {
        this.experiment = experiment;
    }

    @Override
    public MatrixJobSnapshot start(MatrixRequest request) {
        String jobId = UUID.randomUUID().toString();
        int total = computeTotalSims(request);
        JobState state = new JobState(total);
        jobs.put(jobId, state);
        jobRunner.execute(() -> runJob(request, state));
        return state.snapshot(jobId);
    }

    @Override
    public Optional<MatrixJobSnapshot> get(String jobId) {
        JobState state = jobs.get(jobId);
        return state == null ? Optional.empty() : Optional.of(state.snapshot(jobId));
    }

    @PreDestroy
    void shutdown() {
        jobRunner.shutdownNow();
    }

    private void runJob(MatrixRequest request, JobState state) {
        try {
            MatrixReport report = experiment.run(request,
                    (completed, total) -> state.recordProgress(completed));
            state.complete(report);
        } catch (RuntimeException ex) {
            state.fail(ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName());
        }
    }

    private static int computeTotalSims(MatrixRequest request) {
        long participants = UnitCatalog.all().stream()
                .filter(u -> !request.excludeUnits().contains(u.name()))
                .filter(u -> !request.excludeFactions().contains(u.faction()))
                .filter(u -> !request.excludeTiers().contains(u.tier()))
                .count();
        long matchups = participants * (participants - 1) / 2;
        return (int) (matchups * request.seedsPerMatchup() * 2);
    }

    private static final class JobState {
        private final int total;
        private final AtomicInteger completed = new AtomicInteger();
        private volatile MatrixJobStatus status = MatrixJobStatus.RUNNING;
        private volatile @Nullable MatrixReport report;
        private volatile @Nullable String error;

        JobState(int total) {
            this.total = total;
        }

        void recordProgress(int newValue) {
            // Updates kommen aus mehreren Threads — wir wollen Monotonie für die UI.
            completed.accumulateAndGet(newValue, Math::max);
        }

        void complete(MatrixReport report) {
            this.completed.set(total);
            this.report = report;
            this.status = MatrixJobStatus.COMPLETED;
        }

        void fail(String message) {
            this.error = message;
            this.status = MatrixJobStatus.FAILED;
        }

        MatrixJobSnapshot snapshot(String jobId) {
            return new MatrixJobSnapshot(jobId, status, completed.get(), total, report, error);
        }
    }
}
