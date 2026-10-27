package de.zettsystems.h3comsim.armybattle.application;

import de.zettsystems.h3comsim.armybattle.values.ArmyBattleRequest;
import de.zettsystems.h3comsim.armybattle.values.ArmyBattleSimulation;
import de.zettsystems.h3comsim.armybattle.values.ArmySpec;
import de.zettsystems.h3comsim.armybattle.values.CreatureBankSpec;
import de.zettsystems.h3comsim.armybattle.values.StackSpec;
import de.zettsystems.h3comsim.battle.domain.Battle;
import de.zettsystems.h3comsim.battle.domain.BattleResult;
import de.zettsystems.h3comsim.battle.domain.BattleSetup;
import de.zettsystems.h3comsim.battle.domain.Battlefield;
import de.zettsystems.h3comsim.battle.domain.CollectedBonus;
import de.zettsystems.h3comsim.battle.domain.CreatureBank;
import de.zettsystems.h3comsim.battle.domain.Equipment;
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

    /**
     * Die beiden Seiten vor der Schlacht: Stacks, Helden und die Boni des Verteidigers; im Kampf um
     * eine Kreaturenbank deren Wächter und die Stufe.
     */
    private record Sides(List<Stack> attackers, List<Stack> defenders, @Nullable Hero attackerHero,
                         @Nullable Hero defenderHero, @Nullable ArmySpec defender, @Nullable Integer bankLevel) {

        boolean bank() {
            return bankLevel != null;
        }
    }

    @Override
    public ArmyBattleSimulation simulate(ArmyBattleRequest request, long seed) {
        CreatureBankSpec bankSpec = request.creatureBank();
        Sides sides = bankSpec == null ? armies(request, seed) : bank(request, bankSpec, seed);
        Siege siege = siege(request, sides.attackerHero(), sides.defenderHero(), sides.defenders());
        if (sides.bank() && (siege != null || sides.attackerHero() == null)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Only a hero visits a creature bank, and it is no town");
        }
        Terrain terrain = siege == null ? request.terrain() : siegeTerrain(request, siege);
        if (terrain == Terrain.WATER && request.magicTerrain() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "There is no magical terrain at sea");
        }
        // Zwei Helden in Booten kämpfen auf zwei Schiffen, alle übrigen Kämpfe auf Wasser auf dem Deck.
        boolean twoShips = terrain == Terrain.WATER && sides.attackerHero() != null && sides.defenderHero() != null;
        // Belagerungen und Kreaturenbanken haben im Spiel keine Felsen auf dem Feld.
        boolean obstacles = siege == null && !sides.bank() && !twoShips;
        Battlefield battlefield = twoShips ? Battlefield.STANDARD.withObstacles(TerrainObstacles.shipToShip())
                : buildBattlefield(obstacles, sides.attackers().size(), sides.defenders().size(), seed, terrain,
                request.magicTerrain());
        ArmySpec defender = sides.defender();
        BattleSetup setup = new BattleSetup(sides.attackers(), sides.defenders(), battlefield,
                sides.attackerHero(), bonusOf(request.attacker()),
                sides.defenderHero(), defender == null ? CollectedBonus.NONE : bonusOf(defender),
                siege, !sides.bank())
                .sodSpellCosts(Boolean.TRUE.equals(request.sodSpellCosts()))
                .ground(terrain, request.magicTerrain());
        if (defender != null) {
            setup.equip(Side.ATTACKER, machines(request.attacker(), sides.attackerHero()))
                    .equip(Side.DEFENDER, machines(defender, sides.defenderHero()));
        }
        if (obstacles && (terrain != null || request.magicTerrain() != null)) {
            setup.obstacleGroups(obstacleGroups(battlefield, terrain, request.magicTerrain(), seed));
        }

        ListEventCollector collector = new ListEventCollector();
        BattleResult result = new Battle(new Random(seed), new StrategicAutoSolver(), collector).simulate(setup);
        Hero attackerHero = sides.attackerHero();
        Hero defenderHero = sides.defenderHero();
        return new ArmyBattleSimulation(result, collector.events(),
                attackerHero == null ? null : HeroMapper.toDto(attackerHero),
                defenderHero == null ? null : HeroMapper.toDto(defenderHero), sides.bankLevel());
    }

    /** Armee gegen Armee, beide in der üblichen Aufstellung. */
    private static Sides armies(ArmyBattleRequest request, long seed) {
        ArmySpec defender = request.defender();
        if (defender == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "defender is required without a creature bank");
        }
        List<Stack> attackers = buildStacks(request.attacker(), Side.ATTACKER);
        List<Stack> defenders = buildStacks(defender, Side.DEFENDER);
        return new Sides(attackers, defenders,
                resolveHero(request.attacker(), attackers, growthRandom(seed, Side.ATTACKER)),
                resolveHero(defender, defenders, growthRandom(seed, Side.DEFENDER)), defender, null);
    }

    /** Die Armee des Helden gegen die Wächter einer Kreaturenbank, in deren Aufstellung. */
    private static Sides bank(ArmyBattleRequest request, CreatureBankSpec spec, long seed) {
        Random random = new Random(seed * 31 + 3);
        int level = bankLevel(spec, random);
        List<Stack> attackers = bankAttackers(request.attacker());
        return new Sides(attackers, guards(spec.bank(), level, random),
                resolveHero(request.attacker(), attackers, growthRandom(seed, Side.ATTACKER)), null, null, level);
    }

    private static @Nullable Siege siege(ArmyBattleRequest request, @Nullable Hero attackerHero,
                                         @Nullable Hero defenderHero, List<Stack> defenderStacks) {
        Fortification fortification = request.fortification();
        if (fortification == null) {
            return null;
        }
        if (attackerHero == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only a hero can besiege a town");
        }
        return new Siege(fortification, town(request, defenderHero, defenderStacks));
    }

    /** Die gewählte Stufe der Bank, ohne Angabe gewürfelt. */
    private static int bankLevel(CreatureBankSpec spec, Random random) {
        Integer level = spec.level();
        return level == null ? spec.bank().rollLevel(random) : level;
    }

    /** Die Armee des Helden auf den Feldern der Bank, Slot für Slot. */
    private static List<Stack> bankAttackers(ArmySpec army) {
        List<Stack> stacks = new ArrayList<>();
        for (int slot = 0; slot < army.stacks().size(); slot++) {
            StackSpec spec = army.stacks().get(slot);
            stacks.add(new Stack(unit(spec), spec.count(), CreatureBank.attackerHex(slot), Side.ATTACKER, slot));
        }
        return stacks;
    }

    /** Die Wächter der Bank auf ihren Feldern; ausbaufähige Stacks werden gewürfelt. */
    private static List<Stack> guards(CreatureBank bank, int level, Random random) {
        List<CreatureBank.Guard> guards = bank.level(level).guards();
        List<Stack> stacks = new ArrayList<>();
        for (int index = 0; index < guards.size(); index++) {
            CreatureBank.Guard guard = guards.get(index);
            stacks.add(new Stack(CreatureBank.roll(guard, random), guard.count(), bank.guardHex(index),
                    Side.DEFENDER, index));
        }
        return stacks;
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
                    .withSpellbook(Objects.requireNonNullElse(army.spells(), Set.of()))
                    .withArtifacts(new Equipment(Objects.requireNonNullElse(army.artifacts(), Map.of())));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }

    private static CollectedBonus bonusOf(ArmySpec army) {
        // Der kompakte Konstruktor von ArmySpec setzt fehlende Boni auf 0.
        return new CollectedBonus(Objects.requireNonNullElse(army.luckBonus(), 0),
                Objects.requireNonNullElse(army.moraleBonus(), 0));
    }

    private static Unit unit(StackSpec spec) {
        return UnitCatalog.byName(spec.unitName())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Unknown unit: " + spec.unitName()));
    }

    private static List<Stack> buildStacks(ArmySpec army, Side side) {
        int total = army.stacks().size();
        List<Unit> units = new ArrayList<>(total);
        for (StackSpec spec : army.stacks()) {
            units.add(unit(spec));
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

    private static Battlefield buildBattlefield(boolean withObstacles, int attackerStacks, int defenderStacks, long seed,
                                                @Nullable Terrain terrain, @Nullable MagicTerrain magic) {
        if (!withObstacles) {
            return Battlefield.STANDARD;
        }
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
