package de.zettsystems.h3comsim.matrix.application;

import de.zettsystems.h3comsim.battle.domain.UnitCatalog;
import de.zettsystems.h3comsim.matrix.values.FactionMatchupStats;
import de.zettsystems.h3comsim.matrix.values.MatrixJobSnapshot;
import de.zettsystems.h3comsim.matrix.values.MatrixJobStatus;
import de.zettsystems.h3comsim.matrix.values.MatrixReport;
import de.zettsystems.h3comsim.matrix.values.MatrixRequest;
import de.zettsystems.h3comsim.matrix.values.TierAnomaly;
import de.zettsystems.h3comsim.matrix.values.UnitMatchupStats;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Optional;

@Service
@Slf4j
public class DefaultMatrixJobService implements MatrixJobService {

    private final MatrixExperimentService experiment;
    private final JobRegistry<MatrixReport> jobs;

    public DefaultMatrixJobService(MatrixExperimentService experiment, MatrixJobRunner runner) {
        this.experiment = experiment;
        this.jobs = new JobRegistry<>(runner);
    }

    @Override
    public MatrixJobSnapshot start(MatrixRequest request) {
        int total = computeTotalSims(request);
        String jobId = jobs.start(total, progress -> run(request, total, progress));
        return new MatrixJobSnapshot(jobId, MatrixJobStatus.RUNNING, 0, total, null, null);
    }

    @Override
    public Optional<MatrixJobSnapshot> get(String jobId) {
        return jobs.get(jobId).map(view -> new MatrixJobSnapshot(jobId, view.status(), view.completed(),
                view.total(), view.report(), view.error()));
    }

    private MatrixReport run(MatrixRequest request, int total, ProgressListener progress) {
        LOG.info("Matrix run started:\n{}", configSummary(request, total));
        MatrixReport report = experiment.run(request, progress);
        LOG.info("Matrix run completed:\n{}", reportSummary(request, report));
        return report;
    }

    private static String configSummary(MatrixRequest request, int totalSims) {
        return String.join("\n",
                "  mode            = " + request.mode(),
                "  unitCount       = " + request.unitCount(),
                "  seedsPerMatchup = " + request.seedsPerMatchup(),
                "  excludeFactions = " + request.excludeFactions(),
                "  excludeTiers    = " + request.excludeTiers(),
                "  excludeUnits    = " + request.excludeUnits().size() + " entries",
                "  totalSims       = " + totalSims);
    }

    private static String reportSummary(MatrixRequest request, MatrixReport report) {
        StringBuilder sb = new StringBuilder(1024);
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
}
