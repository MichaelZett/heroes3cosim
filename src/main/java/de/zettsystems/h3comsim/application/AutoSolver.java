package de.zettsystems.h3comsim.application;

import de.zettsystems.h3comsim.domain.Battlefield;
import de.zettsystems.h3comsim.domain.Stack;

public interface AutoSolver {
    Action decide(Stack active, Stack opponent, Battlefield battlefield);
}
