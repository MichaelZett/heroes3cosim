package de.zettsystems.h3comsim.domain.events;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.List;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = BattleEvent.BattleStart.class, name = "BattleStart"),
        @JsonSubTypes.Type(value = BattleEvent.Move.class, name = "Move"),
        @JsonSubTypes.Type(value = BattleEvent.Wait.class, name = "Wait"),
        @JsonSubTypes.Type(value = BattleEvent.Shoot.class, name = "Shoot"),
        @JsonSubTypes.Type(value = BattleEvent.Melee.class, name = "Melee"),
        @JsonSubTypes.Type(value = BattleEvent.Retaliation.class, name = "Retaliation"),
        @JsonSubTypes.Type(value = BattleEvent.TwoBlows.class, name = "TwoBlows"),
        @JsonSubTypes.Type(value = BattleEvent.TwoShots.class, name = "TwoShots"),
        @JsonSubTypes.Type(value = BattleEvent.GoodMorale.class, name = "GoodMorale"),
        @JsonSubTypes.Type(value = BattleEvent.MoveBack.class, name = "MoveBack"),
        @JsonSubTypes.Type(value = BattleEvent.DeathStare.class, name = "DeathStare"),
        @JsonSubTypes.Type(value = BattleEvent.Thunderbolts.class, name = "Thunderbolts"),
        @JsonSubTypes.Type(value = BattleEvent.Petrifying.class, name = "Petrifying"),
        @JsonSubTypes.Type(value = BattleEvent.Cursing.class, name = "Cursing"),
        @JsonSubTypes.Type(value = BattleEvent.Poisoning.class, name = "Poisoning"),
        @JsonSubTypes.Type(value = BattleEvent.Diseasing.class, name = "Diseasing"),
        @JsonSubTypes.Type(value = BattleEvent.Aging.class, name = "Aging"),
        @JsonSubTypes.Type(value = BattleEvent.FireShield.class, name = "FireShield"),
        @JsonSubTypes.Type(value = BattleEvent.Rebirth.class, name = "Rebirth"),
        @JsonSubTypes.Type(value = BattleEvent.BattleEnd.class, name = "BattleEnd"),
})
public sealed interface BattleEvent {

    record BattleStart(int battlefieldWidth, int battlefieldHeight,
                       List<HexCoord> obstacles,
                       StackSnapshot attacker, StackSnapshot defender) implements BattleEvent {
        public BattleStart {
            obstacles = List.copyOf(obstacles);
        }
    }

    record Move(Side actor, int fromQ, int fromR, int toQ, int toR,
                List<HexCoord> path) implements BattleEvent {
        public Move {
            path = List.copyOf(path);
        }
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

    record MoveBack(Side actor, int toQ, int toR, List<HexCoord> path) implements BattleEvent {
        public MoveBack {
            path = List.copyOf(path);
        }
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
