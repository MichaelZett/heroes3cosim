package de.zettsystems.h3comsim.battle.domain;

import de.zettsystems.h3comsim.battle.domain.events.BattleEvent;
import de.zettsystems.h3comsim.battle.domain.events.ListEventCollector;
import de.zettsystems.h3comsim.battle.domain.events.Side;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/** Katapult und Ballistics nach heroes.thelazy.net (SoD-Werte). */
class CatapultTest {

    @Test
    void without_ballistics_it_shoots_the_weakest_wall_then_gate_keep_and_towers() {
        Siege siege = new Siege(Fortification.CASTLE, Faction.CASTLE);
        siege.damage(WallPart.MID_LOWER_WALL, 1);
        assertThat(Catapult.target(siege, SkillLevel.NONE, new Random(1))).isEqualTo(WallPart.MID_LOWER_WALL);

        for (WallPart wall : List.of(WallPart.UPPER_WALL, WallPart.MID_UPPER_WALL, WallPart.MID_LOWER_WALL,
                WallPart.LOWER_WALL)) {
            siege.damage(wall, 3);
        }
        assertThat(Catapult.target(siege, SkillLevel.NONE, new Random(1))).isEqualTo(WallPart.GATE);
        siege.damage(WallPart.GATE, 2);
        assertThat(Catapult.target(siege, SkillLevel.NONE, new Random(1))).isEqualTo(WallPart.KEEP);
        siege.damage(WallPart.KEEP, 2);
        siege.damage(WallPart.UPPER_TOWER, 2);
        siege.damage(WallPart.LOWER_TOWER, 2);
        assertThat(Catapult.target(siege, SkillLevel.NONE, new Random(1))).isNull();
    }

    @Test
    void with_ballistics_the_gate_falls_first_then_the_towers_once_a_wall_is_open() {
        Siege siege = new Siege(Fortification.CASTLE, Faction.CASTLE);
        assertThat(Catapult.target(siege, SkillLevel.BASIC, new Random(1))).isEqualTo(WallPart.GATE);
        siege.damage(WallPart.GATE, 2);
        assertThat(Catapult.target(siege, SkillLevel.BASIC, new Random(1))).matches(WallPart::isWall);
        siege.damage(WallPart.LOWER_WALL, 3);
        assertThat(Catapult.target(siege, SkillLevel.BASIC, new Random(1))).isEqualTo(WallPart.KEEP);
    }

    @Test
    void hit_chances_and_damage_follow_the_ballistics_table() {
        assertThat(Catapult.hitPercent(WallPart.UPPER_WALL, SkillLevel.NONE)).isEqualTo(50);
        assertThat(Catapult.hitPercent(WallPart.UPPER_WALL, SkillLevel.EXPERT)).isEqualTo(75);
        assertThat(Catapult.hitPercent(WallPart.GATE, SkillLevel.ADVANCED)).isEqualTo(30);
        assertThat(Catapult.hitPercent(WallPart.KEEP, SkillLevel.BASIC)).isEqualTo(7);
        assertThat(Catapult.hitPercent(WallPart.LOWER_TOWER, SkillLevel.EXPERT)).isEqualTo(20);
        assertThat(Catapult.shots(SkillLevel.BASIC)).isEqualTo(1);
        assertThat(Catapult.shots(SkillLevel.ADVANCED)).isEqualTo(2);

        int[] none = new int[3];
        Random rng = new Random(5);
        for (int i = 0; i < 10_000; i++) {
            none[Catapult.damage(SkillLevel.NONE, rng)]++;
        }
        assertThat(none[0]).isBetween(800, 1200);
        assertThat(none[1]).isBetween(5700, 6300);
        assertThat(Catapult.damage(SkillLevel.EXPERT, rng)).isEqualTo(2);
    }

    @Test
    void a_missed_shot_hits_the_nearest_standing_wall_and_never_a_destroyed_one() {
        Siege siege = new Siege(Fortification.CASTLE, Faction.CASTLE);
        siege.damage(WallPart.LOWER_WALL, 3);
        Random rng = new Random(11);
        for (int i = 0; i < 200; i++) {
            Catapult.Shot shot = Catapult.fire(siege, SkillLevel.NONE, rng);
            assertThat(shot).isNotNull();
            assertThat(shot.hit()).isNotEqualTo(WallPart.LOWER_WALL);
            assertThat(siege.stands(shot.hit())).isTrue();
        }
    }

    @Test
    void every_attacking_hero_brings_a_catapult_to_a_siege_and_it_breaks_in() {
        Hero hero = HeroCatalog.SORSHA.withBuild(0, 0, Map.of(SecondarySkill.BALLISTICS, SkillLevel.EXPERT));
        BattleSetup setup = new BattleSetup(
                List.of(new Stack(UnitCatalog.PIKEMAN, 30, new Hex(0, 5), Side.ATTACKER, 0)),
                List.of(new Stack(UnitCatalog.PEASANT, 5, new Hex(14, 5), Side.DEFENDER, 0)),
                Battlefield.STANDARD, hero, CollectedBonus.NONE, null, CollectedBonus.NONE,
                new Siege(Fortification.FORT, Faction.CASTLE))
                .equip(Side.ATTACKER, Set.of(WarMachine.BALLISTA));

        assertThat(setup.warMachinesOf(Side.ATTACKER)).containsExactlyInAnyOrder(WarMachine.BALLISTA, WarMachine.CATAPULT);

        ListEventCollector collector = new ListEventCollector();
        BattleResult result = new Battle(new Random(9), new GreedyAutoSolver(), collector).simulate(setup);
        List<BattleEvent.CatapultShot> shots = collector.events().stream()
                .filter(BattleEvent.CatapultShot.class::isInstance).map(BattleEvent.CatapultShot.class::cast).toList();
        assertThat(shots).isNotEmpty();
        assertThat(shots.getFirst().aimed()).isEqualTo(WallPart.GATE);
        assertThat(result.winner()).as("die Pikeniere kommen hinein").isEqualTo(
                de.zettsystems.h3comsim.battle.domain.events.Winner.ATTACKER);
    }

    @Test
    void no_catapult_in_a_field_battle_or_without_a_hero() {
        BattleSetup field = new BattleSetup(
                List.of(new Stack(UnitCatalog.PIKEMAN, 1, new Hex(0, 5), Side.ATTACKER, 0)),
                List.of(new Stack(UnitCatalog.PIKEMAN, 1, new Hex(14, 5), Side.DEFENDER, 0)),
                Battlefield.STANDARD, HeroCatalog.SORSHA, null).equip(Side.ATTACKER, Set.of(WarMachine.CATAPULT));
        assertThat(field.warMachinesOf(Side.ATTACKER)).isEmpty();

        BattleSetup leaderless = new BattleSetup(
                List.of(new Stack(UnitCatalog.PIKEMAN, 1, new Hex(0, 5), Side.ATTACKER, 0)),
                List.of(new Stack(UnitCatalog.PIKEMAN, 1, new Hex(14, 5), Side.DEFENDER, 0)),
                Battlefield.STANDARD, null, CollectedBonus.NONE, null, CollectedBonus.NONE,
                new Siege(Fortification.FORT, Faction.CASTLE));
        assertThat(leaderless.warMachinesOf(Side.ATTACKER)).isEmpty();
    }
}
