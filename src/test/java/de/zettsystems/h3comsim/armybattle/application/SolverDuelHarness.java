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

/**
 * Lässt zwei Solver-Varianten <strong>direkt gegeneinander</strong> antreten.
 *
 * <p>Motivation: Die {@code FactionMatrixHarness} beantwortet die Frage nach der Solver-Qualität
 * <em>nicht</em>. Dort spielen beide Seiten denselben Solver, eine Verbesserung hebt also beide
 * Seiten gleichermaßen an und bleibt in der Win-Rate unsichtbar. Wer wissen will, ob eine neue
 * Heuristik trägt, muss sie gegen die Variante ohne sie stellen.
 *
 * <p>Aufbau: {@link SplitSolver} routet nach {@code Stack.side()} — Attacker bekommt Variante A,
 * Defender Variante B. Jedes Pairing läuft zusätzlich mit getauschten Rollen, damit der
 * Attacker-Vorteil (bei Speed-Gleichstand zieht der Attacker zuerst) sich herausmittelt.
 * Gemessen wird die Win-Rate von A über alle Läufe; 0.50 heißt „kein Unterschied".
 *
 * <p>Aktivieren: {@code .\gradlew.bat test --tests "*SolverDuelHarness" "-Ph3.harness=solver-duel"}.
 * Report unter {@code build/reports/solver-duel.md}.
 */
@EnabledIfSystemProperty(named = "h3.harness", matches = "solver-duel")
class SolverDuelHarness {

    private static final int SEEDS_PER_PAIR = 40;

    private static final List<Faction> FACTIONS_IN_ORDER = List.of(
            Faction.CASTLE, Faction.RAMPART, Faction.TOWER, Faction.INFERNO,
            Faction.NECROPOLIS, Faction.DUNGEON, Faction.STRONGHOLD, Faction.FORTRESS,
            Faction.CONFLUX);

    @Test
    void tactical_wait_versus_no_wait() throws IOException {
        FactionPresetCatalog presets = new FactionPresetCatalog();
        Map<Faction, FactionPresetDto> presetByFaction = new EnumMap<>(Faction.class);
        for (FactionPresetDto p : presets.all()) {
            presetByFaction.put(p.faction(), p);
        }

        Map<Faction, Tally> perFaction = new EnumMap<>(Faction.class);
        for (Faction f : FACTIONS_IN_ORDER) {
            perFaction.put(f, new Tally());
        }
        Tally overall = new Tally();

        // (1) Gesamt-Effekt über alle 81 Pairings.
        for (Faction a : FACTIONS_IN_ORDER) {
            for (Faction d : FACTIONS_IN_ORDER) {
                for (int s = 0; s < SEEDS_PER_PAIR; s++) {
                    long seed = (long) a.ordinal() * 9929L + d.ordinal() * 113L + s;
                    // Durchgang 1: Wait-Variante stellt den Attacker.
                    duel(presetByFaction, a, d, seed, true, null, overall);
                    // Durchgang 2: Rollen getauscht — Wait-Variante stellt den Defender.
                    duel(presetByFaction, a, d, seed, false, null, overall);
                }
            }
        }

        // (2) Solver-Effekt je Faktion, isoliert per Spiegel-Duell: dieselbe Armee auf beiden
        // Seiten, einziger Unterschied ist die Heuristik. Eine Aufschlüsselung des Laufs (1)
        // nach Faktion wäre wertlos — sie enthielte die Faction-Stärke, nicht den Solver-Effekt.
        for (Faction f : FACTIONS_IN_ORDER) {
            for (int s = 0; s < SEEDS_PER_PAIR; s++) {
                long seed = (long) f.ordinal() * 9929L + f.ordinal() * 113L + s;
                duel(presetByFaction, f, f, seed, true, perFaction.get(f), null);
                duel(presetByFaction, f, f, seed, false, perFaction.get(f), null);
            }
        }

        String report = render(perFaction, overall);
        Path reportsDir = Path.of("build", "reports");
        Files.createDirectories(reportsDir);
        Path out = reportsDir.resolve("solver-duel.md");
        Files.writeString(out, report);
        System.out.println("Solver-Duell-Report: " + out.toAbsolutePath());
        System.out.println(report);
    }

    private static void duel(Map<Faction, FactionPresetDto> presets, Faction attackerFaction,
                             Faction defenderFaction, long seed, boolean waitPlaysAttacker,
                             @Nullable Tally perFaction, @Nullable Tally overall) {
        List<Stack> attackerStacks = buildStacks(presets.get(attackerFaction).stacks(), Side.ATTACKER);
        List<Stack> defenderStacks = buildStacks(presets.get(defenderFaction).stacks(), Side.DEFENDER);
        BattleSetup setup = new BattleSetup(attackerStacks, defenderStacks,
                buildBattlefield(attackerStacks, defenderStacks, seed));

        AutoSolver withWait = new StrategicAutoSolver(true);
        AutoSolver withoutWait = new StrategicAutoSolver(false);
        AutoSolver split = waitPlaysAttacker
                ? new SplitSolver(withWait, withoutWait)
                : new SplitSolver(withoutWait, withWait);

        BattleResult result = new Battle(new Random(seed), split).simulate(setup);
        Side waitSide = waitPlaysAttacker ? Side.ATTACKER : Side.DEFENDER;

        if (overall != null) {
            record(overall, result, waitSide);
        }
        if (perFaction != null) {
            record(perFaction, result, waitSide);
        }
    }

    private static void record(Tally tally, BattleResult result, Side waitSide) {
        tally.battles++;
        Winner winner = result.winner();
        if (winner == Winner.DRAW) {
            tally.draws++;
        } else if ((winner == Winner.ATTACKER) == (waitSide == Side.ATTACKER)) {
            tally.waitWins++;
        } else {
            tally.waitLosses++;
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

        private AutoSolver solverFor(Stack active) {
            return active.side() == Side.ATTACKER ? attackerSolver : defenderSolver;
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

    private static String render(Map<Faction, Tally> perFaction, Tally overall) {
        StringBuilder sb = new StringBuilder(2048);
        sb.append("# Solver-Duell: Tactical Wait vs. kein Wait\n\n");
        sb.append("Beide Varianten treten **direkt gegeneinander** an — die Wait-Variante spielt ")
                .append("jedes Pairing einmal als Attacker und einmal als Defender. ")
                .append(SEEDS_PER_PAIR).append(" Seeds pro Pairing, alle 9x9 Pairings, ")
                .append("Basis: StrategicAutoSolver.\n\n");
        sb.append("**Lesart**: Win-Rate der Wait-Variante. 0.50 = kein Unterschied, ")
                .append("> 0.50 = Wait ist besser.\n\n");
        sb.append("## Gesamt\n\n");
        sb.append("| Battles | Wait-Siege | Wait-Niederlagen | Draws | Win-Rate Wait |\n");
        sb.append("|--|--|--|--|--|\n");
        sb.append("| ").append(overall.battles)
                .append(" | ").append(overall.waitWins)
                .append(" | ").append(overall.waitLosses)
                .append(" | ").append(overall.draws)
                .append(" | ").append(rate(overall))
                .append(" |\n\n");
        sb.append("Abweichung von 0.500: **").append(sigma(overall))
                .append(" Sigma**. Ab etwa 2 Sigma ist der Unterschied nicht mehr durch Zufall ")
                .append("erklärbar, darunter ist er Rauschen.\n\n");
        sb.append("## Solver-Effekt je Faktion (Spiegel-Duell, identische Armeen)\n\n");
        sb.append("| Faktion | Battles | Siege | Niederlagen | Draws | Win-Rate Wait | Sigma |\n");
        sb.append("|--|--|--|--|--|--|--|\n");
        for (Faction f : FACTIONS_IN_ORDER) {
            Tally t = perFaction.get(f);
            sb.append("| ").append(f.name())
                    .append(" | ").append(t.battles)
                    .append(" | ").append(t.waitWins)
                    .append(" | ").append(t.waitLosses)
                    .append(" | ").append(t.draws)
                    .append(" | ").append(rate(t))
                    .append(" | ").append(sigma(t))
                    .append(" |\n");
        }
        return sb.toString();
    }

    private static String rate(Tally t) {
        int decided = t.waitWins + t.waitLosses;
        return decided == 0 ? "-"
                : String.format(Locale.ROOT, "%.3f", (double) t.waitWins / decided);
    }

    /**
     * Abweichung von der Nullhypothese „beide Varianten sind gleich stark" in
     * Standardabweichungen. Binomialverteilung mit p = 0.5: sigma = sqrt(n / 4).
     */
    private static String sigma(Tally t) {
        int decided = t.waitWins + t.waitLosses;
        if (decided == 0) {
            return "-";
        }
        double expected = decided / 2.0;
        double sd = Math.sqrt(decided / 4.0);
        return String.format(Locale.ROOT, "%+.2f", (t.waitWins - expected) / sd);
    }

    private static final class Tally {
        private int battles;
        private int waitWins;
        private int waitLosses;
        private int draws;
    }
}
