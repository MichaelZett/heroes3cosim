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
import de.zettsystems.h3comsim.battle.domain.Faction;
import de.zettsystems.h3comsim.battle.domain.Fortification;
import de.zettsystems.h3comsim.battle.domain.Hero;
import de.zettsystems.h3comsim.battle.domain.HeroCatalog;
import de.zettsystems.h3comsim.battle.domain.HeroGrowth;
import de.zettsystems.h3comsim.battle.domain.Hex;
import de.zettsystems.h3comsim.battle.domain.MagicTerrain;
import de.zettsystems.h3comsim.battle.domain.ObstacleGenerator;
import de.zettsystems.h3comsim.battle.domain.SecondarySkill;
import de.zettsystems.h3comsim.battle.domain.Siege;
import de.zettsystems.h3comsim.battle.domain.SkillLevel;
import de.zettsystems.h3comsim.battle.domain.Stack;
import de.zettsystems.h3comsim.battle.domain.StrategicAutoSolver;
import de.zettsystems.h3comsim.battle.domain.Terrain;
import de.zettsystems.h3comsim.battle.domain.TerrainObstacles;
import de.zettsystems.h3comsim.battle.domain.Unit;
import de.zettsystems.h3comsim.battle.domain.UnitCatalog;
import de.zettsystems.h3comsim.battle.domain.WarMachine;
import de.zettsystems.h3comsim.battle.domain.events.ListEventCollector;
import de.zettsystems.h3comsim.battle.domain.events.Side;
import de.zettsystems.h3comsim.battle.values.HeroMapper;
import org.jspecify.annotations.Nullable;
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

        Fortification fortification = request.fortification();
        Hero attackerHero = resolveHero(request.attacker(), attackerStacks, growthRandom(seed, Side.ATTACKER));
        Hero defenderHero = resolveHero(request.defender(), defenderStacks, growthRandom(seed, Side.DEFENDER));
        Siege siege = null;
        if (fortification != null) {
            if (attackerHero == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only a hero can besiege a town");
            }
            siege = new Siege(fortification, town(request, defenderHero, defenderStacks));
        }
        Terrain terrain = siege == null ? request.terrain() : siegeTerrain(request, siege);
        // Belagerungen haben im Spiel keine Felsen auf dem Feld.
        Battlefield battlefield = siege == null
                ? buildBattlefield(attackerStacks.size(), defenderStacks.size(), seed, terrain, request.magicTerrain())
                : Battlefield.STANDARD;
        BattleSetup setup = new BattleSetup(attackerStacks, defenderStacks, battlefield,
                attackerHero, bonusOf(request.attacker()),
                defenderHero, bonusOf(request.defender()), siege)
                .equip(Side.ATTACKER, machines(request.attacker(), attackerHero))
                .equip(Side.DEFENDER, machines(request.defender(), defenderHero))
                .sodSpellCosts(Boolean.TRUE.equals(request.sodSpellCosts()))
                .ground(terrain, request.magicTerrain());
        if (siege == null && (terrain != null || request.magicTerrain() != null)) {
            setup.obstacleGroups(obstacleGroups(battlefield, terrain, request.magicTerrain(), seed));
        }

        ListEventCollector collector = new ListEventCollector();
        BattleResult result = new Battle(new Random(seed), new StrategicAutoSolver(), collector).simulate(setup);
        return new ArmyBattleSimulation(result, collector.events(),
                attackerHero == null ? null : HeroMapper.toDto(attackerHero),
                defenderHero == null ? null : HeroMapper.toDto(defenderHero));
    }

    /**
     * Eine Belagerung findet auf dem Heimatgelände der Stadt statt, nie auf magischem Gelände
     * (heroes.thelazy.net, <em>Native terrain</em>).
     */
    private static Terrain siegeTerrain(ArmyBattleRequest request, Siege siege) {
        Terrain own = Objects.requireNonNull(Terrain.nativeOf(siege.town()));
        if (request.magicTerrain() != null || (request.terrain() != null && request.terrain() != own)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A siege is fought on the native terrain of the town (" + own + "), without magical terrain");
        }
        return own;
    }

    /**
     * Die Stadt des Verteidigers: wie angegeben, sonst die Fraktion seines Helden, ohne Helden
     * die Hauptfraktion der Garnison. Eine rein neutrale Garnison braucht die Angabe.
     */
    private static Faction town(ArmyBattleRequest request, @Nullable Hero defenderHero, List<Stack> defenderStacks) {
        Faction town = request.town();
        if (town == null) {
            town = defenderHero != null ? defenderHero.faction() : HeroCatalog.dominantFaction(defenderStacks);
        }
        if (town == null || town == Faction.NEUTRAL) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Town needs one of the nine town factions");
        }
        return town;
    }

    /** Kriegsmaschinen gehören dem Helden; eine Armee ohne ihn hat keine. */
    private static Set<WarMachine> machines(ArmySpec army, @Nullable Hero hero) {
        return hero == null ? Set.of() : Objects.requireNonNullElse(army.warMachines(), Set.of());
    }

    /** Eigener Zufallsstrom je Seite für den Aufstieg, damit die Schlacht selbst ungestört bleibt. */
    private static Random growthRandom(long seed, Side side) {
        return new Random(seed * 31 + (side == Side.ATTACKER ? 1 : 2));
    }

    /**
     * Der Held der Armee; ohne Auswahl den Standardhelden ihrer Hauptfraktion, mit
     * {@code withoutHero} keinen. Level und Aufbau aus dem Request gelten für beide Fälle; mit
     * {@code randomGrowth} steigt der Held zuerst nach den Regeln zufällig auf ({@link HeroGrowth}).
     */
    private static @Nullable Hero resolveHero(ArmySpec army, List<Stack> stacks, Random growth) {
        String name = army.heroName();
        if (Boolean.TRUE.equals(army.withoutHero())) {
            if (name != null && !name.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "heroName and withoutHero exclude each other");
            }
            return null;
        }
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

    /**
     * Die Hindernisse des Gelände-Satzes, die Remove Obstacle als Ganzes entfernt: derselbe Wurf wie
     * in {@link #buildBattlefield}, ohne Klippen und ohne die Aufstellungsfelder.
     */
    private static List<Set<Hex>> obstacleGroups(Battlefield battlefield, @Nullable Terrain terrain,
                                                 @Nullable MagicTerrain magic, long seed) {
        List<Set<Hex>> groups = new ArrayList<>();
        for (Set<Hex> group : TerrainObstacles.layout(terrain, magic, new Random(seed)).removable()) {
            Set<Hex> standing = new HashSet<>(group);
            standing.retainAll(battlefield.obstacles());
            if (!standing.isEmpty()) {
                groups.add(standing);
            }
        }
        return groups;
    }

    private static Battlefield buildBattlefield(int attackerStacks, int defenderStacks, long seed,
                                                @Nullable Terrain terrain, @Nullable MagicTerrain magic) {
        // Obstacle-Layout aus separatem Seed-Strom ableiten, damit der Battle-RNG identische
        // Würfe liefert. Spawn-Hexen werden anschließend aus dem Obstacle-Set entfernt — sonst
        // könnte ein Stack auf einem Hindernis landen. Ohne Gelände zufällige Felsen, sonst der
        // Hindernis-Satz des Geländes.
        Set<Hex> obstacles = new HashSet<>(terrain == null && magic == null
                ? ObstacleGenerator.generate(Battlefield.STANDARD, new Random(seed))
                : TerrainObstacles.generate(terrain, magic, new Random(seed)));
        obstacles.removeAll(SpawnLayout.spawnHexesFor(attackerStacks, defenderStacks));
        return Battlefield.STANDARD.withObstacles(obstacles);
    }
}
