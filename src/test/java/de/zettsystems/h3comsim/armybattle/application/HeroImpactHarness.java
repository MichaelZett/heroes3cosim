package de.zettsystems.h3comsim.armybattle.application;

import de.zettsystems.h3comsim.armybattle.values.FactionPresetDto;
import de.zettsystems.h3comsim.armybattle.values.StackSpec;
import de.zettsystems.h3comsim.battle.domain.Battle;
import de.zettsystems.h3comsim.battle.domain.BattleResult;
import de.zettsystems.h3comsim.battle.domain.BattleSetup;
import de.zettsystems.h3comsim.battle.domain.Battlefield;
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
 * Wie stark wirkt ein Held? Gemessen wird im <strong>Spiegel-Duell</strong>: dieselbe Armee auf
 * beiden Seiten, einziger Unterschied ist der Anführer.
 *
 * <p>Der Umweg über das Spiegel-Duell ist nötig, nicht bequem. Eine Aufschlüsselung nach
 * Faktion aus einem Lauf über wechselnde Gegner würde die Faction-Stärke messen statt den
 * Heldeneffekt (Castle gewinnt sowieso), und die {@code FactionMatrixHarness} kann den Effekt
 * grundsätzlich nicht zeigen, solange beide Seiten gleich ausgestattet sind.
 *
 * <p>Jedes Pairing läuft zweimal mit getauschten Rollen, damit der Attacker-Vorzug (bei
 * Speed-Gleichstand zieht der Attacker zuerst) sich herausmittelt. 0.500 heißt „kein
 * Unterschied".
 *
 * <p>Aktivieren: {@code .\gradlew.bat test --tests "*HeroImpactHarness" "-Ph3.harness=hero-impact"}.
 * Report unter {@code build/reports/hero-impact.md}.
 */
@EnabledIfSystemProperty(named = "h3.harness", matches = "hero-impact")
class HeroImpactHarness {

    private static final int SEEDS_PER_PAIR = 40;

    private static final List<Faction> FACTIONS_IN_ORDER = List.of(
            Faction.CASTLE, Faction.RAMPART, Faction.TOWER, Faction.INFERNO,
            Faction.NECROPOLIS, Faction.DUNGEON, Faction.STRONGHOLD, Faction.FORTRESS,
            Faction.CONFLUX);

    @Test
    void hero_versus_no_hero() throws IOException {
        FactionPresetCatalog presets = new FactionPresetCatalog();
        Map<Faction, FactionPresetDto> presetByFaction = new EnumMap<>(Faction.class);
        for (FactionPresetDto p : presets.all()) {
            presetByFaction.put(p.faction(), p);
        }

        Map<Faction, Tally> mirror = new EnumMap<>(Faction.class);
        for (Faction f : FACTIONS_IN_ORDER) {
            mirror.put(f, new Tally());
        }
        Tally overall = new Tally();

        // (1) Spiegel-Duelle: isolierter Heldeneffekt je Faktion.
        for (Faction f : FACTIONS_IN_ORDER) {
            Hero hero = HeroCatalog.byFaction(f).orElseThrow();
            for (int s = 0; s < SEEDS_PER_PAIR; s++) {
                long seed = (long) f.ordinal() * 9929L + f.ordinal() * 113L + s;
                duel(presetByFaction, f, f, hero, seed, true, mirror.get(f), null);
                duel(presetByFaction, f, f, hero, seed, false, mirror.get(f), null);
            }
        }

        // (2) Gesamteffekt über alle 81 Pairings: der Held der jeweils geführten Seite gegen
        // dieselbe Aufstellung ohne Anführer.
        for (Faction a : FACTIONS_IN_ORDER) {
            for (Faction d : FACTIONS_IN_ORDER) {
                for (int s = 0; s < SEEDS_PER_PAIR; s++) {
                    long seed = (long) a.ordinal() * 9929L + d.ordinal() * 113L + s;
                    duel(presetByFaction, a, d, HeroCatalog.byFaction(a).orElseThrow(),
                            seed, true, null, overall);
                    duel(presetByFaction, a, d, HeroCatalog.byFaction(d).orElseThrow(),
                            seed, false, null, overall);
                }
            }
        }

        String report = render(mirror, overall);
        Path reportsDir = Path.of("build", "reports");
        Files.createDirectories(reportsDir);
        Path out = reportsDir.resolve("hero-impact.md");
        Files.writeString(out, report);
        System.out.println("Hero-Impact-Report: " + out.toAbsolutePath());
        System.out.println(report);
    }

    private static void duel(Map<Faction, FactionPresetDto> presets, Faction attackerFaction,
                             Faction defenderFaction, Hero hero, long seed,
                             boolean heroLeadsAttacker, Tally perFaction, Tally overall) {
        List<Stack> attackerStacks = buildStacks(presets.get(attackerFaction).stacks(), Side.ATTACKER);
        List<Stack> defenderStacks = buildStacks(presets.get(defenderFaction).stacks(), Side.DEFENDER);
        BattleSetup setup = new BattleSetup(attackerStacks, defenderStacks,
                buildBattlefield(attackerStacks, defenderStacks, seed),
                heroLeadsAttacker ? hero : null,
                heroLeadsAttacker ? null : hero);

        BattleResult result = new Battle(new Random(seed), new StrategicAutoSolver()).simulate(setup);
        Side heroSide = heroLeadsAttacker ? Side.ATTACKER : Side.DEFENDER;
        if (perFaction != null) {
            record(perFaction, result, heroSide);
        }
        if (overall != null) {
            record(overall, result, heroSide);
        }
    }

    private static void record(Tally tally, BattleResult result, Side heroSide) {
        tally.battles++;
        Winner winner = result.winner();
        if (winner == Winner.DRAW) {
            tally.draws++;
        } else if ((winner == Winner.ATTACKER) == (heroSide == Side.ATTACKER)) {
            tally.heroWins++;
        } else {
            tally.heroLosses++;
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

    private static String render(Map<Faction, Tally> mirror, Tally overall) {
        StringBuilder sb = new StringBuilder(2048);
        sb.append("# Wirkung eines Helden\n\n");
        sb.append("Dieselbe Armee, derselbe Seed — einziger Unterschied ist der Anführer. ")
                .append(SEEDS_PER_PAIR).append(" Seeds pro Pairing, jede Paarung zusätzlich mit ")
                .append("getauschten Rollen. Solver: StrategicAutoSolver.\n\n");
        sb.append("**Lesart**: Win-Rate der geführten Seite. 0.500 = kein Unterschied.\n\n");
        sb.append("## Gesamt (alle 81 Pairings)\n\n");
        sb.append("| Battles | Siege | Niederlagen | Draws | Win-Rate | Sigma |\n");
        sb.append("|--|--|--|--|--|--|\n");
        sb.append("| ").append(overall.battles)
                .append(" | ").append(overall.heroWins)
                .append(" | ").append(overall.heroLosses)
                .append(" | ").append(overall.draws)
                .append(" | ").append(rate(overall))
                .append(" | ").append(sigma(overall))
                .append(" |\n");
        sb.append("\n## Isolierter Heldeneffekt je Faktion (Spiegel-Duell)\n\n");
        sb.append("| Faktion | Held | A/D | Battles | Siege | Niederlagen | Draws | Win-Rate | Sigma |\n");
        sb.append("|--|--|--|--|--|--|--|--|--|\n");
        for (Faction f : FACTIONS_IN_ORDER) {
            Hero hero = HeroCatalog.byFaction(f).orElseThrow();
            Tally t = mirror.get(f);
            sb.append("| ").append(f.name())
                    .append(" | ").append(hero.name())
                    .append(" | ").append(hero.attack()).append('/').append(hero.defense())
                    .append(" | ").append(t.battles)
                    .append(" | ").append(t.heroWins)
                    .append(" | ").append(t.heroLosses)
                    .append(" | ").append(t.draws)
                    .append(" | ").append(rate(t))
                    .append(" | ").append(sigma(t))
                    .append(" |\n");
        }
        return sb.toString();
    }

    private static String rate(Tally t) {
        int decided = t.heroWins + t.heroLosses;
        return decided == 0 ? "-"
                : String.format(Locale.ROOT, "%.3f", (double) t.heroWins / decided);
    }

    /** Abweichung von „Held wirkt nicht" in Standardabweichungen; Binomial mit p = 0.5. */
    private static String sigma(Tally t) {
        int decided = t.heroWins + t.heroLosses;
        if (decided == 0) {
            return "-";
        }
        double expected = decided / 2.0;
        double sd = Math.sqrt(decided / 4.0);
        return String.format(Locale.ROOT, "%+.2f", (t.heroWins - expected) / sd);
    }

    private static final class Tally {
        private int battles;
        private int heroWins;
        private int heroLosses;
        private int draws;
    }
}
