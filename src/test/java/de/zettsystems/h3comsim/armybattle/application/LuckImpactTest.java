package de.zettsystems.h3comsim.armybattle.application;

import de.zettsystems.h3comsim.armybattle.values.FactionPresetDto;
import de.zettsystems.h3comsim.armybattle.values.StackSpec;
import de.zettsystems.h3comsim.battle.domain.Battle;
import de.zettsystems.h3comsim.battle.domain.BattleResult;
import de.zettsystems.h3comsim.battle.domain.BattleSetup;
import de.zettsystems.h3comsim.battle.domain.Battlefield;
import de.zettsystems.h3comsim.battle.domain.CollectedBonus;
import de.zettsystems.h3comsim.battle.domain.Faction;
import de.zettsystems.h3comsim.battle.domain.Hero;
import de.zettsystems.h3comsim.battle.domain.HeroCatalog;
import de.zettsystems.h3comsim.battle.domain.Hex;
import de.zettsystems.h3comsim.battle.domain.ObstacleGenerator;
import de.zettsystems.h3comsim.battle.domain.Stack;
import de.zettsystems.h3comsim.battle.domain.StrategicAutoSolver;
import de.zettsystems.h3comsim.battle.domain.Unit;
import de.zettsystems.h3comsim.battle.domain.UnitCatalog;
import de.zettsystems.h3comsim.battle.domain.events.Side;
import de.zettsystems.h3comsim.battle.domain.events.Winner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;

/**
 * Wie viel ist ein Glückspunkt wert?
 *
 * <p>Spiegel-Duell: dieselbe Armee auf beiden Seiten, beide geführt vom Standardhelden ihrer
 * Fraktion. Eine Seite bekommt zusätzlich gesammeltes Glück (+1/+2/+3) — der einzige Unterschied.
 * Kein Standardheld hat die Luck-Fertigkeit, die Gegenseite hat also Glück 0.
 *
 * <p>Inferno prüft nebenbei die Devil-Aura: die gegnerischen Devils ziehen der Glücksseite einen
 * Punkt ab, +1 bleibt dort wirkungslos (0.500), +3 wirkt wie +2 anderswo.
 *
 * <p>Aktivieren: {@code .\gradlew.bat test --tests "*LuckImpactTest" "-Ph3.harness=luck-impact"}.
 * Report unter {@code build/reports/luck-impact.md}.
 */
@EnabledIfSystemProperty(named = "h3.harness", matches = "luck-impact")
class LuckImpactTest {

    private static final int SEEDS_PER_PAIR = 80;

    private static final List<Integer> LEVELS = List.of(1, 2, 3);

    private static final List<Faction> FACTIONS_IN_ORDER = List.of(
            Faction.CASTLE, Faction.RAMPART, Faction.TOWER, Faction.INFERNO,
            Faction.NECROPOLIS, Faction.DUNGEON, Faction.STRONGHOLD, Faction.FORTRESS,
            Faction.CONFLUX);

    @Test
    void luck_versus_no_luck() throws Exception {
        FactionPresetCatalog presets = new FactionPresetCatalog();
        Map<Faction, FactionPresetDto> presetByFaction = new EnumMap<>(Faction.class);
        for (FactionPresetDto p : presets.all()) {
            presetByFaction.put(p.faction(), p);
        }

        Map<Integer, Map<Faction, Tally>> byLevel = new TreeMap<>();
        Map<Integer, Tally> overallByLevel = new TreeMap<>();
        for (int level : LEVELS) {
            CollectedBonus bonus = new CollectedBonus(level, 0);
            Map<Faction, Tally> perFaction = new EnumMap<>(Faction.class);
            Tally overall = new Tally();
            for (Faction f : FACTIONS_IN_ORDER) {
                Tally tally = new Tally();
                perFaction.put(f, tally);
                for (int s = 0; s < SEEDS_PER_PAIR; s++) {
                    long seed = (long) f.ordinal() * 9929L + level * 113L + s;
                    duel(presetByFaction, f, bonus, seed, true, tally, overall);
                    duel(presetByFaction, f, bonus, seed, false, tally, overall);
                }
            }
            byLevel.put(level, perFaction);
            overallByLevel.put(level, overall);
        }

        String report = render(byLevel, overallByLevel);
        Path reportsDir = Path.of("build", "reports");
        Files.createDirectories(reportsDir);
        Path out = reportsDir.resolve("luck-impact.md");
        Files.writeString(out, report);
        IO.println("Luck-Impact-Report: " + out.toAbsolutePath());
        IO.println(report);
    }

    private static void duel(Map<Faction, FactionPresetDto> presets, Faction faction, CollectedBonus bonus,
                             long seed, boolean heroLeadsAttacker, Tally perFaction, Tally overall) {
        List<Stack> attackerStacks = buildStacks(presets.get(faction).stacks(), Side.ATTACKER);
        List<Stack> defenderStacks = buildStacks(presets.get(faction).stacks(), Side.DEFENDER);
        Hero hero = HeroCatalog.defaultFor(faction).orElseThrow();
        BattleSetup setup = new BattleSetup(attackerStacks, defenderStacks,
                buildBattlefield(attackerStacks, defenderStacks, seed),
                hero, heroLeadsAttacker ? bonus : CollectedBonus.NONE,
                hero, heroLeadsAttacker ? CollectedBonus.NONE : bonus);

        BattleResult result = new Battle(new Random(seed), new StrategicAutoSolver()).simulate(setup);
        Side heroSide = heroLeadsAttacker ? Side.ATTACKER : Side.DEFENDER;
        tally(perFaction, result, heroSide);
        tally(overall, result, heroSide);
    }

    private static void tally(Tally tally, BattleResult result, Side heroSide) {
        tally.battles++;
        Winner winner = result.winner();
        if (winner == Winner.DRAW) {
            tally.draws++;
        } else if ((winner == Winner.ATTACKER) == (heroSide == Side.ATTACKER)) {
            tally.wins++;
        } else {
            tally.losses++;
        }
    }

    private static List<Stack> buildStacks(List<StackSpec> specs, Side side) {
        List<Unit> units = new ArrayList<>(specs.size());
        for (StackSpec spec : specs) {
            units.add(UnitCatalog.byName(spec.unitName()).orElseThrow());
        }
        List<Hex> positions = SpawnLayout.assignPositions(side, units);
        List<Stack> stacks = new ArrayList<>(specs.size());
        for (int slot = 0; slot < specs.size(); slot++) {
            stacks.add(new Stack(units.get(slot), specs.get(slot).count(),
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

    private static String render(Map<Integer, Map<Faction, Tally>> byLevel,
                                 Map<Integer, Tally> overallByLevel) {
        StringBuilder sb = new StringBuilder(4096);
        sb.append("# Wirkung von Glück\n\n");
        sb.append("Spiegel-Duell: dieselbe Armee auf beiden Seiten, beide geführt vom ")
                .append("Standardhelden ihrer Fraktion; eine Seite hat zusätzlich gesammeltes Glück. ")
                .append("Der einzige Unterschied ist damit das Glücks-Rating.\n\n");
        sb.append(SEEDS_PER_PAIR).append(" Seeds je Faktion und Stufe, jede Paarung zusätzlich ")
                .append("mit getauschten Rollen. Solver: StrategicAutoSolver.\n\n");
        sb.append("**Lesart**: Win-Rate der Glücksseite. 0.500 = kein Unterschied.\n\n");

        sb.append("## Gesamt je Glücksstufe\n\n");
        sb.append("| Glück | Chance auf doppelten Schaden | Battles | Siege | Niederlagen | Draws | Win-Rate | Sigma |\n");
        sb.append("|--|--|--|--|--|--|--|--|\n");
        for (int level : LEVELS) {
            sb.append("| +").append(level).append(" | ").append(chanceOf(level));
            row(sb, overallByLevel.get(level));
        }

        for (int level : LEVELS) {
            sb.append("\n## Glück +").append(level).append(" je Faktion\n\n");
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

    /** Manual S. 44. */
    private static String chanceOf(int level) {
        return switch (level) {
            case 1 -> "4.2 %";
            case 2 -> "8.3 %";
            default -> "12.5 %";
        };
    }

    private static String rate(Tally t) {
        int decided = t.wins + t.losses;
        return decided == 0 ? "-" : String.format(Locale.ROOT, "%.3f", (double) t.wins / decided);
    }

    /** Abweichung von „Glück wirkt nicht" in Standardabweichungen; Binomial mit p = 0.5. */
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
    }
}
