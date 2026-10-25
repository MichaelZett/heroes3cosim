package de.zettsystems.h3comsim.battle.domain;

import de.zettsystems.h3comsim.battle.domain.events.BattleEvent;
import de.zettsystems.h3comsim.battle.domain.events.ListEventCollector;
import de.zettsystems.h3comsim.battle.domain.events.Side;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

/** Zyklopen gegen Mauern (ATTACKS_WALLS; heroes.thelazy.net, <em>Cyclops</em>). */
class WallShotsTest {

    @Test
    void cyclops_shoot_with_basic_and_kings_with_advanced_ballistics() {
        assertThat(WallShots.ballistics(new Stack(UnitCatalog.CYCLOPS, 1, new Hex(0, 5), Side.ATTACKER, 0)))
                .isEqualTo(SkillLevel.BASIC);
        assertThat(WallShots.ballistics(new Stack(UnitCatalog.CYCLOPS_KING, 1, new Hex(0, 5), Side.ATTACKER, 0)))
                .isEqualTo(SkillLevel.ADVANCED);
    }

    @Test
    void the_king_breaks_the_gate_open_then_shoots_creatures_again() {
        Stack kings = new Stack(UnitCatalog.CYCLOPS_KING, 10, new Hex(2, 5), Side.ATTACKER, 0);
        BattleSetup setup = fortSiege(kings);
        int shotsBefore = kings.shotsRemaining();

        ListEventCollector collector = new ListEventCollector();
        new Battle(new Random(6), new GreedyAutoSolver(), collector).simulate(setup);

        List<BattleEvent> events = collector.events();
        List<BattleEvent.CatapultShot> wallShots = events.stream()
                .filter(BattleEvent.CatapultShot.class::isInstance).map(BattleEvent.CatapultShot.class::cast)
                .toList();
        assertThat(wallShots).isNotEmpty().allSatisfy(shot -> assertThat(shot.shooterSlot()).isZero());
        assertThat(wallShots.getFirst().aimed()).isEqualTo(WallPart.GATE);
        assertThat(wallShots.size() % 2).as("zwei Schüsse je Zug").isZero();
        long kingShots = events.stream().filter(e -> e instanceof BattleEvent.Shoot s && s.actor() == Side.ATTACKER
                && s.actorSlot() == 0).count();
        assertThat(kingShots).as("danach wieder auf Kreaturen").isPositive();
        assertThat(shotsBefore - kings.shotsRemaining()).isEqualTo(wallShots.size() / 2 + kingShots);
    }

    @Test
    void only_attackers_shoot_walls_and_a_solver_may_decline() {
        Stack defenders = new Stack(UnitCatalog.CYCLOPS, 5, new Hex(14, 5), Side.DEFENDER, 0);
        Stack kings = new Stack(UnitCatalog.CYCLOPS_KING, 10, new Hex(2, 5), Side.ATTACKER, 0);
        BattleSetup setup = new BattleSetup(List.of(kings), List.of(defenders), Battlefield.STANDARD, null,
                CollectedBonus.NONE, null, CollectedBonus.NONE, new Siege(Fortification.FORT, Faction.CASTLE));
        assertThat(WallShots.possible(defenders, setup)).isFalse();
        assertThat(WallShots.possible(kings, setup)).isTrue();
        assertThat(WallShots.worthwhile(kings, setup)).as("ohne Nahkämpfer, die hinein wollen").isFalse();

        ListEventCollector collector = new ListEventCollector();
        new Battle(new Random(6), new NeverShootsWalls(), collector).simulate(fortSiege(
                new Stack(UnitCatalog.CYCLOPS_KING, 10, new Hex(2, 5), Side.ATTACKER, 0)));
        assertThat(collector.events()).noneMatch(BattleEvent.CatapultShot.class::isInstance);
    }

    @Test
    void no_wall_shots_once_a_breach_is_open() {
        Stack kings = new Stack(UnitCatalog.CYCLOPS_KING, 10, new Hex(2, 5), Side.ATTACKER, 0);
        BattleSetup setup = fortSiege(kings);
        Siege siege = setup.siege();
        assertThat(siege).isNotNull();
        assertThat(WallShots.worthwhile(kings, setup)).isTrue();
        siege.damage(WallPart.LOWER_WALL, 2);
        assertThat(WallShots.worthwhile(kings, setup)).isFalse();
    }

    /** Fort von Castle ohne Helden: kein Katapult, nur der Zyklop kann die Mauern brechen. */
    private static BattleSetup fortSiege(Stack cyclops) {
        Stack pikemen = new Stack(UnitCatalog.PIKEMAN, 40, new Hex(0, 3), Side.ATTACKER, 1);
        Stack peasants = new Stack(UnitCatalog.PEASANT, 20, new Hex(14, 5), Side.DEFENDER, 0);
        return new BattleSetup(List.of(cyclops, pikemen), List.of(peasants), Battlefield.STANDARD, null,
                CollectedBonus.NONE, null, CollectedBonus.NONE, new Siege(Fortification.FORT, Faction.CASTLE));
    }

    private static final class NeverShootsWalls implements AutoSolver {
        private final GreedyAutoSolver greedy = new GreedyAutoSolver();

        @Override
        public Action decide(Stack active, Stack opponent, Battlefield battlefield) {
            return greedy.decide(active, opponent, battlefield);
        }

        @Override
        public boolean shootsWalls(Stack active, BattleSetup setup) {
            return false;
        }
    }
}
