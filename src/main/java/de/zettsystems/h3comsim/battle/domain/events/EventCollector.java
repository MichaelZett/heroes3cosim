package de.zettsystems.h3comsim.battle.domain.events;

@FunctionalInterface
public interface EventCollector {
    void emit(BattleEvent event);
}
