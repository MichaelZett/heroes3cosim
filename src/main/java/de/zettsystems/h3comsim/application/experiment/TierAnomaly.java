package de.zettsystems.h3comsim.application.experiment;

/**
 * Auffälligkeit: eine Tier-N-Einheit verliert mehrheitlich gegen Tier-(N-1)-Einheiten. Wenn
 * eine Truppe eine ganze Klasse unter sich nicht zuverlässig schlägt, ist ihre Tier-Einstufung
 * relativ zu den Catalog-Werten verdächtig.
 */
public record TierAnomaly(
        String unitName,
        int tier,
        int againstTier,
        double winRate,
        int sampleSize
) {
}
