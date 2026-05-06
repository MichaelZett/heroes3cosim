package de.zettsystems.h3comsim;

import de.zettsystems.h3comsim.application.Battle;
import de.zettsystems.h3comsim.application.BattleResult;
import de.zettsystems.h3comsim.application.BattleSetup;
import de.zettsystems.h3comsim.domain.UnitCatalog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;

public final class ComSimApp {

    private static final Logger LOG = LoggerFactory.getLogger(ComSimApp.class);

    private ComSimApp() {
    }

    public static void main(String[] args) {
        LOG.info("Starting heroes3-combat-simulator with {} args", args.length);

        BattleSetup setup = new BattleSetup(
                UnitCatalog.GRAND_ELF, 10,
                UnitCatalog.ARCH_ANGEL, 1);

        RandomGenerator rng = RandomGeneratorFactory.getDefault().create();
        BattleResult result = new Battle(rng).simulate(setup);

        LOG.info("Result: {}", result);
    }
}
