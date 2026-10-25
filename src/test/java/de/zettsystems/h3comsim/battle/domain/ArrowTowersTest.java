package de.zettsystems.h3comsim.battle.domain;

import de.zettsystems.h3comsim.battle.domain.events.BattleEvent;
import de.zettsystems.h3comsim.battle.domain.events.ListEventCollector;
import de.zettsystems.h3comsim.battle.domain.events.Side;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

/** Pfeiltürme nach heroes.thelazy.net, <em>Arrow tower</em>. */
class ArrowTowersTest {

    @Test
    void the_damage_ranges_match_the_table_for_a_full_town() {
        Siege castle = new Siege(Fortification.CASTLE, Faction.CASTLE);
        assertThat(ArrowTowers.structures(castle)).isEqualTo(17);
        assertThat(ArrowTowers.structures(new Siege(Fortification.CASTLE, Faction.TOWER))).isEqualTo(18);
        assertThat(ArrowTowers.structures(new Siege(Fortification.CITADEL, Faction.CASTLE))).isEqualTo(16);

        Random rng = new Random(3);
        int keepMin = Integer.MAX_VALUE;
        int keepMax = 0;
        int sideMin = Integer.MAX_VALUE;
        int sideMax = 0;
        for (int i = 0; i < 20_000; i++) {
            int keep = ArrowTowers.damage(WallPart.KEEP, castle, rng);
            int side = ArrowTowers.damage(WallPart.UPPER_TOWER, castle, rng);
            keepMin = Math.min(keepMin, keep);
            keepMax = Math.max(keepMax, keep);
            sideMin = Math.min(sideMin, side);
            sideMax = Math.max(sideMax, side);
        }
        assertThat(keepMin).isEqualTo(44);
        assertThat(keepMax).isEqualTo(88);
        assertThat(sideMin).isEqualTo(22);
        assertThat(sideMax).isEqualTo(44);
    }

    @Test
    void towers_come_with_citadel_and_castle_and_fall_silent_when_destroyed() {
        assertThat(ArrowTowers.standing(new Siege(Fortification.FORT, Faction.CASTLE))).isEmpty();
        assertThat(ArrowTowers.standing(new Siege(Fortification.CITADEL, Faction.CASTLE))).containsExactly(WallPart.KEEP);
        Siege castle = new Siege(Fortification.CASTLE, Faction.CASTLE);
        assertThat(ArrowTowers.standing(castle))
                .containsExactly(WallPart.KEEP, WallPart.UPPER_TOWER, WallPart.LOWER_TOWER);
        castle.damage(WallPart.KEEP, 2);
        assertThat(ArrowTowers.standing(castle)).containsExactly(WallPart.UPPER_TOWER, WallPart.LOWER_TOWER);
    }

    @Test
    void towers_prefer_shooters_and_ignore_defense() {
        Stack marksmen = new Stack(UnitCatalog.MARKSMAN, 20, new Hex(0, 3), Side.ATTACKER, 0);
        Stack pikemen = new Stack(UnitCatalog.PIKEMAN, 20, new Hex(0, 5), Side.ATTACKER, 1);
        Siege siege = new Siege(Fortification.CASTLE, Faction.CASTLE);
        BattleSetup setup = new BattleSetup(List.of(marksmen, pikemen),
                List.of(new Stack(UnitCatalog.PEASANT, 1, new Hex(14, 5), Side.DEFENDER, 0)), Battlefield.STANDARD,
                null, CollectedBonus.NONE, null, CollectedBonus.NONE, siege);
        assertThat(ArrowTowers.target(WallPart.KEEP, siege, setup)).isSameAs(marksmen);

        ListEventCollector collector = new ListEventCollector();
        new Battle(new Random(4), new GreedyAutoSolver(), collector).simulate(setup);
        List<BattleEvent.TowerShot> shots = collector.events().stream()
                .filter(BattleEvent.TowerShot.class::isInstance).map(BattleEvent.TowerShot.class::cast).toList();
        assertThat(shots).isNotEmpty();
        assertThat(shots.subList(0, 3)).extracting(BattleEvent.TowerShot::tower)
                .containsExactly(WallPart.KEEP, WallPart.UPPER_TOWER, WallPart.LOWER_TOWER);
        assertThat(shots).allSatisfy(shot -> assertThat(shot.damage()).isBetween(22, 88));
    }
}
