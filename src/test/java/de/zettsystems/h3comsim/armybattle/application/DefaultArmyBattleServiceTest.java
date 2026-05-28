package de.zettsystems.h3comsim.armybattle.application;

import de.zettsystems.h3comsim.armybattle.values.ArmyBattleRequest;
import de.zettsystems.h3comsim.armybattle.values.ArmyBattleSimulation;
import de.zettsystems.h3comsim.armybattle.values.ArmySpec;
import de.zettsystems.h3comsim.armybattle.values.StackSpec;
import de.zettsystems.h3comsim.battle.domain.events.BattleEvent;
import de.zettsystems.h3comsim.battle.domain.events.Winner;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefaultArmyBattleServiceTest {

    private final FactionPresetCatalog presets = new FactionPresetCatalog();
    private final DefaultArmyBattleService service = new DefaultArmyBattleService();

    @Test
    void castle_preset_vs_castle_preset_finishes_with_a_winner_or_draw() {
        ArmyBattleRequest request = new ArmyBattleRequest(
                armyFromPreset("CASTLE"),
                armyFromPreset("CASTLE"),
                42L);

        ArmyBattleSimulation simulation = service.simulate(request, 42L);

        assertThat(simulation.result().winner()).isIn(Winner.ATTACKER, Winner.DEFENDER, Winner.DRAW);
        assertThat(simulation.result().turnsTaken()).isGreaterThan(0);
        assertThat(simulation.events()).isNotEmpty();
        assertThat(simulation.events().get(0)).isInstanceOf(BattleEvent.BattleStart.class);
        assertThat(simulation.events().get(simulation.events().size() - 1))
                .isInstanceOf(BattleEvent.BattleEnd.class);
    }

    @Test
    void battle_start_event_lists_all_fourteen_stacks_for_two_full_armies() {
        ArmyBattleRequest request = new ArmyBattleRequest(
                armyFromPreset("RAMPART"),
                armyFromPreset("INFERNO"),
                1L);

        ArmyBattleSimulation simulation = service.simulate(request, 1L);

        BattleEvent.BattleStart start = (BattleEvent.BattleStart) simulation.events().get(0);
        assertThat(start.stacks()).hasSize(14);
    }

    @Test
    void deterministic_for_same_seed() {
        ArmyBattleRequest request = new ArmyBattleRequest(
                armyFromPreset("TOWER"),
                armyFromPreset("DUNGEON"),
                7L);

        ArmyBattleSimulation a = service.simulate(request, 7L);
        ArmyBattleSimulation b = service.simulate(request, 7L);

        assertThat(a.result().winner()).isEqualTo(b.result().winner());
        assertThat(a.result().attackerSurvivors()).isEqualTo(b.result().attackerSurvivors());
        assertThat(a.result().defenderSurvivors()).isEqualTo(b.result().defenderSurvivors());
        assertThat(a.result().turnsTaken()).isEqualTo(b.result().turnsTaken());
    }

    @Test
    void unknown_unit_name_is_rejected_with_400() {
        ArmyBattleRequest request = new ArmyBattleRequest(
                new ArmySpec(List.of(new StackSpec("Definitely Not A Unit", 1))),
                armyFromPreset("CASTLE"),
                1L);

        assertThatThrownBy(() -> service.simulate(request, 1L))
                .hasMessageContaining("Unknown unit");
    }

    private ArmySpec armyFromPreset(String factionName) {
        return new ArmySpec(presets.byFaction(
                de.zettsystems.h3comsim.battle.domain.Faction.valueOf(factionName)).stacks());
    }
}
