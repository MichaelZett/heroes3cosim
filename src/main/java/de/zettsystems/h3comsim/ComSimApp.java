package de.zettsystems.h3comsim;

import de.zettsystems.h3comsim.application.Battle;
import de.zettsystems.h3comsim.application.BattleResult;
import de.zettsystems.h3comsim.application.BattleSetup;
import de.zettsystems.h3comsim.domain.UnitCatalog;

import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;

public class ComSimApp {

    public static void main(String[] args) {
        BattleSetup setup = new BattleSetup(
                UnitCatalog.GRAND_ELF, 10,
                UnitCatalog.ARCH_ANGEL, 1);

        RandomGenerator rng = RandomGeneratorFactory.getDefault().create();
        BattleResult result = new Battle(rng).simulate(setup);

        System.out.println("Ergebnis: " + result);
    }
}
