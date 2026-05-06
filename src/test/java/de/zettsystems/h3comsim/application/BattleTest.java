package de.zettsystems.h3comsim.application;

import de.zettsystems.h3comsim.domain.Unit;
import de.zettsystems.h3comsim.domain.UnitCatalog;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

class BattleTest {

    @Test
    void single_archAngel_overpowers_ten_grand_elves() {
        BattleResult result = simulate(UnitCatalog.GRAND_ELF, 10, UnitCatalog.ARCH_ANGEL, 1, 42L);

        assertThat(result.winner()).isEqualTo(BattleResult.Side.DEFENDER);
        assertThat(result.attackerSurvivors()).isZero();
        assertThat(result.defenderSurvivors()).isEqualTo(1);
    }

    @Test
    void same_seed_yields_identical_result() {
        BattleResult first = simulate(UnitCatalog.GRAND_ELF, 10, UnitCatalog.ARCH_ANGEL, 1, 123L);
        BattleResult second = simulate(UnitCatalog.GRAND_ELF, 10, UnitCatalog.ARCH_ANGEL, 1, 123L);

        assertThat(second).isEqualTo(first);
    }

    @Test
    void titan_kills_pikeman_at_distance_without_taking_damage() {
        BattleSetup setup = new BattleSetup(UnitCatalog.TITAN, 1, UnitCatalog.PIKEMAN, 1);

        BattleResult result = new Battle(new Random(7L)).simulate(setup);

        assertThat(result.winner()).isEqualTo(BattleResult.Side.ATTACKER);
        assertThat(result.attackerSurvivors()).isEqualTo(1);
        assertThat(result.defenderSurvivors()).isZero();
    }

    @Test
    void ranged_attacker_uses_a_shot_per_turn() {
        BattleSetup setup = new BattleSetup(UnitCatalog.TITAN, 1, UnitCatalog.PIKEMAN, 5);

        int initialShots = setup.getAttacker().shotsRemaining();
        new Battle(new Random(5L)).simulate(setup);

        assertThat(setup.getAttacker().shotsRemaining()).isLessThan(initialShots);
    }

    @Test
    void marksman_with_two_shots_consumes_two_shots_per_turn() {
        BattleSetup setup = new BattleSetup(UnitCatalog.MARKSMAN, 1, UnitCatalog.PIKEMAN, 50);

        int initialShots = setup.getAttacker().shotsRemaining();
        BattleResult result = new Battle(new Random(11L)).simulate(setup);
        int shotsUsed = initialShots - setup.getAttacker().shotsRemaining();

        // Marksman shoots from start (distance 14, ranged). Two shots per turn means
        // shotsUsed grows by 2 per turn — must therefore be even.
        assertThat(shotsUsed).isPositive();
        assertThat(shotsUsed % 2).isZero();
        assertThat(result.turnsTaken()).isPositive();
    }

    private BattleResult simulate(Unit attackerUnit, int attackerCount,
                                  Unit defenderUnit, int defenderCount,
                                  long seed) {
        BattleSetup setup = new BattleSetup(attackerUnit, attackerCount, defenderUnit, defenderCount);
        return new Battle(new Random(seed)).simulate(setup);
    }
}
