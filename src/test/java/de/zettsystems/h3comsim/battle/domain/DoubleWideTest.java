package de.zettsystems.h3comsim.battle.domain;

import de.zettsystems.h3comsim.battle.domain.events.BattleEvent;
import de.zettsystems.h3comsim.battle.domain.events.ListEventCollector;
import de.zettsystems.h3comsim.battle.domain.events.Side;
import de.zettsystems.h3comsim.battle.domain.events.StackSnapshot;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

/** Zweifeldrige Kreaturen: das zweite Feld liegt hinter dem Kopf, zum eigenen Rand hin (VCMI). */
class DoubleWideTest {

    private static final Battlefield FIELD = Battlefield.STANDARD;

    @Test
    void the_list_follows_vcmi() {
        assertThat(UnitCatalog.BLACK_DRAGON.doubleWide()).isTrue();
        assertThat(UnitCatalog.ARCH_ANGEL.doubleWide()).isTrue();
        assertThat(UnitCatalog.ANGEL.doubleWide()).isFalse();
        assertThat(UnitCatalog.CAVALIER.doubleWide()).isTrue();
        assertThat(UnitCatalog.PIKEMAN.doubleWide()).isFalse();
        assertThat(WarMachine.BALLISTA.unit().doubleWide()).isTrue();
        assertThat(WarMachine.AMMO_CART.unit().doubleWide()).isFalse();
    }

    @Test
    void the_tail_points_to_the_own_edge_and_counts_for_distance_and_occupancy() {
        Stack attacker = new Stack(UnitCatalog.BLACK_DRAGON, 1, new Hex(5, 5), Side.ATTACKER, 0);
        Stack defender = new Stack(UnitCatalog.BLACK_DRAGON, 1, new Hex(5, 3), Side.DEFENDER, 0);
        assertThat(attacker.hexes()).containsExactly(new Hex(5, 5), new Hex(4, 5));
        assertThat(defender.hexes()).containsExactly(new Hex(5, 3), new Hex(6, 3));
        assertThat(attacker.occupies(new Hex(4, 5))).isTrue();
        assertThat(attacker.distanceTo(new Hex(2, 5))).isEqualTo(2);
        Stack pikemen = new Stack(UnitCatalog.PIKEMAN, 1, new Hex(3, 5), Side.DEFENDER, 0);
        assertThat(attacker.distanceTo(pikemen)).as("über das Heck benachbart").isEqualTo(1);
        assertThat(attacker.nearestHexTo(new Hex(0, 5))).isEqualTo(new Hex(4, 5));
    }

    @Test
    void a_two_hex_creature_on_the_edge_steps_in_so_its_tail_stays_on_the_field() {
        Stack dragon = new Stack(UnitCatalog.BLACK_DRAGON, 1, new Hex(0, 5), Side.ATTACKER, 0);
        Stack pikemen = new Stack(UnitCatalog.PIKEMAN, 1, new Hex(0, 3), Side.ATTACKER, 1);
        Stack enemy = new Stack(UnitCatalog.BLACK_DRAGON, 1, new Hex(14, 5), Side.DEFENDER, 0);
        new BattleSetup(List.of(dragon, pikemen), List.of(enemy), FIELD);

        assertThat(dragon.position()).isEqualTo(new Hex(1, 5));
        assertThat(enemy.position()).isEqualTo(new Hex(13, 5));
        assertThat(pikemen.position()).as("einfeldig bleibt am Rand").isEqualTo(new Hex(0, 3));
        assertThat(FIELD.contains(Stack.tailOf(dragon.position(), Side.ATTACKER))).isTrue();
    }

    @Test
    void nobody_lands_on_a_tail_and_a_tail_needs_room_too() {
        Stack dragon = new Stack(UnitCatalog.BLACK_DRAGON, 1, new Hex(5, 5), Side.ATTACKER, 0);
        Stack pikemen = new Stack(UnitCatalog.PIKEMAN, 1, new Hex(7, 3), Side.ATTACKER, 1);
        Stack enemy = new Stack(UnitCatalog.PEASANT, 1, new Hex(14, 5), Side.DEFENDER, 0);
        BattleSetup setup = new BattleSetup(List.of(dragon, pikemen), List.of(enemy), FIELD);

        assertThat(setup.isFree(new Hex(4, 5))).isFalse();
        assertThat(setup.fits(pikemen, new Hex(4, 5))).isFalse();
        assertThat(setup.fits(dragon, new Hex(8, 3))).as("Heck auf den Pikenieren").isFalse();
        assertThat(setup.fits(dragon, new Hex(6, 5))).as("das eigene Feld zählt als frei").isTrue();
    }

    @Test
    void a_stack_next_to_the_tail_fights_and_is_struck_back() {
        Stack dragon = new Stack(UnitCatalog.BLACK_DRAGON, 1, new Hex(10, 5), Side.DEFENDER, 0);
        // Das Heck des Verteidigers liegt auf (11,5); die Pikeniere stehen daneben.
        Stack pikemen = new Stack(UnitCatalog.PIKEMAN, 30, new Hex(12, 5), Side.ATTACKER, 0);
        BattleSetup setup = new BattleSetup(List.of(pikemen), List.of(dragon), FIELD);

        ListEventCollector collector = new ListEventCollector();
        new Battle(new Random(2), new GreedyAutoSolver(), collector).simulate(setup);

        assertThat(collector.events()).anyMatch(e -> e instanceof BattleEvent.Melee m
                && m.actor() == Side.ATTACKER && m.hexesMoved() == 0);
        assertThat(collector.events()).anyMatch(e -> e instanceof BattleEvent.Retaliation r
                && r.retaliator() == Side.DEFENDER);
        BattleEvent.BattleStart start = (BattleEvent.BattleStart) collector.events().getFirst();
        assertThat(start.defender()).extracting(StackSnapshot::doubleWide).isEqualTo(true);
    }

    @Test
    void splash_and_area_spells_hit_a_two_hex_stack_once() {
        Stack magogs = new Stack(UnitCatalog.MAGOG, 30, new Hex(2, 5), Side.ATTACKER, 0);
        // Eigener Drache, Kopf (9,4), Heck (8,4): beide Felder grenzen an das Ziel.
        Stack dragon = new Stack(UnitCatalog.BONE_DRAGON, 3, new Hex(9, 4), Side.ATTACKER, 1);
        Stack target = new Stack(UnitCatalog.PEASANT, 500, new Hex(8, 5), Side.DEFENDER, 0);
        BattleSetup setup = new BattleSetup(List.of(magogs, dragon), List.of(target), FIELD);

        assertThat(HeroSpellcasting.inArea(Spell.FIREBALL, new Hex(7, 4), setup))
                .as("nur das Heck im Feuerball").containsExactly(dragon);

        ListEventCollector collector = new ListEventCollector();
        Battle battle = new Battle(new Random(1), new GreedyAutoSolver(), collector);
        battle.simulate(setup);
        List<BattleEvent> events = collector.events();
        int firstShot = indexOfFirstShotOnTarget(events);
        assertThat(firstShot).isNotNegative();
        long hitsOnDragon = events.subList(firstShot, Math.min(events.size(), firstShot + 4)).stream()
                .filter(e -> e instanceof BattleEvent.Shoot shot && shot.target() == Side.ATTACKER
                        && shot.targetSlot() == 1).count();
        assertThat(hitsOnDragon).as("einmal je Schuss, nicht je Feld").isOne();
    }

    @Test
    void a_summoned_water_elemental_needs_room_for_its_tail() {
        Stack pikemen = new Stack(UnitCatalog.PIKEMAN, 1, new Hex(0, 5), Side.ATTACKER, 0);
        BattleSetup setup = new BattleSetup(List.of(pikemen),
                List.of(new Stack(UnitCatalog.PEASANT, 1, new Hex(14, 5), Side.DEFENDER, 0)), FIELD);

        Hex single = setup.freeHexFor(Side.ATTACKER, false);
        Hex wide = setup.freeHexFor(Side.ATTACKER, true);
        assertThat(single).isNotNull();
        assertThat(wide).isNotNull();
        assertThat(FIELD.contains(Stack.tailOf(wide, Side.ATTACKER))).isTrue();
        assertThat(setup.isFree(Stack.tailOf(wide, Side.ATTACKER))).isTrue();
    }

    private static int indexOfFirstShotOnTarget(List<BattleEvent> events) {
        for (int i = 0; i < events.size(); i++) {
            if (events.get(i) instanceof BattleEvent.Shoot shot && shot.actor() == Side.ATTACKER
                    && shot.targetSlot() == 0) {
                return i;
            }
        }
        return -1;
    }
}
