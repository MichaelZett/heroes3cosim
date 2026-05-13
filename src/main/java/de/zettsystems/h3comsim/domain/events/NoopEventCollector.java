package de.zettsystems.h3comsim.domain.events;

public enum NoopEventCollector implements EventCollector {
    INSTANCE;

    @Override
    public void emit(BattleEvent event) {
        // no-op
    }
}
