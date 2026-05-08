package de.zettsystems.h3comsim.application;

import de.zettsystems.h3comsim.domain.Unit;

public interface BattleSimulationService {

    BattleSimulation simulate(Unit attacker, int attackerCount,
                              Unit defender, int defenderCount,
                              long seed);
}
