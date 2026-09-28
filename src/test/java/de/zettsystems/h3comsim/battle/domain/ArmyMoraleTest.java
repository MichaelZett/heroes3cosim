package de.zettsystems.h3comsim.battle.domain;

import de.zettsystems.h3comsim.battle.domain.events.Side;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Das Armee-Moral-Rating nach Manual S. 43. Die drei Regeln der Tabelle dort:
 * <pre>
 *   -1  wenn Untote mit Lebenden gemischt sind
 *   +1  wenn alle Kreaturen aus derselben Stadt stammen (außer Necropolis)
 *   -1  je Stadt-Typ jenseits des zweiten
 * </pre>
 * Dazu die beiden Quellen, die dieser Simulator kennt: Leadership des Helden und
 * {@code GOOD_ARMY_MORALE} von Angel und Arch Angel.
 */
class ArmyMoraleTest {

    private static Stack stackOf(Unit unit) {
        return new Stack(unit, 10, new Hex(0, 5), Side.ATTACKER, 0);
    }

    private static int rating(@Nullable Hero hero, Unit... units) {
        return ArmyMorale.ratingOf(Stream.of(units).map(ArmyMoraleTest::stackOf).toList(), hero);
    }

    private static Hero heroWith(SecondarySkill skill, SkillLevel level) {
        return new Hero("Testheld", HeroClass.KNIGHT, Faction.CASTLE, 0, 0, 1, 1,
                Map.of(skill, level));
    }

    @Test
    void one_town_type_grants_a_bonus() {
        assertThat(rating(null, UnitCatalog.PIKEMAN, UnitCatalog.MARKSMAN)).isOne();
    }

    @Test
    void necropolis_is_excluded_from_that_bonus() {
        // Manual S. 43: „+1 if all creatures are of the same town type (except Necropolis)".
        assertThat(rating(null, UnitCatalog.SKELETON, UnitCatalog.WIGHT)).isZero();
    }

    @Test
    void two_town_types_are_neither_rewarded_nor_punished() {
        assertThat(rating(null, UnitCatalog.PIKEMAN, UnitCatalog.CENTAUR)).isZero();
    }

    @Test
    void every_town_type_beyond_the_second_costs_a_point() {
        assertThat(rating(null, UnitCatalog.PIKEMAN, UnitCatalog.CENTAUR, UnitCatalog.GOBLIN))
                .isEqualTo(-1);
        assertThat(rating(null, UnitCatalog.PIKEMAN, UnitCatalog.CENTAUR, UnitCatalog.GOBLIN,
                UnitCatalog.STONE_GARGOYLE)).isEqualTo(-2);
    }

    @Test
    void mixing_undead_with_the_living_costs_a_point_on_top() {
        // Zwei Stadt-Typen: kein Bonus, keine Strafe aus der Anzahl. Bleibt der Untoten-Malus.
        assertThat(rating(null, UnitCatalog.PIKEMAN, UnitCatalog.SKELETON)).isEqualTo(-1);
    }

    @Test
    void leadership_adds_its_documented_points() {
        // Skelette als Armee, damit nur der Held zählt: Necropolis bekommt weder den
        // Stadt-Bonus noch einen Untoten-Malus.
        Map<SkillLevel, Integer> expected = Map.of(
                SkillLevel.BASIC, 1, SkillLevel.ADVANCED, 2, SkillLevel.EXPERT, 3);
        expected.forEach((level, points) ->
                assertThat(rating(heroWith(SecondarySkill.LEADERSHIP, level), UnitCatalog.SKELETON))
                        .as("Leadership %s", level)
                        .isEqualTo(points));
    }

    @Test
    void angels_lift_the_whole_army_not_just_themselves() {
        // Manual S. 96: Angel und Arch Angel heben die Moral der gesamten Armee.
        int withoutAngel = rating(null, UnitCatalog.PIKEMAN);
        assertThat(rating(null, UnitCatalog.PIKEMAN, UnitCatalog.ANGEL)).isEqualTo(withoutAngel + 1);
        assertThat(rating(null, UnitCatalog.PIKEMAN, UnitCatalog.ARCH_ANGEL)).isEqualTo(withoutAngel + 1);
    }

    @Test
    void the_rating_is_capped_only_at_the_stack() {
        // ArmyMorale rechnet ungekappt weiter — erst Stack.getMorale() bindet auf [-3, 3].
        Hero expertLeader = heroWith(SecondarySkill.LEADERSHIP, SkillLevel.EXPERT);
        assertThat(rating(expertLeader, UnitCatalog.PIKEMAN, UnitCatalog.ANGEL)).isEqualTo(5);

        Stack stack = stackOf(UnitCatalog.PIKEMAN);
        stack.assignArmyMorale(5);
        assertThat(stack.getMorale()).isEqualTo(ArmyMorale.MAX);
    }

    @Test
    void a_creature_with_good_morale_adds_its_own_point() {
        Stack minotaur = stackOf(UnitCatalog.MINOTAUR);
        minotaur.assignArmyMorale(1);
        assertThat(minotaur.getMorale()).isEqualTo(2);
    }

    @Test
    void undead_and_elementals_ignore_the_army_rating_entirely() {
        // Manual S. 43: „independent of their army's morale rating" — in beide Richtungen.
        for (Unit unit : List.of(UnitCatalog.SKELETON, UnitCatalog.FIRE_ELEMENTAL)) {
            Stack stack = stackOf(unit);
            stack.assignArmyMorale(3);
            assertThat(stack.getMorale()).as("%s bei Rating +3", unit.name()).isZero();
            stack.assignArmyMorale(-3);
            assertThat(stack.getMorale()).as("%s bei Rating -3", unit.name()).isZero();
        }
    }

    @Test
    void conflux_creatures_that_are_no_elementals_keep_their_morale() {
        // Firebird, Phoenix, Pixie und Sprite sind Conflux, aber keine Elementare.
        for (Unit unit : List.of(UnitCatalog.FIREBIRD, UnitCatalog.PHOENIX,
                UnitCatalog.PIXIE, UnitCatalog.SPRITE)) {
            Stack stack = stackOf(unit);
            stack.assignArmyMorale(2);
            assertThat(stack.getMorale()).as(unit.name()).isEqualTo(2);
        }
    }

    @Test
    void the_setup_hands_the_rating_to_every_stack_of_its_side() {
        Stack attacker = new Stack(UnitCatalog.PIKEMAN, 10, new Hex(0, 5), Side.ATTACKER, 0);
        Stack defender = new Stack(UnitCatalog.SKELETON, 10, new Hex(14, 5), Side.DEFENDER, 0);
        BattleSetup setup = new BattleSetup(List.of(attacker), List.of(defender),
                Battlefield.STANDARD, HeroCatalog.SORSHA, null);

        // Sorsha bringt Basic Leadership mit: +1 Stadt-Typ, +1 Leadership.
        assertThat(setup.moraleOf(Side.ATTACKER)).isEqualTo(2);
        assertThat(attacker.getMorale()).isEqualTo(2);
        // Necropolis bekommt weder den Stadt-Bonus noch überhaupt eine Moral.
        assertThat(setup.moraleOf(Side.DEFENDER)).isZero();
        assertThat(defender.getMorale()).isZero();
    }
}
