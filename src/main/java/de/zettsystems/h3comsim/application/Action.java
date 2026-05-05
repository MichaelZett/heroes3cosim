package de.zettsystems.h3comsim.application;

import de.zettsystems.h3comsim.domain.Hex;
import de.zettsystems.h3comsim.domain.Stack;

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
}
