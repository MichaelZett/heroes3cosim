package de.zettsystems.h3comsim.battle.domain;

import org.junit.jupiter.api.Test;

import java.util.Random;
import java.util.Set;

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

    @Test
    void cavalier_with_impact_damage_deals_more_damage_after_movement() {
        Stack cavalier = new Stack(UnitCatalog.CAVALIER, 1, ORIGIN);

        int damage0 = cavalier.calculateCurrentDamage(AttackType.HAND_TO_HAND, 0, new Random(42L));
        int damage5 = cavalier.calculateCurrentDamage(AttackType.HAND_TO_HAND, 5, new Random(42L));

        // 5 hexes × 5 % = +25 % bonus.
        assertThat(damage5).isEqualTo((int) Math.round(damage0 * 1.25));
    }

    @Test
    void impact_damage_is_capped_at_50_percent() {
        Stack champion = new Stack(UnitCatalog.CHAMPION, 1, ORIGIN);

        int damage0 = champion.calculateCurrentDamage(AttackType.HAND_TO_HAND, 0, new Random(7L));
        int damage10 = champion.calculateCurrentDamage(AttackType.HAND_TO_HAND, 10, new Random(7L));
        int damage20 = champion.calculateCurrentDamage(AttackType.HAND_TO_HAND, 20, new Random(7L));

        assertThat(damage10).isEqualTo((int) Math.round(damage0 * 1.5));
        assertThat(damage20).isEqualTo((int) Math.round(damage0 * 1.5));
    }

    @Test
    void unit_without_impact_damage_ignores_movement() {
        Stack pikeman = new Stack(UnitCatalog.PIKEMAN, 1, ORIGIN);

        int damage0 = pikeman.calculateCurrentDamage(AttackType.HAND_TO_HAND, 0, new Random(13L));
        int damage5 = pikeman.calculateCurrentDamage(AttackType.HAND_TO_HAND, 5, new Random(13L));

        assertThat(damage5).isEqualTo(damage0);
    }

    @Test
    void normal_unit_has_one_retaliation_per_turn() {
        Stack pikemen = new Stack(UnitCatalog.PIKEMAN, 1, ORIGIN);

        assertThat(pikemen.canRetaliate()).isTrue();
        pikemen.recordRetaliation();
        assertThat(pikemen.canRetaliate()).isFalse();

        pikemen.endTurn();
        assertThat(pikemen.canRetaliate()).isTrue();
    }

    @Test
    void griffin_with_counterstrike_twice_can_retaliate_two_times_per_turn() {
        Stack griffin = new Stack(UnitCatalog.GRIFFIN, 1, ORIGIN);

        assertThat(griffin.canRetaliate()).isTrue();
        griffin.recordRetaliation();
        assertThat(griffin.canRetaliate()).isTrue();
        griffin.recordRetaliation();
        assertThat(griffin.canRetaliate()).isFalse();
    }

    @Test
    void royal_griffin_retaliates_unlimited_times_per_turn() {
        Stack royalGriffin = new Stack(UnitCatalog.ROYAL_GRIFFIN, 1, ORIGIN);

        for (int i = 0; i < 100; i++) {
            assertThat(royalGriffin.canRetaliate()).isTrue();
            royalGriffin.recordRetaliation();
        }
    }

    @Test
    void pikeman_defense_is_reduced_against_behemoth_attacker() {
        Stack pikeman = new Stack(UnitCatalog.PIKEMAN, 1, ORIGIN);

        int normal = pikeman.effectiveDefenseAgainst(Set.of());
        int againstBehemoth = pikeman.effectiveDefenseAgainst(UnitCatalog.BEHEMOTH.attackerSpecialities());
        int againstAncient = pikeman.effectiveDefenseAgainst(UnitCatalog.ANCIENT_BEHEMOTH.attackerSpecialities());

        assertThat(normal).isEqualTo(5);
        assertThat(againstBehemoth).isEqualTo(3);  // 5 × 0.6 = 3
        assertThat(againstAncient).isOne();   // 5 × 0.2 = 1
    }

    @Test
    void wight_regenerates_top_creature_at_end_of_turn() {
        Stack wights = new Stack(UnitCatalog.WIGHT, 5, ORIGIN);
        // Wight HP 18, partial damage on top.
        wights.takeDamage(10, Set.of());
        assertThat(wights.getCurrentHealth()).isEqualTo(8);

        wights.endTurn();

        assertThat(wights.getCurrentHealth()).isEqualTo(UnitCatalog.WIGHT.health());
        assertThat(wights.getCount()).isEqualTo(5);
    }

    @Test
    void regeneration_does_not_revive_dead_stack() {
        Stack wight = new Stack(UnitCatalog.WIGHT, 1, ORIGIN);
        wight.takeDamage(100, Set.of());

        wight.endTurn();

        assertThat(wight.isAlive()).isFalse();
        assertThat(wight.getCount()).isZero();
    }

    @Test
    void diseased_stack_has_reduced_attack_and_defense() {
        Stack pikemen = new Stack(UnitCatalog.PIKEMAN, 10, ORIGIN);

        pikemen.disease();

        assertThat(pikemen.isDiseased()).isTrue();
        assertThat(pikemen.getAttack()).isEqualTo(UnitCatalog.PIKEMAN.attack() - 2);
        assertThat(pikemen.getDefense()).isEqualTo(UnitCatalog.PIKEMAN.defense() - 2);
    }

    @Test
    void disease_clears_after_three_turns() {
        Stack pikemen = new Stack(UnitCatalog.PIKEMAN, 10, ORIGIN);
        pikemen.disease();

        pikemen.endTurn();
        pikemen.endTurn();
        pikemen.endTurn();

        assertThat(pikemen.isDiseased()).isFalse();
        assertThat(pikemen.getDefense()).isEqualTo(UnitCatalog.PIKEMAN.defense());
    }

    @Test
    void aged_dragon_loses_half_of_current_top_health() {
        Stack ghostDragon = new Stack(UnitCatalog.GHOST_DRAGON, 1, ORIGIN);
        int before = ghostDragon.getCurrentHealth();

        ghostDragon.age();

        assertThat(ghostDragon.isAged()).isTrue();
        assertThat(ghostDragon.getCurrentHealth()).isEqualTo(before / 2);
    }

    @Test
    void aging_does_not_affect_max_hp_for_subsequent_damage_calc() {
        Stack ghostDragon = new Stack(UnitCatalog.GHOST_DRAGON, 1, ORIGIN);
        ghostDragon.age();
        int healthAfterAging = ghostDragon.getCurrentHealth();

        ghostDragon.takeDamage(50, Set.of());

        assertThat(ghostDragon.getCurrentHealth()).isEqualTo(healthAfterAging - 50);
    }

    @Test
    void efreet_sultan_fire_shield_returns_20_percent_of_damage() {
        Stack efreet = new Stack(UnitCatalog.EFREET_SULTAN, 1, ORIGIN);

        assertThat(efreet.fireShieldDamageFor(100)).isEqualTo(20);
        assertThat(efreet.fireShieldDamageFor(50)).isEqualTo(10);
    }

    @Test
    void unit_without_fire_shield_returns_zero() {
        Stack pikeman = new Stack(UnitCatalog.PIKEMAN, 1, ORIGIN);

        assertThat(pikeman.fireShieldDamageFor(100)).isZero();
    }

    @Test
    void phoenix_revives_with_20_percent_of_start_count_on_first_death() {
        Stack phoenix = new Stack(UnitCatalog.PHOENIX, 10, ORIGIN);

        phoenix.takeDamage(10_000, Set.of());

        assertThat(phoenix.isAlive()).isTrue();
        assertThat(phoenix.getCount()).isEqualTo(2);
        assertThat(phoenix.getCurrentHealth()).isEqualTo(UnitCatalog.PHOENIX.health());
    }

    @Test
    void phoenix_revives_only_once_per_battle() {
        Stack phoenix = new Stack(UnitCatalog.PHOENIX, 10, ORIGIN);
        phoenix.takeDamage(10_000, Set.of());
        assertThat(phoenix.getCount()).isPositive();

        phoenix.takeDamage(10_000, Set.of());

        assertThat(phoenix.isAlive()).isFalse();
        assertThat(phoenix.getCount()).isZero();
    }

    @Test
    void phoenix_revives_at_least_one_unit_for_small_start_counts() {
        Stack phoenix = new Stack(UnitCatalog.PHOENIX, 3, ORIGIN);

        phoenix.takeDamage(10_000, Set.of());

        assertThat(phoenix.getCount()).isOne();
    }

    @Test
    void firebird_without_rebirth_marker_stays_dead() {
        Stack firebird = new Stack(UnitCatalog.FIREBIRD, 5, ORIGIN);

        firebird.takeDamage(10_000, Set.of());

        assertThat(firebird.isAlive()).isFalse();
    }

    @Test
    void attack_bonus_is_capped_at_plus_400_percent() {
        // RoE-Manual S. 43: +5 % pro Attack-Punkt Differenz, gedeckelt bei +400 %.
        // 80 Punkte Differenz erreicht den Cap, 200 Punkte würden uncapped 1000 % geben.
        Stack archAngel = new Stack(UnitCatalog.ARCH_ANGEL, 1, ORIGIN);

        // Attack 30 vs Defense 0 → diff 30 → 150 % (unterhalb Cap)
        assertThat(archAngel.calculateAttackBoniMaliPercentage(0)).isEqualTo(150);
        // Diff 80 → 400 % exakt
        assertThat(archAngel.calculateAttackBoniMaliPercentage(-50)).isEqualTo(400);
        // Diff 200 → uncapped 1000 %, gecappt auf 400 %
        assertThat(archAngel.calculateAttackBoniMaliPercentage(-170)).isEqualTo(400);
    }

    @Test
    void defense_mali_is_capped_at_minus_70_percent() {
        // Manual: −2 % pro Punkt Differenz, min. 30 % Damage (= −70 % Mali, 35 Punkte).
        Stack peasant = new Stack(UnitCatalog.PEASANT, 1, ORIGIN);

        // Attack 1 vs Defense 36 → diff −35 → −70 % exakt
        assertThat(peasant.calculateAttackBoniMaliPercentage(36)).isEqualTo(-70);
        // Diff −100 → uncapped −200 %, gecappt bei −70 %
        assertThat(peasant.calculateAttackBoniMaliPercentage(101)).isEqualTo(-70);
    }
}
