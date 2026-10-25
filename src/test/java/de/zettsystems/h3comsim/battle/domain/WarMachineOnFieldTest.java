package de.zettsystems.h3comsim.battle.domain;

import de.zettsystems.h3comsim.battle.domain.events.BattleEvent;
import de.zettsystems.h3comsim.battle.domain.events.ListEventCollector;
import de.zettsystems.h3comsim.battle.domain.events.Side;
import de.zettsystems.h3comsim.battle.domain.events.StackSnapshot;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Kriegsmaschinen auf dem Feld: feste Felder am eigenen Rand, angreifbar, ohne Vergeltung;
 * zerstört hören sie auf zu wirken.
 */
class WarMachineOnFieldTest {

    private static final Battlefield FIELD = Battlefield.STANDARD;
    private static final Hex ENEMY_BALLISTA = new Hex(FIELD.rightmostQ(3), 3);

    @Test
    void machines_stand_on_their_edge_rows_and_are_announced_at_the_start() {
        BattleSetup setup = new BattleSetup(
                List.of(new Stack(UnitCatalog.PEASANT, 1, new Hex(0, 5), Side.ATTACKER, 0)),
                List.of(new Stack(UnitCatalog.PEASANT, 1, new Hex(14, 5), Side.DEFENDER, 0)), FIELD)
                .equip(Side.ATTACKER, EnumSet.allOf(WarMachine.class))
                .equip(Side.DEFENDER, EnumSet.allOf(WarMachine.class));

        assertThat(setup.machineAt(new Hex(FIELD.leftmostQ(3), 3))).extracting(s -> s.unit().name()).isEqualTo("Ballista");
        assertThat(setup.machineAt(new Hex(FIELD.leftmostQ(1), 1))).extracting(s -> s.unit().name()).isEqualTo("Ammo Cart");
        assertThat(setup.machineAt(new Hex(FIELD.leftmostQ(9), 9))).extracting(s -> s.unit().name())
                .isEqualTo("First Aid Tent");
        assertThat(setup.machineAt(ENEMY_BALLISTA)).extracting(Stack::side).isEqualTo(Side.DEFENDER);
        assertThat(setup.isFree(ENEMY_BALLISTA)).isFalse();
        assertThat(setup.aliveStacks()).as("keine Armee-Stacks").hasSize(2);

        ListEventCollector collector = new ListEventCollector();
        new Battle(new Random(1), new GreedyAutoSolver(), collector).simulate(setup);
        BattleEvent.BattleStart start = (BattleEvent.BattleStart) collector.events().getFirst();
        assertThat(start.warMachineStacks()).hasSize(6)
                .extracting(StackSnapshot::slot).containsOnly(20, 21, 22);
    }

    @Test
    void a_stack_that_reaches_no_creature_destroys_a_machine_which_never_strikes_back() {
        ListEventCollector collector = new ListEventCollector();
        BattleSetup setup = behemothNextToBallista();
        new Battle(new Random(7), new GreedyAutoSolver(), collector).simulate(setup);

        List<BattleEvent.Melee> onBallista = collector.events().stream()
                .filter(BattleEvent.Melee.class::isInstance).map(BattleEvent.Melee.class::cast)
                .filter(m -> m.target() == Side.DEFENDER && m.targetSlot() == 20)
                .toList();
        assertThat(onBallista).isNotEmpty();
        assertThat(onBallista.getLast().targetAfter().count()).isZero();
        assertThat(setup.hasWorking(Side.DEFENDER, WarMachine.BALLISTA)).isFalse();
        assertThat(collector.events()).noneMatch(e -> e instanceof BattleEvent.Retaliation r
                && r.retaliator() == Side.DEFENDER && r.retaliatorSlot() == 20);
        int destroyedAt = collector.events().indexOf(onBallista.getLast());
        assertThat(collector.events().subList(destroyedAt, collector.events().size()))
                .as("eine zerstörte Ballista schießt nicht mehr")
                .noneMatch(e -> e instanceof BattleEvent.BallistaShot shot && shot.actor() == Side.DEFENDER);
    }

    @Test
    void a_solver_can_leave_machines_alone() {
        ListEventCollector collector = new ListEventCollector();
        BattleSetup setup = behemothNextToBallista();
        new Battle(new Random(7), new IgnoresMachines(), collector).simulate(setup);

        assertThat(collector.events()).noneMatch(e -> e instanceof BattleEvent.Melee m
                && m.target() == Side.DEFENDER && m.targetSlot() == 20);
    }

    @Test
    void after_the_ammo_cart_falls_shots_count_again() {
        Stack marksmen = new Stack(UnitCatalog.MARKSMAN, 10, new Hex(0, 5), Side.ATTACKER, 0);
        BattleSetup setup = new BattleSetup(List.of(marksmen),
                List.of(new Stack(UnitCatalog.PEASANT, 1, new Hex(14, 5), Side.DEFENDER, 0)), FIELD)
                .equip(Side.ATTACKER, Set.of(WarMachine.AMMO_CART));
        marksmen.useShot();
        assertThat(marksmen.shotsRemaining()).isEqualTo(24);

        Stack cart = setup.machinesOf(Side.ATTACKER).getFirst();
        cart.takeDamage(1000, Set.of());
        setup.machineHit(cart);
        marksmen.useShot();

        assertThat(setup.hasWorking(Side.ATTACKER, WarMachine.AMMO_CART)).isFalse();
        assertThat(marksmen.shotsRemaining()).isEqualTo(23);
    }

    @Test
    void only_the_machines_of_the_enemy_are_targets() {
        BattleSetup setup = behemothNextToBallista().equip(Side.ATTACKER, Set.of(WarMachine.BALLISTA));
        Stack behemoths = setup.attackerStacks().getFirst();

        assertThat(setup.enemyMachinesOf(behemoths)).extracting(Stack::side).containsOnly(Side.DEFENDER);
        assertThat(WarMachineTargeting.attack(setup.machinesOf(Side.ATTACKER).getFirst(), setup)).isNull();
        assertThat(WarMachineTargeting.attack(behemoths, setup)).isInstanceOf(Action.Melee.class);
    }

    @Test
    void solvers_never_land_on_a_machine_next_to_their_target() {
        Hex cart = new Hex(FIELD.rightmostQ(1), 1);
        for (AutoSolver solver : List.of(new GreedyAutoSolver(), new StrategicAutoSolver())) {
            Stack griffins = new Stack(UnitCatalog.ROYAL_GRIFFIN, 10, new Hex(cart.q() - 8, 1), Side.ATTACKER, 0);
            Stack marksmen = new Stack(UnitCatalog.MARKSMAN, 5, new Hex(cart.q() - 1, 1), Side.DEFENDER, 0);
            BattleSetup setup = new BattleSetup(List.of(griffins), List.of(marksmen), FIELD)
                    .equip(Side.DEFENDER, Set.of(WarMachine.AMMO_CART));
            ListEventCollector collector = new ListEventCollector();
            new Battle(new Random(2), solver, collector).simulate(setup);

            assertThat(collector.events()).as(solver.getClass().getSimpleName())
                    .noneMatch(e -> e instanceof BattleEvent.Move m && m.actor() == Side.ATTACKER
                            && m.toQ() == cart.q() && m.toR() == cart.r())
                    .anyMatch(e -> e instanceof BattleEvent.Melee m && m.actor() == Side.ATTACKER
                            && m.targetSlot() == 0);
        }
    }

    @Test
    void the_most_expensive_reachable_machine_wins_walking_or_flying() {
        Hex ballista = new Hex(FIELD.rightmostQ(3), 3);
        for (Unit unit : List.of(UnitCatalog.ANCIENT_BEHEMOTH, UnitCatalog.ROYAL_GRIFFIN)) {
            Stack attacker = new Stack(unit, 5, new Hex(ballista.q() - 4, 3), Side.ATTACKER, 0);
            BattleSetup setup = new BattleSetup(List.of(attacker),
                    List.of(new Stack(UnitCatalog.PEASANT, 1, new Hex(FIELD.leftmostQ(10), 10), Side.DEFENDER, 0)),
                    FIELD).equip(Side.DEFENDER, EnumSet.allOf(WarMachine.class));

            Action action = WarMachineTargeting.attack(attacker, setup);
            assertThat(action).as(unit.name()).isInstanceOf(Action.MoveAndMelee.class);
            Action.MoveAndMelee move = (Action.MoveAndMelee) action;
            assertThat(move.target().unit().name()).isEqualTo("Ballista");
            assertThat(move.destination().distanceTo(ballista)).isEqualTo(1);
        }
    }

    @Test
    void machines_take_the_primary_skills_of_their_hero() {
        Hero hero = HeroCatalog.SORSHA.withBuild(4, 6, Map.of());
        BattleSetup setup = new BattleSetup(List.of(new Stack(UnitCatalog.PEASANT, 1, new Hex(0, 5), Side.ATTACKER, 0)),
                List.of(new Stack(UnitCatalog.PEASANT, 1, new Hex(14, 5), Side.DEFENDER, 0)), FIELD, hero, null)
                .equip(Side.ATTACKER, Set.of(WarMachine.BALLISTA, WarMachine.FIRST_AID_TENT))
                .equip(Side.DEFENDER, Set.of(WarMachine.BALLISTA));

        Stack ownBallista = setup.machineAt(new Hex(FIELD.leftmostQ(3), 3));
        assertThat(ownBallista).isNotNull();
        assertThat(ownBallista.getDefense()).isEqualTo(10 + hero.defense());
        assertThat(setup.machineAt(new Hex(FIELD.leftmostQ(9), 9))).extracting(Stack::getDefense)
                .isEqualTo(hero.defense());
        assertThat(setup.machineAt(ENEMY_BALLISTA)).extracting(Stack::getDefense).as("ohne Helden").isEqualTo(10);
        assertThat(hero.ballistaAttack()).isEqualTo(10 + hero.attack());
    }

    @Test
    void a_machine_out_of_reach_is_left_alone() {
        Stack peasants = new Stack(UnitCatalog.PEASANT, 5, new Hex(0, 5), Side.ATTACKER, 0);
        BattleSetup setup = new BattleSetup(List.of(peasants),
                List.of(new Stack(UnitCatalog.PEASANT, 1, new Hex(14, 5), Side.DEFENDER, 0)), FIELD)
                .equip(Side.DEFENDER, EnumSet.allOf(WarMachine.class));

        assertThat(WarMachineTargeting.attack(peasants, setup)).isNull();
    }

    @Test
    void a_machine_hex_is_no_landing_spot_for_a_tank() {
        Hex cart = new Hex(FIELD.leftmostQ(1), 1);
        Stack pikemen = new Stack(UnitCatalog.PIKEMAN, 10, new Hex(FIELD.leftmostQ(3), 3), Side.ATTACKER, 0);
        BattleSetup setup = new BattleSetup(List.of(pikemen),
                List.of(new Stack(UnitCatalog.PEASANT, 1, new Hex(14, 5), Side.DEFENDER, 0)), FIELD)
                .equip(Side.ATTACKER, Set.of(WarMachine.AMMO_CART));

        assertThat(StrategicAutoSolver.isReachableLandingSpot(pikemen, cart, FIELD, setup)).isFalse();
        assertThat(StrategicAutoSolver.isReachableLandingSpot(pikemen, new Hex(cart.q() + 1, 1), FIELD, setup))
                .isTrue();
    }

    /**
     * Ancient Behemoths direkt neben der gegnerischen Ballista; der einzige gegnerische Stack steht
     * am anderen Ende des Felds, unerreichbar.
     */
    private static BattleSetup behemothNextToBallista() {
        Hex next = ENEMY_BALLISTA.neighbors().stream().filter(FIELD::contains)
                .filter(h -> h.r() == ENEMY_BALLISTA.r()).findFirst().orElseThrow();
        Stack behemoths = new Stack(UnitCatalog.ANCIENT_BEHEMOTH, 5, next, Side.ATTACKER, 0);
        Stack peasant = new Stack(UnitCatalog.PEASANT, 1, new Hex(FIELD.leftmostQ(10), 10), Side.DEFENDER, 0);
        return new BattleSetup(List.of(behemoths), List.of(peasant), FIELD)
                .equip(Side.DEFENDER, Set.of(WarMachine.BALLISTA));
    }

    private static final class IgnoresMachines implements AutoSolver {
        private final GreedyAutoSolver greedy = new GreedyAutoSolver();

        @Override
        public void planRound(BattleSetup setup) {
            greedy.planRound(setup);
        }

        @Override
        public @Nullable Stack pickTarget(Stack active, List<Stack> opponents, Battlefield battlefield) {
            return greedy.pickTarget(active, opponents, battlefield);
        }

        @Override
        public Action decide(Stack active, Stack opponent, Battlefield battlefield) {
            return greedy.decide(active, opponent, battlefield);
        }

        @Override
        public boolean attacksWarMachines(Stack active) {
            return false;
        }
    }
}
