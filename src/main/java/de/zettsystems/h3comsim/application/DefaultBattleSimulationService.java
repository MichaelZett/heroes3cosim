package de.zettsystems.h3comsim.application;

import de.zettsystems.h3comsim.domain.Battlefield;
import de.zettsystems.h3comsim.domain.Hex;
import de.zettsystems.h3comsim.domain.ObstacleGenerator;
import de.zettsystems.h3comsim.domain.Unit;
import de.zettsystems.h3comsim.domain.events.ListEventCollector;
import org.springframework.stereotype.Service;

import java.util.Random;

@Service
public class DefaultBattleSimulationService implements BattleSimulationService {

    private static final Hex DEFAULT_ATTACKER_POSITION = new Hex(0, 5);
    private static final Hex DEFAULT_DEFENDER_POSITION = new Hex(14, 5);

    @Override
    public BattleSimulation simulate(Unit attacker, int attackerCount,
                                     Unit defender, int defenderCount,
                                     long seed) {
        // Obstacle-Layout aus dem Seed ableiten, aber separater Random-Strom, damit der Battle-RNG
        // bei identischem Seed weiterhin identische Würfe liefert (Determinismus-Test stützt das).
        Battlefield battlefield = Battlefield.STANDARD.withObstacles(
                ObstacleGenerator.generate(Battlefield.STANDARD, new Random(seed)));
        BattleSetup setup = new BattleSetup(attacker, attackerCount, defender, defenderCount,
                battlefield, DEFAULT_ATTACKER_POSITION, DEFAULT_DEFENDER_POSITION);
        ListEventCollector collector = new ListEventCollector();
        BattleResult result = new Battle(new Random(seed), new GreedyAutoSolver(), collector).simulate(setup);
        return new BattleSimulation(result, collector.events());
    }
}
