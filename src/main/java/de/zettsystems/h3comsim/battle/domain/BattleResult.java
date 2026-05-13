package de.zettsystems.h3comsim.battle.domain;

import de.zettsystems.h3comsim.battle.domain.events.Winner;

public record BattleResult(
        Winner winner,
        int attackerCountStart,
        int attackerSurvivors,
        int defenderCountStart,
        int defenderSurvivors,
        int turnsTaken
) {
}
