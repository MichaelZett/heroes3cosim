package de.zettsystems.h3comsim.armybattle.application;

import de.zettsystems.h3comsim.armybattle.values.ArmyBattleRequest;
import de.zettsystems.h3comsim.armybattle.values.ArmyBattleSimulation;
import de.zettsystems.h3comsim.battle.domain.BattleResult;
import de.zettsystems.h3comsim.battle.domain.SolverKind;

public interface ArmyBattleService {

    ArmyBattleSimulation simulate(ArmyBattleRequest request, long seed);

    /**
     * Dieselbe Schlacht wie {@link #simulate}, aber nur das Ergebnis, ohne Ereignisse, und mit dem
     * gewählten Solver auf beiden Seiten: für Auswertungen über viele Kämpfe.
     */
    BattleResult outcome(ArmyBattleRequest request, long seed, SolverKind solver);
}
