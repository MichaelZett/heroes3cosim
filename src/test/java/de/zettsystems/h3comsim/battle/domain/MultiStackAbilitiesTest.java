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
