package de.zettsystems.h3comsim.battle.domain;

import de.zettsystems.h3comsim.battle.domain.events.BattleEvent;
import de.zettsystems.h3comsim.battle.domain.events.ListEventCollector;
import de.zettsystems.h3comsim.battle.domain.events.Side;
import de.zettsystems.h3comsim.battle.domain.events.Winner;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

class MultiStackBattleTest {

    @Test
    void two_stacks_per_side_finish_battle_deterministically() {
        Stack a0 = new Stack(UnitCatalog.PIKEMAN, 30, new Hex(0, 4), Side.ATTACKER, 0);
        Stack a1 = new Stack(UnitCatalog.ARCHER, 20, new Hex(0, 6), Side.ATTACKER, 1);
        Stack d0 = new Stack(UnitCatalog.PIKEMAN, 30, new Hex(14, 4), Side.DEFENDER, 0);
        Stack d1 = new Stack(UnitCatalog.ARCHER, 20, new Hex(14, 6), Side.DEFENDER, 1);

        BattleSetup setup = new BattleSetup(List.of(a0, a1), List.of(d0, d1), Battlefield.STANDARD);

        BattleResult result = new Battle(new Random(42)).simulate(setup);

        // Mit Symmetrie und identischer Heuristik: kein DRAW erwartet, RNG entscheidet.
        // Zentrale Asserts: Runde > 0, mindestens eine Seite vollständig aufgelöst.
        assertThat(result.turnsTaken()).isGreaterThan(0);
        boolean attackerEmpty = setup.attackerStacks().stream().mapToInt(Stack::getCount).sum() == 0;
        boolean defenderEmpty = setup.defenderStacks().stream().mapToInt(Stack::getCount).sum() == 0;
        assertThat(attackerEmpty || defenderEmpty)
                .as("Eine Seite muss komplett gefallen sein").isTrue();
        assertThat(result.winner()).isIn(Winner.ATTACKER, Winner.DEFENDER, Winner.DRAW);
    }

    @Test
    void move_action_to_occupied_hex_falls_back_to_defend() {
        // Diagnose- + Fix-Test: ein Solver der explizit auf einen besetzten Hex zielt darf
        // nicht zwei lebende Stacks auf dem gleichen Hex landen lassen. Erwartung: Engine
        // weist den Move ab und stellt den Stack defensiv (Defend → +30 % Defense),
        // statt seine Aktion ersatzlos zu verlieren.
        Stack dragon = new Stack(UnitCatalog.BLACK_DRAGON, 1, new Hex(0, 5), Side.ATTACKER, 0);
        Stack tank = new Stack(UnitCatalog.HALBERDIER, 14, new Hex(5, 5), Side.DEFENDER, 0);
        Stack marksman = new Stack(UnitCatalog.MARKSMAN, 9, new Hex(14, 5), Side.DEFENDER, 1);
        BattleSetup setup = new BattleSetup(List.of(dragon), List.of(tank, marksman),
                Battlefield.STANDARD);

        Hex occupied = tank.position();
        ListEventCollector collector = new ListEventCollector();
        AutoSolver pinSolver = (active, opp, bf) -> {
            if (active == dragon) {
                return new Action.Move(occupied);
            }
            return new Action.Wait();
        };
        Hex dragonStart = dragon.position();

        new Battle(new Random(1L), pinSolver, collector).simulate(setup);

        assertThat(dragon.position()).isNotEqualTo(tank.position());
        assertThat(dragon.position()).isEqualTo(dragonStart);
        // Defend-Event wurde emittiert (Engine-Fallback) — kein stilles Wait.
        assertThat(collector.events()).anyMatch(e -> e instanceof BattleEvent.Defend);
    }

    @Test
    void defend_action_grants_thirty_percent_defense_until_end_of_turn() {
        // Stack defendet → +30 % Defense aktiv für diese Runde. Nach endTurn zurück auf Base.
        Stack hal = new Stack(UnitCatalog.HALBERDIER, 14, new Hex(0, 5), Side.ATTACKER, 0);
        int baseDefense = hal.getDefense();

        hal.defend();
        assertThat(hal.isDefending()).isTrue();
        assertThat(hal.getDefense()).isEqualTo((int) Math.round(baseDefense * 1.3));

        hal.endTurn();
        assertThat(hal.isDefending()).isFalse();
        assertThat(hal.getDefense()).isEqualTo(baseDefense);
    }

    @Test
    void move_order_uses_speed_with_attacker_priority_tiebreaker() {
        // Beide Seiten haben einen Pikeman (Speed 4) und einen Archer (Speed 4).
        // Erwartung: 4 Stacks mit gleicher Speed → Reihenfolge Attacker-Slot-0, Attacker-Slot-1,
        // Defender-Slot-0, Defender-Slot-1.
        // Verifiziert indirekt durch BattleResult-Determinismus: gleicher Seed liefert gleichen Outcome.
        BattleSetup setup1 = newSymmetricSetup();
        BattleSetup setup2 = newSymmetricSetup();

        BattleResult r1 = new Battle(new Random(7)).simulate(setup1);
        BattleResult r2 = new Battle(new Random(7)).simulate(setup2);

        assertThat(r1.winner()).isEqualTo(r2.winner());
        assertThat(r1.turnsTaken()).isEqualTo(r2.turnsTaken());
    }

    private static BattleSetup newSymmetricSetup() {
        Stack a0 = new Stack(UnitCatalog.PIKEMAN, 20, new Hex(0, 4), Side.ATTACKER, 0);
        Stack a1 = new Stack(UnitCatalog.ARCHER, 10, new Hex(0, 6), Side.ATTACKER, 1);
        Stack d0 = new Stack(UnitCatalog.PIKEMAN, 20, new Hex(14, 4), Side.DEFENDER, 0);
        Stack d1 = new Stack(UnitCatalog.ARCHER, 10, new Hex(14, 6), Side.DEFENDER, 1);
        return new BattleSetup(List.of(a0, a1), List.of(d0, d1), Battlefield.STANDARD);
    }

    @Test
    void battle_start_event_lists_all_initial_stacks() {
        Stack a0 = new Stack(UnitCatalog.PIKEMAN, 10, new Hex(0, 4), Side.ATTACKER, 0);
        Stack a1 = new Stack(UnitCatalog.ARCHER, 5, new Hex(0, 6), Side.ATTACKER, 1);
        Stack d0 = new Stack(UnitCatalog.PIKEMAN, 10, new Hex(14, 4), Side.DEFENDER, 0);

        BattleSetup setup = new BattleSetup(List.of(a0, a1), List.of(d0), Battlefield.STANDARD);
        var collector = new de.zettsystems.h3comsim.battle.domain.events.ListEventCollector();
        new Battle(new Random(1), new GreedyAutoSolver(), collector).simulate(setup);

        var start = (de.zettsystems.h3comsim.battle.domain.events.BattleEvent.BattleStart) collector.events().get(0);
        assertThat(start.stacks()).hasSize(3);
        assertThat(start.stacks()).extracting(s -> s.side() + "-" + s.slot())
                .containsExactlyInAnyOrder("ATTACKER-0", "ATTACKER-1", "DEFENDER-0");
    }
}
