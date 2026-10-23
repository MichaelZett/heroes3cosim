package de.zettsystems.h3comsim.battle.domain;

import org.junit.jupiter.api.Test;

import java.util.Random;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ObstacleGeneratorTest {

    @Test
    void produces_requested_count_and_stays_inside_the_battlefield() {
        Set<Hex> obstacles = ObstacleGenerator.generate(Battlefield.STANDARD, new Random(42L), 12);
        assertThat(obstacles).hasSize(12);
        for (Hex h : obstacles) {
            assertThat(Battlefield.STANDARD.contains(h)).isTrue();
        }
    }

    @Test
    void keeps_the_outer_two_columns_clear_for_spawn_zones() {
        Set<Hex> obstacles = ObstacleGenerator.generate(Battlefield.STANDARD, new Random(7L), 30);
        Battlefield field = Battlefield.STANDARD;
        for (Hex h : obstacles) {
            assertThat(h.q()).isBetween(field.leftmostQ(h.r()) + 2, field.rightmostQ(h.r()) - 2);
        }
    }

    @Test
    void same_seed_yields_identical_obstacle_set() {
        Set<Hex> first = ObstacleGenerator.generate(Battlefield.STANDARD, new Random(123L), 10);
        Set<Hex> second = ObstacleGenerator.generate(Battlefield.STANDARD, new Random(123L), 10);
        assertThat(second).hasSameElementsAs(first);
    }

    @Test
    void caps_count_at_available_hexes() {
        // Ohne die zwei Randzellen je Seite: 5 lange Reihen à 11, 6 kurze à 10.
        Set<Hex> obstacles = ObstacleGenerator.generate(Battlefield.STANDARD, new Random(1L), 500);
        assertThat(obstacles).hasSize(5 * 11 + 6 * 10);
    }
}
