package de.zettsystems.h3comsim.armybattle.application;

import de.zettsystems.h3comsim.armybattle.values.ArmyBattleRequest;
import de.zettsystems.h3comsim.armybattle.values.ArmyBattleSimulation;
import de.zettsystems.h3comsim.armybattle.values.ArmySpec;
import de.zettsystems.h3comsim.armybattle.values.StackSpec;
import de.zettsystems.h3comsim.battle.domain.MagicTerrain;
import de.zettsystems.h3comsim.battle.domain.Terrain;
import de.zettsystems.h3comsim.battle.domain.TerrainObstacles;
import de.zettsystems.h3comsim.battle.domain.events.BattleEvent;
import de.zettsystems.h3comsim.battle.domain.events.HexCoord;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Seeschlacht: zwei Schiffe zwischen zwei Helden, sonst das Schiffsdeck. */
class SeaBattleTest {

    private static final long SEED = 4711L;
    private static final List<StackSpec> PIKEMEN = List.of(new StackSpec("Pikeman", 30), new StackSpec("Archer", 20));

    private final DefaultArmyBattleService service = new DefaultArmyBattleService();

    private ArmyBattleSimulation simulate(ArmySpec attacker, ArmySpec defender, MagicTerrain magic) {
        return service.simulate(new ArmyBattleRequest(attacker, defender, SEED, null, null, Terrain.WATER, magic,
                null), SEED);
    }

    private static BattleEvent.BattleStart start(ArmyBattleSimulation simulation) {
        return (BattleEvent.BattleStart) simulation.events().getFirst();
    }

    @Test
    void two_heroes_in_boats_fight_across_two_ships() {
        ArmyBattleSimulation simulation = simulate(new ArmySpec(PIKEMEN, "Sorsha"), new ArmySpec(PIKEMEN, "Crag Hack"),
                null);
        BattleEvent.BattleStart start = start(simulation);

        assertThat(start.terrain()).isEqualTo(Terrain.WATER);
        assertThat(start.obstacles().stream().map(c -> c.q() + "/" + c.r()).collect(Collectors.toSet()))
                .isEqualTo(TerrainObstacles.shipToShip().stream().map(h -> h.q() + "/" + h.r())
                        .collect(Collectors.toSet()));
        assertThat(simulation.events()).as("über die Planken kommt es zum Nahkampf")
                .anyMatch(BattleEvent.Melee.class::isInstance);
    }

    @Test
    void the_water_between_the_ships_leaves_two_planks() {
        assertThat(TerrainObstacles.shipToShip()).hasSize(32)
                .noneMatch(h -> h.r() == 2 || h.r() == 8)
                .allMatch(h -> h.r() >= 0 && h.r() <= 10);
    }

    @Test
    void without_a_second_hero_the_battle_is_on_a_deck_with_masts() {
        ArmyBattleSimulation simulation = simulate(new ArmySpec(PIKEMEN, "Sorsha"), ArmySpec.withoutHero(PIKEMEN),
                null);
        List<HexCoord> masts = start(simulation).obstacles();

        assertThat(masts).isNotEmpty().hasSizeLessThanOrEqualTo(15);
        assertThat(masts.size() % 3).as("jeder Mast sperrt drei Felder").isZero();
    }

    @Test
    void there_is_no_magical_terrain_at_sea() {
        assertThatThrownBy(() -> simulate(new ArmySpec(PIKEMEN, "Sorsha"), new ArmySpec(PIKEMEN, "Crag Hack"),
                MagicTerrain.HOLY_GROUND)).isInstanceOf(ResponseStatusException.class);
    }
}
