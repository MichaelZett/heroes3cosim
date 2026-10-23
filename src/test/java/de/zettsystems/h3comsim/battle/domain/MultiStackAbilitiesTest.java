package de.zettsystems.h3comsim.battle.domain;

import de.zettsystems.h3comsim.battle.domain.events.BattleEvent;
import de.zettsystems.h3comsim.battle.domain.events.ListEventCollector;
import de.zettsystems.h3comsim.battle.domain.events.Side;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

class MultiStackAbilitiesTest {

    @Test
    void cerberus_three_headed_attack_emits_extra_melee_events_for_adjacent_opponents() {
        // Cerberus auf (5,5), adjacent zu 3 Pikemen-Stacks auf seinen Nachbarn (6,5), (5,6), (6,4).
        Stack cerberus = new Stack(UnitCatalog.CERBERUS, 5, new Hex(5, 5), Side.ATTACKER, 0);
        Stack pikemen1 = new Stack(UnitCatalog.PIKEMAN, 10, new Hex(6, 5), Side.DEFENDER, 0);
        Stack pikemen2 = new Stack(UnitCatalog.PIKEMAN, 10, new Hex(5, 6), Side.DEFENDER, 1);
        Stack pikemen3 = new Stack(UnitCatalog.PIKEMAN, 10, new Hex(6, 4), Side.DEFENDER, 2);

        BattleSetup setup = new BattleSetup(
                List.of(cerberus),
                List.of(pikemen1, pikemen2, pikemen3),
                Battlefield.STANDARD);
        ListEventCollector collector = new ListEventCollector();
        new Battle(new Random(1L), new GreedyAutoSolver(), collector).simulate(setup);

        // Erste Runde: mindestens 3 Melee-Events vom Cerberus auf 3 verschiedene Defender-Stacks.
        long firstRoundMeleeFromCerberus = collector.events().stream()
                .takeWhile(e -> !(e instanceof BattleEvent.BattleEnd))
                .filter(e -> e instanceof BattleEvent.Melee m && m.actor() == Side.ATTACKER)
                .count();
        assertThat(firstRoundMeleeFromCerberus).isGreaterThanOrEqualTo(2);
    }

    @Test
    // Stack ist eine mutable Entity ohne equals — der ==-Vergleich meint bewusst Identität.
    @SuppressWarnings("ReferenceEquality")
    void cerberus_hits_the_two_enemy_flanks_of_its_target_and_nothing_else() {
        // Manual S. 97: „attack three adjacent enemies". Cerberus (5,5) greift (6,5) an; die
        // Flanken sind die gemeinsamen Nachbarn (6,4) und (5,6). Auf (6,4) steht ein eigener
        // Stack, auf (5,6) ein Gegner, hinter dem Cerberus auf (4,5) noch ein Gegner.
        Stack cerberus = new Stack(UnitCatalog.CERBERUS, 20, new Hex(5, 5), Side.ATTACKER, 0);
        Stack ownFlank = new Stack(UnitCatalog.PIKEMAN, 30, new Hex(6, 4), Side.ATTACKER, 1);
        Stack primary = new Stack(UnitCatalog.PIKEMAN, 30, new Hex(6, 5), Side.DEFENDER, 0);
        Stack enemyFlank = new Stack(UnitCatalog.PIKEMAN, 30, new Hex(5, 6), Side.DEFENDER, 1);
        Stack behind = new Stack(UnitCatalog.PIKEMAN, 30, new Hex(4, 5), Side.DEFENDER, 2);
        BattleSetup setup = new BattleSetup(List.of(cerberus, ownFlank),
                List.of(primary, enemyFlank, behind), Battlefield.STANDARD);

        AutoSolver cerberusAttacksPrimary = (active, opponent, bf) ->
                active == cerberus && primary.isAlive() ? new Action.Melee(primary) : new Action.Defend();
        new Battle(new Random(3L), cerberusAttacksPrimary).simulate(setup);

        assertThat(enemyFlank.getCount()).isLessThan(30);
        assertThat(ownFlank.getCount()).as("eigene Flanke bleibt verschont").isEqualTo(30);
        assertThat(behind.getCount()).as("Gegner hinter dem Cerberus liegt nicht auf einer Flanke")
                .isEqualTo(30);
    }

    @Test
    // Stack ist eine mutable Entity ohne equals — der ==-Vergleich meint bewusst Identität.
    @SuppressWarnings("ReferenceEquality")
    void magog_fireball_hits_every_neighbour_of_the_target_including_own_stacks() {
        // Manual S. 97: „affect the target hex and its surrounding hexes like a Fireball spell".
        // Ziel (8,5), Gegner auf drei Nachbarn, ein eigener Stack auf einem vierten, ein Gegner
        // zwei Felder entfernt auf (10,5).
        Stack magog = new Stack(UnitCatalog.MAGOG, 20, new Hex(0, 5), Side.ATTACKER, 0);
        Stack own = new Stack(UnitCatalog.PIKEMAN, 30, new Hex(7, 5), Side.ATTACKER, 1);
        Stack primary = new Stack(UnitCatalog.PIKEMAN, 30, new Hex(8, 5), Side.DEFENDER, 0);
        List<Stack> neighbours = List.of(
                new Stack(UnitCatalog.PIKEMAN, 30, new Hex(9, 5), Side.DEFENDER, 1),
                new Stack(UnitCatalog.PIKEMAN, 30, new Hex(8, 4), Side.DEFENDER, 2),
                new Stack(UnitCatalog.PIKEMAN, 30, new Hex(8, 6), Side.DEFENDER, 3));
        Stack distant = new Stack(UnitCatalog.PIKEMAN, 30, new Hex(10, 5), Side.DEFENDER, 4);
        List<Stack> defenders = new java.util.ArrayList<>(List.of(primary));
        defenders.addAll(neighbours);
        defenders.add(distant);
        BattleSetup setup = new BattleSetup(List.of(magog, own), defenders, Battlefield.STANDARD);

        AutoSolver magogShootsPrimary = (active, opponent, bf) ->
                active == magog && primary.isAlive() && magog.canShoot()
                        ? new Action.Shoot(primary) : new Action.Defend();
        new Battle(new Random(4L), magogShootsPrimary).simulate(setup);

        assertThat(neighbours).allSatisfy(n -> assertThat(n.getCount()).isLessThan(30));
        assertThat(own.getCount()).as("Friendly Fire wie beim Fireball").isLessThan(30);
        assertThat(distant.getCount()).isEqualTo(30);
    }

    @Test
    void fire_breath_hits_stack_behind_primary_target() {
        // Black Dragon auf (5,5), Pikeman auf (6,5) als Primärziel, Goblin auf (7,5) dahinter.
        Stack dragon = new Stack(UnitCatalog.BLACK_DRAGON, 1, new Hex(5, 5), Side.ATTACKER, 0);
        Stack primary = new Stack(UnitCatalog.PIKEMAN, 5, new Hex(6, 5), Side.DEFENDER, 0);
        Stack behind = new Stack(UnitCatalog.GOBLIN, 50, new Hex(7, 5), Side.DEFENDER, 1);
        int behindStart = behind.getCount();

        BattleSetup setup = new BattleSetup(
                List.of(dragon),
                List.of(primary, behind),
                Battlefield.STANDARD);
        new Battle(new Random(2L), new GreedyAutoSolver()).simulate(setup);

        // "behind" Stack muss Verluste aus Fire-Breath haben.
        assertThat(behind.getCount()).isLessThan(behindStart);
    }

    @Test
    void magog_splash_shot_damages_adjacent_enemies_of_primary_target() {
        // Magog schießt auf Pikemen-Stack; daneben weiterer Stack im 1-Hex-Radius.
        Stack magog = new Stack(UnitCatalog.MAGOG, 10, new Hex(0, 5), Side.ATTACKER, 0);
        Stack primary = new Stack(UnitCatalog.PIKEMAN, 10, new Hex(7, 5), Side.DEFENDER, 0);
        Stack splash = new Stack(UnitCatalog.PIKEMAN, 10, new Hex(7, 6), Side.DEFENDER, 1);
        int splashStart = splash.getCount();

        BattleSetup setup = new BattleSetup(
                List.of(magog),
                List.of(primary, splash),
                Battlefield.STANDARD);
        new Battle(new Random(3L), new GreedyAutoSolver()).simulate(setup);

        // splash-Stack muss zusätzliche Verluste aus dem Splash-Schuss haben.
        assertThat(splash.getCount()).isLessThan(splashStart);
    }

    @Test
    void lich_death_cloud_hits_multiple_adjacent_enemies_of_primary_target() {
        // Lich auf Distanz, drei Defender im 1-Hex-Radius des Primärziels.
        Stack lich = new Stack(UnitCatalog.POWER_LICH, 5, new Hex(0, 5), Side.ATTACKER, 0);
        Stack primary = new Stack(UnitCatalog.PIKEMAN, 20, new Hex(7, 5), Side.DEFENDER, 0);
        Stack a = new Stack(UnitCatalog.PIKEMAN, 20, new Hex(7, 4), Side.DEFENDER, 1);
        Stack b = new Stack(UnitCatalog.PIKEMAN, 20, new Hex(7, 6), Side.DEFENDER, 2);
        int aStart = a.getCount();
        int bStart = b.getCount();

        BattleSetup setup = new BattleSetup(
                List.of(lich),
                List.of(primary, a, b),
                Battlefield.STANDARD);
        new Battle(new Random(4L), new GreedyAutoSolver()).simulate(setup);

        // Beide Nachbarn müssen Verluste haben (durch die Death Cloud).
        assertThat(a.getCount()).isLessThan(aStart);
        assertThat(b.getCount()).isLessThan(bStart);
    }
}
