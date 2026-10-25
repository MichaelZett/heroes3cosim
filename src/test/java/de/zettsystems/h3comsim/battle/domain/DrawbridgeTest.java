package de.zettsystems.h3comsim.battle.domain;

import de.zettsystems.h3comsim.battle.domain.events.BattleEvent;
import de.zettsystems.h3comsim.battle.domain.events.ListEventCollector;
import de.zettsystems.h3comsim.battle.domain.events.Side;
import de.zettsystems.h3comsim.battle.domain.events.Winner;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;
import java.util.Set;

import static de.zettsystems.h3comsim.battle.domain.SiegeLayout.GATE_BRIDGE;
import static de.zettsystems.h3comsim.battle.domain.SiegeLayout.GATE_INNER;
import static de.zettsystems.h3comsim.battle.domain.SiegeLayout.GATE_OUTER;
import static org.assertj.core.api.Assertions.assertThat;

/** Die Zugbrücke als Zustand (heroes.thelazy.net, <em>Wall</em>; VCMI, <em>updateGateState</em>). */
class DrawbridgeTest {

    @Test
    void a_defender_on_the_gate_opens_it_and_it_closes_behind_him() {
        Siege siege = new Siege(Fortification.FORT, Faction.CASTLE);
        assertThat(siege.gate()).isEqualTo(Siege.Gate.CLOSED);
        assertThat(siege.blockedFor(Side.ATTACKER)).contains(GATE_OUTER, GATE_INNER);
        assertThat(siege.blockedFor(Side.DEFENDER)).doesNotContain(GATE_OUTER, GATE_INNER);

        assertThat(siege.updateGate(Set.of(GATE_INNER), Set.of(GATE_INNER))).isTrue();
        assertThat(siege.gate()).isEqualTo(Siege.Gate.OPEN);
        assertThat(siege.blockedFor(Side.ATTACKER)).doesNotContain(GATE_OUTER, GATE_INNER);

        assertThat(siege.updateGate(Set.of(), Set.of())).isTrue();
        assertThat(siege.gate()).isEqualTo(Siege.Gate.CLOSED);
        assertThat(siege.updateGate(Set.of(), Set.of())).as("unverändert").isFalse();
    }

    @Test
    void a_stack_in_front_of_the_closed_gate_blocks_it_for_both_sides() {
        Siege siege = new Siege(Fortification.FORT, Faction.CASTLE);
        siege.updateGate(Set.of(GATE_BRIDGE), Set.of());

        assertThat(siege.gate()).isEqualTo(Siege.Gate.BLOCKED);
        assertThat(siege.blockedFor(Side.DEFENDER)).contains(GATE_OUTER, GATE_INNER);
        siege.updateGate(Set.of(GATE_BRIDGE, GATE_OUTER), Set.of());
        assertThat(siege.gate()).as("Stack vor dem offenen Tor hält es nicht offen")
                .isEqualTo(Siege.Gate.OPEN);
        siege.updateGate(Set.of(GATE_BRIDGE), Set.of());
        assertThat(siege.gate()).as("die Brücke geht erst hoch").isEqualTo(Siege.Gate.CLOSED);
        siege.updateGate(Set.of(GATE_BRIDGE), Set.of());
        assertThat(siege.gate()).isEqualTo(Siege.Gate.BLOCKED);
    }

    @Test
    void in_fortress_the_bridge_reaches_over_the_wide_moat() {
        Siege fortress = new Siege(Fortification.CITADEL, Faction.FORTRESS);
        assertThat(fortress.isMoat(GATE_BRIDGE)).isTrue();

        fortress.updateGate(Set.of(GATE_BRIDGE), Set.of(GATE_BRIDGE));
        assertThat(fortress.gate()).as("Verteidiger auf der Brücke").isEqualTo(Siege.Gate.OPEN);
        assertThat(fortress.isMoat(GATE_BRIDGE)).as("über die Brücke").isFalse();
        fortress.updateGate(Set.of(GATE_BRIDGE), Set.of());
        assertThat(fortress.gate()).as("gefallen auf der Brücke").isEqualTo(Siege.Gate.OPEN);

        Siege attacked = new Siege(Fortification.CITADEL, Faction.FORTRESS);
        attacked.updateGate(Set.of(GATE_BRIDGE), Set.of());
        assertThat(attacked.gate()).as("Angreifer im Graben vor dem Tor").isEqualTo(Siege.Gate.BLOCKED);
    }

    @Test
    void a_destroyed_gate_stays_open() {
        Siege siege = new Siege(Fortification.FORT, Faction.CASTLE);
        siege.damage(WallPart.GATE, 2);

        assertThat(siege.gate()).isEqualTo(Siege.Gate.DESTROYED);
        assertThat(siege.updateGate(Set.of(GATE_BRIDGE), Set.of())).isFalse();
        assertThat(siege.blockedFor(Side.DEFENDER)).doesNotContain(GATE_OUTER, GATE_INNER);
        assertThat(siege.blockedFor(Side.ATTACKER)).doesNotContain(GATE_OUTER, GATE_INNER);
    }

    @Test
    void a_defender_slain_on_the_gate_traps_it_open() {
        // Ohne Helden kein Katapult: hinein kommen die Angreifer nur durch das eingeklemmte Tor.
        Stack peasant = new Stack(UnitCatalog.PEASANT, 1, GATE_OUTER, Side.DEFENDER, 0);
        Stack pikemen = new Stack(UnitCatalog.PIKEMAN, 5, new Hex(14, 5), Side.DEFENDER, 1);
        Stack behemoths = new Stack(UnitCatalog.ANCIENT_BEHEMOTH, 10, new Hex(GATE_BRIDGE.q() - 3, 5),
                Side.ATTACKER, 0);
        BattleSetup setup = new BattleSetup(List.of(behemoths), List.of(peasant, pikemen), Battlefield.STANDARD,
                null, CollectedBonus.NONE, null, CollectedBonus.NONE, new Siege(Fortification.FORT, Faction.CASTLE));

        ListEventCollector collector = new ListEventCollector();
        BattleResult result = new Battle(new Random(4), new GreedyAutoSolver(), collector).simulate(setup);

        List<Siege.Gate> states = collector.events().stream()
                .filter(BattleEvent.Drawbridge.class::isInstance).map(BattleEvent.Drawbridge.class::cast)
                .map(BattleEvent.Drawbridge::state).toList();
        assertThat(states).containsExactly(Siege.Gate.OPEN);
        assertThat(peasant.isAlive()).isFalse();
        assertThat(result.winner()).isEqualTo(Winner.ATTACKER);
        assertThat(collector.events()).anyMatch(e -> e instanceof BattleEvent.Melee m
                && m.actor() == Side.ATTACKER && m.targetSlot() == 1);
    }
}
