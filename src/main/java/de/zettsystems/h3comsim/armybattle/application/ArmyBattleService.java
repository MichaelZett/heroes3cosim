package de.zettsystems.h3comsim.armybattle.application;

import de.zettsystems.h3comsim.armybattle.values.ArmyBattleRequest;
import de.zettsystems.h3comsim.armybattle.values.ArmyBattleSimulation;

public interface ArmyBattleService {

    ArmyBattleSimulation simulate(ArmyBattleRequest request, long seed);
}
