package de.zettsystems.h3comsim.matrix.values;

import de.zettsystems.h3comsim.battle.domain.Faction;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Aggregierte Statistik über alle Einheiten einer Faktion, die am Matrix-Lauf teilgenommen
 * haben. Win/Loss/Draw sind die Summen aller Unit-Wins/-Losses/-Draws dieser Faktion;
 * {@code winRate} und {@code avgSurvivorRatio} ergeben sich aus dem Gesamt-Sample.
 */
@Schema(description = "Per-Faction-Aggregat — Summe aller Unit-Stats dieser Faktion.")
public record FactionMatchupStats(
        @Schema(description = "Faction")
        Faction faction,

        @Schema(description = "Anzahl der teilnehmenden Einheiten dieser Faktion (nach Excludes).", example = "12")
        int unitCount,

        @Schema(description = "Summe der Einzel-Sims über alle Units dieser Faktion.", example = "14400")
        int totalSims,

        @Schema(description = "Siege (Summe aller Units)", example = "8920")
        int wins,

        @Schema(description = "Niederlagen (Summe aller Units)", example = "5024")
        int losses,

        @Schema(description = "Unentschieden (Summe aller Units)", example = "456")
        int draws,

        @Schema(description = "Win-Rate dieser Faktion über das Gesamt-Sample, 0..1.", example = "0.619",
                minimum = "0", maximum = "1")
        double winRate,

        @Schema(description = "Mittlere Überlebensrate dieser Faktion über das Gesamt-Sample, 0..1.",
                example = "0.41", minimum = "0", maximum = "1")
        double avgSurvivorRatio
) {
}
