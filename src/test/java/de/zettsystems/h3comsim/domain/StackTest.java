package de.zettsystems.h3comsim.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StackTest {

    private static final Hex ORIGIN = new Hex(0, 0);

    @Test
    void loseTopCreatures_kills_specified_count() {
        Stack pikemen = new Stack(UnitCatalog.PIKEMAN, 100, ORIGIN);

        pikemen.loseTopCreatures(5);

        assertThat(pikemen.getCount()).isEqualTo(95);
        assertThat(pikemen.getCurrentHealth()).isEqualTo(UnitCatalog.PIKEMAN.health());
    }

    @Test
    void loseTopCreatures_caps_at_alive_count() {
        Stack pikemen = new Stack(UnitCatalog.PIKEMAN, 3, ORIGIN);

        pikemen.loseTopCreatures(10);

        assertThat(pikemen.getCount()).isZero();
        assertThat(pikemen.isAlive()).isFalse();
    }

    @Test
    void titan_takes_extra_damage_from_black_dragon_attacker_specialities() {
        Stack titan = new Stack(UnitCatalog.TITAN, 1, ORIGIN);

        titan.takeDamage(100, UnitCatalog.BLACK_DRAGON.attackerSpecialities());

        // 1.5x hate-damage: 100 base → 150 applied. Titan top has 300 HP, so 150 left.
        assertThat(titan.getCurrentHealth()).isEqualTo(300 - 150);
    }

    @Test
    void titan_takes_normal_damage_from_attackers_without_titan_hate() {
        Stack titan = new Stack(UnitCatalog.TITAN, 1, ORIGIN);

        titan.takeDamage(100, UnitCatalog.PIKEMAN.attackerSpecialities());

        assertThat(titan.getCurrentHealth()).isEqualTo(300 - 100);
    }

    @Test
    void angel_devil_hate_still_applies_alongside_titan_hate() {
        Stack angel = new Stack(UnitCatalog.ANGEL, 1, ORIGIN);

        // Devils have ANGEL_HATE → 1.5x damage to angels (ANGEL_RACE marker).
        angel.takeDamage(80, UnitCatalog.DEVIL.attackerSpecialities());

        assertThat(angel.getCurrentHealth()).isEqualTo(200 - 120);
    }

    @Test
    void poison_drains_half_max_hp_per_turn() {
        Stack archangels = new Stack(UnitCatalog.ARCH_ANGEL, 1, ORIGIN);
        archangels.poison();

        archangels.endTurn();

        // Arch Angel max HP 250, half = 125 → top creature on 125.
        assertThat(archangels.getCurrentHealth()).isEqualTo(125);
        assertThat(archangels.isPoisoned()).isTrue();
    }

    @Test
    void poison_kills_creatures_over_three_turns() {
        Stack pikemen = new Stack(UnitCatalog.PIKEMAN, 5, ORIGIN);
        pikemen.poison();

        // Pikeman max HP 10, half = 5 per tick.
        pikemen.endTurn();
        assertThat(pikemen.getCount()).isEqualTo(5);
        assertThat(pikemen.getCurrentHealth()).isEqualTo(5);

        pikemen.endTurn();
        // Top dies, next pikeman steps up at full HP.
        assertThat(pikemen.getCount()).isEqualTo(4);
        assertThat(pikemen.getCurrentHealth()).isEqualTo(10);

        pikemen.endTurn();
        assertThat(pikemen.getCount()).isEqualTo(4);
        assertThat(pikemen.isPoisoned()).isFalse();
    }

    @Test
    void poisoned_stack_can_still_act() {
        Stack pikemen = new Stack(UnitCatalog.PIKEMAN, 1, ORIGIN);
        pikemen.poison();

        // Poison is HP-loss only; affected stacks still act normally.
        assertThat(pikemen.isAbleToAct()).isTrue();
    }
}
