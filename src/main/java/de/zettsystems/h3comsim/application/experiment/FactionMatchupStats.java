package de.zettsystems.h3comsim.application.experiment;

import de.zettsystems.h3comsim.domain.Faction;

/**
 * Aggregierte Statistik über alle Einheiten einer Faktion, die am Matrix-Lauf teilgenommen
 * haben. Win/Loss/Draw sind die Summen aller Unit-Wins/-Losses/-Draws dieser Faktion;
 * {@code winRate} und {@code avgSurvivorRatio} ergeben sich aus dem Gesamt-Sample.
 */
public record FactionMatchupStats(
        Faction faction,
        int unitCount,
        int totalSims,
        int wins,
        int losses,
        int draws,
        double winRate,
        double avgSurvivorRatio
) {
}
