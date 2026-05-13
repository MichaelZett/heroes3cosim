package de.zettsystems.h3comsim.matrix.values;

import de.zettsystems.h3comsim.battle.domain.Faction;

/**
 * Aggregierte Statistik einer Einheit über alle Matrix-Sims. Win-/Loss-/Draw-Counts und
 * mittlere Überlebensrate (eigene Survivor / eigene Start-Count) jeweils kumulativ.
 */
public record UnitMatchupStats(
        String unitName,
        Faction faction,
        int tier,
        boolean upgrade,
        int totalSims,
        int wins,
        int losses,
        int draws,
        double winRate,
        double avgSurvivorRatio
) {
}
