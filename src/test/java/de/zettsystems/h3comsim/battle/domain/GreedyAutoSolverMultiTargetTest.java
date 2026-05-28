package de.zettsystems.h3comsim.battle.domain;

import de.zettsystems.h3comsim.battle.domain.events.Side;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GreedyAutoSolverMultiTargetTest {

    private final GreedyAutoSolver solver = new GreedyAutoSolver();
    private final Battlefield battlefield = Battlefield.STANDARD;

    @Test
    void melee_active_picks_nearest_opponent() {
        Stack active = new Stack(UnitCatalog.PIKEMAN, 10, new Hex(4, 5), Side.ATTACKER, 3);
        Stack far = new Stack(UnitCatalog.PIKEMAN, 10, new Hex(12, 5), Side.DEFENDER, 0);
        Stack near = new Stack(UnitCatalog.PIKEMAN, 10, new Hex(6, 5), Side.DEFENDER, 1);

        Stack target = solver.pickTarget(active, List.of(far, near), battlefield);

        assertThat(target).isSameAs(near);
    }

    @Test
    void melee_active_breaks_distance_tie_by_lower_slot() {
        // Beide Ziele Distanz 2 von (4,5): (6,5) und (6,4) liegen auf gleicher Cube-Distanz.
        Stack active = new Stack(UnitCatalog.PIKEMAN, 10, new Hex(4, 5), Side.ATTACKER, 0);
        Stack highSlot = new Stack(UnitCatalog.PIKEMAN, 10, new Hex(6, 5), Side.DEFENDER, 5);
        Stack lowSlot = new Stack(UnitCatalog.PIKEMAN, 10, new Hex(6, 4), Side.DEFENDER, 2);

        Stack target = solver.pickTarget(active, List.of(highSlot, lowSlot), battlefield);

        assertThat(target).isSameAs(lowSlot);
    }

    @Test
    void shooter_picks_most_dangerous_opponent_by_damage_times_count() {
        // Grand Elf vs zwei Gegner gleich weit weg: 50 Pikemen (1-3 dmg, avg 2 → score 100) vs.
        // 5 Champions (20-25 dmg, avg 22 → score 110). Score wählt Champions.
        Stack shooter = new Stack(UnitCatalog.GRAND_ELF, 10, new Hex(0, 5), Side.ATTACKER, 0);
        Stack pikemen = new Stack(UnitCatalog.PIKEMAN, 50, new Hex(10, 4), Side.DEFENDER, 0);
        Stack champions = new Stack(UnitCatalog.CHAMPION, 5, new Hex(10, 6), Side.DEFENDER, 1);

        Stack target = solver.pickTarget(shooter, List.of(pikemen, champions), battlefield);

        assertThat(target).isSameAs(champions);
    }

    @Test
    void adjacent_opponent_overrides_shooter_target_choice() {
        Stack shooter = new Stack(UnitCatalog.GRAND_ELF, 10, new Hex(0, 5), Side.ATTACKER, 0);
        Stack adjacentLowThreat = new Stack(UnitCatalog.PIKEMAN, 1, new Hex(1, 5), Side.DEFENDER, 0);
        Stack farHighThreat = new Stack(UnitCatalog.CHAMPION, 50, new Hex(12, 5), Side.DEFENDER, 1);

        Stack target = solver.pickTarget(shooter, List.of(farHighThreat, adjacentLowThreat), battlefield);

        assertThat(target).isSameAs(adjacentLowThreat);
    }

    @Test
    void dead_opponents_are_skipped() {
        Stack active = new Stack(UnitCatalog.PIKEMAN, 10, new Hex(4, 5), Side.ATTACKER, 0);
        Stack alive = new Stack(UnitCatalog.PIKEMAN, 10, new Hex(8, 5), Side.DEFENDER, 0);
        Stack dead = new Stack(UnitCatalog.PIKEMAN, 0, new Hex(5, 5), Side.DEFENDER, 1);

        Stack target = solver.pickTarget(active, List.of(dead, alive), battlefield);

        assertThat(target).isSameAs(alive);
    }

    @Test
    void empty_opponent_list_returns_null() {
        Stack active = new Stack(UnitCatalog.PIKEMAN, 10, new Hex(4, 5), Side.ATTACKER, 0);

        Stack target = solver.pickTarget(active, List.of(), battlefield);

        assertThat(target).isNull();
    }
}
