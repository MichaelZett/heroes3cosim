package de.zettsystems.h3comsim.application.experiment;

import de.zettsystems.h3comsim.application.Battle;
import de.zettsystems.h3comsim.application.BattleResult;
import de.zettsystems.h3comsim.application.BattleSetup;
import de.zettsystems.h3comsim.application.GreedyAutoSolver;
import de.zettsystems.h3comsim.domain.Battlefield;
import de.zettsystems.h3comsim.domain.Hex;
import de.zettsystems.h3comsim.domain.ObstacleGenerator;
import de.zettsystems.h3comsim.domain.Unit;
import de.zettsystems.h3comsim.domain.UnitCatalog;
import de.zettsystems.h3comsim.domain.events.Winner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;

@Service
public class DefaultMatrixExperimentService implements MatrixExperimentService {

    private static final Hex ATTACKER_SPAWN = new Hex(0, 5);
    private static final Hex DEFENDER_SPAWN = new Hex(14, 5);

    private final int parallelismPercent;

    public DefaultMatrixExperimentService(
            @Value("${h3.experiment.parallelism-percent:50}") int parallelismPercent) {
        if (parallelismPercent < 1 || parallelismPercent > 100) {
            throw new IllegalArgumentException(
                    "h3.experiment.parallelism-percent must be 1..100, was " + parallelismPercent);
        }
        this.parallelismPercent = parallelismPercent;
    }

    @Override
    public MatrixReport run(MatrixRequest request, ProgressListener listener) {
        long startNs = System.nanoTime();
        List<Unit> participants = UnitCatalog.all().stream()
                .filter(u -> !request.excludeUnits().contains(u.name()))
                .filter(u -> !request.excludeFactions().contains(u.faction()))
                .filter(u -> !request.excludeTiers().contains(u.tier()))
                .toList();

        // Pro Unit ein Akkumulator, der von beliebig vielen Threads befüllt wird.
        List<Accumulator> accumulators = participants.stream()
                .map(Accumulator::new)
                .toList();

        int totalSims = participants.size() * (participants.size() - 1) / 2
                * request.seedsPerMatchup() * 2;
        AtomicInteger completedCounter = new AtomicInteger();

        int threads = Math.max(1, Runtime.getRuntime().availableProcessors() * parallelismPercent / 100);
        ExecutorService pool = Executors.newWorkStealingPool(threads);
        try {
            List<Runnable> tasks = buildTasks(
                    participants, accumulators, request, completedCounter, totalSims, listener);
            List<java.util.concurrent.Future<?>> futures = new ArrayList<>(tasks.size());
            for (Runnable task : tasks) {
                futures.add(pool.submit(task));
            }
            for (var future : futures) {
                future.get();
            }
        } catch (Exception ex) {
            throw new IllegalStateException("Matrix experiment failed", ex);
        } finally {
            pool.shutdown();
        }

        List<UnitMatchupStats> stats = accumulators.stream()
                .map(Accumulator::toStats)
                .sorted(Comparator.comparingDouble(UnitMatchupStats::winRate).reversed())
                .toList();

        List<FactionMatchupStats> factionStats = aggregateByFaction(stats);
        List<TierAnomaly> anomalies = detectAnomalies(accumulators);

        long elapsedMs = (System.nanoTime() - startNs) / 1_000_000L;
        return new MatrixReport(
                participants.size() * (participants.size() - 1) / 2,
                request.seedsPerMatchup(),
                request.unitCount(),
                elapsedMs,
                stats,
                factionStats,
                anomalies);
    }

    private static List<FactionMatchupStats> aggregateByFaction(List<UnitMatchupStats> stats) {
        return stats.stream()
                .collect(java.util.stream.Collectors.groupingBy(UnitMatchupStats::faction))
                .entrySet().stream()
                .map(entry -> {
                    var rows = entry.getValue();
                    int total = rows.stream().mapToInt(UnitMatchupStats::totalSims).sum();
                    int wins = rows.stream().mapToInt(UnitMatchupStats::wins).sum();
                    int losses = rows.stream().mapToInt(UnitMatchupStats::losses).sum();
                    int draws = rows.stream().mapToInt(UnitMatchupStats::draws).sum();
                    double winRate = total == 0 ? 0.0 : (double) wins / total;
                    // Avg-Survivor wird über den Einheiten der Faktion gemittelt (gleiche Gewichtung
                    // pro Unit, unabhängig davon wie viele Gegner sie hatte).
                    double avgSurvivor = rows.stream()
                            .mapToDouble(UnitMatchupStats::avgSurvivorRatio).average().orElse(0.0);
                    return new FactionMatchupStats(
                            entry.getKey(), rows.size(), total, wins, losses, draws, winRate, avgSurvivor);
                })
                .sorted(Comparator.comparingDouble(FactionMatchupStats::winRate).reversed())
                .toList();
    }

    private static List<Runnable> buildTasks(List<Unit> participants,
                                             List<Accumulator> accumulators,
                                             MatrixRequest request,
                                             AtomicInteger completedCounter,
                                             int totalSims,
                                             ProgressListener listener) {
        List<Runnable> tasks = new ArrayList<>();
        for (int i = 0; i < participants.size(); i++) {
            for (int j = i + 1; j < participants.size(); j++) {
                Unit a = participants.get(i);
                Unit b = participants.get(j);
                Accumulator accA = accumulators.get(i);
                Accumulator accB = accumulators.get(j);
                int pairBase = (i * 31 + j) * 0x9E37; // stabiler Seed-Offset je Pair
                tasks.add(() -> simulatePair(
                        a, b, accA, accB, request, pairBase, completedCounter, totalSims, listener));
            }
        }
        return tasks;
    }

    private static void simulatePair(Unit a, Unit b,
                                     Accumulator accA, Accumulator accB,
                                     MatrixRequest request, int pairBase,
                                     AtomicInteger completedCounter, int totalSims,
                                     ProgressListener listener) {
        for (int s = 0; s < request.seedsPerMatchup(); s++) {
            long seed = (long) pairBase * 1_000_000L + s;
            // Rolle 1: a = Attacker, b = Defender
            runOne(a, b, accA, accB, seed, request.unitCount());
            listener.onProgress(completedCounter.incrementAndGet(), totalSims);
            // Rolle 2: b = Attacker, a = Defender — gleicher Seed, getauschte Rollen
            runOne(b, a, accB, accA, seed, request.unitCount());
            listener.onProgress(completedCounter.incrementAndGet(), totalSims);
        }
    }

    private static void runOne(Unit attackerUnit, Unit defenderUnit,
                               Accumulator attackerAcc, Accumulator defenderAcc,
                               long seed, int unitCount) {
        Battlefield battlefield = Battlefield.STANDARD.withObstacles(
                ObstacleGenerator.generate(Battlefield.STANDARD, new Random(seed)));
        BattleSetup setup = new BattleSetup(attackerUnit, unitCount, defenderUnit, unitCount,
                battlefield, ATTACKER_SPAWN, DEFENDER_SPAWN);
        BattleResult result = new Battle(new Random(seed), new GreedyAutoSolver()).simulate(setup);

        double attackerSurvivorRatio = ratio(result.attackerSurvivors(), unitCount);
        double defenderSurvivorRatio = ratio(result.defenderSurvivors(), unitCount);
        attackerAcc.record(result.winner(), Winner.ATTACKER, attackerSurvivorRatio, defenderUnit.tier());
        defenderAcc.record(result.winner(), Winner.DEFENDER, defenderSurvivorRatio, attackerUnit.tier());
    }

    private static double ratio(int survivors, int start) {
        return start == 0 ? 0.0 : (double) survivors / start;
    }

    private static List<TierAnomaly> detectAnomalies(List<Accumulator> accumulators) {
        List<TierAnomaly> anomalies = new ArrayList<>();
        for (Accumulator acc : accumulators) {
            if (acc.unit.tier() <= 1) {
                continue;
            }
            int lowerTier = acc.unit.tier() - 1;
            int wins = acc.winsAgainstTier(lowerTier);
            int totalAgainst = acc.simsAgainstTier(lowerTier);
            if (totalAgainst == 0) {
                continue;
            }
            double winRate = (double) wins / totalAgainst;
            if (winRate < 0.5) {
                anomalies.add(new TierAnomaly(acc.unit.name(), acc.unit.tier(), lowerTier,
                        winRate, totalAgainst));
            }
        }
        anomalies.sort(Comparator.comparingDouble(TierAnomaly::winRate));
        return anomalies;
    }

    private static final class Accumulator {
        private final Unit unit;
        private final LongAdder wins = new LongAdder();
        private final LongAdder losses = new LongAdder();
        private final LongAdder draws = new LongAdder();
        private final LongAdder totalSims = new LongAdder();
        // Survivor-Ratio als ppm (1.0 = 1_000_000), damit wir LongAdder nutzen können.
        private final LongAdder survivorRatioPpmSum = new LongAdder();
        // Tier-spezifische Wins (für Anomalien). Indiziert über Gegner-Tier 1..7.
        private final LongAdder[] winsByOpponentTier = new LongAdder[8];
        private final LongAdder[] simsByOpponentTier = new LongAdder[8];

        Accumulator(Unit unit) {
            this.unit = unit;
            for (int i = 0; i < winsByOpponentTier.length; i++) {
                winsByOpponentTier[i] = new LongAdder();
                simsByOpponentTier[i] = new LongAdder();
            }
        }

        void record(Winner winner, Winner ownSide, double survivorRatio, int opponentTier) {
            totalSims.increment();
            survivorRatioPpmSum.add(Math.round(survivorRatio * 1_000_000.0));
            simsByOpponentTier[opponentTier].increment();
            if (winner == Winner.DRAW) {
                draws.increment();
            } else if (winner == ownSide) {
                wins.increment();
                winsByOpponentTier[opponentTier].increment();
            } else {
                losses.increment();
            }
        }

        int winsAgainstTier(int tier) {
            return winsByOpponentTier[tier].intValue();
        }

        int simsAgainstTier(int tier) {
            return simsByOpponentTier[tier].intValue();
        }

        UnitMatchupStats toStats() {
            long total = totalSims.sum();
            long winsLong = wins.sum();
            double winRate = total == 0 ? 0.0 : (double) winsLong / total;
            double avgSurvivor = total == 0
                    ? 0.0
                    : (survivorRatioPpmSum.sum() / 1_000_000.0) / total;
            return new UnitMatchupStats(
                    unit.name(),
                    unit.faction(),
                    unit.tier(),
                    unit.upgrade(),
                    (int) total,
                    (int) winsLong,
                    losses.intValue(),
                    draws.intValue(),
                    winRate,
                    avgSurvivor);
        }
    }

}
