package de.zettsystems.h3comsim.matrix.values;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Auffälligkeit: eine Tier-N-Einheit verliert mehrheitlich gegen Tier-(N-1)-Einheiten. Wenn
 * eine Truppe eine ganze Klasse unter sich nicht zuverlässig schlägt, ist ihre Tier-Einstufung
 * relativ zu den Catalog-Werten verdächtig.
 */
@Schema(description = "Hinweis auf eine verdächtige Tier-Einstufung: eine Tier-N-Einheit verliert mehrheitlich gegen Einheiten eines niedrigeren Tiers.")
public record TierAnomaly(
        @Schema(description = "Betroffene Einheit", example = "Wraith")
        String unitName,

        @Schema(description = "Tier dieser Einheit", example = "5")
        int tier,

        @Schema(description = "Tier der Gegner, gegen die mehrheitlich verloren wurde (typischerweise `tier - 1`)",
                example = "4")
        int againstTier,

        @Schema(description = "Win-Rate gegen das niedrigere Tier (sollte normalerweise > 0.5 sein).",
                example = "0.41", minimum = "0", maximum = "1")
        double winRate,

        @Schema(description = "Sample-Größe der zugrundeliegenden Sims.", example = "320")
        int sampleSize
) {
}
