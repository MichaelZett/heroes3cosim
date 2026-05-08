package de.zettsystems.h3comsim.application;

import de.zettsystems.h3comsim.domain.UnitCatalog;
import de.zettsystems.h3comsim.domain.events.BattleEvent;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BattleSimulationServiceTest {

    @Test
    void same_seed_yields_identical_simulations() {
        DefaultBattleSimulationService service = new DefaultBattleSimulationService();

        BattleSimulation first = service.simulate(UnitCatalog.GRAND_ELF, 10,
                UnitCatalog.ARCH_ANGEL, 1, 42L);
        BattleSimulation second = service.simulate(UnitCatalog.GRAND_ELF, 10,
                UnitCatalog.ARCH_ANGEL, 1, 42L);

        assertThat(second.result()).isEqualTo(first.result());
        assertThat(second.events()).isEqualTo(first.events());
    }

    @Test
    void event_list_brackets_with_battle_start_and_battle_end() {
        DefaultBattleSimulationService service = new DefaultBattleSimulationService();

        BattleSimulation simulation = service.simulate(UnitCatalog.PIKEMAN, 5,
                UnitCatalog.PIKEMAN, 5, 1L);

        assertThat(simulation.events().get(0)).isInstanceOf(BattleEvent.BattleStart.class);
        assertThat(simulation.events().get(simulation.events().size() - 1))
                .isInstanceOf(BattleEvent.BattleEnd.class);
    }
}
