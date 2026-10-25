package de.zettsystems.h3comsim.armybattle.application;

import de.zettsystems.h3comsim.armybattle.values.ArmyBattleRequest;
import de.zettsystems.h3comsim.armybattle.values.ArmyBattleSimulation;
import de.zettsystems.h3comsim.armybattle.values.ArmySpec;
import de.zettsystems.h3comsim.armybattle.values.FactionPresetDto;
import de.zettsystems.h3comsim.armybattle.values.StackSpec;
import de.zettsystems.h3comsim.battle.domain.Faction;
import de.zettsystems.h3comsim.battle.domain.Fortification;
import de.zettsystems.h3comsim.battle.domain.SecondarySkill;
import de.zettsystems.h3comsim.battle.domain.SkillLevel;
import de.zettsystems.h3comsim.battle.domain.events.BattleEvent;
import de.zettsystems.h3comsim.battle.domain.events.Winner;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Was bringt die Befestigung? Spiegel-Duell über den Army-Battle-Endpunkt: dieselbe Armee
 * (eine Wochenproduktion) auf beiden Seiten, Standardhelden; der Verteidiger steht in einer Stadt
 * seiner Fraktion mit Fort, Citadel oder Castle, zum Vergleich ohne Befestigung. Dazu derselbe
 * Lauf mit Expert Ballistics beim Angreifer, und alles noch einmal mit doppelter und vierfacher
 * Angreifer-Armee. Gemessen: Win-Rate des Verteidigers, Remis, Runden.
 *
 * <p>Aktivieren: {@code .\gradlew.bat test --tests "*SiegeImpactTest" "-Ph3.harness=siege-impact"}.
 * Report unter {@code build/reports/siege-impact.md}.
 */
@EnabledIfSystemProperty(named = "h3.harness", matches = "siege-impact")
class SiegeImpactTest {

    private static final int SEEDS = 40;

    private final DefaultArmyBattleService service = new DefaultArmyBattleService();

    @Test
    void fortifications_versus_open_field() throws Exception {
        List<FactionPresetDto> presets = new FactionPresetCatalog().all().stream()
                .filter(p -> p.faction() != Faction.NEUTRAL).toList();
        List<@Nullable Fortification> levels = new ArrayList<>();
        levels.add(null);
        levels.addAll(Arrays.asList(Fortification.values()));
        StringBuilder sb = new StringBuilder(4096);
        sb.append("# Wirkung der Stadtbefestigung\n\n")
                .append("Spiegel-Duell, Standardhelden, eine Wochenproduktion; der Verteidiger steht in seiner Stadt. ")
                .append(SEEDS).append(" Seeds je Fraktion, StrategicAutoSolver.\n\n")
                .append("| Angreifer | Ballistics | Befestigung | Kämpfe | Siege Verteidiger | Siege Angreifer | Remis | Win-Rate Verteidiger | Ø Runden | Ø Katapulttreffer |\n")
                .append("|--|--|--|--|--|--|--|--|--|--|\n");
        for (int strength : List.of(1, 2, 4)) {
            for (SkillLevel ballistics : List.of(SkillLevel.NONE, SkillLevel.EXPERT)) {
                for (Fortification level : levels) {
                    row(sb, strength, ballistics, level, run(presets, level, ballistics, strength));
                }
            }
        }
        String report = sb.toString();
        Path reportsDir = Path.of("build", "reports");
        Files.createDirectories(reportsDir);
        Path out = reportsDir.resolve("siege-impact.md");
        Files.writeString(out, report);
        IO.println("Siege-Impact-Report: " + out.toAbsolutePath());
        IO.println(report);
    }

    private static void row(StringBuilder sb, int strength, SkillLevel ballistics, @Nullable Fortification level,
                            int[] t) {
        int decided = t[0] + t[1];
        sb.append("| ×").append(strength).append(" | ").append(ballistics).append(" | ").append(level == null ? "Feld" : level.name())
                        .append(" | ").append(t[3]).append(" | ").append(t[0]).append(" | ").append(t[1])
                        .append(" | ").append(t[2]).append(" | ")
                        .append(String.format(Locale.ROOT, "%.3f", decided == 0 ? 0.5 : (double) t[0] / decided))
                        .append(" | ").append(String.format(Locale.ROOT, "%.1f", (double) t[4] / t[3]))
                        .append(" | ").append(String.format(Locale.ROOT, "%.1f", (double) t[5] / t[3]))
                        .append(" |\n");
    }

    /** Siege Verteidiger, Siege Angreifer, Remis, Kämpfe, Runden, Katapulttreffer. */
    private int[] run(List<FactionPresetDto> presets, @Nullable Fortification level, SkillLevel ballistics,
                      int strength) throws Exception {
        List<Future<int[]>> futures = new ArrayList<>();
        try (ExecutorService pool = Executors.newWorkStealingPool()) {
            for (FactionPresetDto preset : presets) {
                for (int s = 0; s < SEEDS; s++) {
                    long seed = preset.faction().ordinal() * 7919L + s;
                    futures.add(pool.submit(() -> {
                        Map<SecondarySkill, SkillLevel> skills = ballistics == SkillLevel.NONE
                                ? Map.of() : Map.of(SecondarySkill.BALLISTICS, ballistics);
                        List<StackSpec> stronger = preset.stacks().stream()
                                .map(spec -> new StackSpec(spec.unitName(), spec.count() * strength)).toList();
                        ArmySpec attacker = new ArmySpec(stronger, null, null, null, null, null, null, null,
                                null, skills, null, null, null);
                        ArmySpec defender = new ArmySpec(preset.stacks(), null);
                        ArmyBattleSimulation sim = service.simulate(
                                new ArmyBattleRequest(attacker, defender, seed, level), seed);
                        int hits = (int) sim.events().stream()
                                .filter(e -> e instanceof BattleEvent.CatapultShot c && c.damage() > 0).count();
                        Winner w = sim.result().winner();
                        return new int[] {w == Winner.DEFENDER ? 1 : 0, w == Winner.ATTACKER ? 1 : 0,
                                w == Winner.DRAW ? 1 : 0, 1, sim.result().turnsTaken(), hits};
                    }));
                }
            }
        }
        int[] total = new int[6];
        for (Future<int[]> f : futures) {
            int[] r = f.get();
            for (int i = 0; i < total.length; i++) {
                total[i] += r[i];
            }
        }
        return total;
    }
}
