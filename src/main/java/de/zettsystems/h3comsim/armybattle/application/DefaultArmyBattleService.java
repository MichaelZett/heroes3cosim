package de.zettsystems.h3comsim.armybattle.application;

import de.zettsystems.h3comsim.armybattle.values.ArmyBattleRequest;
import de.zettsystems.h3comsim.armybattle.values.ArmyBattleSimulation;
import de.zettsystems.h3comsim.armybattle.values.ArmySpec;
import de.zettsystems.h3comsim.armybattle.values.StackSpec;
import de.zettsystems.h3comsim.battle.domain.Battle;
import de.zettsystems.h3comsim.battle.domain.BattleResult;
import de.zettsystems.h3comsim.battle.domain.BattleSetup;
import de.zettsystems.h3comsim.battle.domain.Battlefield;
import de.zettsystems.h3comsim.battle.domain.Hero;
import de.zettsystems.h3comsim.battle.domain.HeroCatalog;
import de.zettsystems.h3comsim.battle.domain.Hex;
import de.zettsystems.h3comsim.battle.domain.ObstacleGenerator;
import de.zettsystems.h3comsim.battle.domain.Stack;
import de.zettsystems.h3comsim.battle.domain.StrategicAutoSolver;
import de.zettsystems.h3comsim.battle.domain.Unit;
import de.zettsystems.h3comsim.battle.domain.UnitCatalog;
import de.zettsystems.h3comsim.battle.domain.events.ListEventCollector;
import de.zettsystems.h3comsim.battle.domain.events.Side;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

@Service
public class DefaultArmyBattleService implements ArmyBattleService {

    @Override
    public ArmyBattleSimulation simulate(ArmyBattleRequest request, long seed) {
        List<Stack> attackerStacks = buildStacks(request.attacker(), Side.ATTACKER);
        List<Stack> defenderStacks = buildStacks(request.defender(), Side.DEFENDER);

        Battlefield battlefield = buildBattlefield(attackerStacks.size(), defenderStacks.size(), seed);
        BattleSetup setup = new BattleSetup(attackerStacks, defenderStacks, battlefield,
                resolveHero(request.attacker()), resolveHero(request.defender()));

        ListEventCollector collector = new ListEventCollector();
        BattleResult result = new Battle(new Random(seed), new StrategicAutoSolver(), collector).simulate(setup);
        return new ArmyBattleSimulation(result, collector.events());
    }

    private static @Nullable Hero resolveHero(ArmySpec army) {
        String name = army.heroName();
        if (name == null || name.isBlank()) {
            return null;
        }
        return HeroCatalog.byName(name)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Unknown hero: " + name));
    }

    private static List<Stack> buildStacks(ArmySpec army, Side side) {
        int total = army.stacks().size();
        List<Unit> units = new ArrayList<>(total);
        for (StackSpec spec : army.stacks()) {
            units.add(UnitCatalog.byName(spec.unitName())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.BAD_REQUEST, "Unknown unit: " + spec.unitName())));
        }
        List<Hex> positions = SpawnLayout.assignPositions(side, units);
        List<Stack> stacks = new ArrayList<>(total);
        for (int slot = 0; slot < total; slot++) {
            stacks.add(new Stack(units.get(slot), army.stacks().get(slot).count(),
                    positions.get(slot), side, slot));
        }
        return stacks;
    }

    private static Battlefield buildBattlefield(int attackerStacks, int defenderStacks, long seed) {
        // Obstacle-Layout aus separatem Seed-Strom ableiten, damit der Battle-RNG identische
        // Würfe liefert. Spawn-Hexen werden anschließend aus dem Obstacle-Set entfernt — sonst
        // könnte ein Stack auf einem Hindernis landen.
        Set<Hex> obstacles = new HashSet<>(
                ObstacleGenerator.generate(Battlefield.STANDARD, new Random(seed)));
        obstacles.removeAll(SpawnLayout.spawnHexesFor(attackerStacks, defenderStacks));
        return Battlefield.STANDARD.withObstacles(obstacles);
    }
}
