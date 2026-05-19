package de.zettsystems.h3comsim.battle.domain;

import de.zettsystems.h3comsim.battle.domain.events.BattleEvent;
import de.zettsystems.h3comsim.battle.domain.events.ListEventCollector;
import de.zettsystems.h3comsim.battle.domain.events.Side;
import de.zettsystems.h3comsim.battle.domain.events.Winner;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

class BattleEventsTest {

    @Test
    void emits_battle_start_with_initial_snapshots() {
        ListEventCollector collector = new ListEventCollector();
        BattleSetup setup = new BattleSetup(UnitCatalog.MARKSMAN, 5, UnitCatalog.PIKEMAN, 5);

        new Battle(new Random(1L), new GreedyAutoSolver(), collector).simulate(setup);

        BattleEvent.BattleStart start = (BattleEvent.BattleStart) collector.events().getFirst();
        assertThat(start.battlefieldWidth()).isEqualTo(15);
        assertThat(start.battlefieldHeight()).isEqualTo(11);
        assertThat(start.attacker().side()).isEqualTo(Side.ATTACKER);
        assertThat(start.attacker().count()).isEqualTo(5);
        assertThat(start.defender().side()).isEqualTo(Side.DEFENDER);
    }

    @Test
    void emits_battle_end_with_winner_and_turns() {
        ListEventCollector collector = new ListEventCollector();
        BattleSetup setup = new BattleSetup(UnitCatalog.ARCH_ANGEL, 5, UnitCatalog.PEASANT, 1);

        BattleResult result = new Battle(new Random(2L), new GreedyAutoSolver(), collector).simulate(setup);

        BattleEvent last = collector.events().get(collector.events().size() - 1);
        assertThat(last).isInstanceOf(BattleEvent.BattleEnd.class);
        BattleEvent.BattleEnd end = (BattleEvent.BattleEnd) last;
        assertThat(end.winner()).isEqualTo(Winner.ATTACKER);
        assertThat(end.turns()).isEqualTo(result.turnsTaken());
    }

    @Test
    void move_event_records_from_and_to_hexes() {
        ListEventCollector collector = new ListEventCollector();
        // Distance forces a Move action in turn 1 (no MoveAndMelee within reach).
        BattleSetup setup = new BattleSetup(UnitCatalog.PIKEMAN, 1, UnitCatalog.PIKEMAN, 1,
                Battlefield.STANDARD, new Hex(0, 5), new Hex(14, 5));

        new Battle(new Random(3L), new GreedyAutoSolver(), collector).simulate(setup);

        BattleEvent.Move firstMove = collector.events().stream()
                .filter(BattleEvent.Move.class::isInstance)
                .map(BattleEvent.Move.class::cast)
                .findFirst()
                .orElseThrow();
        assertThat(firstMove.fromQ()).isZero();
        assertThat(firstMove.fromR()).isEqualTo(5);
        assertThat(firstMove.toQ()).isPositive();
    }

    @Test
    void shoot_event_carries_distance_damage_and_target_snapshot() {
        ListEventCollector collector = new ListEventCollector();
        BattleSetup setup = new BattleSetup(UnitCatalog.TITAN, 1, UnitCatalog.PIKEMAN, 5);

        new Battle(new Random(7L), new GreedyAutoSolver(), collector).simulate(setup);

        BattleEvent.Shoot firstShot = collector.events().stream()
                .filter(BattleEvent.Shoot.class::isInstance)
                .map(BattleEvent.Shoot.class::cast)
                .findFirst()
                .orElseThrow();
        assertThat(firstShot.actor()).isEqualTo(Side.ATTACKER);
        assertThat(firstShot.target()).isEqualTo(Side.DEFENDER);
        assertThat(firstShot.distance()).isPositive();
        assertThat(firstShot.damage()).isPositive();
        assertThat(firstShot.targetAfter().side()).isEqualTo(Side.DEFENDER);
    }

    @Test
    void melee_event_records_hexes_moved_and_damage() {
        ListEventCollector collector = new ListEventCollector();
        // Adjacent setup → first action is pure Melee with hexesMoved = 0.
        BattleSetup setup = new BattleSetup(UnitCatalog.ARCH_ANGEL, 5, UnitCatalog.PIKEMAN, 5,
                Battlefield.STANDARD, new Hex(5, 5), new Hex(6, 5));

        new Battle(new Random(11L), new GreedyAutoSolver(), collector).simulate(setup);

        BattleEvent.Melee firstMelee = collector.events().stream()
                .filter(BattleEvent.Melee.class::isInstance)
                .map(BattleEvent.Melee.class::cast)
                .findFirst()
                .orElseThrow();
        assertThat(firstMelee.hexesMoved()).isZero();
        assertThat(firstMelee.damage()).isPositive();
        assertThat(firstMelee.killed()).isPositive();
    }

    @Test
    void move_back_event_emitted_after_harpy_killing_blow() {
        ListEventCollector collector = new ListEventCollector();
        BattleSetup setup = new BattleSetup(UnitCatalog.HARPY, 50, UnitCatalog.PIKEMAN, 5,
                Battlefield.STANDARD, new Hex(0, 5), new Hex(7, 5));

        new Battle(new Random(1L), new GreedyAutoSolver(), collector).simulate(setup);

        List<BattleEvent.MoveBack> moveBacks = collector.events().stream()
                .filter(BattleEvent.MoveBack.class::isInstance)
                .map(BattleEvent.MoveBack.class::cast)
                .toList();
        assertThat(moveBacks).isNotEmpty();
        BattleEvent.MoveBack firstMoveBack = moveBacks.getFirst();
        assertThat(firstMoveBack.actor()).isEqualTo(Side.ATTACKER);
        assertThat(firstMoveBack.toQ()).isZero();
        assertThat(firstMoveBack.toR()).isEqualTo(5);
    }

    @Test
    void noop_collector_skips_emission_without_breaking_simulation() {
        // No collector argument → uses NoopEventCollector; battle still completes.
        BattleSetup setup = new BattleSetup(UnitCatalog.ARCHER, 5, UnitCatalog.PIKEMAN, 5);

        BattleResult result = new Battle(new Random(13L)).simulate(setup);

        assertThat(result).isNotNull();
        assertThat(result.winner()).isIn(Winner.ATTACKER, Winner.DEFENDER, Winner.DRAW);
    }
}
