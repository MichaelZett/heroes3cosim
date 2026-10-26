package de.zettsystems.h3comsim.battle.domain;

import de.zettsystems.h3comsim.battle.domain.events.BattleEvent;
import de.zettsystems.h3comsim.battle.domain.events.HexCoord;
import de.zettsystems.h3comsim.battle.domain.events.ListEventCollector;
import de.zettsystems.h3comsim.battle.domain.events.Side;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/** Fire Wall, Force Field, Quicksand, Land Mine und Remove Obstacle (heroes.thelazy.net, VCMI). */
class FieldSpellsTest {

    private static final Battlefield FIELD = Battlefield.STANDARD;

    @Test
    void walls_grow_from_the_target_towards_the_enemy_side() {
        Hex center = new Hex(7, 5);
        assertThat(FieldSpells.wall(center, Side.ATTACKER, SkillLevel.BASIC)).containsExactly(center, new Hex(8, 4));
        assertThat(FieldSpells.wall(center, Side.ATTACKER, SkillLevel.EXPERT))
                .containsExactly(center, new Hex(8, 4), new Hex(8, 3));
        assertThat(FieldSpells.wall(center, Side.DEFENDER, SkillLevel.ADVANCED))
                .containsExactly(center, new Hex(7, 4), new Hex(8, 3));
    }

    @Test
    void force_fields_block_everyone_and_quicksand_only_those_who_know_it() {
        BattleSetup setup = setup(walker(), List.of(archers()));
        setup.fieldEffects().add(FieldEffects.Kind.FORCE_FIELD, List.of(new Hex(8, 5)), Side.ATTACKER, 0, 1, null);
        setup.fieldEffects().add(FieldEffects.Kind.QUICKSAND, List.of(new Hex(9, 5)), Side.ATTACKER, 0,
                Integer.MAX_VALUE, null);

        assertThat(setup.battlefieldFor(Side.DEFENDER).isPassable(new Hex(8, 5))).isFalse();
        assertThat(setup.battlefieldFor(Side.ATTACKER).isPassable(new Hex(9, 5))).isFalse();
        assertThat(setup.battlefieldFor(Side.DEFENDER).isPassable(new Hex(9, 5))).as("unsichtbar").isTrue();

        setup.fieldEffects().expire(1);
        assertThat(setup.battlefieldFor(Side.DEFENDER).isPassable(new Hex(8, 5))).as("nach zwei Runden weg").isTrue();
    }

    @Test
    void a_mine_hurts_the_enemy_who_steps_on_it_and_is_gone() {
        BattleSetup setup = setup(walker(), List.of(archers()));
        setup.fieldEffects().add(FieldEffects.Kind.LAND_MINE, row(5, 4, 12), Side.ATTACKER, 100,
                Integer.MAX_VALUE, null);

        List<BattleEvent> events = fight(setup);

        assertThat(events).anyMatch(e -> e instanceof BattleEvent.FieldTrigger t && t.spell() == Spell.LAND_MINE
                && t.side() == Side.DEFENDER && t.damage() == 100);
        assertThat(setup.fieldEffects().patches()).hasSizeLessThan(9);
    }

    @Test
    void quicksand_stops_a_walker_on_the_first_pit() {
        BattleSetup setup = setup(walker(), List.of(archers()));
        setup.fieldEffects().add(FieldEffects.Kind.QUICKSAND, row(5, 4, 12), Side.ATTACKER, 0, Integer.MAX_VALUE,
                null);

        List<BattleEvent> events = fight(setup);

        BattleEvent.FieldTrigger stuck = events.stream()
                .filter(e -> e instanceof BattleEvent.FieldTrigger t && t.spell() == Spell.QUICKSAND)
                .map(BattleEvent.FieldTrigger.class::cast).findFirst().orElseThrow();
        assertThat(stuck.side()).isEqualTo(Side.DEFENDER);
        assertThat(stuck.targetAfter().q()).isEqualTo(stuck.hex().q());
    }

    @Test
    void a_fire_wall_burns_whoever_walks_through() {
        BattleSetup setup = setup(walker(), List.of(archers()));
        setup.fieldEffects().add(FieldEffects.Kind.FIRE_WALL, row(5, 9, 12), Side.ATTACKER, 60, 5, null);

        List<BattleEvent> events = fight(setup);

        assertThat(events).anyMatch(e -> e instanceof BattleEvent.FieldTrigger t && t.spell() == Spell.FIRE_WALL
                && t.damage() == 60);
    }

    @Test
    void the_hero_places_field_spells_against_an_approaching_walker() {
        for (Spell spell : List.of(Spell.FIRE_WALL, Spell.QUICKSAND, Spell.LAND_MINE)) {
            BattleSetup setup = new BattleSetup(List.of(archers()), List.of(walker()), FIELD, mage(spell), null);

            List<BattleEvent> events = fight(setup);

            assertThat(events).as(spell.name())
                    .anyMatch(e -> e instanceof BattleEvent.FieldSpell f && f.spell() == spell && !f.removed());
        }
    }

    @Test
    void a_force_field_is_worth_the_rounds_it_costs_the_enemy() {
        // Eine Mauer aus Felsen in Spalte 9 mit einer Lücke in den Reihen 4–6.
        Set<Hex> rocks = new HashSet<>();
        List<Hex> gap = new ArrayList<>();
        for (int row = 0; row < FIELD.height(); row++) {
            Hex hex = SiegeLayout.h3(row * 17 + 9);
            if (row >= 4 && row <= 6) {
                gap.add(hex);
            } else {
                rocks.add(hex);
            }
        }
        Stack walker = walker();
        BattleSetup setup = new BattleSetup(List.of(archers()), List.of(walker), FIELD.withObstacles(rocks));
        Hero mage = mage(Spell.FORCE_FIELD);

        double closing = FieldSpells.value(mage, Side.ATTACKER,
                new HeroSpellcasting.Cast(Spell.FORCE_FIELD, SkillLevel.EXPERT, walker, List.of(walker), gap), setup);
        double elsewhere = FieldSpells.value(mage, Side.ATTACKER,
                new HeroSpellcasting.Cast(Spell.FORCE_FIELD, SkillLevel.EXPERT, walker, List.of(walker),
                        List.of(new Hex(5, 0), new Hex(6, 0))), setup);

        assertThat(closing).isPositive();
        assertThat(elsewhere).isZero();
    }

    @Test
    void land_mine_is_not_cast_against_an_enemy_on_its_native_terrain() {
        BattleSetup setup = new BattleSetup(List.of(archers()), List.of(walker()), FIELD, mage(Spell.LAND_MINE), null)
                .ground(Terrain.GRASS, null);

        assertThat(FieldSpells.candidates(Side.ATTACKER, Spell.LAND_MINE, SkillLevel.EXPERT,
                setup)).isEmpty();
    }

    @Test
    void remove_obstacle_takes_whole_obstacles_but_not_cliffs() {
        Set<Hex> rock = Set.of(new Hex(8, 5), new Hex(9, 5));
        BattleSetup setup = new BattleSetup(List.of(archers()), List.of(walker()), FIELD.withObstacles(rock));
        setup.obstacleGroups(List.of(rock));

        assertThat(FieldSpells.removable(SkillLevel.BASIC, Side.ATTACKER, setup)).hasSize(1);
        setup.removeObstacle(rock);
        assertThat(setup.battlefieldFor(Side.DEFENDER).isPassable(new Hex(8, 5))).isTrue();
        assertThat(FieldSpells.removable(SkillLevel.BASIC, Side.ATTACKER, setup)).isEmpty();
    }

    @Test
    void the_hero_opens_a_wall_of_rocks_with_remove_obstacle() {
        Set<Hex> rocks = new HashSet<>();
        for (int row = 0; row < FIELD.height(); row++) {
            rocks.add(SiegeLayout.h3(row * 17 + 9));
        }
        Map<SecondarySkill, SkillLevel> skills = Map.of(SecondarySkill.WISDOM, SkillLevel.EXPERT,
                SecondarySkill.WATER_MAGIC, SkillLevel.EXPERT);
        Hero mage = HeroCatalog.SORSHA.withBuild(0, 0, 6, 20, skills).withSpellbook(Set.of(Spell.REMOVE_OBSTACLE));
        BattleSetup setup = new BattleSetup(List.of(new Stack(UnitCatalog.PIKEMAN, 30, new Hex(3, 5), Side.ATTACKER, 0)),
                List.of(new Stack(UnitCatalog.PEASANT, 50, new Hex(14, 5), Side.DEFENDER, 0)),
                FIELD.withObstacles(rocks), mage, null);

        List<BattleEvent> events = fight(setup);

        assertThat(events).anyMatch(e -> e instanceof BattleEvent.FieldSpell f && f.spell() == Spell.REMOVE_OBSTACLE
                && f.removed());
        assertThat(setup.standingObstacles()).hasSizeLessThan(rocks.size());
    }

    @Test
    void expert_remove_obstacle_clears_an_enemy_force_field_in_the_only_gap() {
        Set<Hex> rocks = new HashSet<>();
        Hex gap = SiegeLayout.h3(5 * 17 + 9);
        for (int row = 0; row < FIELD.height(); row++) {
            if (row != 5) {
                rocks.add(SiegeLayout.h3(row * 17 + 9));
            }
        }
        Map<SecondarySkill, SkillLevel> skills = Map.of(SecondarySkill.WISDOM, SkillLevel.EXPERT,
                SecondarySkill.WATER_MAGIC, SkillLevel.EXPERT);
        Hero mage = HeroCatalog.SORSHA.withBuild(0, 0, 6, 20, skills).withSpellbook(Set.of(Spell.REMOVE_OBSTACLE));
        BattleSetup setup = new BattleSetup(List.of(new Stack(UnitCatalog.PIKEMAN, 30, new Hex(3, 5), Side.ATTACKER, 0)),
                List.of(new Stack(UnitCatalog.PEASANT, 50, new Hex(14, 5), Side.DEFENDER, 0)),
                FIELD.withObstacles(rocks), mage, null).obstacleGroups(List.of());
        setup.fieldEffects().add(FieldEffects.Kind.FORCE_FIELD, List.of(gap), Side.DEFENDER, 0, Integer.MAX_VALUE, null);

        List<BattleEvent> events = fight(setup);

        assertThat(events).anyMatch(e -> e instanceof BattleEvent.FieldSpell f && f.spell() == Spell.REMOVE_OBSTACLE
                && f.removed() && f.hexes().equals(List.of(new HexCoord(gap.q(), gap.r()))));
    }

    @Test
    void remove_obstacle_takes_fire_walls_from_advanced_and_all_spell_fields_on_expert() {
        BattleSetup setup = setup(walker(), List.of(archers()));
        setup.fieldEffects().add(FieldEffects.Kind.FIRE_WALL, List.of(new Hex(8, 5)), Side.DEFENDER, 10, 1, null);
        setup.fieldEffects().add(FieldEffects.Kind.FORCE_FIELD, List.of(new Hex(8, 7)), Side.DEFENDER, 0, 1, null);

        assertThat(FieldSpells.removable(SkillLevel.BASIC, Side.ATTACKER, setup)).isEmpty();
        assertThat(FieldSpells.removable(SkillLevel.ADVANCED, Side.ATTACKER, setup)).containsExactly(List.of(new Hex(8, 5)));
        assertThat(FieldSpells.removable(SkillLevel.EXPERT, Side.ATTACKER, setup)).hasSize(2);
    }

    @Test
    void quicksand_and_mines_are_worth_more_the_longer_the_enemy_walks() {
        BattleSetup setup = setup(walker(), List.of(archers()));
        Hero hero = mage(Spell.LAND_MINE);
        List<HeroSpellcasting.Cast> mines = FieldSpells.candidates(Side.ATTACKER, Spell.LAND_MINE, SkillLevel.EXPERT, setup);
        List<HeroSpellcasting.Cast> sand = FieldSpells.candidates(Side.ATTACKER, Spell.QUICKSAND, SkillLevel.EXPERT, setup);

        assertThat(FieldSpells.value(hero, Side.ATTACKER, mines.getFirst(), setup)).isPositive();
        assertThat(FieldSpells.value(hero, Side.ATTACKER, sand.getFirst(), setup)).isPositive();
    }

    @Test
    void fire_walls_and_force_fields_vanish_after_their_second_round() {
        BattleSetup setup = setup(walker(), List.of(archers()));
        setup.fieldEffects().add(FieldEffects.Kind.FORCE_FIELD, List.of(new Hex(5, 0)), Side.ATTACKER, 0, 1, null);

        List<BattleEvent> events = fight(setup);

        assertThat(events).anyMatch(e -> e instanceof BattleEvent.FieldSpell f && f.spell() == Spell.FORCE_FIELD
                && f.removed());
    }

    private static List<Hex> row(int r, int fromQ, int toQ) {
        return IntStream.rangeClosed(fromQ, toQ).mapToObj(q -> new Hex(q, r)).toList();
    }

    private static Stack walker() {
        return new Stack(UnitCatalog.PIKEMAN, 30, new Hex(14, 5), Side.DEFENDER, 0);
    }

    private static Stack archers() {
        return new Stack(UnitCatalog.ARCHER, 5, new Hex(3, 5), Side.ATTACKER, 0);
    }

    private static Hero mage(Spell spell) {
        Map<SecondarySkill, SkillLevel> skills = new HashMap<>(Map.of(SecondarySkill.WISDOM, SkillLevel.EXPERT,
                SecondarySkill.FIRE_MAGIC, SkillLevel.EXPERT, SecondarySkill.EARTH_MAGIC, SkillLevel.EXPERT));
        return HeroCatalog.SORSHA.withBuild(0, 0, 6, 20, skills).withSpellbook(Set.of(spell));
    }

    private static BattleSetup setup(Stack defender, List<Stack> attackers) {
        return new BattleSetup(attackers, List.of(defender), FIELD);
    }

    private static List<BattleEvent> fight(BattleSetup setup) {
        ListEventCollector collector = new ListEventCollector();
        new Battle(new Random(5), new GreedyAutoSolver(), collector).simulate(setup);
        return collector.events();
    }
}
