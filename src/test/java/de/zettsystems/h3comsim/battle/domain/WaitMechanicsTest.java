package de.zettsystems.h3comsim.battle.domain;

import de.zettsystems.h3comsim.battle.domain.events.BattleEvent;
import de.zettsystems.h3comsim.battle.domain.events.ListEventCollector;
import de.zettsystems.h3comsim.battle.domain.events.Side;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.random.RandomGenerator;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Manual S. 43: „If you want a troop to delay its action, click the Wait button. Play will pass on
 * to the next creature and return to the waiting creature at the end of the first phase, after all
 * other creatures have had a chance to move."
 *
 * <p>Belegt ist damit die Phasen-Trennung. Die Reihenfolge <em>innerhalb</em> der Late-Phase —
 * langsamste Wartende zuerst — steht nicht im Manual, sie ist H3-Spielverhalten und hier als
 * Charakterisierung festgenagelt.
 *
 * <p>Alle Tests bauen Armeen, die einander nicht erreichen können, und Solver, die nur Wait und
 * Defend liefern. Damit passiert kein Schaden, die Schlacht läuft ins {@code NO_PROGRESS_LIMIT}
 * und der Event-Strom besteht ausschließlich aus den beobachteten Runden-Ereignissen.
 */
class WaitMechanicsTest {

    /** Speed 7 / 5 / 4 — drei klar getrennte Stufen für die Reihenfolge-Asserts. */
    private static final Unit FAST = UnitCatalog.HOBGOBLIN;
    private static final Unit MEDIUM = UnitCatalog.GOBLIN;
    private static final Unit SLOW = UnitCatalog.PIKEMAN;

    @Test
    void waiting_stack_acts_after_every_other_stack() {
        Stack fast = new Stack(FAST, 10, new Hex(0, 4), Side.ATTACKER, 0);
        Stack slow = new Stack(SLOW, 10, new Hex(0, 6), Side.ATTACKER, 1);
        Stack enemy = new Stack(MEDIUM, 10, new Hex(14, 5), Side.DEFENDER, 0);
        BattleSetup setup = new BattleSetup(List.of(fast, slow), List.of(enemy),
                Battlefield.STANDARD);

        // Nur der schnelle Stack wartet — regulär zöge er als Erster.
        ListEventCollector collector = waitFor(setup, fast);

        // Ohne Wait: fast (7), enemy (5), slow (4). Mit Wait rutscht fast ans Ende.
        assertThat(actorsOfFirstRound(collector, 4)).containsExactly(
                slotOf(fast), slotOf(enemy), slotOf(slow), slotOf(fast));
    }

    @Test
    void late_phase_runs_the_slowest_waiting_stack_first() {
        Stack fast = new Stack(FAST, 10, new Hex(0, 3), Side.ATTACKER, 0);
        Stack medium = new Stack(MEDIUM, 10, new Hex(0, 5), Side.ATTACKER, 1);
        Stack slow = new Stack(SLOW, 10, new Hex(0, 7), Side.ATTACKER, 2);
        Stack enemy = new Stack(SLOW, 10, new Hex(14, 5), Side.DEFENDER, 0);
        BattleSetup setup = new BattleSetup(List.of(fast, medium, slow), List.of(enemy),
                Battlefield.STANDARD);

        ListEventCollector collector = waitFor(setup, fast, medium, slow);

        // Phase 1 nach Speed absteigend (7, 5, 4); bei Gleichstand zwischen slow und enemy
        // (beide Speed 4) zieht der Attacker zuerst. Die drei Attacker emittieren dort Wait.
        // Phase 2 dreht den Speed um: slow (4), medium (5), fast (7).
        assertThat(actorsOfFirstRound(collector, 7)).containsExactly(
                slotOf(fast), slotOf(medium), slotOf(slow), slotOf(enemy),
                slotOf(slow), slotOf(medium), slotOf(fast));
    }

    @Test
    void a_stack_cannot_wait_twice_in_the_same_round() {
        Stack waiter = new Stack(MEDIUM, 10, new Hex(0, 5), Side.ATTACKER, 0);
        Stack enemy = new Stack(SLOW, 10, new Hex(14, 5), Side.DEFENDER, 0);
        BattleSetup setup = new BattleSetup(List.of(waiter), List.of(enemy),
                Battlefield.STANDARD);

        ListEventCollector collector = new ListEventCollector();
        // Solver besteht stur auf Wait — die Engine muss die zweite Verzögerung abweisen.
        AutoSolver alwaysWait = (active, opponent, bf) ->
                active == waiter ? new Action.Wait() : new Action.Defend();
        new Battle(new Random(1L), alwaysWait, collector).simulate(setup);

        List<BattleEvent> firstRound = collector.events().subList(1, 4);
        assertThat(firstRound).first().isInstanceOf(BattleEvent.Wait.class);
        assertThat(firstRound.get(1)).isInstanceOf(BattleEvent.Defend.class);
        // Late-Phase: kein zweites Wait, stattdessen der Defend-Fallback (+20 % Defense).
        assertThat(firstRound.get(2)).isInstanceOf(BattleEvent.Defend.class);
        assertThat(((BattleEvent.Defend) firstRound.get(2)).actorSlot()).isEqualTo(waiter.slot());
    }

    @Test
    void the_wait_option_is_available_again_in_the_next_round() {
        Stack waiter = new Stack(MEDIUM, 10, new Hex(0, 5), Side.ATTACKER, 0);
        Stack enemy = new Stack(SLOW, 10, new Hex(14, 5), Side.DEFENDER, 0);
        BattleSetup setup = new BattleSetup(List.of(waiter), List.of(enemy),
                Battlefield.STANDARD);

        ListEventCollector collector = waitFor(setup, waiter);

        // endTurn setzt das Flag zurück: über die abgebrochenen Leerlauf-Runden hinweg
        // wartet der Stack jede Runde erneut, nicht nur in der ersten.
        long waits = collector.events().stream()
                .filter(BattleEvent.Wait.class::isInstance)
                .count();
        assertThat(waits).isGreaterThan(1L);
    }

    @Test
    void waiting_does_not_consume_a_morale_roll() {
        // Wait ist keine Aktion, sondern deren Verschiebung — der Moral-Wurf gehört in die
        // Late-Phase. Zöge die Engine schon beim Warten, verschöbe sich der komplette
        // Zufallsstrom und damit die Determinismus-Garantie über alle Seeds.
        Stack minotaur = new Stack(UnitCatalog.MINOTAUR, 5, new Hex(0, 5), Side.ATTACKER, 0);
        Stack enemy = new Stack(SLOW, 10, new Hex(14, 5), Side.DEFENDER, 0);
        BattleSetup setup = new BattleSetup(List.of(minotaur), List.of(enemy),
                Battlefield.STANDARD);
        assertThat(minotaur.hasSpeciality(UnitSpeciality.GOOD_MORALE)).isTrue();

        CountingRandom rng = new CountingRandom(1L);
        AutoSolver waitThenDefend = (active, opponent, bf) ->
                active == minotaur && !active.hasWaitedThisTurn()
                        ? new Action.Wait() : new Action.Defend();
        new Battle(rng, waitThenDefend, new ListEventCollector()).simulate(setup);

        // Pro Runde genau ein Moral-Wurf: der aus der Late-Phase, keiner aus dem Wait selbst.
        // Der Defender hat Moral 0 und würfelt gar nicht.
        long rounds = 20L;
        assertThat(rng.intCalls).isEqualTo(rounds);
    }

    // Stack ist eine mutable Entity ohne equals — der ==-Vergleich meint bewusst Identität.
    @SuppressWarnings("ReferenceEquality")
    private static ListEventCollector waitFor(BattleSetup setup, Stack... waiters) {
        List<Stack> waiting = List.of(waiters);
        ListEventCollector collector = new ListEventCollector();
        AutoSolver solver = (active, opponent, bf) -> {
            for (Stack w : waiting) {
                if (w == active && !active.hasWaitedThisTurn()) {
                    return new Action.Wait();
                }
            }
            return new Action.Defend();
        };
        new Battle(new Random(1L), solver, collector).simulate(setup);
        return collector;
    }

    /**
     * Die Slot-Kennung der ersten {@code count} Runden-Events, in Emit-Reihenfolge. Index 0 des
     * Event-Stroms ist {@code BattleStart} und wird übersprungen.
     */
    private static List<String> actorsOfFirstRound(ListEventCollector collector, int count) {
        List<String> actors = new ArrayList<>(count);
        for (BattleEvent event : collector.events().subList(1, 1 + count)) {
            if (event instanceof BattleEvent.Wait wait) {
                actors.add(wait.actor() + "#" + wait.actorSlot());
            } else if (event instanceof BattleEvent.Defend defend) {
                actors.add(defend.actor() + "#" + defend.actorSlot());
            }
        }
        return actors;
    }

    private static String slotOf(Stack stack) {
        return stack.side() + "#" + stack.slot();
    }

    /** Zählt nur {@code nextInt}: genau die Aufrufe, die {@code Stack.hasGoodMorale} macht. */
    private static final class CountingRandom implements RandomGenerator {
        private final Random delegate;
        private int intCalls;

        private CountingRandom(long seed) {
            this.delegate = new Random(seed);
        }

        @Override
        public long nextLong() {
            return delegate.nextLong();
        }

        @Override
        public int nextInt(int bound) {
            intCalls++;
            return delegate.nextInt(bound);
        }
    }
}
