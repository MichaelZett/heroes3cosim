package de.zettsystems.h3comsim.singlebattle.values;

import de.zettsystems.h3comsim.battle.domain.BattleResult;
import de.zettsystems.h3comsim.battle.domain.events.BattleEvent;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Antwort einer Einzel-Simulation: aggregiertes Endergebnis plus chronologischer Event-Stream zum Replay.")
public record BattleSimulationDto(
        @Schema(description = "Aggregiertes Endergebnis (Gewinner, Start-/Überlebende-Counts, Runden).")
        BattleResult result,

        @Schema(description = "Chronologische Liste der Battle-Events. Polymorph über den `type`-Diskriminator.")
        List<BattleEvent> events
) {
}
