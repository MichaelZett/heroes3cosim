package de.zettsystems.h3comsim.battle.domain;

import org.jspecify.annotations.Nullable;

import java.util.List;

public interface AutoSolver {

    /**
     * Hook für stateful Multi-Stack-Solver: wird einmal pro Runde von {@link Battle} vor der
     * Move-Order aufgerufen. Default tut nichts (für Single-Battle-Greedy-Solver irrelevant).
     */
    default void planRound(BattleSetup setup) {
        // no-op
    }

    /**
     * Wählt aus {@code opponents} den Stack aus, gegen den die nächste Aktion gerichtet wird.
     * Default-Implementierung: erster Eintrag — reicht für 1-vs-1-Pfade ohne weitere Heuristik.
     * Multi-Stack-Solver überschreiben diese Methode.
     *
     * @return {@code null}, wenn keine Gegner mehr leben.
     */
    default @Nullable Stack pickTarget(Stack active, List<Stack> opponents, Battlefield battlefield) {
        return opponents.isEmpty() ? null : opponents.get(0);
    }

    Action decide(Stack active, Stack opponent, Battlefield battlefield);
}
