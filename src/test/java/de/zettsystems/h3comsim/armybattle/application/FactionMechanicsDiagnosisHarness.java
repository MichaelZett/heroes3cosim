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
import de.zettsystems.h3comsim.battle.domain.events.BattleEvent;
import de.zettsystems.h3comsim.battle.domain.events.ListEventCollector;
import de.zettsystems.h3comsim.battle.domain.events.Side;
import de.zettsystems.h3comsim.battle.domain.events.Winner;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Diagnose-Harness für die Faction-Mechanik-Analyse. Spielt jedes Faction-Pairing
 * über mehrere Seeds und aggregiert pro Faction:
 * <ul>
 *   <li>Win-Rate als Attacker / als Defender</li>
 *   <li>Pro Unit: durchschnittlicher Damage-Output, Survivor-Quote, Hit-Count</li>
 *   <li>Special-Trigger-Häufigkeit (DEATH_STARE, PETRIFYING, CURSING, POISONOUS,
 *       DISEASES, AGING, TWO_BLOWS, TWO_SHOTS, GOOD_MORALE, FIRE_SHIELD, REBIRTH,
 *       MOVE_BACK, DEFEND, WAIT) pro Faction-Battle</li>
 * </ul>
 *
 * <p>Snapshot landet unter {@code build/reports/faction-mechanics.md} und ist Basis
 * für Faction-Balance-Hypothesen (NEC-Stärke, DUN-Schwäche etc.).
 */
class FactionMechanicsDiagnosisHarness {

    private static final int SEEDS_PER_PAIR = 10;

    private static final List<Faction> FACTIONS = List.of(
            Faction.CASTLE, Faction.RAMPART, Faction.TOWER, Faction.INFERNO,
            Faction.NECROPOLIS, Faction.DUNGEON, Faction.STRONGHOLD, Faction.FORTRESS,
            Faction.CONFLUX);

    @Test
    @Disabled("Diagnose-Harness — manuell aktivieren wenn neue Faction-Balance-Daten "
            + "gebraucht werden. Snapshot in build/reports/faction-mechanics.md.")
    void diagnose_faction_mechanics() throws IOException {
        FactionPresetCatalog presets = new FactionPresetCatalog();
        Map<Faction, FactionPresetDto> presetByFaction = new EnumMap<>(Faction.class);
        for (FactionPresetDto p : presets.all()) {
            presetByFaction.put(p.faction(), p);
        }

        Map<Faction, FactionAggregate> aggregates = new EnumMap<>(Faction.class);
        for (Faction f : FACTIONS) {
            aggregates.put(f, new FactionAggregate(f));
        }

        for (Faction attacker : FACTIONS) {
            for (Faction defender : FACTIONS) {
                FactionPresetDto attackerPreset = presetByFaction.get(attacker);
                FactionPresetDto defenderPreset = presetByFaction.get(defender);
                for (int s = 0; s < SEEDS_PER_PAIR; s++) {
                    long seed = (long) (attacker.ordinal() * 9929L + defender.ordinal() * 113L + s);
                    runOne(attackerPreset, defenderPreset, seed,
                            aggregates.get(attacker), aggregates.get(defender));
                }
            }
        }

        String report = renderReport(aggregates);
        Path reportsDir = Path.of("build", "reports");
        Files.createDirectories(reportsDir);
        Path out = reportsDir.resolve("faction-mechanics.md");
        Files.writeString(out, report);
        System.out.println("Faction-Mechanics-Report: " + out.toAbsolutePath());
    }

    private static void runOne(FactionPresetDto attackerPreset, FactionPresetDto defenderPreset,
                               long seed, FactionAggregate attackerAgg, FactionAggregate defenderAgg) {
        List<Stack> attackerStacks = buildStacks(attackerPreset.stacks(), Side.ATTACKER);
        List<Stack> defenderStacks = buildStacks(defenderPreset.stacks(), Side.DEFENDER);
        Map<String, Integer> startCounts = new HashMap<>();
        for (int i = 0; i < attackerStacks.size(); i++) {
            startCounts.put(key(Side.ATTACKER, i), attackerPreset.stacks().get(i).count());
        }
        for (int i = 0; i < defenderStacks.size(); i++) {
            startCounts.put(key(Side.DEFENDER, i), defenderPreset.stacks().get(i).count());
        }

        Battlefield bf = buildBattlefield(attackerStacks, defenderStacks, seed);
        BattleSetup setup = new BattleSetup(attackerStacks, defenderStacks, bf);
        ListEventCollector collector = new ListEventCollector();
        BattleResult result = new Battle(new Random(seed), new StrategicAutoSolver(), collector).simulate(setup);

        // Pro Stack die Events durchrechnen.
        Map<String, StackBattleStats> perStack = new HashMap<>();
        for (BattleEvent e : collector.events()) {
            processEvent(e, perStack);
        }

        // Aggregieren auf Faction-Ebene.
        attackerAgg.totalBattles++;
        defenderAgg.totalBattles++;
        if (result.winner() == Winner.ATTACKER) {
            attackerAgg.winsAsAttacker++;
        } else if (result.winner() == Winner.DEFENDER) {
            defenderAgg.winsAsDefender++;
        }
        attackerAgg.totalRounds += result.turnsTaken();
        defenderAgg.totalRounds += result.turnsTaken();

        for (Stack st : attackerStacks) {
            StackBattleStats s = perStack.getOrDefault(key(Side.ATTACKER, st.slot()), new StackBattleStats());
            int start = startCounts.get(key(Side.ATTACKER, st.slot()));
            attackerAgg.recordStack(st.unit().name(), s, start, st.getCount());
        }
        for (Stack st : defenderStacks) {
            StackBattleStats s = perStack.getOrDefault(key(Side.DEFENDER, st.slot()), new StackBattleStats());
            int start = startCounts.get(key(Side.DEFENDER, st.slot()));
            defenderAgg.recordStack(st.unit().name(), s, start, st.getCount());
        }
    }

    private static void processEvent(BattleEvent e, Map<String, StackBattleStats> perStack) {
        switch (e) {
            case BattleEvent.Melee m -> {
                StackBattleStats s = perStack.computeIfAbsent(
                        key(m.actor(), m.actorSlot()), k -> new StackBattleStats());
                s.damageDealt += m.damage();
                s.hits++;
            }
            case BattleEvent.Shoot sh -> {
                StackBattleStats s = perStack.computeIfAbsent(
                        key(sh.actor(), sh.actorSlot()), k -> new StackBattleStats());
                s.damageDealt += sh.damage();
                s.hits++;
            }
            case BattleEvent.Retaliation r -> {
                StackBattleStats s = perStack.computeIfAbsent(
                        key(r.retaliator(), r.retaliatorSlot()), k -> new StackBattleStats());
                s.damageDealt += r.damage();
                s.hits++;
            }
            case BattleEvent.DeathStare ds -> bumpTrigger(perStack, ds.actor(), ds.actorSlot(), "DEATH_STARE");
            case BattleEvent.Petrifying p -> bumpTrigger(perStack, p.actor(), p.actorSlot(), "PETRIFY");
            case BattleEvent.Cursing c -> bumpTrigger(perStack, c.actor(), c.actorSlot(), "CURSE");
            case BattleEvent.Poisoning p -> bumpTrigger(perStack, p.actor(), p.actorSlot(), "POISON");
            case BattleEvent.Diseasing d -> bumpTrigger(perStack, d.actor(), d.actorSlot(), "DISEASE");
            case BattleEvent.Aging a -> bumpTrigger(perStack, a.actor(), a.actorSlot(), "AGING");
            case BattleEvent.Thunderbolts t -> bumpTrigger(perStack, t.actor(), t.actorSlot(), "THUNDERBOLTS");
            case BattleEvent.TwoBlows t -> bumpTrigger(perStack, t.actor(), t.actorSlot(), "TWO_BLOWS");
            case BattleEvent.TwoShots t -> bumpTrigger(perStack, t.actor(), t.actorSlot(), "TWO_SHOTS");
            case BattleEvent.GoodMorale g -> bumpTrigger(perStack, g.actor(), g.actorSlot(), "MORALE");
            case BattleEvent.FireShield fs -> bumpTrigger(perStack, fs.shielded(), fs.shieldedSlot(), "FIRE_SHIELD");
            case BattleEvent.Rebirth r -> bumpTrigger(perStack, r.actor(), r.actorSlot(), "REBIRTH");
            case BattleEvent.MoveBack mb -> bumpTrigger(perStack, mb.actor(), mb.actorSlot(), "MOVE_BACK");
            case BattleEvent.Defend d -> bumpTrigger(perStack, d.actor(), d.actorSlot(), "DEFEND");
            case BattleEvent.Wait w -> bumpTrigger(perStack, w.actor(), w.actorSlot(), "WAIT");
            default -> {
                // BattleStart, BattleEnd, Move — ignorieren
            }
        }
    }

    private static void bumpTrigger(Map<String, StackBattleStats> perStack, Side side, int slot, String key) {
        StackBattleStats s = perStack.computeIfAbsent(key(side, slot), k -> new StackBattleStats());
        s.triggers.merge(key, 1L, Long::sum);
    }

    private static String key(Side side, int slot) {
        return side + ":" + slot;
    }

    private static List<Stack> buildStacks(List<StackSpec> specs, Side side) {
        int total = specs.size();
        List<Unit> units = new ArrayList<>(total);
        for (StackSpec spec : specs) {
            units.add(UnitCatalog.byName(spec.unitName()).orElseThrow());
        }
        List<Hex> positions = SpawnLayout.assignPositions(side, units);
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

    private static String renderReport(Map<Faction, FactionAggregate> aggregates) {
        StringBuilder sb = new StringBuilder(8192);
        sb.append("# Faction-Mechanik-Diagnose\n\n");
        sb.append("**Setup**: jedes Faction-Pairing über ").append(SEEDS_PER_PAIR)
                .append(" Seeds, beide Rollen (Attacker/Defender) gezählt — insgesamt 9×9×")
                .append(SEEDS_PER_PAIR).append(" = ").append(9 * 9 * SEEDS_PER_PAIR)
                .append(" Sims. Solver: StrategicAutoSolver mit allen aktuellen Heuristiken ")
                .append("(Tank-Wall, Defend-bei-adjacent, Flieger-pickTarget, Multi-Stack-Kite-Suppression).\n\n");

        // Übersichtstabelle
        sb.append("## Faction-Übersicht\n\n");
        sb.append("| Faction | Win-Rate Att | Win-Rate Def | Win-Rate Ø | Ø Damage/Battle | ")
                .append("Ø Hits/Battle | Survivor-Ø | Trigger-Ø |\n");
        sb.append("|--|--|--|--|--|--|--|--|\n");
        List<Faction> sortedByWinrate = new ArrayList<>(FACTIONS);
        sortedByWinrate.sort((a, b) -> Double.compare(aggregates.get(b).overallWinRate(),
                aggregates.get(a).overallWinRate()));
        for (Faction f : sortedByWinrate) {
            FactionAggregate agg = aggregates.get(f);
            sb.append("| ").append(f).append(" | ")
                    .append(String.format(Locale.ROOT, "%.2f", agg.winRateAttacker())).append(" | ")
                    .append(String.format(Locale.ROOT, "%.2f", agg.winRateDefender())).append(" | ")
                    .append(String.format(Locale.ROOT, "%.2f", agg.overallWinRate())).append(" | ")
                    .append(String.format(Locale.ROOT, "%.0f", agg.avgDamagePerBattle())).append(" | ")
                    .append(String.format(Locale.ROOT, "%.1f", agg.avgHitsPerBattle())).append(" | ")
                    .append(String.format(Locale.ROOT, "%.2f", agg.avgSurvivorRatio())).append(" | ")
                    .append(String.format(Locale.ROOT, "%.1f", agg.avgTriggersPerBattle())).append(" |\n");
        }
        sb.append("\n");

        // Per-Faction-Details
        for (Faction f : sortedByWinrate) {
            renderFactionDetail(sb, aggregates.get(f));
        }

        return sb.toString();
    }

    private static void renderFactionDetail(StringBuilder sb, FactionAggregate agg) {
        sb.append("## ").append(agg.faction).append("\n\n");
        sb.append("**Win-Rate Ø**: ").append(String.format(Locale.ROOT, "%.2f", agg.overallWinRate()))
                .append("  •  **Ø Battle-Runden**: ")
                .append(String.format(Locale.ROOT, "%.1f", agg.avgRounds())).append("\n\n");

        // Per Unit (sortiert nach Damage)
        sb.append("### Per-Unit (Ø über alle Battles wo die Faction beteiligt war)\n\n");
        sb.append("| Unit | Damage Ø | Hits Ø | Survivor % Ø | Top-Trigger |\n");
        sb.append("|--|--|--|--|--|\n");
        List<Map.Entry<String, UnitStats>> sortedUnits = new ArrayList<>(agg.perUnit.entrySet());
        sortedUnits.sort((a, b) -> Long.compare(b.getValue().totalDamage, a.getValue().totalDamage));
        for (Map.Entry<String, UnitStats> e : sortedUnits) {
            UnitStats u = e.getValue();
            sb.append("| ").append(e.getKey()).append(" | ")
                    .append(String.format(Locale.ROOT, "%.0f", u.avgDamagePerBattle())).append(" | ")
                    .append(String.format(Locale.ROOT, "%.1f", u.avgHitsPerBattle())).append(" | ")
                    .append(String.format(Locale.ROOT, "%.0f %%", 100 * u.avgSurvivorRatio())).append(" | ")
                    .append(topTriggers(u, 3)).append(" |\n");
        }
        sb.append("\n");
    }

    private static String topTriggers(UnitStats u, int max) {
        List<Map.Entry<String, Long>> sorted = new ArrayList<>(u.triggerTotals.entrySet());
        sorted.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));
        if (sorted.isEmpty()) {
            return "—";
        }
        StringBuilder sb = new StringBuilder();
        int n = Math.min(max, sorted.size());
        for (int i = 0; i < n; i++) {
            if (i > 0) sb.append(", ");
            Map.Entry<String, Long> e = sorted.get(i);
            double perBattle = (double) e.getValue() / Math.max(1, u.appearances);
            sb.append(e.getKey()).append(" ")
                    .append(String.format(Locale.ROOT, "%.1f", perBattle));
        }
        return sb.toString();
    }

    private static final class StackBattleStats {
        long damageDealt;
        int hits;
        Map<String, Long> triggers = new HashMap<>();
    }

    private static final class UnitStats {
        long totalDamage;
        long totalHits;
        long totalSurvivors;
        long totalStartCount;
        long appearances;
        Map<String, Long> triggerTotals = new LinkedHashMap<>();

        double avgDamagePerBattle() {
            return appearances == 0 ? 0 : (double) totalDamage / appearances;
        }

        double avgHitsPerBattle() {
            return appearances == 0 ? 0 : (double) totalHits / appearances;
        }

        double avgSurvivorRatio() {
            return totalStartCount == 0 ? 0 : (double) totalSurvivors / totalStartCount;
        }
    }

    private static final class FactionAggregate {
        final Faction faction;
        long totalBattles;
        long winsAsAttacker;
        long winsAsDefender;
        long totalRounds;
        Map<String, UnitStats> perUnit = new LinkedHashMap<>();

        FactionAggregate(Faction faction) {
            this.faction = faction;
        }

        void recordStack(String unitName, StackBattleStats s, int startCount, int survivors) {
            UnitStats u = perUnit.computeIfAbsent(unitName, k -> new UnitStats());
            u.totalDamage += s.damageDealt;
            u.totalHits += s.hits;
            u.totalSurvivors += survivors;
            u.totalStartCount += startCount;
            u.appearances++;
            for (Map.Entry<String, Long> t : s.triggers.entrySet()) {
                u.triggerTotals.merge(t.getKey(), t.getValue(), Long::sum);
            }
        }

        double winRateAttacker() {
            // totalBattles / 2 = sims als Attacker (gleicher Wert für Defender)
            long asAttacker = totalBattles / 2;
            return asAttacker == 0 ? 0 : (double) winsAsAttacker / asAttacker;
        }

        double winRateDefender() {
            long asDefender = totalBattles / 2;
            return asDefender == 0 ? 0 : (double) winsAsDefender / asDefender;
        }

        double overallWinRate() {
            return totalBattles == 0 ? 0
                    : (double) (winsAsAttacker + winsAsDefender) / totalBattles;
        }

        double avgRounds() {
            return totalBattles == 0 ? 0 : (double) totalRounds / totalBattles;
        }

        double avgDamagePerBattle() {
            long total = 0;
            for (UnitStats u : perUnit.values()) total += u.totalDamage;
            return totalBattles == 0 ? 0 : (double) total / totalBattles;
        }

        double avgHitsPerBattle() {
            long total = 0;
            for (UnitStats u : perUnit.values()) total += u.totalHits;
            return totalBattles == 0 ? 0 : (double) total / totalBattles;
        }

        double avgSurvivorRatio() {
            long surv = 0;
            long start = 0;
            for (UnitStats u : perUnit.values()) {
                surv += u.totalSurvivors;
                start += u.totalStartCount;
            }
            return start == 0 ? 0 : (double) surv / start;
        }

        double avgTriggersPerBattle() {
            long total = 0;
            for (UnitStats u : perUnit.values()) {
                for (long v : u.triggerTotals.values()) total += v;
            }
            return totalBattles == 0 ? 0 : (double) total / totalBattles;
        }
    }
}
