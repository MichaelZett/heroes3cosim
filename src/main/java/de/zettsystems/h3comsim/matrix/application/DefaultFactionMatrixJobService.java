package de.zettsystems.h3comsim.matrix.application;

import de.zettsystems.h3comsim.matrix.values.FactionMatrixJobSnapshot;
import de.zettsystems.h3comsim.matrix.values.FactionMatrixReport;
import de.zettsystems.h3comsim.matrix.values.FactionMatrixRequest;
import de.zettsystems.h3comsim.matrix.values.FactionStanding;
import de.zettsystems.h3comsim.matrix.values.MatrixJobStatus;
import de.zettsystems.h3comsim.matrix.values.SolverMatrix;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Optional;

@Service
@Slf4j
public class DefaultFactionMatrixJobService implements FactionMatrixJobService {

    private final FactionMatrixExperiment experiment;
    private final JobRegistry<FactionMatrixReport> jobs;

    DefaultFactionMatrixJobService(FactionMatrixExperiment experiment, MatrixJobRunner runner) {
        this.experiment = experiment;
        this.jobs = new JobRegistry<>(runner);
    }

    @Override
    public FactionMatrixJobSnapshot start(FactionMatrixRequest request) {
        int total = request.totalSims();
        String jobId = jobs.start(total, progress -> run(request, progress));
        return new FactionMatrixJobSnapshot(jobId, MatrixJobStatus.RUNNING, 0, total, null, null);
    }

    @Override
    public Optional<FactionMatrixJobSnapshot> get(String jobId) {
        return jobs.get(jobId).map(view -> new FactionMatrixJobSnapshot(jobId, view.status(), view.completed(),
                view.total(), view.report(), view.error()));
    }

    private FactionMatrixReport run(FactionMatrixRequest request, ProgressListener progress) {
        LOG.info("Faction matrix started: {} ({} sims)", request, request.totalSims());
        FactionMatrixReport report = experiment.run(request, progress);
        LOG.info("Faction matrix completed in {} ms:\n{}", report.elapsedMs(), summary(report));
        return report;
    }

    private static String summary(FactionMatrixReport report) {
        StringBuilder sb = new StringBuilder(512);
        for (SolverMatrix matrix : report.matrices()) {
            sb.append(String.format(Locale.ROOT, "  %s (side bias %.3f):%n", matrix.solver(), matrix.mirrorSideBias()));
            for (FactionStanding standing : matrix.standings()) {
                sb.append(String.format(Locale.ROOT, "    %-11s %5.1f%%%n", standing.faction(),
                        standing.winRate() * 100.0));
            }
        }
        return sb.toString();
    }
}
