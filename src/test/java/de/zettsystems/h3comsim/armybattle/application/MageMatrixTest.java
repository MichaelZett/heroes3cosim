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
import de.zettsystems.h3comsim.battle.domain.HeroGrowth;
import de.zettsystems.h3comsim.battle.domain.Hex;
import de.zettsystems.h3comsim.battle.domain.ObstacleGenerator;
import de.zettsystems.h3comsim.battle.domain.Spell;
import de.zettsystems.h3comsim.battle.domain.Stack;
import de.zettsystems.h3comsim.battle.domain.StrategicAutoSolver;
import de.zettsystems.h3comsim.battle.domain.Unit;
import de.zettsystems.h3comsim.battle.domain.UnitCatalog;
import de.zettsystems.h3comsim.battle.domain.events.BattleEvent;
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
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Function;

/**
 * Faction-Matrix mit Magier-Helden: wie {@code FactionMatrixTest}, aber jede Armee führt der
 * Magier ihrer Fraktion ({@link HeroCatalog#mageFor}) statt des Standardhelden. Zum Vergleich
 * läuft dieselbe Matrix mit den Standardhelden, auf jedem Level aus {@link #LEVELS}; ab Level 2
 * steigen beide Helden wie im Endpunkt zufällig auf ({@link HeroGrowth#grow}, Zufall je Seite aus
 * dem Seed). Dazu je Fraktion das Spiegel-Duell Magier gegen Standardheld.
 *
 * <p>Gezählt wird exakt (Siege je Fraktion über alle acht Gegner und beide Rollen), nicht aus
 * gerundeten Zellen.
 *
 * <p>Aktivieren: {@code .\gradlew.bat test --tests "*MageMatrixTest" "-Ph3.harness=mage-matrix"}.
 * Report unter {@code build/reports/mage-matrix.md}.
 */
@EnabledIfSystemProperty(named = "h3.harness", matches = "mage-matrix")
class MageMatrixTest {

    private static final int SEEDS_PER_PAIR = 40;

    private static final List<Integer> LEVELS = List.of(1, 10, 20);

    private static final List<Faction> FACTIONS_IN_ORDER = List.of(
            Faction.CASTLE, Faction.RAMPART, Faction.TOWER, Faction.INFERNO,
            Faction.NECROPOLIS, Faction.DUNGEON, Faction.STRONGHOLD, Faction.FORTRESS,
            Faction.CONFLUX);

    private enum Leader {
        STANDARD(HeroCatalog::defaultFor),
        MAGE(HeroCatalog::mageFor);

        private final Function<Faction, Optional<Hero>> catalog;

        Leader(Function<Faction, Optional<Hero>> catalog) {
            this.catalog = catalog;
        }

        Hero of(Faction faction) {
            return catalog.apply(faction).orElseThrow();
        }
    }

    @Test
    void faction_matrix_with_mages() throws Exception {
        FactionPresetCatalog presets = new FactionPresetCatalog();
        Map<Faction, FactionPresetDto> presetByFaction = new EnumMap<>(Faction.class);
        for (FactionPresetDto p : presets.all()) {
            presetByFaction.put(p.faction(), p);
        }

        StringBuilder sb = new StringBuilder(8192);
        sb.append("# Faction-Matrix mit Magier-Helden\n\n")
                .append("Jede Armee führt entweder der Standardheld ihrer Fraktion (Kämpfer) oder ihr Magier ")
                .append("(`HeroCatalog.mageFor`). Ab Level 2 steigen beide zufällig auf, Zufall je Seite aus ")
                .append("dem Seed. ").append(SEEDS_PER_PAIR).append(" Seeds je Paarung und Rolle, also ")
                .append(SEEDS_PER_PAIR * 2).append(" Kämpfe je Zelle und ").append(SEEDS_PER_PAIR * 16)
                .append(" je Fraktion. Solver: StrategicAutoSolver.\n\n")
                .append("**Lesart**: Win-Rate = Siege / Kämpfe gegen die acht anderen Fraktionen. ")
                .append("Spiegel-Duell: dieselbe Armee auf beiden Seiten, Magier gegen Kämpfer, ")
                .append("Win-Rate des Magiers über entschiedene Kämpfe.\n\n");
        for (int level : LEVELS) {
            Map<Faction, Tally> standard = runMatrix(presetByFaction, Leader.STANDARD, level);
            Map<Faction, Tally> mage = runMatrix(presetByFaction, Leader.MAGE, level);
            Map<Faction, Tally> mirror = runMirror(presetByFaction, level);
            renderLevel(sb, level, standard, mage, mirror);
        }

        String report = sb.toString();
        Path reportsDir = Path.of("build", "reports");
        Files.createDirectories(reportsDir);
        Path out = reportsDir.resolve("mage-matrix.md");
        Files.writeString(out, report);
        IO.println("Mage-Matrix-Report: " + out.toAbsolutePath());
        IO.println(report);
    }

    private static Map<Faction, Tally> runMatrix(Map<Faction, FactionPresetDto> presets, Leader leader,
                                                 int level) {
        Map<Faction, Tally> byFaction = new ConcurrentHashMap<>();
        for (Faction f : FACTIONS_IN_ORDER) {
            byFaction.put(f, new Tally());
        }
        try (ExecutorService pool = Executors.newWorkStealingPool()) {
            for (Faction attacker : FACTIONS_IN_ORDER) {
                for (Faction defender : FACTIONS_IN_ORDER) {
                    if (attacker == defender) {
                        continue;
                    }
                    for (int s = 0; s < SEEDS_PER_PAIR; s++) {
                        long seed = attacker.ordinal() * 9929L + defender.ordinal() * 113L + s;
                        pool.execute(() -> {
                            Fight fight = fight(presets, attacker, leader.of(attacker), defender,
                                    leader.of(defender), level, seed);
                            byFaction.get(attacker).add(fight, Side.ATTACKER);
                            byFaction.get(defender).add(fight, Side.DEFENDER);
                        });
                    }
                }
            }
        }
        return byFaction;
    }

    /** Magier gegen Kämpfer derselben Fraktion; gezählt aus Sicht des Magiers. */
    private static Map<Faction, Tally> runMirror(Map<Faction, FactionPresetDto> presets, int level) {
        Map<Faction, Tally> byFaction = new ConcurrentHashMap<>();
        try (ExecutorService pool = Executors.newWorkStealingPool()) {
            for (Faction f : FACTIONS_IN_ORDER) {
                Tally tally = new Tally();
                byFaction.put(f, tally);
                Hero mage = Leader.MAGE.of(f);
                Hero standard = Leader.STANDARD.of(f);
                for (int s = 0; s < SEEDS_PER_PAIR * 4; s++) {
                    long seed = f.ordinal() * 7919L + 331L + s;
                    pool.execute(() -> tally.add(fight(presets, f, mage, f, standard, level, seed), Side.ATTACKER));
                    pool.execute(() -> tally.add(fight(presets, f, standard, f, mage, level, seed), Side.DEFENDER));
                }
            }
        }
        return byFaction;
    }

    private static Fight fight(Map<Faction, FactionPresetDto> presets, Faction attacker, Hero attackerBase,
                               Faction defender, Hero defenderBase, int level, long seed) {
        List<Stack> attackerStacks = buildStacks(presets.get(attacker).stacks(), Side.ATTACKER);
        List<Stack> defenderStacks = buildStacks(presets.get(defender).stacks(), Side.DEFENDER);
        BattleSetup setup = new BattleSetup(attackerStacks, defenderStacks,
                buildBattlefield(attackerStacks, defenderStacks, seed),
                grown(attackerBase, level, seed, Side.ATTACKER), grown(defenderBase, level, seed, Side.DEFENDER));
        Map<Side, Integer> casts = new EnumMap<>(Side.class);
        BattleResult result = new Battle(new Random(seed), new StrategicAutoSolver(), event -> {
            if (event instanceof BattleEvent.HeroSpell cast) {
                casts.merge(cast.actor(), 1, Integer::sum);
            }
        }).simulate(setup);
        return new Fight(result.winner(), casts.getOrDefault(Side.ATTACKER, 0),
                casts.getOrDefault(Side.DEFENDER, 0));
    }

    /** Wie {@code DefaultArmyBattleService}: Level 1 ist der Katalogstand, darüber zufälliger Aufstieg. */
    private static Hero grown(Hero base, int level, long seed, Side side) {
        return level <= base.level() ? base
                : HeroGrowth.grow(base, level, new Random(seed * 31 + (side == Side.ATTACKER ? 1 : 2)));
    }

    private static List<Stack> buildStacks(List<StackSpec> specs, Side side) {
        List<Unit> units = new ArrayList<>(specs.size());
        for (StackSpec spec : specs) {
            units.add(UnitCatalog.byName(spec.unitName()).orElseThrow());
        }
        List<Hex> positions = SpawnLayout.assignPositions(side, units);
        List<Stack> stacks = new ArrayList<>(specs.size());
        for (int slot = 0; slot < specs.size(); slot++) {
            stacks.add(new Stack(units.get(slot), specs.get(slot).count(), positions.get(slot), side, slot));
        }
        return stacks;
    }

    private static Battlefield buildBattlefield(List<Stack> a, List<Stack> d, long seed) {
        Set<Hex> obstacles = new HashSet<>(
                ObstacleGenerator.generate(Battlefield.STANDARD, new Random(seed)));
        obstacles.removeAll(SpawnLayout.spawnHexesFor(a.size(), d.size()));
        return Battlefield.STANDARD.withObstacles(obstacles);
    }

    private static void renderLevel(StringBuilder sb, int level, Map<Faction, Tally> standard,
                                    Map<Faction, Tally> mage, Map<Faction, Tally> mirror) {
        sb.append("## Level ").append(level).append("\n\n")
                .append("| Faktion | Magier | Kämpfer Siege | Kämpfer Win-Rate | Magier Siege | Magier Win-Rate | Δ ")
                .append("| Zauber je Kampf Kämpfer | Zauber je Kampf Magier | Spiegel Siege:Niederlagen:Draws ")
                .append("| Spiegel Win-Rate | Spiegel Sigma |\n")
                .append("|--|--|--|--|--|--|--|--|--|--|--|--|\n");
        List<Faction> ranked = FACTIONS_IN_ORDER.stream()
                .sorted((a, b) -> Double.compare(mage.get(b).winRate(), mage.get(a).winRate()))
                .toList();
        for (Faction f : ranked) {
            Tally s = standard.get(f);
            Tally m = mage.get(f);
            Tally d = mirror.get(f);
            sb.append("| ").append(f.name())
                    .append(" | ").append(Leader.MAGE.of(f).name())
                    .append(" | ").append(s.wins.sum())
                    .append(" | ").append(fmt("%.3f", s.winRate()))
                    .append(" | ").append(m.wins.sum())
                    .append(" | ").append(fmt("%.3f", m.winRate()))
                    .append(" | ").append(fmt("%+.3f", m.winRate() - s.winRate()))
                    .append(" | ").append(fmt("%.1f", s.castsPerFight()))
                    .append(" | ").append(fmt("%.1f", m.castsPerFight()))
                    .append(" | ").append(d.wins.sum()).append(':').append(d.losses.sum()).append(':')
                    .append(d.draws.sum())
                    .append(" | ").append(fmt("%.3f", d.decidedRate()))
                    .append(" | ").append(fmt("%+.2f", d.sigma()))
                    .append(" |\n");
        }
        sb.append('\n');
    }

    private static String fmt(String pattern, double value) {
        return String.format(Locale.ROOT, pattern, value);
    }

    private record Fight(Winner winner, int attackerCasts, int defenderCasts) {
    }

    private static final class Tally {
        private final LongAdder fights = new LongAdder();
        private final LongAdder wins = new LongAdder();
        private final LongAdder losses = new LongAdder();
        private final LongAdder draws = new LongAdder();
        private final LongAdder casts = new LongAdder();

        void add(Fight fight, Side side) {
            fights.increment();
            casts.add(side == Side.ATTACKER ? fight.attackerCasts() : fight.defenderCasts());
            if (fight.winner() == Winner.DRAW) {
                draws.increment();
            } else if ((fight.winner() == Winner.ATTACKER) == (side == Side.ATTACKER)) {
                wins.increment();
            } else {
                losses.increment();
            }
        }

        double winRate() {
            long n = fights.sum();
            return n == 0 ? 0.0 : (double) wins.sum() / n;
        }

        double decidedRate() {
            long decided = wins.sum() + losses.sum();
            return decided == 0 ? 0.5 : (double) wins.sum() / decided;
        }

        double castsPerFight() {
            long n = fights.sum();
            return n == 0 ? 0.0 : (double) casts.sum() / n;
        }

        /** Abweichung von „Magier und Kämpfer gleich stark"; Binomial mit p = 0.5. */
        double sigma() {
            long decided = wins.sum() + losses.sum();
            return decided == 0 ? 0.0 : (wins.sum() - decided / 2.0) / Math.sqrt(decided / 4.0);
        }
    }
}
