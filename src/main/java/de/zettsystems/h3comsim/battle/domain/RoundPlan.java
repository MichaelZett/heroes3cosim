package de.zettsystems.h3comsim.battle.domain;

import de.zettsystems.h3comsim.battle.domain.events.Side;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.Set;

/**
 * Pro-Runden-Plan: kategorisiert beide Seiten in eine {@link TeamStance}, wählt pro Seite einen
 * Focus-Fire-Target und markiert eigene Schützen, die Tank-Schutz brauchen. Wird vom
 * {@link StrategicAutoSolver} in {@link AutoSolver#planRound(BattleSetup)} erzeugt und für die
 * gesamte Runde stabil gehalten.
 */
public record RoundPlan(
        Map<Side, TeamStance> stance,
        Map<Side, Stack> focusTarget,
        Set<Stack> protectedShooters
) {

    public static final RoundPlan EMPTY = new RoundPlan(Map.of(), Map.of(), Set.of());

    public RoundPlan {
        stance = Map.copyOf(stance);
        focusTarget = Map.copyOf(focusTarget);
        protectedShooters = Set.copyOf(protectedShooters);
    }

    public TeamStance stanceOf(Side side) {
        return stance.getOrDefault(side, TeamStance.BALANCED);
    }

    public @Nullable Stack focusOf(Side side) {
        return focusTarget.get(side);
    }

    public boolean isProtected(Stack stack) {
        return protectedShooters.contains(stack);
    }

    /**
     * Hat die Seite mindestens einen lebenden, schutzwürdigen Schützen? Wenn ja, sollen
     * eigene Nahkämpfer Tank-Positionen statt Charge wählen — unabhängig von der
     * {@link TeamStance}. Wird vom {@link StrategicAutoSolver#decide} als Trigger
     * für das Tank-Pattern genutzt.
     */
    public boolean hasTankDuty(Side side) {
        for (Stack s : protectedShooters) {
            if (s.side() == side && s.isAlive()) {
                return true;
            }
        }
        return false;
    }
}
