package de.zettsystems.h3comsim.singlebattle.application;

import de.zettsystems.h3comsim.battle.domain.Unit;
import de.zettsystems.h3comsim.singlebattle.values.BattleSimulation;

public interface BattleSimulationService {

    BattleSimulation simulate(Unit attacker, int attackerCount,
                              Unit defender, int defenderCount,
                              long seed);
}
