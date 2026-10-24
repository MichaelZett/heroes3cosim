package de.zettsystems.h3comsim.armybattle.application;

import de.zettsystems.h3comsim.armybattle.values.ArmyBattleRequest;
import de.zettsystems.h3comsim.armybattle.values.ArmyBattleSimulation;
import de.zettsystems.h3comsim.armybattle.values.ArmySpec;
import de.zettsystems.h3comsim.armybattle.values.StackSpec;
import de.zettsystems.h3comsim.battle.domain.Battle;
import de.zettsystems.h3comsim.battle.domain.BattleResult;
import de.zettsystems.h3comsim.battle.domain.BattleSetup;
import de.zettsystems.h3comsim.battle.domain.Battlefield;
import de.zettsystems.h3comsim.battle.domain.CollectedBonus;
import de.zettsystems.h3comsim.battle.domain.Hero;
import de.zettsystems.h3comsim.battle.domain.HeroCatalog;
import de.zettsystems.h3comsim.battle.domain.HeroGrowth;
import de.zettsystems.h3comsim.battle.domain.Hex;
import de.zettsystems.h3comsim.battle.domain.ObstacleGenerator;
import de.zettsystems.h3comsim.battle.domain.SecondarySkill;
import de.zettsystems.h3comsim.battle.domain.SkillLevel;
import de.zettsystems.h3comsim.battle.domain.Stack;
import de.zettsystems.h3comsim.battle.domain.StrategicAutoSolver;
import de.zettsystems.h3comsim.battle.domain.Unit;
import de.zettsystems.h3comsim.battle.domain.UnitCatalog;
import de.zettsystems.h3comsim.battle.domain.events.ListEventCollector;
import de.zettsystems.h3comsim.battle.domain.events.Side;
import de.zettsystems.h3comsim.battle.values.HeroMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;

@Service
public class DefaultArmyBattleService implements ArmyBattleService {

    @Override
    public ArmyBattleSimulation simulate(ArmyBattleRequest request, long seed) {
        List<Stack> attackerStacks = buildStacks(request.attacker(), Side.ATTACKER);
        List<Stack> defenderStacks = buildStacks(request.defender(), Side.DEFENDER);

        Battlefield battlefield = buildBattlefield(attackerStacks.size(), defenderStacks.size(), seed);
        Hero attackerHero = resolveHero(request.attacker(), attackerStacks, growthRandom(seed, Side.ATTACKER));
        Hero defenderHero = resolveHero(request.defender(), defenderStacks, growthRandom(seed, Side.DEFENDER));
        BattleSetup setup = new BattleSetup(attackerStacks, defenderStacks, battlefield,
                attackerHero, bonusOf(request.attacker()),
                defenderHero, bonusOf(request.defender()))
                .equip(Side.ATTACKER, Objects.requireNonNullElse(request.attacker().warMachines(), Set.of()))
                .equip(Side.DEFENDER, Objects.requireNonNullElse(request.defender().warMachines(), Set.of()));

        ListEventCollector collector = new ListEventCollector();
        BattleResult result = new Battle(new Random(seed), new StrategicAutoSolver(), collector).simulate(setup);
        return new ArmyBattleSimulation(result, collector.events(),
                HeroMapper.toDto(attackerHero), HeroMapper.toDto(defenderHero));
    }

    /** Eigener Zufallsstrom je Seite für den Aufstieg, damit die Schlacht selbst ungestört bleibt. */
    private static Random growthRandom(long seed, Side side) {
        return new Random(seed * 31 + (side == Side.ATTACKER ? 1 : 2));
    }

    /**
     * Jede Armee hat einen Helden; ohne Auswahl den Standardhelden ihrer Hauptfraktion. Level
     * und Aufbau aus dem Request gelten für beide Fälle; mit {@code randomGrowth} steigt der Held
     * zuerst nach den Regeln zufällig auf ({@link HeroGrowth}).
     */
    private static Hero resolveHero(ArmySpec army, List<Stack> stacks, Random growth) {
        String name = army.heroName();
        Hero hero = name == null || name.isBlank()
                ? HeroCatalog.defaultForArmy(stacks)
                : HeroCatalog.byName(name)
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.BAD_REQUEST, "Unknown hero: " + name));
        Map<SecondarySkill, SkillLevel> learned = Objects.requireNonNullElse(army.skills(), Map.of());
        try {
            int level = Objects.requireNonNullElse(army.heroLevel(), 1);
            Hero leveled = Boolean.TRUE.equals(army.randomGrowth())
                    ? HeroGrowth.grow(hero, level, growth)
                    : hero.withLevel(level);
            return leveled
                    .withBuild(Objects.requireNonNullElse(army.attackBonus(), 0),
                            Objects.requireNonNullElse(army.defenseBonus(), 0),
                            Objects.requireNonNullElse(army.powerBonus(), 0),
                            Objects.requireNonNullElse(army.knowledgeBonus(), 0), learned)
                    .withSpellbook(Objects.requireNonNullElse(army.spells(), Set.of()));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }

    private static CollectedBonus bonusOf(ArmySpec army) {
        // Der kompakte Konstruktor von ArmySpec setzt fehlende Boni auf 0.
        return new CollectedBonus(Objects.requireNonNullElse(army.luckBonus(), 0),
                Objects.requireNonNullElse(army.moraleBonus(), 0));
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
