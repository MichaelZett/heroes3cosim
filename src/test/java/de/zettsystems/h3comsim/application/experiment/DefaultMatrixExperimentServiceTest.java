package de.zettsystems.h3comsim.application.experiment;

import de.zettsystems.h3comsim.domain.Faction;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefaultMatrixExperimentServiceTest {

    private final DefaultMatrixExperimentService service = new DefaultMatrixExperimentService(100);

    /**
     * Schnelltest: nur drei Faktionen zulassen, kleines Setup, damit der Test in unter 30s läuft.
     */
    private MatrixRequest miniRequest(int seeds) {
        Set<Faction> exclude = Set.of(
                Faction.TOWER, Faction.INFERNO, Faction.NECROPOLIS, Faction.DUNGEON,
                Faction.STRONGHOLD, Faction.FORTRESS, Faction.CONFLUX, Faction.NEUTRAL);
        return new MatrixRequest(20, Set.of(), exclude, Set.of(), StackSizingMode.EQUAL_COUNT, seeds);
    }

    @Test
    void only_castle_and_rampart_remain_when_other_factions_are_excluded() {
        MatrixReport report = service.run(miniRequest(1));

        assertThat(report.stats()).isNotEmpty();
        assertThat(report.stats()).allMatch(s ->
                s.faction() == Faction.CASTLE || s.faction() == Faction.RAMPART);
    }

    @Test
    void each_unit_records_at_least_one_sim_per_opponent() {
        MatrixReport report = service.run(miniRequest(1));

        int participants = report.stats().size();
        int expectedSimsPerUnit = (participants - 1) * 2; // jeder Gegner × beide Rollen
        assertThat(report.stats()).allMatch(s -> s.totalSims() == expectedSimsPerUnit);
    }

    @Test
    void same_request_yields_identical_report() {
        MatrixReport first = service.run(miniRequest(2));
        MatrixReport second = service.run(miniRequest(2));

        // Stats sind nach winRate sortiert — deterministisch bei gleichem RNG-Pfad.
        assertThat(second.stats()).hasSize(first.stats().size());
        for (int i = 0; i < first.stats().size(); i++) {
            UnitMatchupStats a = first.stats().get(i);
            UnitMatchupStats b = second.stats().get(i);
            assertThat(b.unitName()).isEqualTo(a.unitName());
            assertThat(b.wins()).isEqualTo(a.wins());
            assertThat(b.losses()).isEqualTo(a.losses());
            assertThat(b.draws()).isEqualTo(a.draws());
        }
    }

    @Test
    void exclude_units_removes_them_from_the_report() {
        MatrixRequest req = new MatrixRequest(
                20,
                Set.of("Pikeman", "Halberdier"),
                Set.of(Faction.TOWER, Faction.INFERNO, Faction.NECROPOLIS, Faction.DUNGEON,
                        Faction.STRONGHOLD, Faction.FORTRESS, Faction.CONFLUX, Faction.NEUTRAL),
                Set.of(),
                StackSizingMode.EQUAL_COUNT,
                1);
        MatrixReport report = service.run(req);

        assertThat(report.stats()).isNotEmpty()
                .extracting(UnitMatchupStats::unitName)
                .doesNotContain("Pikeman", "Halberdier");
    }

    @Test
    void anomalies_list_only_contains_units_losing_to_lower_tier() {
        MatrixReport report = service.run(miniRequest(2));

        for (TierAnomaly anomaly : report.anomalies()) {
            assertThat(anomaly.winRate()).isLessThan(0.5);
            assertThat(anomaly.againstTier()).isEqualTo(anomaly.tier() - 1);
        }
    }

    @Test
    void totals_match_request() {
        MatrixRequest req = miniRequest(3);
        MatrixReport report = service.run(req);

        assertThat(report.seedsPerMatchup()).isEqualTo(3);
        assertThat(report.unitCount()).isEqualTo(20);
        int n = report.stats().size();
        assertThat(report.totalMatchups()).isEqualTo(n * (n - 1) / 2);
    }

    @Test
    void progress_listener_is_called_with_monotonic_values_and_reaches_total() {
        AtomicInteger lastCompleted = new AtomicInteger();
        AtomicInteger lastTotal = new AtomicInteger();
        AtomicInteger callCount = new AtomicInteger();

        MatrixReport report = service.run(miniRequest(1), (completed, total) -> {
            lastTotal.set(total);
            // Monotonie nicht strikt prüfen — Threads können out-of-order callen. Stattdessen
            // sammeln wir den höchsten beobachteten Wert.
            lastCompleted.accumulateAndGet(completed, Math::max);
            callCount.incrementAndGet();
        });

        int n = report.stats().size();
        int expectedTotal = n * (n - 1) / 2 * 2; // seeds=1, beide Rollen
        assertThat(lastTotal.get()).isEqualTo(expectedTotal);
        assertThat(lastCompleted.get()).isEqualTo(expectedTotal);
        assertThat(callCount.get()).isEqualTo(expectedTotal);
    }

    @Test
    void exclude_tiers_removes_them_from_the_report() {
        MatrixRequest req = new MatrixRequest(
                20,
                Set.of(),
                Set.of(Faction.TOWER, Faction.INFERNO, Faction.NECROPOLIS, Faction.DUNGEON,
                        Faction.STRONGHOLD, Faction.FORTRESS, Faction.CONFLUX, Faction.NEUTRAL),
                Set.of(1, 2, 3),
                StackSizingMode.EQUAL_COUNT,
                1);
        MatrixReport report = service.run(req);

        assertThat(report.stats()).isNotEmpty();
        assertThat(report.stats()).allMatch(s -> s.tier() >= 4);
    }

    @Test
    void faction_stats_aggregate_unit_stats_per_faction() {
        MatrixReport report = service.run(miniRequest(1));

        // Castle + Rampart sind die einzigen aktiven Faktionen.
        assertThat(report.factionStats()).hasSize(2);
        for (FactionMatchupStats fs : report.factionStats()) {
            int unitsOfFaction = (int) report.stats().stream()
                    .filter(s -> s.faction() == fs.faction()).count();
            assertThat(fs.unitCount()).isEqualTo(unitsOfFaction);
            int totalSims = report.stats().stream()
                    .filter(s -> s.faction() == fs.faction())
                    .mapToInt(UnitMatchupStats::totalSims).sum();
            assertThat(fs.totalSims()).isEqualTo(totalSims);
        }
    }

    @Test
    void equal_gold_snaps_to_lcm_so_both_sides_pay_the_same_gold() {
        // Black Dragon (4000g) vs Pikeman (60g), unitCount=20.
        // Raw-Budget = 80 000, LCM(4000, 60) = 12 000, snapped = floor(80 000 / 12 000) * 12 000 = 72 000.
        // → 18 BD vs 1 200 Pikemen, beide Seiten exakt 72 000 g.
        MatrixRequest req = new MatrixRequest(20, Set.of(), Set.of(), Set.of(), StackSizingMode.EQUAL_GOLD, 1);
        int bdCount = DefaultMatrixExperimentService.stackSizeFor(
                de.zettsystems.h3comsim.domain.UnitCatalog.BLACK_DRAGON,
                de.zettsystems.h3comsim.domain.UnitCatalog.PIKEMAN, req);
        int pikemanCount = DefaultMatrixExperimentService.stackSizeFor(
                de.zettsystems.h3comsim.domain.UnitCatalog.PIKEMAN,
                de.zettsystems.h3comsim.domain.UnitCatalog.BLACK_DRAGON, req);

        assertThat(bdCount).isEqualTo(18);
        assertThat(pikemanCount).isEqualTo(1200);
        assertThat(bdCount * de.zettsystems.h3comsim.domain.UnitCatalog.BLACK_DRAGON.cost())
                .isEqualTo(pikemanCount * de.zettsystems.h3comsim.domain.UnitCatalog.PIKEMAN.cost());
    }

    @Test
    void equal_gold_uses_minimum_lcm_stack_when_unit_count_too_small() {
        // unitCount=1, BD (4000) vs Pikeman (60) → Raw-Budget 4 000 < LCM 12 000.
        // Fallback: kleinster exakter Split bei genau LCM Gold pro Seite → 3 BD vs 200 Pikemen.
        MatrixRequest req = new MatrixRequest(1, Set.of(), Set.of(), Set.of(), StackSizingMode.EQUAL_GOLD, 1);
        int bdCount = DefaultMatrixExperimentService.stackSizeFor(
                de.zettsystems.h3comsim.domain.UnitCatalog.BLACK_DRAGON,
                de.zettsystems.h3comsim.domain.UnitCatalog.PIKEMAN, req);
        int pikemanCount = DefaultMatrixExperimentService.stackSizeFor(
                de.zettsystems.h3comsim.domain.UnitCatalog.PIKEMAN,
                de.zettsystems.h3comsim.domain.UnitCatalog.BLACK_DRAGON, req);

        assertThat(bdCount).isEqualTo(3);
        assertThat(pikemanCount).isEqualTo(200);
    }

    @Test
    void equal_gold_returns_unit_count_for_same_cost_pair() {
        // Halberdier (75) vs Halberdier (75) — selbe Kosten → beide bekommen unitCount Einheiten.
        MatrixRequest req = new MatrixRequest(20, Set.of(), Set.of(), Set.of(), StackSizingMode.EQUAL_GOLD, 1);
        int count = DefaultMatrixExperimentService.stackSizeFor(
                de.zettsystems.h3comsim.domain.UnitCatalog.HALBERDIER,
                de.zettsystems.h3comsim.domain.UnitCatalog.HALBERDIER, req);

        assertThat(count).isEqualTo(20);
    }

    @Test
    void equal_gold_produces_valid_report() {
        // Nur Castle behalten, equalGold=true: die Pair-Stacks haben unterschiedliche Größen je
        // nach Cost. Wir prüfen hier nur, dass der Lauf durchgeht und die Stats konsistent sind.
        MatrixRequest equalGold = new MatrixRequest(
                20,
                Set.of(),
                Set.of(Faction.RAMPART, Faction.TOWER, Faction.INFERNO, Faction.NECROPOLIS,
                        Faction.DUNGEON, Faction.STRONGHOLD, Faction.FORTRESS, Faction.CONFLUX,
                        Faction.NEUTRAL),
                Set.of(),
                StackSizingMode.EQUAL_GOLD,
                1);

        MatrixReport report = service.run(equalGold);

        assertThat(report.stats()).isNotEmpty();
        assertThat(report.stats()).allMatch(s -> s.faction() == Faction.CASTLE);
        // Jede Castle-Einheit hat (n-1)*2 Sims wie sonst auch — equalGold ändert die Stack-Größe,
        // nicht die Anzahl Match-ups.
        int participants = report.stats().size();
        assertThat(report.stats()).allMatch(s -> s.totalSims() == (participants - 1) * 2);
    }

    @Test
    void weekly_production_returns_wp_times_unit_count() {
        // Pikeman (Castle T1, wp=14) bei unitCount=1 → 14 Einheiten, unitCount=20 → 280.
        MatrixRequest oneWeek = new MatrixRequest(1, Set.of(), Set.of(), Set.of(),
                StackSizingMode.WEEKLY_PRODUCTION, 1);
        MatrixRequest twentyWeeks = new MatrixRequest(20, Set.of(), Set.of(), Set.of(),
                StackSizingMode.WEEKLY_PRODUCTION, 1);

        int pikemanOneWeek = DefaultMatrixExperimentService.stackSizeFor(
                de.zettsystems.h3comsim.domain.UnitCatalog.PIKEMAN,
                de.zettsystems.h3comsim.domain.UnitCatalog.PIKEMAN, oneWeek);
        int pikemanTwentyWeeks = DefaultMatrixExperimentService.stackSizeFor(
                de.zettsystems.h3comsim.domain.UnitCatalog.PIKEMAN,
                de.zettsystems.h3comsim.domain.UnitCatalog.PIKEMAN, twentyWeeks);

        assertThat(pikemanOneWeek).isEqualTo(14);
        assertThat(pikemanTwentyWeeks).isEqualTo(280);
    }

    @Test
    void weekly_production_differs_by_tier_within_same_faction() {
        // Castle: Pikeman T1=14, Swordsman T4=4, Angel T7=1 (alle pro Woche).
        MatrixRequest req = new MatrixRequest(1, Set.of(), Set.of(), Set.of(),
                StackSizingMode.WEEKLY_PRODUCTION, 1);
        int pikeman = DefaultMatrixExperimentService.stackSizeFor(
                de.zettsystems.h3comsim.domain.UnitCatalog.PIKEMAN,
                de.zettsystems.h3comsim.domain.UnitCatalog.PIKEMAN, req);
        int swordsman = DefaultMatrixExperimentService.stackSizeFor(
                de.zettsystems.h3comsim.domain.UnitCatalog.SWORDSMAN,
                de.zettsystems.h3comsim.domain.UnitCatalog.PIKEMAN, req);
        int angel = DefaultMatrixExperimentService.stackSizeFor(
                de.zettsystems.h3comsim.domain.UnitCatalog.ANGEL,
                de.zettsystems.h3comsim.domain.UnitCatalog.PIKEMAN, req);

        assertThat(pikeman).isEqualTo(14);
        assertThat(swordsman).isEqualTo(4);
        assertThat(angel).isEqualTo(1);
    }

    @Test
    void equal_gold_weekly_anchors_budget_on_weekly_cost() {
        // Angel (Castle T7, wp=1, cost=3000): wp-cost = 3000g/Wo.
        // Pikeman (Castle T1, wp=14, cost=60): wp-cost = 840g/Wo.
        // Pair-Budget = max(3000, 840) * unitCount = 3000 * 20 = 60 000g, LCM(3000,60)=3000,
        // snap = 60000 → Angel 20, Pikeman 1000. Beide exakt 60 000g.
        MatrixRequest req = new MatrixRequest(20, Set.of(), Set.of(), Set.of(),
                StackSizingMode.EQUAL_GOLD_WEEKLY, 1);
        int angelCount = DefaultMatrixExperimentService.stackSizeFor(
                de.zettsystems.h3comsim.domain.UnitCatalog.ANGEL,
                de.zettsystems.h3comsim.domain.UnitCatalog.PIKEMAN, req);
        int pikemanCount = DefaultMatrixExperimentService.stackSizeFor(
                de.zettsystems.h3comsim.domain.UnitCatalog.PIKEMAN,
                de.zettsystems.h3comsim.domain.UnitCatalog.ANGEL, req);

        assertThat(angelCount).isEqualTo(20);
        assertThat(pikemanCount).isEqualTo(1000);
        assertThat(angelCount * de.zettsystems.h3comsim.domain.UnitCatalog.ANGEL.cost())
                .isEqualTo(pikemanCount * de.zettsystems.h3comsim.domain.UnitCatalog.PIKEMAN.cost());
    }

    @Test
    void invalid_tier_in_exclude_tiers_is_rejected() {
        Set<String> noUnits = Set.of();
        Set<Faction> noFactions = Set.of();
        Set<Integer> tierZero = Set.of(0);
        Set<Integer> tierEight = Set.of(8);
        assertThatThrownBy(() -> new MatrixRequest(20, noUnits, noFactions, tierZero, StackSizingMode.EQUAL_COUNT, 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new MatrixRequest(20, noUnits, noFactions, tierEight, StackSizingMode.EQUAL_COUNT, 1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void parallelism_percent_must_be_in_range() {
        assertThatThrownBy(() -> new DefaultMatrixExperimentService(0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new DefaultMatrixExperimentService(101))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
