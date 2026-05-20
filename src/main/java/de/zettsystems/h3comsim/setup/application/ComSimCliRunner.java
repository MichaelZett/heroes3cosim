package de.zettsystems.h3comsim.setup.application;

import de.zettsystems.h3comsim.battle.domain.Battle;
import de.zettsystems.h3comsim.battle.domain.BattleResult;
import de.zettsystems.h3comsim.battle.domain.BattleSetup;
import de.zettsystems.h3comsim.battle.domain.UnitCatalog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;

/**
 * Demo-CLI: läuft nur unter Profil {@code cli} und führt eine Beispiel-Battle aus.
 * Aktivierung via {@code .\gradlew.bat bootRun --args='--spring.profiles.active=cli'}
 * oder {@code SPRING_PROFILES_ACTIVE=cli java -jar app.jar}.
 */
@Component
@Profile("cli")
public class ComSimCliRunner implements CommandLineRunner {

    private static final Logger LOG = LoggerFactory.getLogger(ComSimCliRunner.class);

    @Override
    public void run(String... args) {
        LOG.info("CLI-Demo: Grand Elf vs Arch Angel");
        BattleSetup setup = new BattleSetup(
                UnitCatalog.GRAND_ELF, 10,
                UnitCatalog.ARCH_ANGEL, 1);
        RandomGenerator rng = RandomGeneratorFactory.getDefault().create();
        BattleResult result = new Battle(rng).simulate(setup);
        LOG.info("Result: {}", result);
    }
}
