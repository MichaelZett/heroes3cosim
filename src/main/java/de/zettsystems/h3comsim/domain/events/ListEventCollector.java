package de.zettsystems.h3comsim.domain.events;

import java.util.ArrayList;
import java.util.List;

public final class ListEventCollector implements EventCollector {

    private final List<BattleEvent> events = new ArrayList<>();

    @Override
    public void emit(BattleEvent event) {
        events.add(event);
    }

    public List<BattleEvent> events() {
        return List.copyOf(events);
    }
}
