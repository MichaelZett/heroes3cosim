package de.zettsystems.h3comsim.application;

import de.zettsystems.h3comsim.domain.Unit;
import de.zettsystems.h3comsim.domain.events.ListEventCollector;
import org.springframework.stereotype.Service;

import java.util.Random;

@Service
public class DefaultBattleSimulationService implements BattleSimulationService {

    @Override
    public BattleSimulation simulate(Unit attacker, int attackerCount,
                                     Unit defender, int defenderCount,
                                     long seed) {
        BattleSetup setup = new BattleSetup(attacker, attackerCount, defender, defenderCount);
        ListEventCollector collector = new ListEventCollector();
        BattleResult result = new Battle(new Random(seed), new GreedyAutoSolver(), collector).simulate(setup);
        return new BattleSimulation(result, collector.events());
    }
}
