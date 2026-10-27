package de.zettsystems.h3comsim.battle.domain;

import io.swagger.v3.oas.annotations.media.Schema;

/** Die beiden Auto-Solver, die beide Seiten einer Schlacht führen können. */
@Schema(description = "Auto-Solver: `GREEDY` (einfache Regeln je Zug) oder `STRATEGIC` (Fokus, Warten, Aufstellung; der Solver des Army-Battle).",
        enumAsRef = true)
public enum SolverKind {
    GREEDY,
    STRATEGIC;

    /** Ein neuer Solver dieser Art; Solver halten Zustand je Schlacht. */
    public AutoSolver create() {
        return switch (this) {
            case GREEDY -> new GreedyAutoSolver();
            case STRATEGIC -> new StrategicAutoSolver();
        };
    }
}
