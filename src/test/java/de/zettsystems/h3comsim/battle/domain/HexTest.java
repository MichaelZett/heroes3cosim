package de.zettsystems.h3comsim.battle.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HexTest {

    @Test
    void distance_to_self_is_zero() {
        Hex h = new Hex(3, 4);
        assertThat(h.distanceTo(h)).isZero();
    }

    @Test
    void distance_between_default_starting_positions_is_fourteen() {
        Hex attackerStart = new Hex(0, 5);
        Hex defenderStart = new Hex(14, 5);
        assertThat(attackerStart.distanceTo(defenderStart)).isEqualTo(14);
    }

    @Test
    void neighbors_are_adjacent_in_six_directions() {
        Hex center = new Hex(5, 5);
        assertThat(center.isAdjacent(new Hex(6, 5))).isTrue();
        assertThat(center.isAdjacent(new Hex(4, 5))).isTrue();
        assertThat(center.isAdjacent(new Hex(5, 6))).isTrue();
        assertThat(center.isAdjacent(new Hex(5, 4))).isTrue();
        assertThat(center.isAdjacent(new Hex(6, 4))).isTrue();
        assertThat(center.isAdjacent(new Hex(4, 6))).isTrue();
    }

    @Test
    void distance_two_is_not_adjacent() {
        Hex a = new Hex(0, 0);
        Hex b = new Hex(2, 0);
        assertThat(a.isAdjacent(b)).isFalse();
        assertThat(a.distanceTo(b)).isEqualTo(2);
    }

    @Test
    void self_is_not_adjacent_to_self() {
        Hex h = new Hex(3, 3);
        assertThat(h.isAdjacent(h)).isFalse();
    }
}
