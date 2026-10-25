package de.zettsystems.h3comsim.battle.domain;

import de.zettsystems.h3comsim.battle.domain.events.BattleEvent;
import de.zettsystems.h3comsim.battle.domain.events.ListEventCollector;
import de.zettsystems.h3comsim.battle.domain.events.Side;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

/** Graben und Landminen nach heroes.thelazy.net, <em>Moat</em> (SoD). */
class MoatTest {

    /** Hinter dem Grabenfeld (9,4) liegt das Mauersegment über dem Tor. */
    private static final Hex BEFORE_MOAT = new Hex(7, 4);
    private static final Hex MOAT = new Hex(9, 4);
    private static final Hex BEHIND_WALL = new Hex(12, 4);

    @Test
    void the_moat_stops_a_walker_on_its_first_moat_hex_and_hurts_on_entry_and_next_turn() {
        assertThat(SiegeLayout.MOAT).contains(MOAT);
        ListEventCollector collector = new ListEventCollector();
        // Ein Zug: er bleibt im Graben stehen und verteidigt in der nächsten Runde dort.
        Stack walkers = run(Faction.CASTLE, UnitCatalog.ANCIENT_BEHEMOTH, collector, 1);

        // Er bleibt danach stehen und nimmt jede Runde wieder Schaden.
        List<BattleEvent.MoatDamage> hits = moatHits(collector);
        assertThat(walkers.position()).isEqualTo(MOAT);
        assertThat(hits).hasSizeGreaterThan(2);
        assertThat(hits.subList(0, 2)).allSatisfy(hit -> {
            assertThat(hit.damage()).isEqualTo(70);
            assertThat(hit.mine()).isFalse();
            assertThat(hit.targetAfter().q()).isEqualTo(MOAT.q());
        });
    }

    @Test
    void dungeon_moat_hurts_ninety_and_fortress_is_two_hexes_deep() {
        assertThat(new Siege(Fortification.CITADEL, Faction.DUNGEON).moatDamage()).isEqualTo(90);
        assertThat(new Siege(Fortification.CITADEL, Faction.RAMPART).moatDamage()).isEqualTo(70);
        Siege fortress = new Siege(Fortification.CITADEL, Faction.FORTRESS);
        assertThat(fortress.isMoat(new Hex(8, 4))).isTrue();
        assertThat(fortress.isMoat(SiegeLayout.GATE_BRIDGE)).isTrue();
        assertThat(new Siege(Fortification.FORT, Faction.CASTLE).isMoat(MOAT)).as("Fort hat keinen Graben").isFalse();
    }

    @Test
    void tower_lays_mines_that_hit_attackers_once_and_spare_fire_immune_creatures() {
        ListEventCollector collector = new ListEventCollector();
        Stack walkers = run(Faction.TOWER, UnitCatalog.ANCIENT_BEHEMOTH, collector, 2);

        List<BattleEvent.MoatDamage> hits = moatHits(collector);
        assertThat(hits).hasSize(1);
        assertThat(hits.getFirst().mine()).isTrue();
        assertThat(hits.getFirst().damage()).isEqualTo(150);
        assertThat(walkers.position()).as("Minen halten nicht auf").isEqualTo(BEHIND_WALL);

        ListEventCollector fire = new ListEventCollector();
        run(Faction.TOWER, UnitCatalog.FIRE_ELEMENTAL, fire, 1);
        assertThat(moatHits(fire)).isEmpty();
    }

    private static List<BattleEvent.MoatDamage> moatHits(ListEventCollector collector) {
        return collector.events().stream()
                .filter(BattleEvent.MoatDamage.class::isInstance).map(BattleEvent.MoatDamage.class::cast).toList();
    }

    /** Ein Stack läuft durch die Bresche über dem Tor hinter die Mauer; danach verteidigt er nur. */
    private static Stack run(Faction town, Unit unit, ListEventCollector collector, int rounds) {
        Stack walkers = new Stack(unit, 20, BEFORE_MOAT, Side.ATTACKER, 0);
        Stack keeper = new Stack(UnitCatalog.PEASANT, 1, new Hex(14, 9), Side.DEFENDER, 0);
        Siege siege = new Siege(Fortification.CITADEL, town);
        siege.damage(WallPart.MID_UPPER_WALL, 2);
        BattleSetup setup = new BattleSetup(List.of(walkers), List.of(keeper), Battlefield.STANDARD, null,
                CollectedBonus.NONE, null, CollectedBonus.NONE, siege);
        new Battle(new Random(1), new Scripted(rounds), collector).simulate(setup);
        return walkers;
    }

    /** Der Angreifer will hinter die Mauer, sonst verteidigt jeder; nach {@code rounds} Zügen nur noch Defend. */
    private static final class Scripted implements AutoSolver {
        private int movesLeft;

        Scripted(int rounds) {
            this.movesLeft = rounds;
        }

        @Override
        public Action decide(Stack active, Stack opponent, Battlefield battlefield) {
            if (active.side() == Side.ATTACKER && movesLeft-- > 0 && !active.position().equals(BEHIND_WALL)) {
                return new Action.Move(BEHIND_WALL);
            }
            return new Action.Defend();
        }

        @Override
        public boolean attacksWarMachines(Stack active) {
            return false;
        }
    }
}
