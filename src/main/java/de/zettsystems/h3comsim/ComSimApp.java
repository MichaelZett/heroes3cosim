package de.zettsystems.h3comsim;

import de.zettsystems.h3comsim.application.Battle;
import de.zettsystems.h3comsim.application.BattleResult;
import de.zettsystems.h3comsim.application.BattleSetup;
import de.zettsystems.h3comsim.domain.Stack;
import de.zettsystems.h3comsim.domain.UnitCatalog;

import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;

public class ComSimApp {

    public static void main(String[] args) {
        Stack attacker = new Stack(UnitCatalog.GRAND_ELF, 10);
        Stack defender = new Stack(UnitCatalog.ARCH_ANGEL, 1);
        BattleSetup setup = new BattleSetup(attacker, defender);

        RandomGenerator rng = RandomGeneratorFactory.getDefault().create();
        BattleResult result = new Battle(rng).simulate(setup);

        System.out.println("Ergebnis: " + result);
    }
}
