package de.zettsystems.h3comsim.matrix.values;

import java.util.List;

public record MatrixReport(
        int totalMatchups,
        int seedsPerMatchup,
        int unitCount,
        long elapsedMs,
        List<UnitMatchupStats> stats,
        List<FactionMatchupStats> factionStats,
        List<TierAnomaly> anomalies
) {
    public MatrixReport {
        stats = List.copyOf(stats);
        factionStats = List.copyOf(factionStats);
        anomalies = List.copyOf(anomalies);
    }
}
