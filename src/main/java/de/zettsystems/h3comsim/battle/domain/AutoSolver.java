package de.zettsystems.h3comsim.battle.domain;

public interface AutoSolver {
    Action decide(Stack active, Stack opponent, Battlefield battlefield);
}
