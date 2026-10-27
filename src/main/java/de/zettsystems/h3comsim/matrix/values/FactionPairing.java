package de.zettsystems.h3comsim.matrix.values;

import de.zettsystems.h3comsim.battle.domain.Faction;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Eine Zelle der Faction-Matrix aus Sicht von {@code faction}: beide Rollen zusammengezählt. Im
 * Spiegel ({@code faction == opponent}) zählen die Siege des Angreifers — Abweichungen von 0.5
 * zeigen dort eine Seitenlast.
 */
@Schema(description = "Zelle der Faction-Matrix aus Sicht von `faction`, beide Rollen zusammengezählt. Im Spiegel (`faction` = `opponent`) zählen die Siege des Angreifers.")
public record FactionPairing(
        @Schema(description = "Zeilen-Fraktion")
        Faction faction,

        @Schema(description = "Gegner-Fraktion")
        Faction opponent,

        @Schema(description = "Kämpfe dieser Paarung (beide Rollen)", example = "20")
        int sims,

        @Schema(description = "Siege von `faction`; im Spiegel Siege des Angreifers", example = "13")
        int wins,

        @Schema(description = "Niederlagen von `faction`; im Spiegel Siege des Verteidigers", example = "6")
        int losses,

        @Schema(description = "Unentschieden", example = "1")
        int draws,

        @Schema(description = "wins / sims, 0..1", example = "0.65", minimum = "0", maximum = "1")
        double winRate,

        @Schema(description = "Mittlere Rundenzahl", example = "6.4")
        double avgRounds
) {
}
