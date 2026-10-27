package de.zettsystems.h3comsim.matrix.values;

import de.zettsystems.h3comsim.battle.domain.Faction;
import de.zettsystems.h3comsim.battle.domain.MagicTerrain;
import de.zettsystems.h3comsim.battle.domain.SolverKind;
import de.zettsystems.h3comsim.battle.domain.Terrain;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FactionMatrixRequestTest {

    private static final Set<Faction> TWO = Set.of(Faction.CASTLE, Faction.TOWER);
    private static final Set<SolverKind> ONE = Set.of(SolverKind.GREEDY);

    @Test
    void defaults_cover_all_towns_and_both_solvers() {
        FactionMatrixRequest request = new FactionMatrixRequestDto(null, null, null, null, null, null, null)
                .toApplication();

        assertThat(request.factions()).hasSize(9).doesNotContain(Faction.NEUTRAL);
        assertThat(request.solvers()).containsExactlyInAnyOrder(SolverKind.values());
        assertThat(request.seedsPerPair()).isEqualTo(10);
        assertThat(request.weeks()).isEqualTo(1);
        assertThat(request.heroes()).isTrue();
        assertThat(request.totalSims()).isEqualTo(81 * 10 * 2);
    }

    @Test
    void invalid_requests_are_rejected() {
        Set<Faction> one = Set.of(Faction.CASTLE);
        Set<Faction> neutral = Set.of(Faction.CASTLE, Faction.NEUTRAL);
        Set<SolverKind> none = Set.of();
        assertThatThrownBy(() -> new FactionMatrixRequest(one, ONE, 1, 1, true, null, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new FactionMatrixRequest(neutral, ONE, 1, 1, true, null, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new FactionMatrixRequest(TWO, none, 1, 1, true, null, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new FactionMatrixRequest(TWO, ONE, 0, 1, true, null, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new FactionMatrixRequest(TWO, ONE, 1, 0, true, null, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new FactionMatrixRequest(TWO, ONE, 1, 1, true, Terrain.WATER,
                MagicTerrain.MAGIC_PLAINS)).isInstanceOf(IllegalArgumentException.class);
    }
}
