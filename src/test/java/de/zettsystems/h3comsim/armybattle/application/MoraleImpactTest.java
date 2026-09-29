package de.zettsystems.h3comsim.armybattle.application;

import de.zettsystems.h3comsim.armybattle.values.FactionPresetDto;
import de.zettsystems.h3comsim.armybattle.values.StackSpec;
import de.zettsystems.h3comsim.battle.domain.Battle;
import de.zettsystems.h3comsim.battle.domain.BattleResult;
import de.zettsystems.h3comsim.battle.domain.BattleSetup;
import de.zettsystems.h3comsim.battle.domain.Battlefield;
import de.zettsystems.h3comsim.battle.domain.Faction;
import de.zettsystems.h3comsim.battle.domain.Hero;
import de.zettsystems.h3comsim.battle.domain.HeroClass;
import de.zettsystems.h3comsim.battle.domain.Hex;
import de.zettsystems.h3comsim.battle.domain.ObstacleGenerator;
import de.zettsystems.h3comsim.battle.domain.SecondarySkill;
import de.zettsystems.h3comsim.battle.domain.SkillLevel;
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
 * Wie viel ist ein Moralpunkt wert?
 *
 * <p>Gemessen wird im Spiegel-Duell mit einem Anführer, der <strong>nichts</strong> kann außer
 * Leadership: Attack 0, Defense 0, keine Schadens-Fertigkeit. Damit ist der einzige Unterschied
 * zwischen den beiden identischen Armeen das Moral-Rating — genau die Isolation, die eine
 * Aufschlüsselung über wechselnde Gegner nicht liefern würde.
 *
 * <p>Der Aufbau prüft zugleich eine Aussage des Manuals: Untote und Elementare stehen außerhalb
 * des Armee-Ratings. Necropolis sollte deshalb bei 0.500 bleiben, und Conflux nur so weit
 * darüber, wie Firebird, Phoenix, Pixie und Sprite in der Aufstellung wiegen — die vier sind
 * Conflux, aber keine Elementare.
 *
 * <p>Negative Moral kommt hier nicht vor: kein Held kann sie verursachen, sie entsteht nur aus
 * gemischten Armeen. Dieser Pfad ist über {@code MoraleInBattleTest} abgedeckt.
 *
 * <p>Aktivieren: {@code .\gradlew.bat test --tests "*MoraleImpactTest" "-Ph3.harness=morale-impact"}.
 * Report unter {@code build/reports/morale-impact.md}.
 */
@EnabledIfSystemProperty(named = "h3.harness", matches = "morale-impact")
class MoraleImpactTest {

    private static final int SEEDS_PER_PAIR = 80;

    private static final List<SkillLevel> LEVELS =
            List.of(SkillLevel.BASIC, SkillLevel.ADVANCED, SkillLevel.EXPERT);

    private static final List<Faction> FACTIONS_IN_ORDER = List.of(
            Faction.CASTLE, Faction.RAMPART, Faction.TOWER, Faction.INFERNO,
            Faction.NECROPOLIS, Faction.DUNGEON, Faction.STRONGHOLD, Faction.FORTRESS,
            Faction.CONFLUX);

    /** Kann nur eines: Moral heben. Keine Primärwerte, keine Schadens-Fertigkeit. */
    private static Hero leadershipOnly(SkillLevel level) {
        return new Hero("Moral-Statist", HeroClass.KNIGHT, Faction.CASTLE, 0, 0, 1, 1,
                Map.of(SecondarySkill.LEADERSHIP, level));
    }

    @Test
    void morale_versus_no_morale() throws IOException {
        FactionPresetCatalog presets = new FactionPresetCatalog();
        Map<Faction, FactionPresetDto> presetByFaction = new EnumMap<>(Faction.class);
        for (FactionPresetDto p : presets.all()) {
            presetByFaction.put(p.faction(), p);
        }

        Map<SkillLevel, Map<Faction, Tally>> byLevel = new EnumMap<>(SkillLevel.class);
        Map<SkillLevel, Tally> overallByLevel = new EnumMap<>(SkillLevel.class);
        for (SkillLevel level : LEVELS) {
            Hero hero = leadershipOnly(level);
            Map<Faction, Tally> perFaction = new EnumMap<>(Faction.class);
            Tally overall = new Tally();
            for (Faction f : FACTIONS_IN_ORDER) {
                Tally tally = new Tally();
                perFaction.put(f, tally);
                for (int s = 0; s < SEEDS_PER_PAIR; s++) {
                    long seed = (long) f.ordinal() * 9929L + level.ordinal() * 113L + s;
                    duel(presetByFaction, f, hero, seed, true, tally, overall);
                    duel(presetByFaction, f, hero, seed, false, tally, overall);
                }
            }
            byLevel.put(level, perFaction);
            overallByLevel.put(level, overall);
        }

        String report = render(byLevel, overallByLevel);
        Path reportsDir = Path.of("build", "reports");
        Files.createDirectories(reportsDir);
        Path out = reportsDir.resolve("morale-impact.md");
        Files.writeString(out, report);
        System.out.println("Morale-Impact-Report: " + out.toAbsolutePath());
        System.out.println(report);
    }

    private static void duel(Map<Faction, FactionPresetDto> presets, Faction faction, Hero hero,
                             long seed, boolean heroLeadsAttacker, Tally perFaction, Tally overall) {
        List<Stack> attackerStacks = buildStacks(presets.get(faction).stacks(), Side.ATTACKER);
        List<Stack> defenderStacks = buildStacks(presets.get(faction).stacks(), Side.DEFENDER);
        BattleSetup setup = new BattleSetup(attackerStacks, defenderStacks,
                buildBattlefield(attackerStacks, defenderStacks, seed),
                heroLeadsAttacker ? hero : null,
                heroLeadsAttacker ? null : hero);

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

    private static String render(Map<SkillLevel, Map<Faction, Tally>> byLevel,
                                 Map<SkillLevel, Tally> overallByLevel) {
        StringBuilder sb = new StringBuilder(4096);
        sb.append("# Wirkung von Moral\n\n");
        sb.append("Spiegel-Duell: dieselbe Armee auf beiden Seiten, eine davon geführt von einem ")
                .append("Helden, der außer Leadership nichts kann (Attack 0, Defense 0). ")
                .append("Der einzige Unterschied ist damit das Moral-Rating.\n\n");
        sb.append(SEEDS_PER_PAIR).append(" Seeds je Faktion und Stufe, jede Paarung zusätzlich ")
                .append("mit getauschten Rollen. Solver: StrategicAutoSolver.\n\n");
        sb.append("**Lesart**: Win-Rate der geführten Seite. 0.500 = kein Unterschied.\n\n");

        sb.append("## Gesamt je Leadership-Stufe\n\n");
        sb.append("| Stufe | Moral | Chance auf Zusatzaktion | Battles | Win-Rate | Sigma |\n");
        sb.append("|--|--|--|--|--|--|\n");
        for (SkillLevel level : LEVELS) {
            Tally t = overallByLevel.get(level);
            sb.append("| ").append(level.name())
                    .append(" | +").append(moraleOf(level))
                    .append(" | ").append(chanceOf(level))
                    .append(" | ").append(t.battles)
                    .append(" | ").append(rate(t))
                    .append(" | ").append(sigma(t))
                    .append(" |\n");
        }

        for (SkillLevel level : LEVELS) {
            sb.append("\n## ").append(level.name()).append(" Leadership (+")
                    .append(moraleOf(level)).append(" Moral) je Faktion\n\n");
            sb.append("| Faktion | Battles | Siege | Niederlagen | Draws | Win-Rate | Sigma |\n");
            sb.append("|--|--|--|--|--|--|--|\n");
            Map<Faction, Tally> perFaction = byLevel.get(level);
            for (Faction f : FACTIONS_IN_ORDER) {
                Tally t = perFaction.get(f);
                sb.append("| ").append(f.name())
                        .append(" | ").append(t.battles)
                        .append(" | ").append(t.wins)
                        .append(" | ").append(t.losses)
                        .append(" | ").append(t.draws)
                        .append(" | ").append(rate(t))
                        .append(" | ").append(sigma(t))
                        .append(" |\n");
            }
        }
        return sb.toString();
    }

    private static int moraleOf(SkillLevel level) {
        return switch (level) {
            case NONE -> 0;
            case BASIC -> 1;
            case ADVANCED -> 2;
            case EXPERT -> 3;
        };
    }

    /** Manual S. 43 — die Chance, die ein Moralpunkt mehr bringt, hängt vom Ausgangswert ab. */
    private static String chanceOf(SkillLevel level) {
        return switch (level) {
            case NONE -> "-";
            case BASIC -> "8.3 % statt 4.2 %";
            case ADVANCED -> "12.5 % statt 4.2 %";
            case EXPERT -> "12.5 % statt 4.2 % (gekappt)";
        };
    }

    private static String rate(Tally t) {
        int decided = t.wins + t.losses;
        return decided == 0 ? "-" : String.format(Locale.ROOT, "%.3f", (double) t.wins / decided);
    }

    /** Abweichung von „Moral wirkt nicht" in Standardabweichungen; Binomial mit p = 0.5. */
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
