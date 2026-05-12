package de.zettsystems.h3comsim.application.experiment;

import de.zettsystems.h3comsim.domain.Faction;

import java.util.Set;

/**
 * Eingabe für den Matrix-Lauf: jede zugelassene Einheit kämpft gegen jede andere zugelassene
 * Einheit. Pro Pair laufen {@code seedsPerMatchup} Seeds, jeweils mit getauschten Rollen — so
 * mittelt sich der Attacker-Vorteil heraus.
 */
public record MatrixRequest(
        int unitCount,
        Set<String> excludeUnits,
        Set<Faction> excludeFactions,
        Set<Integer> excludeTiers,
        int seedsPerMatchup
) {
    public MatrixRequest {
        if (unitCount < 1) {
            throw new IllegalArgumentException("unitCount must be >= 1, was " + unitCount);
        }
        if (seedsPerMatchup < 1) {
            throw new IllegalArgumentException("seedsPerMatchup must be >= 1, was " + seedsPerMatchup);
        }
        for (int tier : excludeTiers) {
            if (tier < 1 || tier > 7) {
                throw new IllegalArgumentException("tier in excludeTiers must be 1..7, was " + tier);
            }
        }
        excludeUnits = Set.copyOf(excludeUnits);
        excludeFactions = Set.copyOf(excludeFactions);
        excludeTiers = Set.copyOf(excludeTiers);
    }
}
