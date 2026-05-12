package de.zettsystems.h3comsim.application.experiment;

import de.zettsystems.h3comsim.domain.UnitCatalog;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Slf4j
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
        jobRunner.execute(() -> runJob(jobId, request, state));
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

    private void runJob(String jobId, MatrixRequest request, JobState state) {
        LOG.info("Matrix run started:\n{}", configSummary(jobId, request, state.total));
        try {
            MatrixReport report = experiment.run(request,
                    (completed, total) -> state.recordProgress(completed));
            state.complete(report);
            LOG.info("Matrix run completed:\n{}", reportSummary(jobId, request, report));
        } catch (RuntimeException ex) {
            String message = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
            state.fail(message);
            LOG.warn("Matrix run failed: jobId={} reason={}", jobId, message, ex);
        }
    }

    private static String configSummary(String jobId, MatrixRequest request, int totalSims) {
        return String.join("\n",
                "  jobId           = " + jobId,
                "  mode            = " + request.mode(),
                "  unitCount       = " + request.unitCount(),
                "  seedsPerMatchup = " + request.seedsPerMatchup(),
                "  excludeFactions = " + request.excludeFactions(),
                "  excludeTiers    = " + request.excludeTiers(),
                "  excludeUnits    = " + request.excludeUnits().size() + " entries",
                "  totalSims       = " + totalSims);
    }

    private static String reportSummary(String jobId, MatrixRequest request, MatrixReport report) {
        StringBuilder sb = new StringBuilder(1024);
        sb.append("  jobId           = ").append(jobId).append('\n');
        sb.append("  mode            = ").append(request.mode()).append('\n');
        sb.append("  elapsed         = ").append(String.format(Locale.ROOT, "%.1fs", report.elapsedMs() / 1000.0)).append('\n');
        sb.append("  participants    = ").append(report.stats().size()).append(" units, ")
                .append(report.factionStats().size()).append(" factions\n");
        sb.append("  matchups        = ").append(report.totalMatchups())
                .append(", seeds/matchup = ").append(report.seedsPerMatchup()).append('\n');

        sb.append("  faction stats (sorted by win-rate desc):\n");
        for (FactionMatchupStats fs : report.factionStats()) {
            sb.append(String.format(Locale.ROOT,
                    "    %-12s units=%-3d sims=%-6d W/L/D=%d/%d/%d  win-rate=%5.1f%%  avg-survivor=%5.1f%%%n",
                    fs.faction(), fs.unitCount(), fs.totalSims(),
                    fs.wins(), fs.losses(), fs.draws(),
                    fs.winRate() * 100.0, fs.avgSurvivorRatio() * 100.0));
        }

        int topN = Math.min(10, report.stats().size());
        sb.append("  top ").append(topN).append(" units by win-rate:\n");
        for (int i = 0; i < topN; i++) {
            UnitMatchupStats u = report.stats().get(i);
            sb.append(String.format(Locale.ROOT,
                    "    %-22s %-11s T%d  win-rate=%5.1f%%  avg-survivor=%5.1f%%%n",
                    u.unitName(), u.faction(), u.tier(),
                    u.winRate() * 100.0, u.avgSurvivorRatio() * 100.0));
        }

        int bottomN = Math.min(5, report.stats().size());
        sb.append("  bottom ").append(bottomN).append(" units by win-rate:\n");
        for (int i = report.stats().size() - bottomN; i < report.stats().size(); i++) {
            UnitMatchupStats u = report.stats().get(i);
            sb.append(String.format(Locale.ROOT,
                    "    %-22s %-11s T%d  win-rate=%5.1f%%  avg-survivor=%5.1f%%%n",
                    u.unitName(), u.faction(), u.tier(),
                    u.winRate() * 100.0, u.avgSurvivorRatio() * 100.0));
        }

        sb.append("  anomalies (").append(report.anomalies().size()).append("):\n");
        for (TierAnomaly a : report.anomalies()) {
            sb.append(String.format(Locale.ROOT,
                    "    %-22s T%d loses to T%d  win-rate=%5.1f%%  sample=%d%n",
                    a.unitName(), a.tier(), a.againstTier(),
                    a.winRate() * 100.0, a.sampleSize()));
        }
        return sb.toString();
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
