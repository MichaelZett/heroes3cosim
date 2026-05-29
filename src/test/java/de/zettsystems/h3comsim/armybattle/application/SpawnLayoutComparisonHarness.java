package de.zettsystems.h3comsim.armybattle.application;

import de.zettsystems.h3comsim.armybattle.values.FactionPresetDto;
import de.zettsystems.h3comsim.armybattle.values.StackSpec;
import de.zettsystems.h3comsim.battle.domain.Battle;
import de.zettsystems.h3comsim.battle.domain.BattleResult;
import de.zettsystems.h3comsim.battle.domain.BattleSetup;
import de.zettsystems.h3comsim.battle.domain.Battlefield;
import de.zettsystems.h3comsim.battle.domain.Faction;
import de.zettsystems.h3comsim.battle.domain.Hex;
import de.zettsystems.h3comsim.battle.domain.ObstacleGenerator;
import de.zettsystems.h3comsim.battle.domain.Stack;
import de.zettsystems.h3comsim.battle.domain.StrategicAutoSolver;
import de.zettsystems.h3comsim.battle.domain.Unit;
import de.zettsystems.h3comsim.battle.domain.UnitCatalog;
import de.zettsystems.h3comsim.battle.domain.events.Side;
import de.zettsystems.h3comsim.battle.domain.events.Winner;
import org.junit.jupiter.api.Disabled;
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

/**
 * Misst den isolierten Effekt der taktischen {@link SpawnLayout#assignPositions} gegenüber
 * der alten Slot-Index-Direktabbildung {@link SpawnLayout#positionFor}. Solver bleibt
 * konstant auf {@link StrategicAutoSolver} — was im Produktiv-Service läuft —, damit
 * Unterschiede ausschließlich aus der Aufstellung kommen.
 *
 * <p>9 × 9 × {@value #SEEDS_PER_PAIR} × 2 Layouts = 810 Sims bei SEEDS_PER_PAIR=5.
 * Report unter {@code build/reports/spawn-layout-comparison.md}.
 */
class SpawnLayoutComparisonHarness {

    private static final int SEEDS_PER_PAIR = 5;

    private static final List<Faction> FACTIONS_IN_ORDER = List.of(
            Faction.CASTLE, Faction.RAMPART, Faction.TOWER, Faction.INFERNO,
            Faction.NECROPOLIS, Faction.DUNGEON, Faction.STRONGHOLD, Faction.FORTRESS,
            Faction.CONFLUX);

    private enum LayoutMode { SLOT_DIRECT, TACTICAL }

    @Test
    @Disabled("Ad-hoc-Vergleich: Snapshot liegt in build/reports/spawn-layout-comparison.md. "
            + "Manuell aktivieren, wenn assignPositions/Tank-Pattern verändert wurden und ein "
            + "neuer Baseline-Vergleich erstellt werden soll.")
    void compare_layouts_and_write_report() throws IOException {
        FactionPresetCatalog presets = new FactionPresetCatalog();
        Map<Faction, FactionPresetDto> presetByFaction = new EnumMap<>(Faction.class);
        for (FactionPresetDto p : presets.all()) {
            presetByFaction.put(p.faction(), p);
        }

        long t0 = System.currentTimeMillis();
        Map<String, AggregatedPair> slotDirect = runMatrix(presetByFaction, LayoutMode.SLOT_DIRECT);
        long tSlot = System.currentTimeMillis() - t0;

        long t1 = System.currentTimeMillis();
        Map<String, AggregatedPair> tactical = runMatrix(presetByFaction, LayoutMode.TACTICAL);
        long tTactical = System.currentTimeMillis() - t1;

        String report = renderComparison(slotDirect, tactical, tSlot, tTactical);
        Path reportsDir = Path.of("build", "reports");
        Files.createDirectories(reportsDir);
        Path out = reportsDir.resolve("spawn-layout-comparison.md");
        Files.writeString(out, report);

        System.out.println("Spawn-Layout-Comparison-Report: " + out.toAbsolutePath());
        System.out.println(report);
    }

    private static Map<String, AggregatedPair> runMatrix(Map<Faction, FactionPresetDto> presets,
                                                         LayoutMode mode) {
        Map<String, PairStats> stats = new ConcurrentHashMap<>();
        for (Faction a : FACTIONS_IN_ORDER) {
            for (Faction d : FACTIONS_IN_ORDER) {
                stats.put(key(a, d), new PairStats());
            }
        }
        try (ExecutorService pool = Executors.newWorkStealingPool()) {
            for (Faction attacker : FACTIONS_IN_ORDER) {
                for (Faction defender : FACTIONS_IN_ORDER) {
                    List<StackSpec> attackerSpec = presets.get(attacker).stacks();
                    List<StackSpec> defenderSpec = presets.get(defender).stacks();
                    PairStats acc = stats.get(key(attacker, defender));
                    for (int s = 0; s < SEEDS_PER_PAIR; s++) {
                        long seed = (long) (attacker.ordinal() * 9929L + defender.ordinal() * 113L + s);
                        pool.execute(() -> runOne(attackerSpec, defenderSpec, seed, mode, acc));
                    }
                }
            }
        }
        return aggregate(stats);
    }

    private static void runOne(List<StackSpec> attacker, List<StackSpec> defender,
                               long seed, LayoutMode mode, PairStats acc) {
        List<Stack> attackerStacks = buildStacks(attacker, Side.ATTACKER, mode);
        List<Stack> defenderStacks = buildStacks(defender, Side.DEFENDER, mode);
        Battlefield bf = buildBattlefield(attackerStacks, defenderStacks, seed);
        BattleSetup setup = new BattleSetup(attackerStacks, defenderStacks, bf);
        BattleResult result = new Battle(new Random(seed), new StrategicAutoSolver()).simulate(setup);

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

    private static List<Stack> buildStacks(List<StackSpec> specs, Side side, LayoutMode mode) {
        int total = specs.size();
        List<Unit> units = new ArrayList<>(total);
        for (StackSpec spec : specs) {
            units.add(UnitCatalog.byName(spec.unitName()).orElseThrow());
        }
        List<Hex> positions = switch (mode) {
            case SLOT_DIRECT -> {
                List<Hex> p = new ArrayList<>(total);
                for (int slot = 0; slot < total; slot++) {
                    p.add(SpawnLayout.positionFor(side, slot, total));
                }
                yield p;
            }
            case TACTICAL -> SpawnLayout.assignPositions(side, units);
        };
        List<Stack> stacks = new ArrayList<>(total);
        for (int slot = 0; slot < total; slot++) {
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

    private static String renderComparison(Map<String, AggregatedPair> slot,
                                           Map<String, AggregatedPair> tactical,
                                           long slotMs, long tacticalMs) {
        StringBuilder sb = new StringBuilder(4096);
        sb.append("# Spawn-Layout-Vergleich: Slot-Direct vs Tactical\n\n");
        sb.append("**Sims**: ").append(SEEDS_PER_PAIR * 2)
                .append(" pro Pairing (Roll-Swap). Solver konstant: StrategicAutoSolver.\n");
        sb.append("**Layouts**: SLOT_DIRECT = positionFor (Slot 0 → r=0), ")
                .append("TACTICAL = assignPositions (Schützen außen, schnellster Melee zentriert).\n");
        sb.append("**Zeit**: Slot-Direct ").append(slotMs).append(" ms, Tactical ")
                .append(tacticalMs).append(" ms.\n\n");

        renderRanking(sb, "## Ranking Slot-Direct (Baseline)", slot);
        renderRanking(sb, "## Ranking Tactical", tactical);

        sb.append("## Ranking-Delta (Tactical Ø − Slot-Direct Ø)\n\n");
        sb.append("| Faktion | Slot-Direct Ø | Tactical Ø | Δ |\n");
        sb.append("|--|--|--|--|\n");
        List<Faction> deltaSorted = FACTIONS_IN_ORDER.stream()
                .sorted((a, b) -> Double.compare(
                        avgWin(tactical, b) - avgWin(slot, b),
                        avgWin(tactical, a) - avgWin(slot, a)))
                .toList();
        for (Faction f : deltaSorted) {
            double g = avgWin(slot, f);
            double t = avgWin(tactical, f);
            sb.append("| ").append(abbr(f)).append(" | ")
                    .append(String.format(Locale.ROOT, "%.2f", g)).append(" | ")
                    .append(String.format(Locale.ROOT, "%.2f", t)).append(" | ")
                    .append(String.format(Locale.ROOT, "%+.2f", t - g)).append(" |\n");
        }
        sb.append("\n");

        renderMatrix(sb, "## Win-Rate-Matrix Tactical\n\n", tactical);
        renderMatrix(sb, "## Win-Rate-Matrix Slot-Direct (Baseline)\n\n", slot);

        sb.append("## Cells mit größtem Layout-Effekt (|Δ| > 0.20)\n\n");
        sb.append("| Attacker | Defender | Slot-Direct | Tactical | Δ |\n");
        sb.append("|--|--|--|--|--|\n");
        List<Map.Entry<String, Double>> jumps = new ArrayList<>();
        for (Map.Entry<String, AggregatedPair> e : tactical.entrySet()) {
            String[] parts = e.getKey().split("\\|");
            if (parts[0].equals(parts[1])) continue;
            double delta = e.getValue().aWinRate() - slot.get(e.getKey()).aWinRate();
            if (Math.abs(delta) > 0.20) {
                jumps.add(Map.entry(e.getKey(), delta));
            }
        }
        jumps.sort((x, y) -> Double.compare(Math.abs(y.getValue()), Math.abs(x.getValue())));
        for (Map.Entry<String, Double> e : jumps) {
            String[] parts = e.getKey().split("\\|");
            sb.append("| ").append(abbr(Faction.valueOf(parts[0]))).append(" | ")
                    .append(abbr(Faction.valueOf(parts[1]))).append(" | ")
                    .append(String.format(Locale.ROOT, "%.2f", slot.get(e.getKey()).aWinRate())).append(" | ")
                    .append(String.format(Locale.ROOT, "%.2f", tactical.get(e.getKey()).aWinRate())).append(" | ")
                    .append(String.format(Locale.ROOT, "%+.2f", e.getValue())).append(" |\n");
        }

        sb.append("\n## Aggregat\n\n");
        double avgTurnsSlot = avgTurns(slot);
        double avgTurnsTac = avgTurns(tactical);
        double avgSurvSlot = avgWinnerSurv(slot);
        double avgSurvTac = avgWinnerSurv(tactical);
        sb.append("- Ø Runden bis Entscheidung: Slot-Direct ")
                .append(String.format(Locale.ROOT, "%.2f", avgTurnsSlot))
                .append(", Tactical ").append(String.format(Locale.ROOT, "%.2f", avgTurnsTac))
                .append(" (Δ ").append(String.format(Locale.ROOT, "%+.2f", avgTurnsTac - avgTurnsSlot))
                .append(")\n");
        sb.append("- Ø Survivor-Quote Sieger: Slot-Direct ")
                .append(String.format(Locale.ROOT, "%.2f", avgSurvSlot))
                .append(", Tactical ").append(String.format(Locale.ROOT, "%.2f", avgSurvTac))
                .append(" (Δ ").append(String.format(Locale.ROOT, "%+.2f", avgSurvTac - avgSurvSlot))
                .append(")\n");
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

    private static double avgTurns(Map<String, AggregatedPair> m) {
        double sum = 0;
        int n = 0;
        for (AggregatedPair p : m.values()) {
            sum += p.avgTurns();
            n++;
        }
        return n == 0 ? 0 : sum / n;
    }

    private static double avgWinnerSurv(Map<String, AggregatedPair> m) {
        double sum = 0;
        int n = 0;
        for (AggregatedPair p : m.values()) {
            sum += p.avgWinnerSurvivor();
            n++;
        }
        return n == 0 ? 0 : sum / n;
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
