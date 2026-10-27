package de.zettsystems.h3comsim.matrix.application;

import de.zettsystems.h3comsim.armybattle.application.DefaultArmyBattleService;
import de.zettsystems.h3comsim.armybattle.application.FactionPresetCatalog;
import de.zettsystems.h3comsim.battle.domain.Faction;
import de.zettsystems.h3comsim.battle.domain.SolverKind;
import de.zettsystems.h3comsim.battle.domain.Terrain;
import de.zettsystems.h3comsim.matrix.values.FactionMatrixReport;
import de.zettsystems.h3comsim.matrix.values.FactionMatrixRequest;
import de.zettsystems.h3comsim.matrix.values.FactionPairing;
import de.zettsystems.h3comsim.matrix.values.FactionStanding;
import de.zettsystems.h3comsim.matrix.values.SolverMatrix;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FactionMatrixExperimentTest {

    private final FactionMatrixExperiment experiment =
            new FactionMatrixExperiment(new DefaultArmyBattleService(), new FactionPresetCatalog(), 50);

    @Test
    void every_ordered_pairing_and_the_mirror_are_played_per_solver() {
        FactionMatrixRequest request = new FactionMatrixRequest(
                Set.of(Faction.TOWER, Faction.CASTLE, Faction.INFERNO), EnumSet.allOf(SolverKind.class),
                2, 1, true, null, null);
        AtomicInteger progress = new AtomicInteger();

        FactionMatrixReport report = experiment.run(request, (completed, total) -> progress.accumulateAndGet(completed, Math::max));

        assertThat(progress).hasValue(request.totalSims()).hasValue(3 * 3 * 2 * 2);
        assertThat(report.factions()).containsExactly(Faction.CASTLE, Faction.TOWER, Faction.INFERNO);
        assertThat(report.matrices()).extracting(SolverMatrix::solver)
                .containsExactly(SolverKind.GREEDY, SolverKind.STRATEGIC);
        for (SolverMatrix matrix : report.matrices()) {
            assertThat(matrix.pairings()).hasSize(9);
            for (FactionPairing cell : matrix.pairings()) {
                // Ohne Spiegel beide Rollen, im Spiegel nur eine Folge von Seeds.
                assertThat(cell.sims()).isEqualTo(cell.faction() == cell.opponent() ? 2 : 4);
                assertThat(cell.wins() + cell.losses() + cell.draws()).isEqualTo(cell.sims());
                FactionPairing back = cell(matrix, cell.opponent(), cell.faction());
                if (cell.faction() != cell.opponent()) {
                    assertThat(back.wins()).isEqualTo(cell.losses());
                }
            }
            assertThat(matrix.standings()).extracting(FactionStanding::sims).containsOnly(8);
            assertThat(matrix.standings()).isSortedAccordingTo(
                    (a, b) -> Double.compare(b.winRate(), a.winRate()));
            assertThat(matrix.mirrorSideBias()).isBetween(0.0, 0.5);
        }
    }

    @Test
    void same_seeds_give_same_result_and_options_reach_the_battles() {
        FactionMatrixRequest heroes = new FactionMatrixRequest(Set.of(Faction.RAMPART, Faction.DUNGEON),
                Set.of(SolverKind.STRATEGIC), 3, 1, true, Terrain.GRASS, null);
        FactionMatrixRequest bare = new FactionMatrixRequest(Set.of(Faction.RAMPART, Faction.DUNGEON),
                Set.of(SolverKind.STRATEGIC), 3, 2, false, Terrain.GRASS, null);

        List<FactionPairing> first = experiment.run(heroes, ProgressListener.NOOP).matrices().getFirst().pairings();
        List<FactionPairing> again = experiment.run(heroes, ProgressListener.NOOP).matrices().getFirst().pairings();
        FactionMatrixReport other = experiment.run(bare, ProgressListener.NOOP);

        assertThat(again).isEqualTo(first);
        assertThat(other.weeks()).isEqualTo(2);
        assertThat(other.heroes()).isFalse();
        assertThat(other.terrain()).isEqualTo(Terrain.GRASS);
        assertThat(other.matrices().getFirst().pairings()).hasSize(4);
    }

    @Test
    void parallelism_must_be_a_percentage() {
        DefaultArmyBattleService service = new DefaultArmyBattleService();
        FactionPresetCatalog presets = new FactionPresetCatalog();

        assertThatThrownBy(() -> new FactionMatrixExperiment(service, presets, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new FactionMatrixExperiment(service, presets, 101))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static FactionPairing cell(SolverMatrix matrix, Faction faction, Faction opponent) {
        return matrix.pairings().stream()
                .filter(p -> p.faction() == faction && p.opponent() == opponent)
                .findFirst().orElseThrow();
    }
}
