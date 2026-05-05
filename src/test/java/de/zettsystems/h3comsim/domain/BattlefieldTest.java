package de.zettsystems.h3comsim.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BattlefieldTest {

    private final Battlefield field = Battlefield.STANDARD;

    @Test
    void standard_battlefield_is_15_by_11() {
        assertThat(field.width()).isEqualTo(15);
        assertThat(field.height()).isEqualTo(11);
    }

    @Test
    void contains_origin_and_excludes_negative_and_oversized_coordinates() {
        assertThat(field.contains(new Hex(0, 0))).isTrue();
        assertThat(field.contains(new Hex(14, 10))).isTrue();
        assertThat(field.contains(new Hex(-1, 0))).isFalse();
        assertThat(field.contains(new Hex(15, 0))).isFalse();
        assertThat(field.contains(new Hex(0, 11))).isFalse();
    }

    @Test
    void move_toward_stops_one_hex_short_so_target_is_never_overlapped() {
        Hex from = new Hex(0, 5);
        Hex target = new Hex(14, 5);
        Hex result = field.moveToward(from, target, /* speed */ 18);
        assertThat(result.distanceTo(target)).isEqualTo(1);
    }

    @Test
    void move_toward_advances_by_speed_when_target_is_far() {
        Hex from = new Hex(0, 5);
        Hex target = new Hex(14, 5);
        Hex result = field.moveToward(from, target, /* speed */ 4);
        assertThat(from.distanceTo(result)).isEqualTo(4);
    }

    @Test
    void move_toward_already_adjacent_does_not_move() {
        Hex from = new Hex(13, 5);
        Hex target = new Hex(14, 5);
        Hex result = field.moveToward(from, target, 10);
        assertThat(result).isEqualTo(from);
    }
}
