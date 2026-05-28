package de.zettsystems.h3comsim.armybattle.application;

import de.zettsystems.h3comsim.armybattle.values.FactionPresetDto;
import de.zettsystems.h3comsim.battle.domain.AutoSolver;
import de.zettsystems.h3comsim.battle.domain.Battle;
import de.zettsystems.h3comsim.battle.domain.BattleResult;
import de.zettsystems.h3comsim.battle.domain.BattleSetup;
import de.zettsystems.h3comsim.battle.domain.Battlefield;
import de.zettsystems.h3comsim.battle.domain.Faction;
import de.zettsystems.h3comsim.battle.domain.GreedyAutoSolver;
import de.zettsystems.h3comsim.battle.domain.Hex;
import de.zettsystems.h3comsim.battle.domain.ObstacleGenerator;
import de.zettsystems.h3comsim.battle.domain.Stack;
import de.zettsystems.h3comsim.battle.domain.StrategicAutoSolver;
import de.zettsystems.h3comsim.battle.domain.UnitCatalog;
import de.zettsystems.h3comsim.battle.domain.events.Side;
import de.zettsystems.h3comsim.battle.domain.events.Winner;
import org.junit.jupiter.api.Test;

import java.io.IOException;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Supplier;

/**
 * Empirische Faction-vs-Faction-Matrix: rennt jede Wochenproduktions-Composition gegen jede,
 * über mehrere Seeds und mit getauschten Seiten. Läuft mit beiden Solvern (Greedy, Strategic),
 * schreibt einen Vergleichs-Report nach {@code build/reports/faction-matrix.md} und prüft nur,
 * dass kein Lauf crasht — die eigentliche Auswertung passiert beim Sichten des Reports.
 *
 * <p>Cost pro Default-Run: 9 × 9 × {@value #SEEDS_PER_PAIR} × 2 × 2 Solver = 1620 Sims bei
 * SEEDS_PER_PAIR=5.
 */
class FactionMatrixHarness {

    private static final int SEEDS_PER_PAIR = 5;

    private static final List<Faction> FACTIONS_IN_ORDER = List.of(
            Faction.CASTLE, Faction.RAMPART, Faction.TOWER, Faction.INFERNO,
            Faction.NECROPOLIS, Faction.DUNGEON, Faction.STRONGHOLD, Faction.FORTRESS,
            Faction.CONFLUX);

    @Test
    void run_full_faction_matrix_and_write_report() throws IOException {
        FactionPresetCatalog presets = new FactionPresetCatalog();
        Map<Faction, FactionPresetDto> presetByFaction = new EnumMap<>(Faction.class);
        for (FactionPresetDto p : presets.all()) {
            presetByFaction.put(p.faction(), p);
        }

        long t0 = System.currentTimeMillis();
        Map<String, AggregatedPair> greedy = runMatrix(presetByFaction, GreedyAutoSolver::new);
        long tGreedy = System.currentTimeMillis() - t0;

        long t1 = System.currentTimeMillis();
        Map<String, AggregatedPair> strategic = runMatrix(presetByFaction, StrategicAutoSolver::new);
        long tStrategic = System.currentTimeMillis() - t1;

        String report = renderComparison(greedy, strategic, tGreedy, tStrategic);
        Path reportsDir = Path.of("build", "reports");
        Files.createDirectories(reportsDir);
        Path out = reportsDir.resolve("faction-matrix.md");
        Files.writeString(out, report);

        System.out.println("Faction-Matrix-Report: " + out.toAbsolutePath());
        System.out.println(report);
    }

    private static Map<String, AggregatedPair> runMatrix(Map<Faction, FactionPresetDto> presets,
                                                         Supplier<AutoSolver> solverFactory) {
        Map<String, PairStats> stats = new ConcurrentHashMap<>();
        for (Faction a : FACTIONS_IN_ORDER) {
            for (Faction d : FACTIONS_IN_ORDER) {
                stats.put(key(a, d), new PairStats());
            }
        }
        try (ExecutorService pool = Executors.newWorkStealingPool()) {
            for (Faction attacker : FACTIONS_IN_ORDER) {
                for (Faction defender : FACTIONS_IN_ORDER) {
                    List<de.zettsystems.h3comsim.armybattle.values.StackSpec> attackerSpec =
                            presets.get(attacker).stacks();
                    List<de.zettsystems.h3comsim.armybattle.values.StackSpec> defenderSpec =
                            presets.get(defender).stacks();
                    PairStats acc = stats.get(key(attacker, defender));
                    for (int s = 0; s < SEEDS_PER_PAIR; s++) {
                        long seed = (long) (attacker.ordinal() * 9929L + defender.ordinal() * 113L + s);
                        pool.execute(() -> runOne(attackerSpec, defenderSpec, seed,
                                solverFactory.get(), acc));
                    }
                }
            }
        }
        return aggregate(stats);
    }

    private static void runOne(List<de.zettsystems.h3comsim.armybattle.values.StackSpec> attacker,
                               List<de.zettsystems.h3comsim.armybattle.values.StackSpec> defender,
                               long seed, AutoSolver solver, PairStats acc) {
        List<Stack> attackerStacks = buildStacks(attacker, Side.ATTACKER);
        List<Stack> defenderStacks = buildStacks(defender, Side.DEFENDER);
        Battlefield bf = buildBattlefield(attackerStacks, defenderStacks, seed);
        BattleSetup setup = new BattleSetup(attackerStacks, defenderStacks, bf);
        BattleResult result = new Battle(new Random(seed), solver).simulate(setup);

        acc.total.increment();
        switch (result.winner()) {
            case ATTACKER -> acc.attackerWins.increment();
            case DEFENDER -> acc.defenderWins.increment();
            case DRAW -> acc.draws.increment();
        }
        acc.turnSum.add(result.turnsTaken());
        if (result.winner() == Winner.ATTACKER) {
            acc.winnerSurvivorRatio.add(
                    (long) (1000.0 * result.attackerSurvivors() / result.attackerCountStart()));
            acc.winnerSurvivorSamples.increment();
        } else if (result.winner() == Winner.DEFENDER) {
            acc.winnerSurvivorRatio.add(
                    (long) (1000.0 * result.defenderSurvivors() / result.defenderCountStart()));
            acc.winnerSurvivorSamples.increment();
        }
    }

    private static List<Stack> buildStacks(List<de.zettsystems.h3comsim.armybattle.values.StackSpec> specs,
                                           Side side) {
        int total = specs.size();
        List<Stack> stacks = new ArrayList<>(total);
        for (int slot = 0; slot < total; slot++) {
            var spec = specs.get(slot);
            var unit = UnitCatalog.byName(spec.unitName()).orElseThrow();
            Hex pos = SpawnLayout.positionFor(side, slot, total);
            stacks.add(new Stack(unit, spec.count(), pos, side, slot));
        }
        return stacks;
    }

    private static Battlefield buildBattlefield(List<Stack> a, List<Stack> d, long seed) {
        Set<Hex> obstacles = new HashSet<>(
                ObstacleGenerator.generate(Battlefield.STANDARD, new Random(seed)));
        obstacles.removeAll(SpawnLayout.spawnHexesFor(a.size(), d.size()));
        return Battlefield.STANDARD.withObstacles(obstacles);
    }

    private static String key(Faction attacker, Faction defender) {
        return attacker.name() + "|" + defender.name();
    }

    private static Map<String, AggregatedPair> aggregate(Map<String, PairStats> raw) {
        Map<String, AggregatedPair> result = new java.util.LinkedHashMap<>();
        for (Faction a : FACTIONS_IN_ORDER) {
            for (Faction d : FACTIONS_IN_ORDER) {
                PairStats forward = raw.get(key(a, d));
                PairStats reverse = raw.get(key(d, a));
                long aWins = forward.attackerWins.sum() + reverse.defenderWins.sum();
                long dWins = forward.defenderWins.sum() + reverse.attackerWins.sum();
                long draws = forward.draws.sum() + reverse.draws.sum();
                long total = aWins + dWins + draws;
                double aWinRate = total == 0 ? 0.5 : (double) aWins / total;
                long turnSum = forward.turnSum.sum() + reverse.turnSum.sum();
                double avgTurns = total == 0 ? 0.0 : (double) turnSum / total;
                long survSamples = forward.winnerSurvivorSamples.sum() + reverse.winnerSurvivorSamples.sum();
                double avgWinnerSurv = survSamples == 0 ? 0.0
                        : (forward.winnerSurvivorRatio.sum() + reverse.winnerSurvivorRatio.sum())
                        / (double) survSamples / 1000.0;
                result.put(key(a, d),
                        new AggregatedPair(aWinRate, (int) total, avgTurns, avgWinnerSurv));
            }
        }
        return result;
    }

    private static String renderComparison(Map<String, AggregatedPair> greedy,
                                           Map<String, AggregatedPair> strategic,
                                           long greedyMs, long strategicMs) {
        StringBuilder sb = new StringBuilder(4096);
        sb.append("# Faction-vs-Faction Matrix: Greedy vs Strategic\n\n");
        sb.append("**Sims**: ").append(SEEDS_PER_PAIR * 2)
                .append(" pro Pairing (Roll-Swap). Greedy: ").append(greedyMs).append(" ms, ")
                .append("Strategic: ").append(strategicMs).append(" ms.\n\n");

        renderRanking(sb, "## Ranking Greedy", greedy);
        renderRanking(sb, "## Ranking Strategic", strategic);

        sb.append("## Ranking-Delta (Strategic Ø − Greedy Ø)\n\n");
        sb.append("| Faktion | Greedy Ø | Strategic Ø | Δ |\n");
        sb.append("|--|--|--|--|\n");
        List<Faction> deltaSorted = FACTIONS_IN_ORDER.stream()
                .sorted((a, b) -> Double.compare(
                        avgWin(strategic, b) - avgWin(greedy, b),
                        avgWin(strategic, a) - avgWin(greedy, a)))
                .toList();
        for (Faction f : deltaSorted) {
            double g = avgWin(greedy, f);
            double s = avgWin(strategic, f);
            sb.append("| ").append(abbr(f)).append(" | ")
                    .append(String.format(Locale.ROOT, "%.2f", g)).append(" | ")
                    .append(String.format(Locale.ROOT, "%.2f", s)).append(" | ")
                    .append(String.format(Locale.ROOT, "%+.2f", s - g)).append(" |\n");
        }
        sb.append("\n");

        renderMatrix(sb, "## Win-Rate-Matrix Strategic\n\n", strategic);
        renderMatrix(sb, "## Win-Rate-Matrix Greedy (Baseline)\n\n", greedy);

        sb.append("## Cells mit größtem Strategic-Sprung (|Δ| > 0.30)\n\n");
        sb.append("| Attacker | Defender | Greedy | Strategic | Δ |\n");
        sb.append("|--|--|--|--|--|\n");
        List<Map.Entry<String, Double>> jumps = new ArrayList<>();
        for (Map.Entry<String, AggregatedPair> e : strategic.entrySet()) {
            String[] parts = e.getKey().split("\\|");
            if (parts[0].equals(parts[1])) continue;
            double delta = e.getValue().aWinRate() - greedy.get(e.getKey()).aWinRate();
            if (Math.abs(delta) > 0.30) {
                jumps.add(Map.entry(e.getKey(), delta));
            }
        }
        jumps.sort((x, y) -> Double.compare(Math.abs(y.getValue()), Math.abs(x.getValue())));
        for (Map.Entry<String, Double> e : jumps) {
            String[] parts = e.getKey().split("\\|");
            sb.append("| ").append(abbr(Faction.valueOf(parts[0]))).append(" | ")
                    .append(abbr(Faction.valueOf(parts[1]))).append(" | ")
                    .append(String.format(Locale.ROOT, "%.2f", greedy.get(e.getKey()).aWinRate())).append(" | ")
                    .append(String.format(Locale.ROOT, "%.2f", strategic.get(e.getKey()).aWinRate())).append(" | ")
                    .append(String.format(Locale.ROOT, "%+.2f", e.getValue())).append(" |\n");
        }
        return sb.toString();
    }

    private static void renderRanking(StringBuilder sb, String title, Map<String, AggregatedPair> m) {
        sb.append(title).append("\n\n");
        sb.append("| Rang | Faktion | Ø Win-Rate |\n|--|--|--|\n");
        List<Faction> ranked = FACTIONS_IN_ORDER.stream()
                .sorted((a, b) -> Double.compare(avgWin(m, b), avgWin(m, a)))
                .toList();
        int r = 1;
        for (Faction f : ranked) {
            sb.append("| ").append(r++).append(" | ").append(abbr(f)).append(" | ")
                    .append(String.format(Locale.ROOT, "%.2f", avgWin(m, f))).append(" |\n");
        }
        sb.append("\n");
    }

    private static void renderMatrix(StringBuilder sb, String title, Map<String, AggregatedPair> m) {
        sb.append(title);
        sb.append("| | ");
        for (Faction d : FACTIONS_IN_ORDER) sb.append(abbr(d)).append(" | ");
        sb.append("\n|--|");
        for (int i = 0; i < FACTIONS_IN_ORDER.size(); i++) sb.append("---|");
        sb.append("\n");
        for (Faction a : FACTIONS_IN_ORDER) {
            sb.append("| **").append(abbr(a)).append("** | ");
            for (Faction d : FACTIONS_IN_ORDER) {
                sb.append(String.format(Locale.ROOT, "%.2f", m.get(key(a, d)).aWinRate())).append(" | ");
            }
            sb.append("\n");
        }
        sb.append("\n");
    }

    private static double avgWin(Map<String, AggregatedPair> aggregated, Faction f) {
        double sum = 0;
        int n = 0;
        for (Faction other : FACTIONS_IN_ORDER) {
            if (other == f) continue;
            sum += aggregated.get(key(f, other)).aWinRate();
            n++;
        }
        return sum / n;
    }

    private static String abbr(Faction f) {
        return switch (f) {
            case CASTLE -> "CAS";
            case RAMPART -> "RAM";
            case TOWER -> "TOW";
            case INFERNO -> "INF";
            case NECROPOLIS -> "NEC";
            case DUNGEON -> "DUN";
            case STRONGHOLD -> "STR";
            case FORTRESS -> "FOR";
            case CONFLUX -> "CON";
            case NEUTRAL -> "NEU";
        };
    }

    private static final class PairStats {
        final LongAdder total = new LongAdder();
        final LongAdder attackerWins = new LongAdder();
        final LongAdder defenderWins = new LongAdder();
        final LongAdder draws = new LongAdder();
        final LongAdder turnSum = new LongAdder();
        final LongAdder winnerSurvivorRatio = new LongAdder();
        final LongAdder winnerSurvivorSamples = new LongAdder();
    }

    private record AggregatedPair(double aWinRate, int totalSims, double avgTurns,
                                  double avgWinnerSurvivor) {
    }
}
