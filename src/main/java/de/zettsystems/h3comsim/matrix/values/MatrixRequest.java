package de.zettsystems.h3comsim.matrix.values;

import de.zettsystems.h3comsim.battle.domain.Faction;

import java.util.Set;

/**
 * Eingabe für den Matrix-Lauf: jede zugelassene Einheit kämpft gegen jede andere zugelassene
 * Einheit. Pro Pair laufen {@code seedsPerMatchup} Seeds, jeweils mit getauschten Rollen — so
 * mittelt sich der Attacker-Vorteil heraus.
 *
 * <p>{@code unitCount} ist der Skalierungsfaktor pro Stack. Bei {@link StackSizingMode#EQUAL_COUNT}
 * direkt die Stack-Größe; bei {@link StackSizingMode#EQUAL_GOLD} ein Multiplikator für das
 * Pair-Budget; bei {@link StackSizingMode#WEEKLY_PRODUCTION} ein Multiplikator für die
 * Wochenproduktion ({@code unitCount=1} = eine Woche).
 */
public record MatrixRequest(
        int unitCount,
        Set<String> excludeUnits,
        Set<Faction> excludeFactions,
        Set<Integer> excludeTiers,
        StackSizingMode mode,
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
        if (mode == null) {
            throw new IllegalArgumentException("mode must not be null");
        }
        excludeUnits = Set.copyOf(excludeUnits);
        excludeFactions = Set.copyOf(excludeFactions);
        excludeTiers = Set.copyOf(excludeTiers);
    }
}
