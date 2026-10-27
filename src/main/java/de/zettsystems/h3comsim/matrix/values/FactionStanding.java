package de.zettsystems.h3comsim.matrix.values;

import de.zettsystems.h3comsim.battle.domain.Faction;
import io.swagger.v3.oas.annotations.media.Schema;

/** Bilanz einer Fraktion gegen alle anderen, ohne den Spiegel. */
@Schema(description = "Bilanz einer Fraktion gegen alle anderen gewählten Fraktionen, ohne den Spiegel.")
public record FactionStanding(
        @Schema(description = "Fraktion")
        Faction faction,

        @Schema(description = "Kämpfe gegen andere Fraktionen", example = "160")
        int sims,

        @Schema(description = "Siege", example = "97")
        int wins,

        @Schema(description = "Niederlagen", example = "58")
        int losses,

        @Schema(description = "Unentschieden", example = "5")
        int draws,

        @Schema(description = "wins / sims, 0..1", example = "0.61", minimum = "0", maximum = "1")
        double winRate,

        @Schema(description = "Mittlerer Anteil überlebender Kreaturen nach einem Sieg, 0..1", example = "0.47",
                minimum = "0", maximum = "1")
        double avgSurvivorRatio
) {
}
