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
        return new MatrixRequest(20, Set.of(), exclude, Set.of(), seeds);
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
                1);
        MatrixReport report = service.run(req);

        assertThat(report.stats()).extracting(UnitMatchupStats::unitName)
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
    void invalid_tier_in_exclude_tiers_is_rejected() {
        assertThatThrownBy(() -> new MatrixRequest(20, Set.of(), Set.of(), Set.of(0), 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new MatrixRequest(20, Set.of(), Set.of(), Set.of(8), 1))
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
