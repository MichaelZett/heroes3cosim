package de.zettsystems.h3comsim.battle.domain;

import de.zettsystems.h3comsim.battle.domain.events.BattleEvent;
import de.zettsystems.h3comsim.battle.domain.events.ListEventCollector;
import de.zettsystems.h3comsim.battle.domain.events.Side;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.random.RandomGenerator;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Wie sich Moral in der Runden-Schleife auswirkt (Manual S. 43): positive Moral gibt eine
 * Chance auf eine zweite Aktion, negative eine Chance, die Aktion ganz zu verlieren.
 *
 * <p>Statt über Seeds zu suchen wird der Zufall hier vorgegeben: {@code ScriptedRandom} liefert
 * die Werte der Reihe nach. So steht im Test, welcher Wurf welche Wirkung hat, statt einer
 * Seed-Zahl, deren Bedeutung niemand nachrechnen kann.
 */
class MoraleInBattleTest {

    /** Beide Seiten stehen außer Reichweite und verteidigen — es passiert nur, was die Moral tut. */
    private static ListEventCollector runIdleBattle(RandomGenerator rng, int attackerMorale) {
        Stack attacker = new Stack(UnitCatalog.PIKEMAN, 10, new Hex(0, 5), Side.ATTACKER, 0);
        // Skelette als Gegenseite: Untote haben fest Moral 0 und ziehen nie einen Zufallswert,
        // der Strom gehört damit vollständig dem Attacker.
        Stack defender = new Stack(UnitCatalog.SKELETON, 10, new Hex(14, 5), Side.DEFENDER, 0);
        BattleSetup setup = new BattleSetup(List.of(attacker), List.of(defender), Battlefield.STANDARD);
        attacker.assignArmyMorale(attackerMorale);

        ListEventCollector collector = new ListEventCollector();
        new Battle(rng, (active, opponent, bf) -> new Action.Defend(), collector).simulate(setup);
        return collector;
    }

    private static long count(ListEventCollector collector, Class<? extends BattleEvent> type) {
        return collector.events().stream().filter(type::isInstance).count();
    }

    @Test
    void negative_morale_costs_the_action_entirely() {
        // 41 liegt unter der Schwelle 42 für Moral -1: der Stack friert ein.
        ListEventCollector collector = runIdleBattle(new ScriptedRandom(41), -1);

        assertThat(count(collector, BattleEvent.BadMorale.class)).isEqualTo(1);
        // Eingefroren heisst: keine Aktion, auch keine ersatzweise Verteidigung. Die erste Runde
        // besteht deshalb nur aus dem Marker und dem Zug der Gegenseite.
        assertThat(collector.events().subList(1, 3)).satisfiesExactly(
                e -> assertThat(e).isInstanceOf(BattleEvent.BadMorale.class),
                e -> assertThat(e).isInstanceOfSatisfying(BattleEvent.Defend.class,
                        d -> assertThat(d.actor()).isEqualTo(Side.DEFENDER)));
    }

    @Test
    void a_roll_on_the_threshold_does_not_freeze() {
        // 42 ist der erste Wert außerhalb der 4.2 % — die Grenze gehört nicht mehr dazu.
        ListEventCollector collector = runIdleBattle(new ScriptedRandom(42), -1);

        assertThat(count(collector, BattleEvent.BadMorale.class)).isZero();
        assertThat(collector.events().get(1)).isInstanceOf(BattleEvent.Defend.class);
    }

    @Test
    void the_freeze_chance_follows_the_manual_table() {
        // -1 -> 4.2 %, -2 -> 8.3 %, -3 -> 12.5 %. Geprüft am jeweils letzten Wert, der noch
        // trifft, und am ersten, der es nicht mehr tut.
        assertThat(count(runIdleBattle(new ScriptedRandom(82), -2), BattleEvent.BadMorale.class))
                .isEqualTo(1);
        assertThat(count(runIdleBattle(new ScriptedRandom(83), -2), BattleEvent.BadMorale.class))
                .isZero();
        assertThat(count(runIdleBattle(new ScriptedRandom(124), -3), BattleEvent.BadMorale.class))
                .isEqualTo(1);
        assertThat(count(runIdleBattle(new ScriptedRandom(125), -3), BattleEvent.BadMorale.class))
                .isZero();
    }

    @Test
    void positive_morale_grants_a_second_action() {
        ListEventCollector collector = runIdleBattle(new ScriptedRandom(41), 1);

        assertThat(count(collector, BattleEvent.GoodMorale.class)).isEqualTo(1);
        // Marker, dann die zweite Aktion: Defend, Marker, Defend.
        assertThat(collector.events().subList(1, 4)).satisfiesExactly(
                e -> assertThat(e).isInstanceOf(BattleEvent.Defend.class),
                e -> assertThat(e).isInstanceOf(BattleEvent.GoodMorale.class),
                e -> assertThat(e).isInstanceOf(BattleEvent.Defend.class));
    }

    @Test
    void morale_zero_never_rolls_at_all() {
        // Ein Zufallsgenerator, der bei jedem Zugriff wirft: was nicht gezogen wird, kann den
        // Strom auch nicht verschieben — die Determinismus-Garantie über alle Seeds hängt daran.
        ListEventCollector collector = runIdleBattle(new ForbiddenRandom(), 0);

        assertThat(count(collector, BattleEvent.GoodMorale.class)).isZero();
        assertThat(count(collector, BattleEvent.BadMorale.class)).isZero();
    }

    // Stack ist eine mutable Entity ohne equals — der ==-Vergleich meint bewusst Identität.
    @SuppressWarnings("ReferenceEquality")
    @Test
    void a_frozen_stack_gets_no_second_chance_by_waiting() {
        // Wer wartet, hat den Freeze-Wurf in Phase 1 schon hinter sich. Ein zweiter Wurf in der
        // Late-Phase würde das Warten mit doppelter Einfrier-Gefahr bestrafen.
        Stack waiter = new Stack(UnitCatalog.PIKEMAN, 10, new Hex(0, 5), Side.ATTACKER, 0);
        Stack enemy = new Stack(UnitCatalog.SKELETON, 10, new Hex(14, 5), Side.DEFENDER, 0);
        BattleSetup setup = new BattleSetup(List.of(waiter), List.of(enemy), Battlefield.STANDARD);
        waiter.assignArmyMorale(-3);

        // Der Wurf aus Phase 1 trifft nicht (125). Der zweite Wert ist die Falle: zöge die
        // Late-Phase erneut, läge dort eine 0 und der Stack fröre ein.
        ScriptedRandom rng = new ScriptedRandom(125, 0);
        ListEventCollector collector = new ListEventCollector();
        AutoSolver waitOnce = (active, opponent, bf) ->
                active == waiter && !active.hasWaitedThisTurn() ? new Action.Wait() : new Action.Defend();
        new Battle(rng, waitOnce, collector).simulate(setup);

        List<BattleEvent> firstRound = collector.events().subList(1, 4);
        assertThat(firstRound).satisfiesExactly(
                e -> assertThat(e).isInstanceOf(BattleEvent.Wait.class),
                e -> assertThat(e).isInstanceOf(BattleEvent.Defend.class),
                e -> assertThat(e).isInstanceOfSatisfying(BattleEvent.Defend.class,
                        d -> assertThat(d.actor()).isEqualTo(Side.ATTACKER)));
    }

    /**
     * Liefert die vorgegebenen Werte der Reihe nach. Danach 999 — ein Wert oberhalb jeder
     * Moral-Schwelle, der nie auslöst. Die Leerlauf-Schlacht läuft über zwanzig Runden;
     * ohne diesen Auslauf würde sich der letzte Wurf in jeder weiteren Runde wiederholen.
     */
    private static final class ScriptedRandom implements RandomGenerator {
        private static final int NEVER_TRIGGERS = 999;

        private final int[] values;
        private int index;

        private ScriptedRandom(int... values) {
            this.values = values.clone();
        }

        @Override
        public int nextInt(int bound) {
            int value = index < values.length ? values[index] : NEVER_TRIGGERS;
            index++;
            return value % bound;
        }

        @Override
        public long nextLong() {
            throw new UnsupportedOperationException("nur nextInt(int) wird gebraucht");
        }
    }

    /** Wirft bei jedem Zugriff — belegt, dass gar nicht gewürfelt wird. */
    private static final class ForbiddenRandom implements RandomGenerator {
        @Override
        public int nextInt(int bound) {
            throw new AssertionError("Es hätte kein Zufallswert gezogen werden dürfen");
        }

        @Override
        public long nextLong() {
            throw new AssertionError("Es hätte kein Zufallswert gezogen werden dürfen");
        }
    }
}
