package de.zettsystems.h3comsim.armybattle.application;

import de.zettsystems.h3comsim.armybattle.values.FactionPresetDto;
import de.zettsystems.h3comsim.armybattle.values.StackSpec;
import de.zettsystems.h3comsim.battle.domain.Action;
import de.zettsystems.h3comsim.battle.domain.AttackType;
import de.zettsystems.h3comsim.battle.domain.AutoSolver;
import de.zettsystems.h3comsim.battle.domain.Battle;
import de.zettsystems.h3comsim.battle.domain.BattleResult;
import de.zettsystems.h3comsim.battle.domain.BattleSetup;
import de.zettsystems.h3comsim.battle.domain.Battlefield;
import de.zettsystems.h3comsim.battle.domain.Faction;
import de.zettsystems.h3comsim.battle.domain.GreedyAutoSolver;
import de.zettsystems.h3comsim.battle.domain.HeroCatalog;
import de.zettsystems.h3comsim.battle.domain.Hex;
import de.zettsystems.h3comsim.battle.domain.ObstacleGenerator;
import de.zettsystems.h3comsim.battle.domain.Stack;
import de.zettsystems.h3comsim.battle.domain.StrategicAutoSolver;
import de.zettsystems.h3comsim.battle.domain.Unit;
import de.zettsystems.h3comsim.battle.domain.UnitCatalog;
import de.zettsystems.h3comsim.battle.domain.events.Side;
import de.zettsystems.h3comsim.battle.domain.events.Winner;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Supplier;

/**
 * Lässt zwei Varianten <strong>direkt gegeneinander</strong> antreten: zwei Solver, zwei
 * Aufstellungen oder beides.
 *
 * <p>Motivation: Die {@code FactionMatrixTest} beantwortet die Frage nach der Solver-Qualität
 * <em>nicht</em>. Dort spielen beide Seiten denselben Solver, eine Verbesserung hebt also beide
 * Seiten gleichermaßen an und bleibt in der Win-Rate unsichtbar. Wer wissen will, ob eine neue
 * Heuristik trägt, muss sie gegen die Variante ohne sie stellen — und gegen Greedy, das am
 * 2026-10-26 den alten Strategic-Solver mit 0.603 schlug, ohne dass die Matrix es zeigte.
 *
 * <p>Aufbau: {@link SplitSolver} routet nach {@code Stack.side()} — Attacker bekommt Variante A,
 * Defender Variante B; jede Seite stellt ihre Armee mit dem {@link Layout} ihrer Variante auf.
 * Alle 81 Pairings laufen mit getauschten Rollen, damit der Attacker-Vorteil (bei
 * Speed-Gleichstand zu Kampfbeginn zieht der Attacker zuerst) sich herausmittelt. Beide Armeen
 * führt wie im Endpunkt der Standardheld ihrer Fraktion. Gemessen wird die Win-Rate von A;
 * 0.50 heißt „kein Unterschied". Zusätzlich je Faktion das Spiegel-Duell (identische Armee
 * beiderseits) — eine Aufschlüsselung des Gesamtlaufs nach Faktion enthielte die
 * Faktionsstärke, nicht den Effekt der Variante.
 *
 * <p>Aktivieren: {@code .\gradlew.bat test --tests "*SolverDuelTest" "-Ph3.harness=solver-duel"},
 * einzelne Duelle mit {@code "-Ph3.duel=strategic-vs-greedy,shooters-inside"}.
 * Report unter {@code build/reports/solver-duel.md}.
 */
@EnabledIfSystemProperty(named = "h3.harness", matches = "solver-duel")
class SolverDuelTest {

    private static final int SEEDS_PER_PAIR = 40;

    private static final List<Faction> FACTIONS_IN_ORDER = List.of(
            Faction.CASTLE, Faction.RAMPART, Faction.TOWER, Faction.INFERNO,
            Faction.NECROPOLIS, Faction.DUNGEON, Faction.STRONGHOLD, Faction.FORTRESS,
            Faction.CONFLUX);

    /** Reihen-Zuteilung einer Armee, wie {@link SpawnLayout#assignPositions(Side, List)}. */
    @FunctionalInterface
    interface Layout {
        List<Hex> place(Side side, List<Unit> units);
    }

    /** Eine Seite im Duell: Solver und Aufstellung. */
    record Contender(String name, Supplier<AutoSolver> solver, Layout layout) {
    }

    /** Zwei Varianten; die Win-Rate wird für {@code a} gezählt. */
    record Duel(String key, Contender a, Contender b) {
    }

    private static final Layout TACTICAL = SpawnLayout::assignPositions;

    private static final List<Duel> DUELS = List.of(
            new Duel("strategic-vs-greedy",
                    new Contender("Strategic", StrategicAutoSolver::new, TACTICAL),
                    new Contender("Greedy", GreedyAutoSolver::new, TACTICAL)),
            new Duel("tactical-wait",
                    new Contender("Strategic mit Wait", () -> new StrategicAutoSolver(true), TACTICAL),
                    new Contender("Strategic ohne Wait", () -> new StrategicAutoSolver(false), TACTICAL)),
            new Duel("shooters-inside",
                    new Contender("Schützen innen", StrategicAutoSolver::new, shootersFirstOn(SolverDuelTest::centerOut)),
                    new Contender("Schützen außen", StrategicAutoSolver::new, TACTICAL)),
            new Duel("shooters-second-ring",
                    new Contender("Schützen auf r=2/8", StrategicAutoSolver::new, shootersFirstOn(SolverDuelTest::secondRing)),
                    new Contender("Schützen außen", StrategicAutoSolver::new, TACTICAL)));

    @Test
    void duels() throws Exception {
        Map<Faction, FactionPresetDto> presets = new EnumMap<>(Faction.class);
        for (FactionPresetDto p : new FactionPresetCatalog().all()) {
            presets.put(p.faction(), p);
        }
        Set<String> selected = selectedDuels();
        StringBuilder report = new StringBuilder(4096).append("# Solver-Duelle\n\n")
                .append("Variante A gegen Variante B, alle 9x9 Pairings mit getauschten Rollen, ")
                .append(SEEDS_PER_PAIR).append(" Seeds je Pairing, Standardheld je Armee. ")
                .append("Win-Rate von A; 0.50 = kein Unterschied, ab etwa 2 Sigma kein Zufall mehr.\n\n");
        for (Duel duel : DUELS) {
            if (selected.isEmpty() || selected.contains(duel.key())) {
                render(report, duel, run(presets, duel));
            }
        }
        Path reportsDir = Path.of("build", "reports");
        Files.createDirectories(reportsDir);
        Path out = reportsDir.resolve("solver-duel.md");
        Files.writeString(out, report);
        IO.println("Solver-Duell-Report: " + out.toAbsolutePath());
        IO.println(report);
    }

    private static Set<String> selectedDuels() {
        String raw = System.getProperty("h3.duel", "");
        return raw.isBlank() ? Set.of() : Set.of(raw.split(","));
    }

    private record Result(Tally overall, Map<Faction, Tally> mirror) {
    }

    private static Result run(Map<Faction, FactionPresetDto> presets, Duel duel) {
        Tally overall = new Tally();
        Map<Faction, Tally> mirror = new LinkedHashMap<>();
        FACTIONS_IN_ORDER.forEach(f -> mirror.put(f, new Tally()));
        try (ExecutorService pool = Executors.newWorkStealingPool()) {
            for (Faction a : FACTIONS_IN_ORDER) {
                for (Faction d : FACTIONS_IN_ORDER) {
                    for (int s = 0; s < SEEDS_PER_PAIR; s++) {
                        long seed = (long) a.ordinal() * 9929L + d.ordinal() * 113L + s;
                        for (boolean aAttacks : new boolean[]{true, false}) {
                            Tally perFaction = a == d ? mirror.get(a) : null;
                            pool.execute(() -> tally(fight(presets, duel, a, d, seed, aAttacks),
                                    aAttacks, overall, perFaction));
                        }
                    }
                }
            }
        }
        return new Result(overall, mirror);
    }

    private static BattleResult fight(Map<Faction, FactionPresetDto> presets, Duel duel,
                                      Faction attackerFaction, Faction defenderFaction, long seed,
                                      boolean aAttacks) {
        Contender attacker = aAttacks ? duel.a() : duel.b();
        Contender defender = aAttacks ? duel.b() : duel.a();
        List<Stack> attackerStacks = buildStacks(presets.get(attackerFaction).stacks(), Side.ATTACKER,
                attacker.layout());
        List<Stack> defenderStacks = buildStacks(presets.get(defenderFaction).stacks(), Side.DEFENDER,
                defender.layout());
        BattleSetup setup = new BattleSetup(attackerStacks, defenderStacks,
                buildBattlefield(attackerStacks, defenderStacks, seed),
                HeroCatalog.defaultForArmy(attackerStacks), HeroCatalog.defaultForArmy(defenderStacks));
        AutoSolver split = new SplitSolver(attacker.solver().get(), defender.solver().get());
        return new Battle(new Random(seed), split).simulate(setup);
    }

    private static void tally(BattleResult result, boolean aAttacks, Tally overall,
                              @Nullable Tally perFaction) {
        Winner aWins = aAttacks ? Winner.ATTACKER : Winner.DEFENDER;
        for (Tally t : perFaction == null ? List.of(overall) : List.of(overall, perFaction)) {
            t.battles.increment();
            if (result.winner() == Winner.DRAW) {
                t.draws.increment();
            } else if (result.winner() == aWins) {
                t.aWins.increment();
            } else {
                t.aLosses.increment();
            }
        }
    }

    /**
     * Routet die Solver-Aufrufe nach Seite. {@code planRound} geht bewusst an beide — jeder
     * Solver hält eigenen Rundenzustand ({@code currentSetup}, Team-Plan), der sonst veraltet.
     */
    private record SplitSolver(AutoSolver attackerSolver, AutoSolver defenderSolver)
            implements AutoSolver {

        @Override
        public void planRound(BattleSetup setup) {
            attackerSolver.planRound(setup);
            defenderSolver.planRound(setup);
        }

        @Override
        public @Nullable Stack pickTarget(Stack active, List<Stack> opponents, Battlefield bf) {
            return solverFor(active).pickTarget(active, opponents, bf);
        }

        @Override
        public Action decide(Stack active, Stack opponent, Battlefield bf) {
            return solverFor(active).decide(active, opponent, bf);
        }

        @Override
        public boolean attacksWarMachines(Stack active) {
            return solverFor(active).attacksWarMachines(active);
        }

        @Override
        public boolean shootsWalls(Stack active, BattleSetup setup) {
            return solverFor(active).shootsWalls(active, setup);
        }

        private AutoSolver solverFor(Stack active) {
            return active.side() == Side.ATTACKER ? attackerSolver : defenderSolver;
        }
    }

    // ------------------------------------------------------------------ //
    // Aufstellungs-Varianten
    // ------------------------------------------------------------------ //

    /**
     * Wie {@link SpawnLayout#assignPositions}, nur bekommen die Schützen (schwächster zuerst)
     * die Reihen in der Reihenfolge {@code shooterRows}; die Nahkämpfer füllen den Rest von der
     * Mitte aus, der schnellste zuerst.
     */
    private static Layout shootersFirstOn(java.util.function.Function<int[], List<Integer>> shooterRows) {
        return (side, units) -> {
            int[] rows = rowsFor(units.size());
            List<Integer> shooterOrder = shooterRows.apply(rows);
            List<Integer> meleeOrder = centerOut(rows);
            List<Integer> shooters = new ArrayList<>();
            List<Integer> melees = new ArrayList<>();
            for (int i = 0; i < units.size(); i++) {
                (units.get(i).attackType() == AttackType.LONG_RANGE ? shooters : melees).add(i);
            }
            shooters.sort(Comparator.comparingInt((Integer slot) -> units.get(slot).health()));
            melees.sort(Comparator.comparingInt((Integer slot) -> -units.get(slot).speed()));
            Hex[] result = new Hex[units.size()];
            Set<Integer> used = new HashSet<>();
            for (int i = 0; i < shooters.size(); i++) {
                int row = shooterOrder.get(i);
                used.add(row);
                result[shooters.get(i)] = SpawnLayout.edgeHex(side, row);
            }
            int next = 0;
            for (int slot : melees) {
                while (used.contains(meleeOrder.get(next))) {
                    next++;
                }
                result[slot] = SpawnLayout.edgeHex(side, meleeOrder.get(next++));
            }
            return Arrays.asList(result);
        };
    }

    private static List<Integer> centerOut(int[] rows) {
        return Arrays.stream(rows).boxed()
                .sorted(Comparator.<Integer>comparingInt(r -> Math.abs(r - SpawnLayout.CENTER_ROW))
                        .thenComparingInt(r -> r))
                .toList();
    }

    /** Erst die Reihen 2/8 (eine Reihe Nahkämpfer zwischen Schütze und Rand), dann wie innen. */
    private static List<Integer> secondRing(int[] rows) {
        return Arrays.stream(rows).boxed()
                .sorted(Comparator.<Integer>comparingInt(r -> Math.abs(Math.abs(r - SpawnLayout.CENTER_ROW) - 3))
                        .thenComparingInt(r -> r))
                .toList();
    }

    private static int[] rowsFor(int total) {
        int start = (SpawnLayout.ROWS_7.length - total) / 2;
        return Arrays.copyOfRange(SpawnLayout.ROWS_7, start, start + total);
    }

    private static List<Stack> buildStacks(List<StackSpec> specs, Side side, Layout layout) {
        List<Unit> units = new ArrayList<>(specs.size());
        for (StackSpec spec : specs) {
            units.add(UnitCatalog.byName(spec.unitName()).orElseThrow());
        }
        List<Hex> positions = layout.place(side, units);
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

    // ------------------------------------------------------------------ //
    // Report
    // ------------------------------------------------------------------ //

    private static void render(StringBuilder sb, Duel duel, Result result) {
        sb.append("## ").append(duel.a().name()).append(" gegen ").append(duel.b().name())
                .append(" (`").append(duel.key()).append("`)\n\n");
        sb.append("| | Battles | Siege A | Niederlagen A | Draws | Win-Rate A | Sigma |\n");
        sb.append("|--|--|--|--|--|--|--|\n");
        row(sb, "Gesamt", result.overall());
        for (Map.Entry<Faction, Tally> e : result.mirror().entrySet()) {
            row(sb, "Spiegel " + e.getKey().name(), e.getValue());
        }
        sb.append('\n');
    }

    private static void row(StringBuilder sb, String label, Tally t) {
        long wins = t.aWins.sum();
        long losses = t.aLosses.sum();
        long decided = wins + losses;
        String rate = decided == 0 ? "-" : String.format(Locale.ROOT, "%.3f", (double) wins / decided);
        // Binomialverteilung mit p = 0.5: Standardabweichung sqrt(n / 4).
        String sigma = decided == 0 ? "-"
                : String.format(Locale.ROOT, "%+.2f", (wins - decided / 2.0) / Math.sqrt(decided / 4.0));
        sb.append("| ").append(label).append(" | ").append(t.battles.sum()).append(" | ").append(wins)
                .append(" | ").append(losses).append(" | ").append(t.draws.sum()).append(" | ")
                .append(rate).append(" | ").append(sigma).append(" |\n");
    }

    private static final class Tally {
        private final LongAdder battles = new LongAdder();
        private final LongAdder aWins = new LongAdder();
        private final LongAdder aLosses = new LongAdder();
        private final LongAdder draws = new LongAdder();
    }
}
