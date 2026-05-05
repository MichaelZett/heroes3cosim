package de.zettsystems.h3comsim.application;

import de.zettsystems.h3comsim.domain.Stack;
import de.zettsystems.h3comsim.domain.UnitCatalog;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

class BattleTest {

    @Test
    void single_archAngel_overpowers_ten_grand_elves() {
        BattleResult result = simulateGrandElvesVsArchAngel(42L);

        assertThat(result.winner()).isEqualTo(BattleResult.Side.DEFENDER);
        assertThat(result.attackerSurvivors()).isZero();
        assertThat(result.defenderSurvivors()).isEqualTo(1);
    }

    @Test
    void same_seed_yields_identical_result() {
        BattleResult first = simulateGrandElvesVsArchAngel(123L);
        BattleResult second = simulateGrandElvesVsArchAngel(123L);

        assertThat(second).isEqualTo(first);
    }

    private BattleResult simulateGrandElvesVsArchAngel(long seed) {
        Stack attacker = new Stack(UnitCatalog.GRAND_ELF, 10);
        Stack defender = new Stack(UnitCatalog.ARCH_ANGEL, 1);
        return new Battle(new Random(seed)).simulate(new BattleSetup(attacker, defender));
    }
}
