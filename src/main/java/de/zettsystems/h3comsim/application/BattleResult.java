package de.zettsystems.h3comsim.application;

import de.zettsystems.h3comsim.domain.events.Winner;

public record BattleResult(
        Winner winner,
        int attackerCountStart,
        int attackerSurvivors,
        int defenderCountStart,
        int defenderSurvivors,
        int turnsTaken
) {
}
