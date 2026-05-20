package de.zettsystems.h3comsim.battle.domain;

import de.zettsystems.h3comsim.battle.domain.events.Winner;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Aggregiertes Endergebnis einer Einzelschlacht. Wird sowohl in der Single-Battle-Response geliefert als auch intern in Matrix-Aggregaten verrechnet.")
public record BattleResult(
        @Schema(description = "Gewinner der Schlacht oder DRAW bei beidseitiger Auslöschung / Turn-Limit.")
        Winner winner,

        @Schema(description = "Start-Stack-Größe der Attacker-Seite", example = "20")
        int attackerCountStart,

        @Schema(description = "Überlebende Attacker am Ende der Schlacht (kann 0 sein)", example = "7")
        int attackerSurvivors,

        @Schema(description = "Start-Stack-Größe der Defender-Seite", example = "15")
        int defenderCountStart,

        @Schema(description = "Überlebende Defender am Ende der Schlacht (kann 0 sein)", example = "0")
        int defenderSurvivors,

        @Schema(description = "Anzahl der ausgeführten Runden (= ein Durchlauf der gesamten Move-Order)", example = "5")
        int turnsTaken
) {
}
