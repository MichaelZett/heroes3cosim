package de.zettsystems.h3comsim.armybattle.application;

import de.zettsystems.h3comsim.armybattle.values.FactionPresetDto;
import de.zettsystems.h3comsim.armybattle.values.StackSpec;
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
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

/**
 * Diagnose-Harness für die Hypothese: Tank-Wall + HP-sortierte Aufstellung clustert
 * gegnerische Schützen mit Tanks, was Power Lich's DEATH_CLOUD-Splash drastisch
 * verstärkt. Vergleicht NEC vs RAM in beiden Layouts (SLOT_DIRECT vs TACTICAL) und
 * misst pro Power-Lich-Schuss:
 * <ul>
 *   <li>Primärschaden (direkt am Ziel)</li>
 *   <li>Splash-Hits (DEATH_CLOUD trifft alle 1-Hex-Nachbarn des Ziels)</li>
 *   <li>Splash-Schaden + Splash-Kills</li>
 * </ul>
 * Ergebnis: {@code build/reports/lich-splash-diagnosis.md}. Falsifiziert oder bestätigt
 * die Hypothese durch konkrete Splash-Hit-Zahlen statt aggregierter Win-Rates.
 */
class LichSplashDiagnosisHarness {

    private static final int SEEDS = 30;

    private enum LayoutMode { SLOT_DIRECT, TACTICAL }

    @Test
    @Disabled("Diagnose-Harness — manuell aktivieren, wenn der Tank-Wall-vs-AoE-Effekt für "
            + "andere Splash/Death-Cloud-Schützen erneut quantifiziert werden soll. "
            + "Snapshot liegt in build/reports/lich-splash-diagnosis.md.")
    void diagnose_lich_splash_in_nec_vs_ram() throws IOException {
        FactionPresetCatalog presets = new FactionPresetCatalog();
        FactionPresetDto nec = presetFor(presets, Faction.NECROPOLIS);
        FactionPresetDto ram = presetFor(presets, Faction.RAMPART);

        LichAggregate slotStats = aggregateLichStats(nec, ram, LayoutMode.SLOT_DIRECT);
        LichAggregate tacticalStats = aggregateLichStats(nec, ram, LayoutMode.TACTICAL);

        String report = renderReport(slotStats, tacticalStats,
                singleBattleBreakdown(nec, ram, 1000L));
        Path reportsDir = Path.of("build", "reports");
        Files.createDirectories(reportsDir);
        Path out = reportsDir.resolve("lich-splash-diagnosis.md");
        Files.writeString(out, report);
        System.out.println("Lich-Diagnose-Report: " + out.toAbsolutePath());
        System.out.println(report);
    }

    private static FactionPresetDto presetFor(FactionPresetCatalog presets, Faction faction) {
        return presets.all().stream()
                .filter(p -> p.faction() == faction)
                .findFirst()
                .orElseThrow();
    }

    private static LichAggregate aggregateLichStats(FactionPresetDto attacker,
                                                    FactionPresetDto defender,
                                                    LayoutMode mode) {
        LichAggregate agg = new LichAggregate();
        for (int s = 0; s < SEEDS; s++) {
            long seed = 1000L + s;
            LichRunStats run = runOne(attacker, defender, seed, mode);
            agg.add(run);
        }
        return agg;
    }

    private static LichRunStats runOne(FactionPresetDto attackerPreset,
                                       FactionPresetDto defenderPreset,
                                       long seed, LayoutMode mode) {
        List<Stack> attackerStacks = buildStacks(attackerPreset.stacks(), Side.ATTACKER, mode);
        List<Stack> defenderStacks = buildStacks(defenderPreset.stacks(), Side.DEFENDER, mode);
        Battlefield bf = buildBattlefield(attackerStacks, defenderStacks, seed);
        BattleSetup setup = new BattleSetup(attackerStacks, defenderStacks, bf);

        Stack lich = findPowerLich(attackerStacks);
        ListEventCollector collector = new ListEventCollector();
        new Battle(new Random(seed), new StrategicAutoSolver(), collector).simulate(setup);

        LichRunStats stats = new LichRunStats();
        // Splash-Hits werden NACH dem Primär-Shoot emittiert (same actor, different target).
        // Wir gruppieren: jede zusammenhängende Sequenz von Lich-Shoot-Events == 1 Schuss.
        // Tatsächlich emittiert die Engine pro applyRangedSplash mehrere Shoot-Events ohne
        // dazwischen ein anderes Event vom selben Lich — wir zählen einfach alle Lich-Shoots,
        // und identifizieren primary vs splash über consecutive index.
        List<BattleEvent> events = collector.events();
        int i = 0;
        while (i < events.size()) {
            BattleEvent e = events.get(i);
            if (isLichShoot(e, lich)) {
                // Primär-Schuss
                BattleEvent.Shoot primary = (BattleEvent.Shoot) e;
                stats.shootCount++;
                stats.primaryDamage += primary.damage();
                stats.primaryKills += primary.killed();
                // Folgende Lich-Shoots sind Splash-Hits (bis nicht-Lich-Event kommt).
                int j = i + 1;
                while (j < events.size() && isLichShoot(events.get(j), lich)) {
                    BattleEvent.Shoot splash = (BattleEvent.Shoot) events.get(j);
                    stats.splashHits++;
                    stats.splashDamage += splash.damage();
                    stats.splashKills += splash.killed();
                    j++;
                }
                i = j;
            } else {
                i++;
            }
        }
        return stats;
    }

    private static boolean isLichShoot(BattleEvent e, Stack lich) {
        return e instanceof BattleEvent.Shoot s
                && s.actor() == lich.side()
                && s.actorSlot() == lich.slot();
    }

    private static Stack findPowerLich(List<Stack> stacks) {
        return stacks.stream()
                .filter(s -> s.unit() == UnitCatalog.POWER_LICH)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Power Lich not in attacker stacks"));
    }

    private static String singleBattleBreakdown(FactionPresetDto attackerPreset,
                                                FactionPresetDto defenderPreset, long seed) {
        StringBuilder sb = new StringBuilder();
        for (LayoutMode mode : LayoutMode.values()) {
            sb.append("### ").append(mode).append(" (Seed ").append(seed).append(")\n\n");
            List<Stack> a = buildStacks(attackerPreset.stacks(), Side.ATTACKER, mode);
            List<Stack> d = buildStacks(defenderPreset.stacks(), Side.DEFENDER, mode);
            Battlefield bf = buildBattlefield(a, d, seed);
            BattleSetup setup = new BattleSetup(a, d, bf);
            Stack lich = findPowerLich(a);
            sb.append("Power Lich startet auf ").append(lich.position())
                    .append(" — Adjacents zu RAM-Schützen werden hier sichtbar.\n\n");
            ListEventCollector collector = new ListEventCollector();
            new Battle(new Random(seed), new StrategicAutoSolver(), collector).simulate(setup);

            List<BattleEvent> events = collector.events();
            int shotIndex = 0;
            int i = 0;
            sb.append("| Schuss | Primärziel | Primär-Damage | Splash-Hits | Splash-Damage |\n");
            sb.append("|--|--|--|--|--|\n");
            while (i < events.size()) {
                BattleEvent e = events.get(i);
                if (isLichShoot(e, lich)) {
                    BattleEvent.Shoot primary = (BattleEvent.Shoot) e;
                    shotIndex++;
                    int splashHits = 0;
                    int splashDamage = 0;
                    int j = i + 1;
                    while (j < events.size() && isLichShoot(events.get(j), lich)) {
                        BattleEvent.Shoot splash = (BattleEvent.Shoot) events.get(j);
                        splashHits++;
                        splashDamage += splash.damage();
                        j++;
                    }
                    sb.append("| ").append(shotIndex).append(" | ")
                            .append(primary.target()).append("/slot").append(primary.targetSlot()).append(" | ")
                            .append(primary.damage()).append(" | ")
                            .append(splashHits).append(" | ")
                            .append(splashDamage).append(" |\n");
                    i = j;
                } else {
                    i++;
                }
            }
            if (shotIndex == 0) {
                sb.append("| _(keiner)_ | — | — | — | — |\n");
            }
            sb.append("\n");
        }
        return sb.toString();
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

    private static String renderReport(LichAggregate slot, LichAggregate tactical,
                                       String singleBattle) {
        StringBuilder sb = new StringBuilder(2048);
        sb.append("# Lich-Splash-Diagnose: NEC vs RAM\n\n");
        sb.append("**Hypothese**: Tank-Wall + HP-sortierte Aufstellung clustert gegnerische ")
                .append("Schützen mit Tanks, was Power Lich's DEATH_CLOUD signifikant verstärkt.\n\n");
        sb.append("**Setup**: ").append(SEEDS).append(" Seeds, NEC als Attacker, RAM als Defender, ")
                .append("StrategicAutoSolver konstant. Splash-Hits = Lich-Shoot-Events die ")
                .append("unmittelbar auf einen Primär-Schuss folgen (gleicher actor, gleiche Runde).\n\n");

        sb.append("## Aggregat über ").append(SEEDS).append(" Seeds\n\n");
        sb.append("| Metrik | Slot-Direct | Tactical | Δ | Δ % |\n");
        sb.append("|--|--|--|--|--|\n");
        appendRow(sb, "Lich-Schüsse total", slot.shootCount, tactical.shootCount);
        appendRow(sb, "Primärschaden total", slot.primaryDamage, tactical.primaryDamage);
        appendRow(sb, "Primärkills total", slot.primaryKills, tactical.primaryKills);
        appendRow(sb, "Splash-Hits total", slot.splashHits, tactical.splashHits);
        appendRow(sb, "Splash-Schaden total", slot.splashDamage, tactical.splashDamage);
        appendRow(sb, "Splash-Kills total", slot.splashKills, tactical.splashKills);
        sb.append("\n");

        sb.append("**Splash pro Schuss**: Slot-Direct ")
                .append(formatRatio(slot.splashHits, slot.shootCount))
                .append(", Tactical ")
                .append(formatRatio(tactical.splashHits, tactical.shootCount))
                .append(".\n\n");
        sb.append("**Gesamtschaden pro Schuss**: Slot-Direct ")
                .append(formatRatio(slot.primaryDamage + slot.splashDamage, slot.shootCount))
                .append(", Tactical ")
                .append(formatRatio(tactical.primaryDamage + tactical.splashDamage, tactical.shootCount))
                .append(".\n\n");

        sb.append("## Konkrete Battle-Aufschlüsselung\n\n");
        sb.append(singleBattle);
        return sb.toString();
    }

    private static void appendRow(StringBuilder sb, String label, long slotValue, long tacticalValue) {
        long delta = tacticalValue - slotValue;
        double pct = slotValue == 0 ? 0 : 100.0 * delta / slotValue;
        sb.append("| ").append(label).append(" | ")
                .append(slotValue).append(" | ")
                .append(tacticalValue).append(" | ")
                .append(String.format(Locale.ROOT, "%+d", delta)).append(" | ")
                .append(String.format(Locale.ROOT, "%+.1f %%", pct)).append(" |\n");
    }

    private static String formatRatio(long numerator, long denominator) {
        if (denominator == 0) {
            return "—";
        }
        return String.format(Locale.ROOT, "%.2f", (double) numerator / denominator);
    }

    private static final class LichRunStats {
        int shootCount;
        long primaryDamage;
        int primaryKills;
        int splashHits;
        long splashDamage;
        int splashKills;
    }

    private static final class LichAggregate {
        int shootCount;
        long primaryDamage;
        int primaryKills;
        int splashHits;
        long splashDamage;
        int splashKills;

        void add(LichRunStats r) {
            shootCount += r.shootCount;
            primaryDamage += r.primaryDamage;
            primaryKills += r.primaryKills;
            splashHits += r.splashHits;
            splashDamage += r.splashDamage;
            splashKills += r.splashKills;
        }
    }
}
