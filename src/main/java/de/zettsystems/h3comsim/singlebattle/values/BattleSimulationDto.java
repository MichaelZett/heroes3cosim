package de.zettsystems.h3comsim.singlebattle.values;

import de.zettsystems.h3comsim.battle.domain.BattleResult;
import de.zettsystems.h3comsim.battle.domain.events.BattleEvent;

import java.util.List;

public record BattleSimulationDto(BattleResult result, List<BattleEvent> events) {
}
