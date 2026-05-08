package de.zettsystems.h3comsim.domain.events;

public final class NoopEventCollector implements EventCollector {

    public static final NoopEventCollector INSTANCE = new NoopEventCollector();

    private NoopEventCollector() {
    }

    @Override
    public void emit(BattleEvent event) {
        // no-op
    }
}
