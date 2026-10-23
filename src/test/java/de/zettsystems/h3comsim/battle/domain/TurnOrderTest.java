package de.zettsystems.h3comsim.battle.domain;

import de.zettsystems.h3comsim.battle.domain.events.Side;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Zugreihenfolge. Manual S. 40: schnellere Einheiten zuerst, bei gleichem Speed der Angreifer.
 * Das Feinverhalten nach Kampfbeginn — es handelt die Seite zuerst, die zuletzt nicht am Zug war —
 * stammt aus heroes.thelazy.net (Combat, „Ties").
 */
class TurnOrderTest {

    /** Speed 4 / 7 — Pikeman ist langsam, Hobgoblin schnell. */
    private static final Unit SLOW = UnitCatalog.PIKEMAN;
    private static final Unit FAST = UnitCatalog.HOBGOBLIN;

    private static Stack stack(Unit unit, Side side, int slot) {
        return new Stack(unit, 10, new Hex(side == Side.ATTACKER ? 0 : 14, 1 + 2 * slot), side, slot);
    }

    /** Lässt die Phase komplett durchlaufen, jeder gewählte Stack handelt. */
    private static List<String> phase(TurnOrder order, boolean fastestFirst, Stack... stacks) {
        List<Stack> pending = new ArrayList<>(List.of(stacks));
        List<String> sequence = new ArrayList<>();
        while (!pending.isEmpty()) {
            Stack next = order.next(pending, fastestFirst);
            order.acted(next);
            sequence.add(next.side().name().charAt(0) + "" + next.slot());
        }
        return sequence;
    }

    @Test
    void faster_stacks_act_first() {
        Stack slowAttacker = stack(SLOW, Side.ATTACKER, 0);
        Stack fastDefender = stack(FAST, Side.DEFENDER, 0);

        assertThat(phase(new TurnOrder(), true, slowAttacker, fastDefender)).containsExactly("D0", "A0");
    }

    @Test
    void the_attacker_wins_a_tie_at_the_start_of_combat() {
        assertThat(phase(new TurnOrder(), true, stack(SLOW, Side.DEFENDER, 0), stack(SLOW, Side.ATTACKER, 0)))
                .containsExactly("A0", "D0");
    }

    @Test
    void after_the_start_the_side_that_did_not_act_last_wins_the_tie() {
        // Der schnelle Angreifer zieht zuerst. Im Gleichstand danach ist der Verteidiger dran,
        // weil der Angreifer zuletzt gehandelt hat — nach der alten Regel wäre es A1 gewesen.
        Stack fastAttacker = stack(FAST, Side.ATTACKER, 0);
        Stack slowAttacker = stack(SLOW, Side.ATTACKER, 1);
        Stack slowDefender = stack(SLOW, Side.DEFENDER, 0);

        assertThat(phase(new TurnOrder(), true, fastAttacker, slowAttacker, slowDefender))
                .containsExactly("A0", "D0", "A1");
    }

    @Test
    void tied_stacks_of_both_sides_alternate() {
        assertThat(phase(new TurnOrder(), true,
                stack(SLOW, Side.ATTACKER, 0), stack(SLOW, Side.ATTACKER, 1),
                stack(SLOW, Side.DEFENDER, 0), stack(SLOW, Side.DEFENDER, 1)))
                .containsExactly("A0", "D0", "A1", "D1");
    }

    @Test
    void the_alternation_carries_over_into_the_next_round() {
        TurnOrder order = new TurnOrder();
        Stack a0 = stack(SLOW, Side.ATTACKER, 0);
        Stack a1 = stack(SLOW, Side.ATTACKER, 1);
        Stack a2 = stack(SLOW, Side.ATTACKER, 2);
        Stack d0 = stack(SLOW, Side.DEFENDER, 0);

        // Runde 1 endet mit dem Angreifer, also beginnt Runde 2 mit dem Verteidiger.
        assertThat(phase(order, true, a0, a1, a2, d0)).containsExactly("A0", "D0", "A1", "A2");
        assertThat(phase(order, true, a0, a1, a2, d0)).containsExactly("D0", "A0", "A1", "A2");
    }

    @Test
    void within_one_side_the_lower_slot_goes_first() {
        assertThat(phase(new TurnOrder(), true, stack(SLOW, Side.ATTACKER, 2), stack(SLOW, Side.ATTACKER, 0)))
                .containsExactly("A0", "A2");
    }

    @Test
    void the_wait_phase_runs_slowest_first_with_the_same_tie_rule() {
        TurnOrder order = new TurnOrder();
        order.acted(stack(SLOW, Side.ATTACKER, 0));

        assertThat(phase(order, false,
                stack(FAST, Side.ATTACKER, 1), stack(SLOW, Side.ATTACKER, 0), stack(SLOW, Side.DEFENDER, 0)))
                .containsExactly("D0", "A0", "A1");
    }
}
