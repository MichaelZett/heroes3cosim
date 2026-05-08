package de.zettsystems.h3comsim.adapter.web.dto;

import de.zettsystems.h3comsim.application.BattleResult;
import de.zettsystems.h3comsim.domain.events.BattleEvent;

import java.util.List;

public record BattleSimulationDto(BattleResult result, List<BattleEvent> events) {
}
