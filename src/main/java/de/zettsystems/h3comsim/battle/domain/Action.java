package de.zettsystems.h3comsim.battle.domain;

public sealed interface Action {

    record Move(Hex destination) implements Action {
    }

    record MoveAndMelee(Hex destination, Stack target) implements Action {
    }

    record Melee(Stack target) implements Action {
    }

    record Shoot(Stack target) implements Action {
    }

    record Wait() implements Action {
    }

    /**
     * H3-Defend: Stack bewegt sich nicht, bekommt +30 % Defense bis Rundenende. Klassische
     * Schützen-Tank-Strategie, wenn der Tank vor dem Schützen schon richtig steht.
     */
    record Defend() implements Action {
    }
}
