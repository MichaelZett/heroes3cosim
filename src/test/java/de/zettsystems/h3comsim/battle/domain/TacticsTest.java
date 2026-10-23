package de.zettsystems.h3comsim.battle.domain;

import de.zettsystems.h3comsim.battle.domain.events.BattleEvent;
import de.zettsystems.h3comsim.battle.domain.events.ListEventCollector;
import de.zettsystems.h3comsim.battle.domain.events.Side;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tactics (Manual S. 42): Aufstellung in den ersten 3 / 5 / 7 Spalten bei einem, zwei oder drei
 * Graden Vorsprung; bei gleicher Stufe stellt keiner auf.
 */
class TacticsTest {

    private static Hero withTactics(SkillLevel level) {
        return new Hero("Taktiker", HeroClass.KNIGHT, Faction.CASTLE, 0, 0, 1, 1,
                Map.of(SecondarySkill.TACTICS, level));
    }

    private static Hero plain() {
        return new Hero("Statist", HeroClass.KNIGHT, Faction.CASTLE, 0, 0, 1, 1, Map.of());
    }

    @Test
    void the_zone_depends_on_the_advantage_over_the_enemy() {
        assertThat(Tactics.columns(withTactics(SkillLevel.BASIC), plain())).isEqualTo(3);
        assertThat(Tactics.columns(withTactics(SkillLevel.ADVANCED), null)).isEqualTo(5);
        assertThat(Tactics.columns(withTactics(SkillLevel.EXPERT), plain())).isEqualTo(7);
        assertThat(Tactics.columns(withTactics(SkillLevel.EXPERT), withTactics(SkillLevel.BASIC))).isEqualTo(5);
        assertThat(Tactics.columns(withTactics(SkillLevel.ADVANCED), withTactics(SkillLevel.ADVANCED))).isZero();
        assertThat(Tactics.columns(withTactics(SkillLevel.BASIC), withTactics(SkillLevel.ADVANCED))).isZero();
        assertThat(Tactics.columns(null, withTactics(SkillLevel.EXPERT))).isZero();
    }

    @Test
    void melee_stacks_advance_to_the_front_of_the_zone_while_shooters_stay() {
        Battlefield field = Battlefield.STANDARD;
        Stack pikemen = new Stack(UnitCatalog.PIKEMAN, 10, new Hex(field.leftmostQ(5), 5), Side.ATTACKER, 0);
        Stack archers = new Stack(UnitCatalog.ARCHER, 10, new Hex(field.leftmostQ(0), 0), Side.ATTACKER, 1);
        Stack enemy = new Stack(UnitCatalog.PIKEMAN, 10, new Hex(field.rightmostQ(5), 5), Side.DEFENDER, 0);

        BattleSetup setup = new BattleSetup(List.of(pikemen, archers), List.of(enemy), field,
                withTactics(SkillLevel.BASIC), plain());

        assertThat(setup.tacticsOf(Side.ATTACKER)).isEqualTo(3);
        assertThat(setup.tacticsOf(Side.DEFENDER)).isZero();
        assertThat(pikemen.position()).isEqualTo(new Hex(field.leftmostQ(5) + 2, 5));
        assertThat(archers.position()).isEqualTo(new Hex(field.leftmostQ(0), 0));
        assertThat(enemy.position()).isEqualTo(new Hex(field.rightmostQ(5), 5));
    }

    @Test
    void expert_tactics_advances_no_deeper_than_five_columns_and_skips_obstacles() {
        Battlefield field = Battlefield.STANDARD.withObstacles(
                Set.of(new Hex(Battlefield.STANDARD.rightmostQ(5) - 4, 5)));
        Stack enemy = new Stack(UnitCatalog.PIKEMAN, 10, new Hex(field.leftmostQ(5), 5), Side.ATTACKER, 0);
        Stack defender = new Stack(UnitCatalog.PIKEMAN, 10, new Hex(field.rightmostQ(5), 5), Side.DEFENDER, 0);

        new BattleSetup(List.of(enemy), List.of(defender), field, plain(), withTactics(SkillLevel.EXPERT));

        // Spalte 5 (Tiefe 4) ist blockiert, also die nächste freie dahinter.
        assertThat(defender.position()).isEqualTo(new Hex(field.rightmostQ(5) - 3, 5));
    }

    @Test
    void the_battle_announces_the_tactics_phase() {
        Stack pikemen = new Stack(UnitCatalog.PIKEMAN, 10, new Hex(0, 5), Side.ATTACKER, 0);
        Stack enemy = new Stack(UnitCatalog.PIKEMAN, 10, new Hex(14, 5), Side.DEFENDER, 0);
        BattleSetup setup = new BattleSetup(List.of(pikemen), List.of(enemy), Battlefield.STANDARD,
                withTactics(SkillLevel.ADVANCED), null);
        ListEventCollector collector = new ListEventCollector();

        new Battle(new Random(1), new GreedyAutoSolver(), collector).simulate(setup);

        assertThat(collector.events().get(1)).isEqualTo(new BattleEvent.TacticsPhase(Side.ATTACKER, 5));
    }
}
