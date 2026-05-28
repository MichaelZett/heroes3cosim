package de.zettsystems.h3comsim.battle.domain;

import de.zettsystems.h3comsim.battle.domain.events.Side;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BattleSetupMultiStackTest {

    @Test
    void single_battle_convenience_constructor_creates_one_stack_per_side() {
        BattleSetup setup = new BattleSetup(UnitCatalog.PIKEMAN, 10, UnitCatalog.MARKSMAN, 5);

        assertThat(setup.attackerStacks()).hasSize(1);
        assertThat(setup.defenderStacks()).hasSize(1);
        assertThat(setup.getAttacker().unit().name()).isEqualTo("Pikeman");
        assertThat(setup.getDefender().unit().name()).isEqualTo("Marksman");
        assertThat(setup.getAttacker().slot()).isZero();
    }

    @Test
    void multi_stack_constructor_keeps_lists_immutable() {
        Stack a0 = new Stack(UnitCatalog.PIKEMAN, 10, new Hex(0, 5), Side.ATTACKER, 0);
        Stack a1 = new Stack(UnitCatalog.ARCHER, 5, new Hex(0, 7), Side.ATTACKER, 1);
        Stack d0 = new Stack(UnitCatalog.PIKEMAN, 10, new Hex(14, 5), Side.DEFENDER, 0);

        BattleSetup setup = new BattleSetup(List.of(a0, a1), List.of(d0), Battlefield.STANDARD);

        assertThat(setup.attackerStacks()).containsExactly(a0, a1);
        assertThat(setup.defenderStacks()).containsExactly(d0);
        assertThatThrownBy(() -> setup.attackerStacks().add(d0))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void alive_stacks_skips_dead_ones() {
        Stack a0 = new Stack(UnitCatalog.PIKEMAN, 10, new Hex(0, 5), Side.ATTACKER, 0);
        Stack a1 = new Stack(UnitCatalog.PIKEMAN, 0, new Hex(0, 7), Side.ATTACKER, 1);
        Stack d0 = new Stack(UnitCatalog.PIKEMAN, 10, new Hex(14, 5), Side.DEFENDER, 0);

        BattleSetup setup = new BattleSetup(List.of(a0, a1), List.of(d0), Battlefield.STANDARD);

        assertThat(setup.aliveStacks()).containsExactly(a0, d0);
    }

    @Test
    void opponents_of_returns_only_living_enemies() {
        Stack a0 = new Stack(UnitCatalog.PIKEMAN, 10, new Hex(0, 5), Side.ATTACKER, 0);
        Stack d0 = new Stack(UnitCatalog.PIKEMAN, 10, new Hex(14, 5), Side.DEFENDER, 0);
        Stack d1 = new Stack(UnitCatalog.PIKEMAN, 0, new Hex(14, 7), Side.DEFENDER, 1);

        BattleSetup setup = new BattleSetup(List.of(a0), List.of(d0, d1), Battlefield.STANDARD);

        assertThat(setup.opponentsOf(a0)).containsExactly(d0);
        assertThat(setup.opponentsOf(d0)).containsExactly(a0);
    }

    @Test
    void get_target_throws_when_more_than_one_living_opponent() {
        Stack a0 = new Stack(UnitCatalog.PIKEMAN, 10, new Hex(0, 5), Side.ATTACKER, 0);
        Stack d0 = new Stack(UnitCatalog.PIKEMAN, 10, new Hex(14, 5), Side.DEFENDER, 0);
        Stack d1 = new Stack(UnitCatalog.PIKEMAN, 10, new Hex(14, 7), Side.DEFENDER, 1);

        BattleSetup setup = new BattleSetup(List.of(a0), List.of(d0, d1), Battlefield.STANDARD);

        assertThatThrownBy(() -> setup.getTarget(a0))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Multi-Stack");
    }

    @Test
    void counts_aggregate_across_stacks_per_side() {
        Stack a0 = new Stack(UnitCatalog.PIKEMAN, 10, new Hex(0, 5), Side.ATTACKER, 0);
        Stack a1 = new Stack(UnitCatalog.ARCHER, 7, new Hex(0, 7), Side.ATTACKER, 1);
        Stack d0 = new Stack(UnitCatalog.PIKEMAN, 12, new Hex(14, 5), Side.DEFENDER, 0);

        BattleSetup setup = new BattleSetup(List.of(a0, a1), List.of(d0), Battlefield.STANDARD);

        assertThat(setup.getAttackerCount()).isEqualTo(17);
        assertThat(setup.getDefenderCount()).isEqualTo(12);
    }

    @Test
    void rejects_empty_stack_list() {
        Stack d0 = new Stack(UnitCatalog.PIKEMAN, 10, new Hex(14, 5), Side.DEFENDER, 0);
        assertThatThrownBy(() -> new BattleSetup(List.of(), List.of(d0), Battlefield.STANDARD))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
