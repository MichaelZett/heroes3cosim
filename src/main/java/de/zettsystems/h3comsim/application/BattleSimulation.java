package de.zettsystems.h3comsim.application;

import de.zettsystems.h3comsim.domain.events.BattleEvent;

import java.util.List;

public record BattleSimulation(BattleResult result, List<BattleEvent> events) {

    public BattleSimulation {
        events = List.copyOf(events);
    }
}
