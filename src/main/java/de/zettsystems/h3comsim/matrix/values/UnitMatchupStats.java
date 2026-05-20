package de.zettsystems.h3comsim.matrix.values;

import de.zettsystems.h3comsim.battle.domain.Faction;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Aggregierte Statistik einer Einheit über alle Matrix-Sims. Win-/Loss-/Draw-Counts und
 * mittlere Überlebensrate (eigene Survivor / eigene Start-Count) jeweils kumulativ.
 */
@Schema(description = "Per-Unit-Aggregat über alle Sims, in denen diese Einheit beteiligt war.")
public record UnitMatchupStats(
        @Schema(description = "Unit-Name (Case-sensitive, vgl. /api/units)", example = "Marksman")
        String unitName,

        @Schema(description = "Faction der Einheit")
        Faction faction,

        @Schema(description = "Tier 1..7 laut UnitCatalog", example = "2")
        int tier,

        @Schema(description = "True wenn es sich um die Upgrade-Variante handelt", example = "true")
        boolean upgrade,

        @Schema(description = "Anzahl Einzelsimulationen mit Beteiligung dieser Einheit.", example = "1200")
        int totalSims,

        @Schema(description = "Siege", example = "742")
        int wins,

        @Schema(description = "Niederlagen", example = "401")
        int losses,

        @Schema(description = "Unentschieden", example = "57")
        int draws,

        @Schema(description = "Win-Rate (wins / totalSims), 0..1.", example = "0.618", minimum = "0", maximum = "1")
        double winRate,

        @Schema(description = "Mittlere Überlebensrate (eigene Survivor / eigene Start-Count) über alle Sims, 0..1.",
                example = "0.34", minimum = "0", maximum = "1")
        double avgSurvivorRatio
) {
}
