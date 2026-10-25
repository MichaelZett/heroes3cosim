package de.zettsystems.h3comsim.armybattle.application;

import de.zettsystems.h3comsim.armybattle.values.FactionPresetDto;
import de.zettsystems.h3comsim.armybattle.values.StackSpec;
import de.zettsystems.h3comsim.battle.domain.Action;
import de.zettsystems.h3comsim.battle.domain.AutoSolver;
import de.zettsystems.h3comsim.battle.domain.Battle;
import de.zettsystems.h3comsim.battle.domain.BattleResult;
import de.zettsystems.h3comsim.battle.domain.BattleSetup;
import de.zettsystems.h3comsim.battle.domain.Battlefield;
import de.zettsystems.h3comsim.battle.domain.Faction;
import de.zettsystems.h3comsim.battle.domain.Hero;
import de.zettsystems.h3comsim.battle.domain.HeroCatalog;
import de.zettsystems.h3comsim.battle.domain.Hex;
import de.zettsystems.h3comsim.battle.domain.ObstacleGenerator;
import de.zettsystems.h3comsim.battle.domain.SecondarySkill;
import de.zettsystems.h3comsim.battle.domain.SkillLevel;
import de.zettsystems.h3comsim.battle.domain.Stack;
import de.zettsystems.h3comsim.battle.domain.StrategicAutoSolver;
import de.zettsystems.h3comsim.battle.domain.Unit;
import de.zettsystems.h3comsim.battle.domain.UnitCatalog;
import de.zettsystems.h3comsim.battle.domain.WarMachine;
import de.zettsystems.h3comsim.battle.domain.events.Side;
import de.zettsystems.h3comsim.battle.domain.events.Winner;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Was bringen Kriegsmaschinen?
 *
 * <p>Spiegel-Duell: dieselbe Armee auf beiden Seiten, beide geführt vom Standardhelden ihrer
 * Fraktion; eine Seite führt zusätzlich eine Ausrüstung mit. Gemessen werden die Ballista allein,
 * die Ballista mit Expert Artillery, das Sanitätszelt mit Expert First Aid und alle drei
 * Maschinen zusammen. Der Munitionswagen zählt nur, wo Schützen ihre Schüsse aufbrauchen.
 *
 * <p>Zusätzlich läuft jede Ausrüstung mit vierfacher Armee (vier Wochenproduktionen). Die
 * Maschinen wirken mit festen Werten, ihr Anteil schrumpft mit der Armee — beim Sanitätszelt
 * deutlich: bei einer Woche holt es rund ein Viertel der Lebenspunkte zurück, bei vier Wochen
 * rund ein Zehntel. Das Spiegel-Duell entscheidet schon kleine Dauervorteile fast immer.
 *
 * <p>Seit die Maschinen auf dem Feld stehen, kann die Gegenseite sie zerstören. Ein zweiter
 * Lauf misst, ob sich das lohnt: alle drei Maschinen auf beiden Seiten, nur eine greift sie an.
 *
 * <p>Aktivieren: {@code .\gradlew.bat test --tests "*WarMachineImpactTest" "-Ph3.harness=war-machine-impact"}.
 * Reports unter {@code build/reports/war-machine-impact.md} und {@code war-machine-targets.md}.
 */
@EnabledIfSystemProperty(named = "h3.harness", matches = "war-machine-impact")
class WarMachineImpactTest {

    private static final int SEEDS_PER_PAIR = 80;

    /** Ausrüstungen: Maschinen plus die Fertigkeiten, die der Held dazu lernt. */
    private enum Kit {
        BALLISTA(Set.of(WarMachine.BALLISTA), Map.of()),
        BALLISTA_EXPERT_ARTILLERY(Set.of(WarMachine.BALLISTA), Map.of(SecondarySkill.ARTILLERY, SkillLevel.EXPERT)),
        TENT_EXPERT_FIRST_AID(Set.of(WarMachine.FIRST_AID_TENT), Map.of(SecondarySkill.FIRST_AID, SkillLevel.EXPERT)),
        ALL_THREE(EnumSet.allOf(WarMachine.class), Map.of());

        private final Set<WarMachine> machines;
        private final Map<SecondarySkill, SkillLevel> skills;

        Kit(Set<WarMachine> machines, Map<SecondarySkill, SkillLevel> skills) {
            this.machines = machines;
            this.skills = skills;
        }
    }

    private static final List<Kit> LEVELS = List.of(Kit.values());

    private static final int FOUR_WEEKS = 4;

    private static final List<Faction> FACTIONS_IN_ORDER = List.of(
            Faction.CASTLE, Faction.RAMPART, Faction.TOWER, Faction.INFERNO,
            Faction.NECROPOLIS, Faction.DUNGEON, Faction.STRONGHOLD, Faction.FORTRESS,
            Faction.CONFLUX);

    @Test
    void war_machines_versus_none() throws Exception {
        FactionPresetCatalog presets = new FactionPresetCatalog();
        Map<Faction, FactionPresetDto> presetByFaction = new EnumMap<>(Faction.class);
        for (FactionPresetDto p : presets.all()) {
            presetByFaction.put(p.faction(), p);
        }

        Map<Kit, Map<Faction, Tally>> byLevel = new EnumMap<>(Kit.class);
        Map<Kit, Tally> overallByLevel = new EnumMap<>(Kit.class);
        for (Kit level : LEVELS) {
            Map<Faction, Tally> perFaction = new EnumMap<>(Faction.class);
            Tally overall = new Tally();
            for (Faction f : FACTIONS_IN_ORDER) {
                Tally tally = new Tally();
                perFaction.put(f, tally);
                for (int s = 0; s < SEEDS_PER_PAIR; s++) {
                    long seed = (long) f.ordinal() * 9929L + level.ordinal() * 113L + s;
                    duel(presetByFaction, f, level, 1, seed, true, tally, overall);
                    duel(presetByFaction, f, level, 1, seed, false, tally, overall);
                }
            }
            byLevel.put(level, perFaction);
            overallByLevel.put(level, overall);
        }

        Map<Kit, Tally> fourWeeks = new EnumMap<>(Kit.class);
        for (Kit level : LEVELS) {
            Tally overall = new Tally();
            for (Faction f : FACTIONS_IN_ORDER) {
                Tally tally = new Tally();
                for (int s = 0; s < SEEDS_PER_PAIR; s++) {
                    long seed = (long) f.ordinal() * 9929L + level.ordinal() * 113L + s;
                    duel(presetByFaction, f, level, FOUR_WEEKS, seed, true, tally, overall);
                    duel(presetByFaction, f, level, FOUR_WEEKS, seed, false, tally, overall);
                }
            }
            fourWeeks.put(level, overall);
        }

        String report = render(byLevel, overallByLevel, fourWeeks);
        Path reportsDir = Path.of("build", "reports");
        Files.createDirectories(reportsDir);
        Path out = reportsDir.resolve("war-machine-impact.md");
        Files.writeString(out, report);
        IO.println("War-Machine-Impact-Report: " + out.toAbsolutePath());
        IO.println(report);
    }

    /**
     * Lohnt es, Maschinen anzugreifen? Beide Seiten führen dieselbe Armee, denselben Helden und alle
     * drei Maschinen; nur eine Seite greift Maschinen an ({@code WarMachineTargeting}), sobald ein
     * Stack keine Kreatur erreicht. Win-Rate der angreifenden Seite, dazu wie viele gegnerische
     * Maschinen sie je Kampf zerstört.
     */
    @Test
    void attacking_machines_versus_leaving_them() throws Exception {
        FactionPresetCatalog presets = new FactionPresetCatalog();
        Map<Faction, FactionPresetDto> presetByFaction = new EnumMap<>(Faction.class);
        for (FactionPresetDto p : presets.all()) {
            presetByFaction.put(p.faction(), p);
        }
        StringBuilder sb = new StringBuilder(2048);
        sb.append("# Maschinen angreifen oder stehen lassen\n\n")
                .append("Spiegel-Duell mit Standardheld und allen drei Maschinen auf beiden Seiten; nur eine Seite ")
                .append("greift Maschinen an, wenn ein Stack keine Kreatur erreicht. ").append(SEEDS_PER_PAIR)
                .append(" Seeds, getauschte Rollen, StrategicAutoSolver.\n\n")
                .append("| Armee | Faktion | Battles | Siege | Niederlagen | Draws | Win-Rate | Sigma | Zerstört je Kampf |\n")
                .append("|--|--|--|--|--|--|--|--|--|\n");
        for (int weeks : List.of(1, FOUR_WEEKS)) {
            Tally overall = new Tally();
            for (Faction f : FACTIONS_IN_ORDER) {
                Tally tally = new Tally();
                for (int s = 0; s < SEEDS_PER_PAIR; s++) {
                    long seed = (long) f.ordinal() * 9929L + 557L + s;
                    for (Side hunter : Side.values()) {
                        machineDuel(presetByFaction, f, weeks, seed, hunter, tally, overall);
                    }
                }
                sb.append("| ").append(weeks).append(" Woche(n) | ").append(f.name());
                machineRow(sb, tally);
            }
            sb.append("| ").append(weeks).append(" Woche(n) | Gesamt");
            machineRow(sb, overall);
        }
        String report = sb.toString();
        Path reportsDir = Path.of("build", "reports");
        Files.createDirectories(reportsDir);
        Path out = reportsDir.resolve("war-machine-targets.md");
        Files.writeString(out, report);
        IO.println("War-Machine-Targets-Report: " + out.toAbsolutePath());
        IO.println(report);
    }

    private static void machineDuel(Map<Faction, FactionPresetDto> presets, Faction faction, int weeks, long seed,
                                    Side hunter, Tally perFaction, Tally overall) {
        List<Stack> attackerStacks = buildStacks(presets.get(faction).stacks(), Side.ATTACKER, weeks);
        List<Stack> defenderStacks = buildStacks(presets.get(faction).stacks(), Side.DEFENDER, weeks);
        Hero hero = HeroCatalog.defaultFor(faction).orElseThrow();
        BattleSetup setup = new BattleSetup(attackerStacks, defenderStacks,
                buildBattlefield(attackerStacks, defenderStacks, seed), hero, hero)
                .equip(Side.ATTACKER, EnumSet.allOf(WarMachine.class))
                .equip(Side.DEFENDER, EnumSet.allOf(WarMachine.class));
        Side prey = hunter == Side.ATTACKER ? Side.DEFENDER : Side.ATTACKER;
        BattleResult result = new Battle(new Random(seed), new HuntsMachines(hunter)).simulate(setup);
        int destroyed = (int) setup.machinesOf(prey).stream().filter(m -> !m.isAlive()).count();
        perFaction.destroyed += destroyed;
        overall.destroyed += destroyed;
        tally(perFaction, result, hunter);
        tally(overall, result, hunter);
    }

    private static void machineRow(StringBuilder sb, Tally t) {
        int decided = t.wins + t.losses;
        sb.append(" | ").append(t.battles)
                .append(" | ").append(t.wins)
                .append(" | ").append(t.losses)
                .append(" | ").append(t.draws)
                .append(" | ").append(decided == 0 ? "-" : String.format(Locale.ROOT, "%.3f", (double) t.wins / decided))
                .append(" | ").append(decided == 0 ? "-" : String.format(Locale.ROOT, "%+.2f",
                        (t.wins - decided / 2.0) / Math.sqrt(decided / 4.0)))
                .append(" | ").append(String.format(Locale.ROOT, "%.2f", (double) t.destroyed / t.battles))
                .append(" |\n");
    }

    /** Strategic auf beiden Seiten; nur die Seite {@code hunter} greift Maschinen an. */
    private static final class HuntsMachines implements AutoSolver {
        private final StrategicAutoSolver solver = new StrategicAutoSolver();
        private final Side hunter;

        HuntsMachines(Side hunter) {
            this.hunter = hunter;
        }

        @Override
        public void planRound(BattleSetup setup) {
            solver.planRound(setup);
        }

        @Override
        public @Nullable Stack pickTarget(Stack active, List<Stack> opponents, Battlefield battlefield) {
            return solver.pickTarget(active, opponents, battlefield);
        }

        @Override
        public Action decide(Stack active, Stack opponent, Battlefield battlefield) {
            return solver.decide(active, opponent, battlefield);
        }

        @Override
        public boolean attacksWarMachines(Stack active) {
            return active.side() == hunter;
        }
    }

    private static void duel(Map<Faction, FactionPresetDto> presets, Faction faction, Kit kit, int weeks,
                             long seed, boolean heroLeadsAttacker, Tally perFaction, Tally overall) {
        List<Stack> attackerStacks = buildStacks(presets.get(faction).stacks(), Side.ATTACKER, weeks);
        List<Stack> defenderStacks = buildStacks(presets.get(faction).stacks(), Side.DEFENDER, weeks);
        Hero hero = HeroCatalog.defaultFor(faction).orElseThrow();
        Hero equipped = hero.withBuild(0, 0, kit.skills);
        BattleSetup setup = new BattleSetup(attackerStacks, defenderStacks,
                buildBattlefield(attackerStacks, defenderStacks, seed),
                heroLeadsAttacker ? equipped : hero,
                heroLeadsAttacker ? hero : equipped)
                .equip(heroLeadsAttacker ? Side.ATTACKER : Side.DEFENDER, kit.machines);

        BattleResult result = new Battle(new Random(seed), new StrategicAutoSolver()).simulate(setup);
        Side heroSide = heroLeadsAttacker ? Side.ATTACKER : Side.DEFENDER;
        tally(perFaction, result, heroSide);
        tally(overall, result, heroSide);
    }

    private static void tally(Tally tally, BattleResult result, Side heroSide) {
        tally.battles++;
        if (result.attackerSurvivors() > 0 && result.defenderSurvivors() > 0) {
            tally.undecided++;
        }
        Winner winner = result.winner();
        if (winner == Winner.DRAW) {
            tally.draws++;
        } else if ((winner == Winner.ATTACKER) == (heroSide == Side.ATTACKER)) {
            tally.wins++;
        } else {
            tally.losses++;
        }
    }

    private static List<Stack> buildStacks(List<StackSpec> specs, Side side, int weeks) {
        List<Unit> units = new ArrayList<>(specs.size());
        for (StackSpec spec : specs) {
            units.add(UnitCatalog.byName(spec.unitName()).orElseThrow());
        }
        List<Hex> positions = SpawnLayout.assignPositions(side, units);
        List<Stack> stacks = new ArrayList<>(specs.size());
        for (int slot = 0; slot < specs.size(); slot++) {
            stacks.add(new Stack(units.get(slot), specs.get(slot).count() * weeks,
                    positions.get(slot), side, slot));
        }
        return stacks;
    }

    private static Battlefield buildBattlefield(List<Stack> a, List<Stack> d, long seed) {
        Set<Hex> obstacles = new HashSet<>(
                ObstacleGenerator.generate(Battlefield.STANDARD, new Random(seed)));
        obstacles.removeAll(SpawnLayout.spawnHexesFor(a.size(), d.size()));
        return Battlefield.STANDARD.withObstacles(obstacles);
    }

    private static String render(Map<Kit, Map<Faction, Tally>> byLevel,
                                 Map<Kit, Tally> overallByLevel, Map<Kit, Tally> fourWeeks) {
        StringBuilder sb = new StringBuilder(4096);
        sb.append("# Wirkung von Kriegsmaschinen\n\n");
        sb.append("Spiegel-Duell: dieselbe Armee auf beiden Seiten, beide geführt vom ")
                .append("Standardhelden ihrer Fraktion; eine Seite führt zusätzlich die Ausrüstung mit. ")
                .append("Der einzige Unterschied sind Maschinen und die zugehörige Fertigkeit.\n\n");
        sb.append(SEEDS_PER_PAIR).append(" Seeds je Faktion und Stufe, jede Paarung zusätzlich ")
                .append("mit getauschten Rollen. Solver: StrategicAutoSolver.\n\n");
        sb.append("**Lesart**: Win-Rate der ausgerüsteten Seite. 0.500 = kein Unterschied.\n\n");

        sb.append("## Gesamt je Ausrüstung\n\n");
        sb.append("| Ausrüstung | Battles | Siege | Niederlagen | Draws | Win-Rate | Sigma |\n");
        sb.append("|--|--|--|--|--|--|--|\n");
        for (Kit level : LEVELS) {
            sb.append("| ").append(level.name());
            row(sb, overallByLevel.get(level));
        }

        sb.append("\n## Gesamt je Ausrüstung, vier Wochenproduktionen\n\n");
        sb.append("Alle Stacks mit vierfacher Anzahl. `Ohne Sieger` = Kampf endete am Runden- oder ")
                .append("Patt-Limit und wurde über die verbleibenden Lebenspunkte entschieden.\n\n");
        sb.append("| Ausrüstung | Battles | Siege | Niederlagen | Draws | Win-Rate | Sigma | Ohne Sieger |\n");
        sb.append("|--|--|--|--|--|--|--|--|\n");
        for (Kit level : LEVELS) {
            Tally t = fourWeeks.get(level);
            sb.append("| ").append(level.name());
            row(sb, t);
            sb.setLength(sb.length() - 1);
            sb.append(" ").append(t.undecided).append(" |\n");
        }

        for (Kit level : LEVELS) {
            sb.append("\n## ").append(level.name()).append(" je Faktion\n\n");
            sb.append("| Faktion | Battles | Siege | Niederlagen | Draws | Win-Rate | Sigma |\n");
            sb.append("|--|--|--|--|--|--|--|\n");
            for (Faction f : FACTIONS_IN_ORDER) {
                sb.append("| ").append(f.name());
                row(sb, byLevel.get(level).get(f));
            }
        }
        return sb.toString();
    }

    private static void row(StringBuilder sb, Tally t) {
        sb.append(" | ").append(t.battles)
                .append(" | ").append(t.wins)
                .append(" | ").append(t.losses)
                .append(" | ").append(t.draws)
                .append(" | ").append(rate(t))
                .append(" | ").append(sigma(t))
                .append(" |\n");
    }

    private static String rate(Tally t) {
        int decided = t.wins + t.losses;
        return decided == 0 ? "-" : String.format(Locale.ROOT, "%.3f", (double) t.wins / decided);
    }

    /** Abweichung von „Maschinen wirken nicht" in Standardabweichungen; Binomial mit p = 0.5. */
    private static String sigma(Tally t) {
        int decided = t.wins + t.losses;
        if (decided == 0) {
            return "-";
        }
        double expected = decided / 2.0;
        double sd = Math.sqrt(decided / 4.0);
        return String.format(Locale.ROOT, "%+.2f", (t.wins - expected) / sd);
    }

    private static final class Tally {
        private int battles;
        private int wins;
        private int losses;
        private int draws;
        private int undecided;
        private int destroyed;
    }
}
