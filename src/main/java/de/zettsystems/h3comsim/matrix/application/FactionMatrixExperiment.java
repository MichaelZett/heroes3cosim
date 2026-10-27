package de.zettsystems.h3comsim.matrix.application;

import de.zettsystems.h3comsim.armybattle.application.ArmyBattleService;
import de.zettsystems.h3comsim.armybattle.application.FactionPresetCatalog;
import de.zettsystems.h3comsim.armybattle.values.ArmyBattleRequest;
import de.zettsystems.h3comsim.armybattle.values.ArmySpec;
import de.zettsystems.h3comsim.armybattle.values.StackSpec;
import de.zettsystems.h3comsim.battle.domain.BattleResult;
import de.zettsystems.h3comsim.battle.domain.Faction;
import de.zettsystems.h3comsim.battle.domain.SolverKind;
import de.zettsystems.h3comsim.matrix.values.FactionMatrixReport;
import de.zettsystems.h3comsim.matrix.values.FactionMatrixRequest;
import de.zettsystems.h3comsim.matrix.values.FactionPairing;
import de.zettsystems.h3comsim.matrix.values.FactionStanding;
import de.zettsystems.h3comsim.matrix.values.SolverMatrix;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;

/**
 * Rechnet die Faction-Matrix: jede Wochenproduktion aus den Presets gegen jede, in beiden Rollen
 * und im Spiegel, je Solver. Jeder Kampf läuft wie im Army-Battle-Endpunkt (Gelände, Hindernisse,
 * Standardheld) über {@link ArmyBattleService#outcome}. Der Seed hängt nur an Paarung und Nummer,
 * nicht am Solver — beide Solver kämpfen auf denselben Feldern.
 */
@Component
class FactionMatrixExperiment {

    private final ArmyBattleService battles;
    private final FactionPresetCatalog presets;
    private final int parallelismPercent;

    FactionMatrixExperiment(ArmyBattleService battles, FactionPresetCatalog presets,
                            @Value("${h3.experiment.parallelism-percent:50}") int parallelismPercent) {
        if (parallelismPercent < 1 || parallelismPercent > 100) {
            throw new IllegalArgumentException(
                    "h3.experiment.parallelism-percent must be 1..100, was " + parallelismPercent);
        }
        this.battles = battles;
        this.presets = presets;
        this.parallelismPercent = parallelismPercent;
    }

    FactionMatrixReport run(FactionMatrixRequest request, ProgressListener listener) {
        long start = System.nanoTime();
        List<Faction> factions = Arrays.stream(Faction.values()).filter(request.factions()::contains).toList();
        List<ArmySpec> armies = factions.stream().map(faction -> army(faction, request)).toList();
        AtomicInteger completed = new AtomicInteger();
        int total = request.totalSims();
        List<SolverMatrix> matrices = new ArrayList<>();
        for (SolverKind solver : SolverKind.values()) {
            if (request.solvers().contains(solver)) {
                long solverStart = System.nanoTime();
                Cell[][] cells = play(factions, armies, request, solver,
                        () -> listener.onProgress(completed.incrementAndGet(), total));
                matrices.add(matrix(solver, factions, cells, (System.nanoTime() - solverStart) / 1_000_000L));
            }
        }
        return new FactionMatrixReport(factions, request.seedsPerPair(), request.weeks(), request.heroes(),
                request.terrain(), request.magicTerrain(), matrices, (System.nanoTime() - start) / 1_000_000L);
    }

    /** Die Wochenproduktion der Fraktion, vervielfacht, mit ihrem Standardhelden oder ohne Helden. */
    private ArmySpec army(Faction faction, FactionMatrixRequest request) {
        List<StackSpec> stacks = presets.byFaction(faction).stacks().stream()
                .map(stack -> new StackSpec(stack.unitName(), stack.count() * request.weeks()))
                .toList();
        return request.heroes() ? new ArmySpec(stacks) : ArmySpec.withoutHero(stacks);
    }

    /** Alle Kämpfe eines Solvers; Zelle [i][j]: Fraktion i greift Fraktion j an. */
    // Der Seed hängt wie im FactionMatrixTest an der Stelle der Fraktion im Enum.
    @SuppressWarnings("EnumOrdinal")
    private Cell[][] play(List<Faction> factions, List<ArmySpec> armies, FactionMatrixRequest request,
                          SolverKind solver, Runnable done) {
        int n = factions.size();
        Cell[][] cells = new Cell[n][n];
        int threads = Math.max(1, Runtime.getRuntime().availableProcessors() * parallelismPercent / 100);
        try (ExecutorService pool = Executors.newWorkStealingPool(threads)) {
            List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    Cell cell = new Cell();
                    cells[i][j] = cell;
                    for (int s = 0; s < request.seedsPerPair(); s++) {
                        long seed = factions.get(i).ordinal() * 9929L + factions.get(j).ordinal() * 113L + s;
                        ArmyBattleRequest battle = new ArmyBattleRequest(armies.get(i), armies.get(j),
                                seed, null, null, request.terrain(), request.magicTerrain(), null);
                        futures.add(pool.submit(() -> {
                            cell.add(battles.outcome(battle, seed, solver));
                            done.run();
                        }));
                    }
                }
            }
            await(futures);
        }
        return cells;
    }

    private static void await(List<Future<?>> futures) {
        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Faction matrix interrupted", ie);
            } catch (ExecutionException ee) {
                throw new IllegalStateException("Faction matrix failed", ee);
            }
        }
    }

    private static SolverMatrix matrix(SolverKind solver, List<Faction> factions, Cell[][] cells, long elapsedMs) {
        List<FactionPairing> pairings = new ArrayList<>();
        List<FactionStanding> standings = new ArrayList<>();
        double bias = 0;
        for (int i = 0; i < factions.size(); i++) {
            Tally total = new Tally();
            for (int j = 0; j < factions.size(); j++) {
                Tally tally = i == j ? cells[i][j].asAttacker()
                        : cells[i][j].asAttacker().plus(cells[j][i].asDefender());
                pairings.add(tally.pairing(factions.get(i), factions.get(j)));
                if (i == j) {
                    bias += Math.abs(tally.winRate() - 0.5);
                } else {
                    total = total.plus(tally);
                }
            }
            standings.add(total.standing(factions.get(i)));
        }
        standings.sort(Comparator.comparingDouble(FactionStanding::winRate).reversed());
        return new SolverMatrix(solver, standings, pairings, bias / factions.size(), elapsedMs);
    }

    /** Die Kämpfe einer geordneten Paarung (Angreifer, Verteidiger), aus mehreren Threads gefüllt. */
    private static final class Cell {
        private final LongAdder attackerWins = new LongAdder();
        private final LongAdder defenderWins = new LongAdder();
        private final LongAdder draws = new LongAdder();
        private final LongAdder rounds = new LongAdder();
        private final LongAdder attackerSurvivorMille = new LongAdder();
        private final LongAdder defenderSurvivorMille = new LongAdder();

        void add(BattleResult result) {
            rounds.add(result.turnsTaken());
            switch (result.winner()) {
                case ATTACKER -> {
                    attackerWins.increment();
                    attackerSurvivorMille.add(1000L * result.attackerSurvivors() / result.attackerCountStart());
                }
                case DEFENDER -> {
                    defenderWins.increment();
                    defenderSurvivorMille.add(1000L * result.defenderSurvivors() / result.defenderCountStart());
                }
                case DRAW -> draws.increment();
            }
        }

        Tally asAttacker() {
            return new Tally(attackerWins.sum(), defenderWins.sum(), draws.sum(), rounds.sum(),
                    attackerSurvivorMille.sum());
        }

        Tally asDefender() {
            return new Tally(defenderWins.sum(), attackerWins.sum(), draws.sum(), rounds.sum(),
                    defenderSurvivorMille.sum());
        }
    }

    /** Kämpfe aus Sicht einer Fraktion. */
    private record Tally(long wins, long losses, long draws, long rounds, long survivorMille) {

        Tally() {
            this(0, 0, 0, 0, 0);
        }

        long sims() {
            return wins + losses + draws;
        }

        double winRate() {
            return sims() == 0 ? 0.0 : (double) wins / sims();
        }

        Tally plus(Tally other) {
            return new Tally(wins + other.wins, losses + other.losses, draws + other.draws,
                    rounds + other.rounds, survivorMille + other.survivorMille);
        }

        FactionPairing pairing(Faction faction, Faction opponent) {
            return new FactionPairing(faction, opponent, (int) sims(), (int) wins, (int) losses, (int) draws,
                    winRate(), sims() == 0 ? 0.0 : (double) rounds / sims());
        }

        FactionStanding standing(Faction faction) {
            return new FactionStanding(faction, (int) sims(), (int) wins, (int) losses, (int) draws, winRate(),
                    wins == 0 ? 0.0 : survivorMille / 1000.0 / wins);
        }
    }
}
