package de.zettsystems.h3comsim.armybattle.application;

import de.zettsystems.h3comsim.battle.domain.Hex;
import de.zettsystems.h3comsim.battle.domain.events.Side;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SpawnLayoutTest {

    @Test
    void seven_slot_layout_uses_rows_0_2_4_5_6_8_10() {
        Set<Integer> attackerRows = new HashSet<>();
        for (int slot = 0; slot < 7; slot++) {
            Hex pos = SpawnLayout.positionFor(Side.ATTACKER, slot, 7);
            assertThat(pos.q()).isZero();
            attackerRows.add(pos.r());
        }
        assertThat(attackerRows).containsExactlyInAnyOrder(0, 2, 4, 5, 6, 8, 10);
    }

    @Test
    void defender_spawns_on_column_14() {
        for (int slot = 0; slot < 7; slot++) {
            Hex pos = SpawnLayout.positionFor(Side.DEFENDER, slot, 7);
            assertThat(pos.q()).isEqualTo(14);
        }
    }

    @Test
    void smaller_armies_centre_around_row_5() {
        // 3 Stacks → mittlere Auswahl {4, 5, 6}.
        Hex slot0 = SpawnLayout.positionFor(Side.ATTACKER, 0, 3);
        Hex slot1 = SpawnLayout.positionFor(Side.ATTACKER, 1, 3);
        Hex slot2 = SpawnLayout.positionFor(Side.ATTACKER, 2, 3);
        assertThat(slot0.r()).isEqualTo(4);
        assertThat(slot1.r()).isEqualTo(5);
        assertThat(slot2.r()).isEqualTo(6);
    }

    @Test
    void single_stack_spawns_in_middle() {
        Hex single = SpawnLayout.positionFor(Side.ATTACKER, 0, 1);
        assertThat(single.r()).isEqualTo(5);
    }

    @Test
    void spawn_hexes_cover_both_sides() {
        Set<Hex> hexes = SpawnLayout.spawnHexesFor(7, 7);
        assertThat(hexes).hasSize(14);
        assertThat(hexes.stream().filter(h -> h.q() == 0).count()).isEqualTo(7);
        assertThat(hexes.stream().filter(h -> h.q() == 14).count()).isEqualTo(7);
    }

    @Test
    void out_of_range_slot_index_throws() {
        assertThatThrownBy(() -> SpawnLayout.positionFor(Side.ATTACKER, 7, 7))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SpawnLayout.positionFor(Side.ATTACKER, -1, 7))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void out_of_range_total_slots_throws() {
        assertThatThrownBy(() -> SpawnLayout.positionFor(Side.ATTACKER, 0, 8))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SpawnLayout.positionFor(Side.ATTACKER, 0, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
