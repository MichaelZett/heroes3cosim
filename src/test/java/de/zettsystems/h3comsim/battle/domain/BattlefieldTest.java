package de.zettsystems.h3comsim.battle.domain;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BattlefieldTest {

    private final Battlefield field = Battlefield.STANDARD;

    @Test
    void standard_battlefield_is_15_by_11() {
        assertThat(field.width()).isEqualTo(15);
        assertThat(field.height()).isEqualTo(11);
    }

    @Test
    void the_center_row_keeps_q_0_to_14() {
        assertThat(field.centerRow()).isEqualTo(5);
        assertThat(field.leftmostQ(5)).isZero();
        assertThat(field.rightmostQ(5)).isEqualTo(14);
        assertThat(field.contains(new Hex(-1, 5))).isFalse();
        assertThat(field.contains(new Hex(15, 5))).isFalse();
        assertThat(field.contains(new Hex(5, -1))).isFalse();
        assertThat(field.contains(new Hex(5, 11))).isFalse();
    }

    @Test
    void the_field_is_a_rectangle_with_offset_rows_not_a_parallelogram() {
        // Waagerechte Lage x = q + r/2: Jede Reihe überdeckt dieselbe Breite. Die Ecken des alten
        // Parallelogramms (0,0) und (14,10) liegen außerhalb.
        assertThat(field.contains(new Hex(0, 0))).isFalse();
        assertThat(field.contains(new Hex(14, 10))).isFalse();
        for (int r = 0; r < field.height(); r++) {
            double left = field.leftmostQ(r) + r / 2.0;
            double right = field.rightmostQ(r) + r / 2.0;
            assertThat(left).as("linker Rand Reihe %d", r).isBetween(2.5, 3.0);
            assertThat(right).as("rechter Rand Reihe %d", r).isBetween(16.0, 16.5);
        }
        // Reihen mit der Parität der Mittelreihe haben 15 Zellen, die anderen 14.
        assertThat(field.rightmostQ(5) - field.leftmostQ(5) + 1).isEqualTo(15);
        assertThat(field.rightmostQ(4) - field.leftmostQ(4) + 1).isEqualTo(14);
        assertThat(field.cells()).hasSize(5 * 15 + 6 * 14);
    }

    @Test
    void the_mirror_maps_the_field_onto_itself_and_keeps_distances() {
        List<Hex> cells = field.cells();
        for (Hex cell : cells) {
            Hex mirrored = field.mirror(cell);
            assertThat(field.contains(mirrored)).as("%s -> %s", cell, mirrored).isTrue();
            assertThat(field.mirror(mirrored)).isEqualTo(cell);
        }
        Hex a = new Hex(3, 0);
        Hex b = new Hex(9, 7);
        assertThat(field.mirror(a).distanceTo(field.mirror(b))).isEqualTo(a.distanceTo(b));
        assertThat(field.mirror(new Hex(0, 5))).isEqualTo(new Hex(14, 5));
    }

    @Test
    void move_toward_stops_one_hex_short_so_target_is_never_overlapped() {
        Hex from = new Hex(0, 5);
        Hex target = new Hex(14, 5);
        Hex result = field.moveToward(from, target, /* speed */ 18, Movement.GROUND);
        assertThat(result.distanceTo(target)).isOne();
    }

    @Test
    void move_toward_advances_by_speed_when_target_is_far() {
        Hex from = new Hex(0, 5);
        Hex target = new Hex(14, 5);
        Hex result = field.moveToward(from, target, /* speed */ 4, Movement.GROUND);
        assertThat(from.distanceTo(result)).isEqualTo(4);
    }

    @Test
    void flying_unit_crosses_obstacles_in_straight_line_while_ground_routes_around() {
        // Hindernisriegel quer durch die Bahn zwischen (0,5) und (5,5).
        Battlefield obstacled = Battlefield.STANDARD.withObstacles(java.util.Set.of(
                new Hex(2, 4), new Hex(2, 5), new Hex(2, 6)));

        Hex flying = obstacled.moveToward(new Hex(0, 5), new Hex(5, 5), 4, Movement.FLYING);
        Hex grounded = obstacled.moveToward(new Hex(0, 5), new Hex(5, 5), 4, Movement.GROUND);

        // FLYING geht gerade Linie über die Mauer hinweg.
        assertThat(flying).isEqualTo(new Hex(4, 5));
        // GROUND muss um den Riegel rum, landet auf einer Position abseits der geraden Linie.
        assertThat(grounded).isNotEqualTo(new Hex(4, 5));
    }

    @Test
    void move_toward_already_adjacent_does_not_move() {
        Hex from = new Hex(13, 5);
        Hex target = new Hex(14, 5);
        Hex result = field.moveToward(from, target, 10, Movement.GROUND);
        assertThat(result).isEqualTo(from);
    }
}
