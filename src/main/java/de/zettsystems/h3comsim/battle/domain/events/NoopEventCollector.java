package de.zettsystems.h3comsim.battle.domain.events;

// Enum-Singleton ist Absicht: zustandsloser Default-Collector (CLI), Serializable+Threadsafe gratis.
@SuppressWarnings("java:S6548")
public enum NoopEventCollector implements EventCollector {
    INSTANCE;

    @Override
    public void emit(BattleEvent event) {
        // no-op
    }
}
