package de.zettsystems.h3comsim.battle.domain;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PathFinderTest {

    @Test
    void hasObstacleInLine_false_when_obstacles_empty() {
        Battlefield bf = Battlefield.STANDARD;
        assertThat(PathFinder.hasObstacleInLine(bf, new Hex(0, 5), new Hex(14, 5))).isFalse();
    }

    @Test
    void hasObstacleInLine_true_when_obstacle_sits_on_the_line() {
        Battlefield bf = Battlefield.STANDARD.withObstacles(Set.of(new Hex(7, 5)));
        assertThat(PathFinder.hasObstacleInLine(bf, new Hex(0, 5), new Hex(14, 5))).isTrue();
    }

    @Test
    void hasObstacleInLine_false_when_obstacle_sits_next_to_the_line() {
        Battlefield bf = Battlefield.STANDARD.withObstacles(Set.of(new Hex(7, 4)));
        assertThat(PathFinder.hasObstacleInLine(bf, new Hex(0, 5), new Hex(14, 5))).isFalse();
    }

    @Test
    void stepToward_routes_around_a_single_obstacle_in_the_way() {
        Battlefield bf = Battlefield.STANDARD.withObstacles(Set.of(new Hex(1, 5)));
        Hex next = PathFinder.stepToward(bf, new Hex(0, 5), new Hex(14, 5), /* speed */ 1);
        assertThat(next).isNotEqualTo(new Hex(1, 5));
        assertThat(new Hex(0, 5).distanceTo(next)).isOne();
    }

    @Test
    void stepToward_returns_from_when_target_is_fully_walled_off() {
        // Surround the target with obstacles on every passable neighbour hex.
        Hex target = new Hex(14, 5);
        Set<Hex> wall = Set.copyOf(target.neighbors());
        Battlefield bf = Battlefield.STANDARD.withObstacles(wall);
        Hex next = PathFinder.stepToward(bf, new Hex(0, 5), target, /* speed */ 10);
        assertThat(next).isEqualTo(new Hex(0, 5));
    }
}
