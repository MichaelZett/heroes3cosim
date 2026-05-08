package de.zettsystems.h3comsim.domain.events;

@FunctionalInterface
public interface EventCollector {
    void emit(BattleEvent event);
}
