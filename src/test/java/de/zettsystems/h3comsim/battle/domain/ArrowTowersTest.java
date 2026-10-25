package de.zettsystems.h3comsim.battle.domain;

import de.zettsystems.h3comsim.battle.domain.events.BattleEvent;
import de.zettsystems.h3comsim.battle.domain.events.ListEventCollector;
import de.zettsystems.h3comsim.battle.domain.events.Side;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
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
    void the_game_ai_counts_one_point_of_damage_and_artillery_takes_over() {
        Stack many = new Stack(UnitCatalog.MARKSMAN, 50, new Hex(0, 3), Side.ATTACKER, 0);
        Stack single = new Stack(UnitCatalog.MARKSMAN, 1, new Hex(0, 7), Side.ATTACKER, 1);
        Stack defender = new Stack(UnitCatalog.PEASANT, 1, new Hex(14, 5), Side.DEFENDER, 0);
        Siege siege = new Siege(Fortification.CASTLE, Faction.CASTLE);

        BattleSetup uncontrolled = new BattleSetup(List.of(many, single), List.of(defender), Battlefield.STANDARD,
                null, CollectedBonus.NONE, null, CollectedBonus.NONE, siege);
        assertThat(ArrowTowers.target(WallPart.KEEP, siege, uncontrolled))
                .as("gleicher Wert je Lebenspunkt: der Stack ganz rechts").isSameAs(single);

        Hero gunner = HeroCatalog.SORSHA.withBuild(0, 0, Map.of(SecondarySkill.ARTILLERY, SkillLevel.BASIC));
        BattleSetup controlled = new BattleSetup(List.of(many, single), List.of(defender), Battlefield.STANDARD,
                null, CollectedBonus.NONE, gunner, CollectedBonus.NONE, siege);
        assertThat(ArrowTowers.target(WallPart.KEEP, siege, controlled)).isSameAs(many);
    }

    @Test
    void the_game_ai_ranks_creatures_by_when_they_can_strike() {
        Stack defender = new Stack(UnitCatalog.PEASANT, 1, new Hex(10, 5), Side.DEFENDER, 0);
        Siege siege = new Siege(Fortification.CASTLE, Faction.CASTLE);
        // Pikeniere (Speed 4) greifen aus 5 Feldern sofort an, aus 9 im nächsten Zug; Hellebardiere
        // (Speed 5, mehr wert je Lebenspunkt) aus 12 Feldern erst danach.
        Stack near = new Stack(UnitCatalog.PIKEMAN, 1, new Hex(5, 5), Side.ATTACKER, 0);
        Stack later = new Stack(UnitCatalog.HALBERDIER, 1, new Hex(-2, 9), Side.ATTACKER, 1);
        BattleSetup setup = new BattleSetup(List.of(near, later), List.of(defender), Battlefield.STANDARD,
                null, CollectedBonus.NONE, null, CollectedBonus.NONE, siege);
        assertThat(ArrowTowers.target(WallPart.KEEP, siege, setup)).isSameAs(near);

        Stack next = new Stack(UnitCatalog.PIKEMAN, 1, new Hex(1, 5), Side.ATTACKER, 0);
        BattleSetup secondTurn = new BattleSetup(List.of(next, new Stack(UnitCatalog.HALBERDIER, 1, new Hex(-2, 9),
                Side.ATTACKER, 1)), List.of(defender), Battlefield.STANDARD, null, CollectedBonus.NONE, null,
                CollectedBonus.NONE, siege);
        assertThat(ArrowTowers.target(WallPart.KEEP, siege, secondTurn)).as("halber gegen Drittel-Wert")
                .isSameAs(secondTurn.attackerStacks().getFirst());
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
