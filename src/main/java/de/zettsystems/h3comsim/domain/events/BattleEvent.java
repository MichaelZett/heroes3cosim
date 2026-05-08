package de.zettsystems.h3comsim.domain.events;

public sealed interface BattleEvent {

    record BattleStart(int battlefieldWidth, int battlefieldHeight,
                       StackSnapshot attacker, StackSnapshot defender) implements BattleEvent {
    }

    record Move(Side actor, int fromQ, int fromR, int toQ, int toR) implements BattleEvent {
    }

    record Wait(Side actor) implements BattleEvent {
    }

    record Shoot(Side actor, Side target, int distance, int damage, int killed,
                 StackSnapshot targetAfter) implements BattleEvent {
    }

    record Melee(Side actor, Side target, int hexesMoved, int damage, int killed,
                 StackSnapshot targetAfter) implements BattleEvent {
    }

    record Retaliation(Side retaliator, Side target, int damage, int killed,
                       StackSnapshot targetAfter) implements BattleEvent {
    }

    record TwoBlows(Side actor) implements BattleEvent {
    }

    record TwoShots(Side actor) implements BattleEvent {
    }

    record GoodMorale(Side actor) implements BattleEvent {
    }

    record MoveBack(Side actor, int toQ, int toR) implements BattleEvent {
    }

    record DeathStare(Side actor, Side target, int kills, StackSnapshot targetAfter) implements BattleEvent {
    }

    record Thunderbolts(Side actor, Side target, int damage, StackSnapshot targetAfter) implements BattleEvent {
    }

    record Petrifying(Side actor, Side target) implements BattleEvent {
    }

    record Cursing(Side actor, Side target) implements BattleEvent {
    }

    record Poisoning(Side actor, Side target) implements BattleEvent {
    }

    record Diseasing(Side actor, Side target) implements BattleEvent {
    }

    record Aging(Side actor, Side target) implements BattleEvent {
    }

    record FireShield(Side shielded, Side attacker, int damage,
                      StackSnapshot attackerAfter) implements BattleEvent {
    }

    record Rebirth(Side actor, int restoredCount, StackSnapshot actorAfter) implements BattleEvent {
    }

    record BattleEnd(Winner winner, int attackerSurvivors, int defenderSurvivors,
                     int turns) implements BattleEvent {
    }
}
