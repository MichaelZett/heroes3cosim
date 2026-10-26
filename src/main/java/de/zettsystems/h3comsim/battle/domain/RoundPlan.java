package de.zettsystems.h3comsim.battle.domain;

import de.zettsystems.h3comsim.battle.domain.events.Side;
import org.jspecify.annotations.Nullable;

import java.util.Map;

/**
 * Pro-Runden-Plan: wählt pro Seite einen Focus-Fire-Target. Wird vom {@link StrategicAutoSolver}
 * in {@link AutoSolver#planRound(BattleSetup)} erzeugt und für die gesamte Runde stabil gehalten.
 */
public record RoundPlan(Map<Side, Stack> focusTarget) {

    public static final RoundPlan EMPTY = new RoundPlan(Map.of());

    public RoundPlan {
        focusTarget = Map.copyOf(focusTarget);
    }

    public @Nullable Stack focusOf(Side side) {
        return focusTarget.get(side);
    }
}
