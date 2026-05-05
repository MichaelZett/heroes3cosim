package de.zettsystems.h3comsim.application;

public record BattleResult(
        Side winner,
        int attackerCountStart,
        int attackerSurvivors,
        int defenderCountStart,
        int defenderSurvivors,
        int turnsTaken
) {
    public enum Side {
        ATTACKER,
        DEFENDER,
        DRAW
    }
}
