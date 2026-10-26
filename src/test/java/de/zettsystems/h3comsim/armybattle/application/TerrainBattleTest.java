package de.zettsystems.h3comsim.armybattle.application;

import de.zettsystems.h3comsim.armybattle.values.ArmyBattleRequest;
import de.zettsystems.h3comsim.armybattle.values.ArmyBattleSimulation;
import de.zettsystems.h3comsim.armybattle.values.ArmySpec;
import de.zettsystems.h3comsim.armybattle.values.StackSpec;
import de.zettsystems.h3comsim.battle.domain.Faction;
import de.zettsystems.h3comsim.battle.domain.Fortification;
import de.zettsystems.h3comsim.battle.domain.MagicTerrain;
import de.zettsystems.h3comsim.battle.domain.Terrain;
import de.zettsystems.h3comsim.battle.domain.events.BattleEvent;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Das Gelände im Army-Battle: frei gewählt auf dem Feld, in der Belagerung das der Stadt. */
class TerrainBattleTest {

    private static final long SEED = 99L;
    private static final List<StackSpec> PIKEMEN = List.of(new StackSpec("Pikeman", 30));
    private static final List<StackSpec> IMPS = List.of(new StackSpec("Imp", 60));

    private final DefaultArmyBattleService service = new DefaultArmyBattleService();

    @Test
    void a_field_battle_reports_its_terrain_and_uses_its_obstacles() {
        BattleEvent.BattleStart start = start(request(null, Terrain.SNOW, MagicTerrain.HOLY_GROUND));

        assertThat(start.terrain()).isEqualTo(Terrain.SNOW);
        assertThat(start.magicTerrain()).isEqualTo(MagicTerrain.HOLY_GROUND);
        assertThat(start.obstacles()).isNotEmpty();
        assertThat(start(request(null, null, null)).terrain()).as("ohne Gelände").isNull();
    }

    @Test
    void a_siege_is_fought_on_the_native_terrain_of_the_town() {
        BattleEvent.BattleStart start = start(request(Fortification.FORT, null, null));

        assertThat(start.terrain()).isEqualTo(Terrain.LAVA);
        assertThat(start.obstacles()).isEmpty();
        assertThat(start(request(Fortification.FORT, Terrain.LAVA, null)).terrain()).isEqualTo(Terrain.LAVA);
    }

    @Test
    void a_siege_rejects_other_terrains_and_magical_ones() {
        ArmyBattleRequest grass = request(Fortification.FORT, Terrain.GRASS, null);
        ArmyBattleRequest plains = request(Fortification.FORT, null, MagicTerrain.MAGIC_PLAINS);

        assertThatThrownBy(() -> service.simulate(grass, SEED)).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> service.simulate(plains, SEED)).isInstanceOf(ResponseStatusException.class);
    }

    private static ArmyBattleRequest request(@Nullable Fortification fortification, @Nullable Terrain terrain,
                                             @Nullable MagicTerrain magic) {
        return new ArmyBattleRequest(new ArmySpec(PIKEMEN), new ArmySpec(IMPS, null), SEED, fortification,
                fortification == null ? null : Faction.INFERNO, terrain, magic, null);
    }

    private BattleEvent.BattleStart start(ArmyBattleRequest request) {
        ArmyBattleSimulation simulation = service.simulate(request, SEED);
        return (BattleEvent.BattleStart) simulation.events().getFirst();
    }
}
