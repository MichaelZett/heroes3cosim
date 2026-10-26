package de.zettsystems.h3comsim.battle.domain;

import de.zettsystems.h3comsim.battle.domain.events.Side;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/** Grundgelände, magische Gelände und ihre Hindernisse (heroes.thelazy.net, VCMI). */
class TerrainTest {

    private static final Battlefield FIELD = Battlefield.STANDARD;

    @Test
    void each_town_has_its_native_terrain_and_neutrals_have_none() {
        assertThat(Terrain.nativeOf(Faction.CASTLE)).isEqualTo(Terrain.GRASS);
        assertThat(Terrain.nativeOf(Faction.CONFLUX)).isEqualTo(Terrain.GRASS);
        assertThat(Terrain.nativeOf(Faction.TOWER)).isEqualTo(Terrain.SNOW);
        assertThat(Terrain.nativeOf(Faction.DUNGEON)).isEqualTo(Terrain.SUBTERRANEAN);
        assertThat(Terrain.nativeOf(Faction.NEUTRAL)).isNull();
    }

    @Test
    void creatures_on_native_terrain_gain_one_attack_defense_and_speed() {
        Stack pikemen = stack(UnitCatalog.PIKEMAN);
        Stack peasants = stack(UnitCatalog.PEASANT);
        setup(List.of(pikemen, peasants)).ground(Terrain.GRASS, null);

        assertThat(List.of(pikemen.getAttack(), pikemen.getDefense(), pikemen.getSpeed())).containsExactly(5, 6, 5);
        assertThat(pikemen.onNativeTerrain()).isTrue();
        assertThat(peasants.onNativeTerrain()).isFalse();

        setup(List.of(pikemen)).ground(Terrain.GRASS, MagicTerrain.CURSED_GROUND);
        assertThat(pikemen.onNativeTerrain()).as("Cursed Ground hebt den Bonus auf").isFalse();
        assertThat(pikemen.getAttack()).isEqualTo(4);
    }

    @Test
    void holy_ground_and_evil_fog_move_the_morale_of_good_and_evil_creatures() {
        assertThat(moraleShift(UnitCatalog.PIKEMAN, MagicTerrain.HOLY_GROUND)).isEqualTo(1);
        assertThat(moraleShift(UnitCatalog.IMP, MagicTerrain.HOLY_GROUND)).isEqualTo(-1);
        assertThat(moraleShift(UnitCatalog.GOBLIN, MagicTerrain.HOLY_GROUND)).isZero();
        assertThat(moraleShift(UnitCatalog.SKELETON, MagicTerrain.HOLY_GROUND)).as("Untote bleiben bei 0").isZero();
        assertThat(moraleShift(UnitCatalog.PIKEMAN, MagicTerrain.EVIL_FOG)).isEqualTo(-1);
        assertThat(moraleShift(UnitCatalog.IMP, MagicTerrain.EVIL_FOG)).isEqualTo(1);
    }

    private static int moraleShift(Unit unit, MagicTerrain magic) {
        Stack stack = stack(unit);
        BattleSetup setup = setup(List.of(stack));
        int before = stack.getMorale();
        setup.ground(null, magic);
        return stack.getMorale() - before;
    }

    @Test
    void clover_field_lifts_the_luck_of_the_neutral_towns_under_a_hero() {
        Stack goblins = stack(UnitCatalog.GOBLIN);
        Stack pikemen = stack(UnitCatalog.PIKEMAN);
        Stack trolls = stack(UnitCatalog.TROLL);
        Stack leaderless = new Stack(UnitCatalog.GOBLIN, 10, new Hex(0, 9), Side.DEFENDER, 0);
        new BattleSetup(List.of(goblins, pikemen, trolls), List.of(leaderless), FIELD, HeroCatalog.TAZAR, null)
                .ground(null, MagicTerrain.CLOVER_FIELD);

        assertThat(List.of(goblins.getLuck(), pikemen.getLuck(), trolls.getLuck(), leaderless.getLuck()))
                .containsExactly(2, 0, 0, 0);
    }

    @Test
    void cursed_ground_takes_positive_morale_and_all_luck_and_most_magic() {
        Hero leader = HeroCatalog.SORSHA.withBuild(0, 0, 0, 0,
                Map.of(SecondarySkill.LEADERSHIP, SkillLevel.EXPERT, SecondarySkill.LUCK, SkillLevel.EXPERT));
        Stack pikemen = stack(UnitCatalog.PIKEMAN);
        BattleSetup setup = new BattleSetup(List.of(pikemen), List.of(stack(UnitCatalog.IMP, Side.DEFENDER)), FIELD,
                leader, null).ground(Terrain.GRASS, MagicTerrain.CURSED_GROUND);

        assertThat(pikemen.getMorale()).isZero();
        assertThat(pikemen.getLuck()).isZero();
        assertThat(HeroSpellcasting.castable(Spell.MAGIC_ARROW, setup)).isTrue();
        assertThat(HeroSpellcasting.castable(Spell.LIGHTNING_BOLT, setup)).isFalse();
        assertThat(CreatureSpellcasting.blocked(setup)).isTrue();
    }

    @Test
    void magic_plains_and_the_school_fields_raise_spells_to_expert() {
        Hero novice = HeroCatalog.SORSHA;
        BattleSetup plains = setup(List.of(stack(UnitCatalog.PIKEMAN))).ground(null, MagicTerrain.MAGIC_PLAINS);
        BattleSetup fiery = setup(List.of(stack(UnitCatalog.PIKEMAN))).ground(null, MagicTerrain.FIERY_FIELDS);

        assertThat(HeroSpellcasting.castLevel(novice, Spell.LIGHTNING_BOLT, plains)).isEqualTo(SkillLevel.EXPERT);
        assertThat(HeroSpellcasting.castLevel(novice, Spell.FIREBALL, fiery)).isEqualTo(SkillLevel.EXPERT);
        assertThat(HeroSpellcasting.castLevel(novice, Spell.MAGIC_ARROW, fiery)).as("auch Feuerschule")
                .isEqualTo(SkillLevel.EXPERT);
        assertThat(HeroSpellcasting.castLevel(novice, Spell.LIGHTNING_BOLT, fiery)).isEqualTo(SkillLevel.BASIC);
        assertThat(CreatureSpellcasting.level(SkillLevel.ADVANCED, plains)).isEqualTo(SkillLevel.EXPERT);
        assertThat(CreatureSpellcasting.level(SkillLevel.ADVANCED, fiery)).isEqualTo(SkillLevel.ADVANCED);
    }

    @Test
    void obstacles_come_from_the_terrain_and_leave_the_outer_columns_free() {
        for (Terrain terrain : Terrain.values()) {
            for (long seed = 0; seed < 20; seed++) {
                assertFieldShape(TerrainObstacles.generate(terrain, null, new Random(seed)));
            }
        }
        for (MagicTerrain magic : MagicTerrain.values()) {
            for (long seed = 0; seed < 20; seed++) {
                assertFieldShape(TerrainObstacles.generate(Terrain.GRASS, magic, new Random(seed)));
            }
        }
        assertThat(TerrainObstacles.generate(null, null, new Random(1))).isEmpty();
        assertThat(TerrainObstacles.generate(Terrain.LAVA, null, new Random(7)))
                .isEqualTo(TerrainObstacles.generate(Terrain.LAVA, null, new Random(7)));
    }

    /** Nur die Spalten 3–13 des Spiels, also nie die beiden Aufstellungsspalten an jedem Rand. */
    private static void assertFieldShape(Set<Hex> obstacles) {
        Set<Hex> inner = new HashSet<>();
        for (int row = 0; row < FIELD.height(); row++) {
            for (int column = 3; column <= 13; column++) {
                inner.add(SiegeLayout.h3(row * 17 + column));
            }
        }
        assertThat(obstacles).isNotEmpty().hasSizeLessThanOrEqualTo(24).allMatch(inner::contains)
                .allMatch(FIELD::contains);
    }

    private static Stack stack(Unit unit) {
        return stack(unit, Side.ATTACKER);
    }

    private static Stack stack(Unit unit, Side side) {
        return new Stack(unit, 10, new Hex(side == Side.ATTACKER ? 5 : 12, 5), side, 0);
    }

    private static BattleSetup setup(List<Stack> attackers) {
        return new BattleSetup(attackers, List.of(stack(UnitCatalog.PEASANT, Side.DEFENDER)), FIELD);
    }
}
