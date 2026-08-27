package de.zettsystems.h3comsim.armybattle.application;

import de.zettsystems.h3comsim.armybattle.values.FactionPresetDto;
import de.zettsystems.h3comsim.armybattle.values.StackSpec;
import de.zettsystems.h3comsim.battle.domain.AutoSolver;
import de.zettsystems.h3comsim.battle.domain.Battle;
import de.zettsystems.h3comsim.battle.domain.BattleSetup;
import de.zettsystems.h3comsim.battle.domain.Battlefield;
import de.zettsystems.h3comsim.battle.domain.Faction;
import de.zettsystems.h3comsim.battle.domain.Hex;
import de.zettsystems.h3comsim.battle.domain.ObstacleGenerator;
import de.zettsystems.h3comsim.battle.domain.Stack;
import de.zettsystems.h3comsim.battle.domain.StrategicAutoSolver;
import de.zettsystems.h3comsim.battle.domain.Unit;
import de.zettsystems.h3comsim.battle.domain.UnitCatalog;
import de.zettsystems.h3comsim.battle.domain.events.BattleEvent;
import de.zettsystems.h3comsim.battle.domain.events.ListEventCollector;
import de.zettsystems.h3comsim.battle.domain.events.Side;
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
 * Misst den <strong>Eigenbeschuss</strong> (Friendly Fire) pro Faktion.
 *
 * <p>Die Engine verteilt Splash an beide Seiten ({@code Battle.findStackAt} iteriert
 * Attacker- und Defender-Stacks). Ein Treffer-Event, dessen {@code actor}-Seite gleich
 * der {@code target}-Seite ist, ist damit per Definition ein Eigentor. Genau diese Events
 * zählt der Harness — getrennt nach Fern- (SPLASH_SHOT / DEATH_CLOUD) und Nahkampf
 * (THREE_HEADED_ATTACK / FIRE_BREATH).
 *
 * <p>Zweck: Vorher/Nachher-Vergleich für die Friendly-Fire-Awareness der Solver-Heuristiken.
 * Der Report landet unter {@code build/reports/friendly-fire.md}; ein Lauf ist deterministisch
 * (Seed pro Pairing aus den Faction-Ordinals abgeleitet), zwei Läufe desselben Codes liefern
 * identische Zahlen.
 *
 * <p>Aktivieren: {@code .\gradlew.bat test --tests "*FriendlyFireDiagnosisHarness"
 * "-Ph3.harness=friendly-fire"}.
 */
@EnabledIfSystemProperty(named = "h3.harness", matches = "friendly-fire")
class FriendlyFireDiagnosisHarness {

    private static final int SEEDS_PER_PAIR = 10;

    private static final List<Faction> FACTIONS_IN_ORDER = List.of(
            Faction.CASTLE, Faction.RAMPART, Faction.TOWER, Faction.INFERNO,
            Faction.NECROPOLIS, Faction.DUNGEON, Faction.STRONGHOLD, Faction.FORTRESS,
            Faction.CONFLUX);

    @Test
    void measure_friendly_fire_per_faction() throws IOException {
        FactionPresetCatalog presets = new FactionPresetCatalog();
        Map<Faction, FactionPresetDto> presetByFaction = new EnumMap<>(Faction.class);
        for (FactionPresetDto p : presets.all()) {
            presetByFaction.put(p.faction(), p);
        }

        Map<Faction, Tally> tallies = new EnumMap<>(Faction.class);
        for (Faction f : FACTIONS_IN_ORDER) {
            tallies.put(f, new Tally());
        }

        for (Faction attacker : FACTIONS_IN_ORDER) {
            for (Faction defender : FACTIONS_IN_ORDER) {
                for (int s = 0; s < SEEDS_PER_PAIR; s++) {
                    long seed = (long) attacker.ordinal() * 9929L + defender.ordinal() * 113L + s;
                    runOne(presetByFaction.get(attacker).stacks(),
                            presetByFaction.get(defender).stacks(),
                            attacker, defender, seed, tallies);
                }
            }
        }

        String report = render(tallies);
        Path reportsDir = Path.of("build", "reports");
        Files.createDirectories(reportsDir);
        Path out = reportsDir.resolve("friendly-fire.md");
        Files.writeString(out, report);
        System.out.println("Friendly-Fire-Report: " + out.toAbsolutePath());
        System.out.println(report);
    }

    private static void runOne(List<StackSpec> attackerSpec, List<StackSpec> defenderSpec,
                               Faction attackerFaction, Faction defenderFaction,
                               long seed, Map<Faction, Tally> tallies) {
        List<Stack> attackerStacks = buildStacks(attackerSpec, Side.ATTACKER);
        List<Stack> defenderStacks = buildStacks(defenderSpec, Side.DEFENDER);
        BattleSetup setup = new BattleSetup(attackerStacks, defenderStacks,
                buildBattlefield(attackerStacks, defenderStacks, seed));

        ListEventCollector collector = new ListEventCollector();
        AutoSolver solver = new StrategicAutoSolver();
        new Battle(new Random(seed), solver, collector).simulate(setup);

        tallies.get(attackerFaction).battles++;
        tallies.get(defenderFaction).battles++;
        for (BattleEvent event : collector.events()) {
            // Record-Patterns würden hier ungenutzte Bindings erzeugen (SpotBugs DLS_DEAD_LOCAL_STORE),
            // deshalb über die Accessoren.
            if (event instanceof BattleEvent.Shoot shoot && shoot.actor() == shoot.target()) {
                Tally t = tallies.get(shoot.actor() == Side.ATTACKER ? attackerFaction : defenderFaction);
                t.rangedHits++;
                t.rangedDamage += shoot.damage();
                t.rangedKills += shoot.killed();
            } else if (event instanceof BattleEvent.Melee melee && melee.actor() == melee.target()) {
                Tally t = tallies.get(melee.actor() == Side.ATTACKER ? attackerFaction : defenderFaction);
                t.meleeHits++;
                t.meleeDamage += melee.damage();
                t.meleeKills += melee.killed();
            }
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

    private static String render(Map<Faction, Tally> tallies) {
        StringBuilder sb = new StringBuilder(2048);
        sb.append("# Friendly Fire pro Faktion\n\n");
        sb.append("Ein Treffer-Event mit `actor == target` ist Eigenbeschuss. ")
                .append(SEEDS_PER_PAIR).append(" Seeds pro Pairing, alle 9x9 Pairings, ")
                .append("Solver: StrategicAutoSolver. Deterministisch — identischer Code ")
                .append("liefert identische Zahlen.\n\n");
        sb.append("| Faktion | Battles | FF-Treffer fern | FF-Schaden fern | FF-Kills fern ")
                .append("| FF-Treffer nah | FF-Schaden nah | FF-Kills nah | Ø FF-Schaden/Battle |\n");
        sb.append("|--|--|--|--|--|--|--|--|--|\n");
        long totalDamage = 0;
        for (Faction f : FACTIONS_IN_ORDER) {
            Tally t = tallies.get(f);
            long dmg = t.rangedDamage + t.meleeDamage;
            totalDamage += dmg;
            sb.append("| ").append(f.name()).append(" | ").append(t.battles)
                    .append(" | ").append(t.rangedHits)
                    .append(" | ").append(t.rangedDamage)
                    .append(" | ").append(t.rangedKills)
                    .append(" | ").append(t.meleeHits)
                    .append(" | ").append(t.meleeDamage)
                    .append(" | ").append(t.meleeKills)
                    .append(" | ").append(String.format(Locale.ROOT, "%.1f",
                            t.battles == 0 ? 0.0 : (double) dmg / t.battles))
                    .append(" |\n");
        }
        sb.append("\n**Gesamt-Eigenschaden**: ").append(totalDamage).append("\n");
        return sb.toString();
    }

    private static final class Tally {
        private int battles;
        private int rangedHits;
        private long rangedDamage;
        private int rangedKills;
        private int meleeHits;
        private long meleeDamage;
        private int meleeKills;
    }
}
