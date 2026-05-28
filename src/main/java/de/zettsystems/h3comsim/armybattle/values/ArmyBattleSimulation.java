package de.zettsystems.h3comsim.armybattle.values;

import de.zettsystems.h3comsim.battle.domain.BattleResult;
import de.zettsystems.h3comsim.battle.domain.events.BattleEvent;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Antwort einer Army-Battle-Simulation: aggregiertes Endergebnis plus chronologischer Event-Stream zum Replay.")
public record ArmyBattleSimulation(
        @Schema(description = "Aggregiertes Endergebnis (Gewinner, Start-/Überlebende-Counts, Runden).")
        BattleResult result,

        @Schema(description = "Chronologische Liste der Battle-Events. Polymorph über den `type`-Diskriminator. Action-Events tragen `actorSlot`/`targetSlot` (0..6).")
        List<BattleEvent> events) {
    public ArmyBattleSimulation {
        events = List.copyOf(events);
    }
}
