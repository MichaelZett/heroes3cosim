package de.zettsystems.h3comsim.application;

import de.zettsystems.h3comsim.domain.Stack;
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
        Stack attacker = new Stack(UnitCatalog.TITAN, 1);
        Stack defender = new Stack(UnitCatalog.PIKEMAN, 1);
        BattleSetup setup = new BattleSetup(attacker, defender);

        BattleResult result = new Battle(new Random(7L)).simulate(setup);

        assertThat(result.winner()).isEqualTo(BattleResult.Side.ATTACKER);
        assertThat(result.attackerSurvivors()).isEqualTo(1);
        assertThat(result.defenderSurvivors()).isZero();
    }

    @Test
    void ranged_attacker_uses_a_shot_per_turn() {
        Stack titan = new Stack(UnitCatalog.TITAN, 1);
        Stack pikeman = new Stack(UnitCatalog.PIKEMAN, 5);
        BattleSetup setup = new BattleSetup(titan, pikeman);

        int initialShots = titan.shotsRemaining();
        new Battle(new Random(5L)).simulate(setup);

        assertThat(titan.shotsRemaining()).isLessThan(initialShots);
    }

    private BattleResult simulate(de.zettsystems.h3comsim.domain.Unit attackerUnit, int attackerCount,
                                  de.zettsystems.h3comsim.domain.Unit defenderUnit, int defenderCount,
                                  long seed) {
        Stack attacker = new Stack(attackerUnit, attackerCount);
        Stack defender = new Stack(defenderUnit, defenderCount);
        return new Battle(new Random(seed)).simulate(new BattleSetup(attacker, defender));
    }
}
