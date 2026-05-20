package de.zettsystems.h3comsim.matrix.values;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Aggregierter Bericht eines abgeschlossenen Matrix-Laufs. Enthält Lauf-Metadaten plus drei Aggregations-Achsen (Unit, Faction, Tier-Anomalien).")
public record MatrixReport(
        @Schema(description = "Anzahl unterschiedlicher Match-ups (Pair-Kombinationen) im Lauf.", example = "1485")
        int totalMatchups,

        @Schema(description = "Seeds pro Match-up; mit Rollen-Swap entspricht das `2 × seedsPerMatchup` Einzelsimulationen pro Pair.", example = "20")
        int seedsPerMatchup,

        @Schema(description = "Anzahl der teilnehmenden Einheiten (nach Excludes).", example = "55")
        int unitCount,

        @Schema(description = "Wall-clock-Dauer des Laufs in Millisekunden.", example = "42183")
        long elapsedMs,

        @Schema(description = "Per-Unit-Aggregate (Win/Loss/Draw, Win-Rate, mittlere Überlebensrate).")
        List<UnitMatchupStats> stats,

        @Schema(description = "Per-Faction-Aggregate (Summe über alle Units dieser Faktion).")
        List<FactionMatchupStats> factionStats,

        @Schema(description = "Tier-Anomalien — Einheiten, die mehrheitlich gegen niedrigere Tiers verlieren.")
        List<TierAnomaly> anomalies
) {
    public MatrixReport {
        stats = List.copyOf(stats);
        factionStats = List.copyOf(factionStats);
        anomalies = List.copyOf(anomalies);
    }
}
