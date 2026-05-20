package de.zettsystems.h3comsim.battle.domain.events;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Konstruktor-Coverage für alle BattleEvent-Subtypes. Die Battle-Simulation triggert nicht
 * jede Skill-Variante zuverlässig (z. B. FireShield, Petrifying, Aging hängen von seltenen
 * RNG-Outcomes und passenden Match-ups ab); dieser Test instanziiert jede Subtype direkt
 * und prüft die compact-constructor-Defensive-Kopien.
 */
class BattleEventSubtypesTest {

    private static final StackSnapshot ATTACKER_SNAP =
            new StackSnapshot(Side.ATTACKER, "Pikeman", 5, 10, 0, 5);
    private static final StackSnapshot DEFENDER_SNAP =
            new StackSnapshot(Side.DEFENDER, "Marksman", 5, 10, 14, 5);

    @Test
    void battle_start_copies_obstacle_list_defensively() {
        List<HexCoord> mutable = new ArrayList<>(List.of(new HexCoord(7, 5)));
        BattleEvent.BattleStart event =
                new BattleEvent.BattleStart(15, 11, mutable, ATTACKER_SNAP, DEFENDER_SNAP);

        mutable.add(new HexCoord(99, 99));

        assertThat(event.obstacles()).containsExactly(new HexCoord(7, 5));
        assertThat(event.battlefieldWidth()).isEqualTo(15);
        assertThat(event.battlefieldHeight()).isEqualTo(11);
        assertThat(event.attacker()).isSameAs(ATTACKER_SNAP);
        assertThat(event.defender()).isSameAs(DEFENDER_SNAP);
    }

    @Test
    void move_copies_path_defensively() {
        List<HexCoord> path = new ArrayList<>(List.of(new HexCoord(0, 5), new HexCoord(1, 5)));
        BattleEvent.Move move = new BattleEvent.Move(Side.ATTACKER, 0, 5, 1, 5, path);

        path.clear();

        assertThat(move.actor()).isEqualTo(Side.ATTACKER);
        assertThat(move.fromQ()).isZero();
        assertThat(move.fromR()).isEqualTo(5);
        assertThat(move.toQ()).isEqualTo(1);
        assertThat(move.toR()).isEqualTo(5);
        assertThat(move.path()).hasSize(2);
    }

    @Test
    void move_back_copies_path_defensively() {
        List<HexCoord> path = new ArrayList<>(List.of(new HexCoord(3, 5), new HexCoord(0, 5)));
        BattleEvent.MoveBack moveBack = new BattleEvent.MoveBack(Side.ATTACKER, 0, 5, path);

        path.clear();

        assertThat(moveBack.actor()).isEqualTo(Side.ATTACKER);
        assertThat(moveBack.toQ()).isZero();
        assertThat(moveBack.toR()).isEqualTo(5);
        assertThat(moveBack.path()).hasSize(2);
    }

    @Test
    void wait_event_carries_actor() {
        assertThat(new BattleEvent.Wait(Side.DEFENDER).actor()).isEqualTo(Side.DEFENDER);
    }

    @Test
    void shoot_event_carries_distance_damage_kills_snapshot() {
        BattleEvent.Shoot shoot =
                new BattleEvent.Shoot(Side.ATTACKER, Side.DEFENDER, 8, 47, 3, DEFENDER_SNAP);

        assertThat(shoot.actor()).isEqualTo(Side.ATTACKER);
        assertThat(shoot.target()).isEqualTo(Side.DEFENDER);
        assertThat(shoot.distance()).isEqualTo(8);
        assertThat(shoot.damage()).isEqualTo(47);
        assertThat(shoot.killed()).isEqualTo(3);
        assertThat(shoot.targetAfter()).isSameAs(DEFENDER_SNAP);
    }

    @Test
    void melee_event_carries_hexes_moved_damage_kills() {
        BattleEvent.Melee melee =
                new BattleEvent.Melee(Side.ATTACKER, Side.DEFENDER, 3, 62, 4, DEFENDER_SNAP);

        assertThat(melee.hexesMoved()).isEqualTo(3);
        assertThat(melee.damage()).isEqualTo(62);
        assertThat(melee.killed()).isEqualTo(4);
        assertThat(melee.targetAfter()).isSameAs(DEFENDER_SNAP);
    }

    @Test
    void retaliation_event_carries_retaliator_target_damage_kills() {
        BattleEvent.Retaliation retaliation =
                new BattleEvent.Retaliation(Side.DEFENDER, Side.ATTACKER, 30, 2, ATTACKER_SNAP);

        assertThat(retaliation.retaliator()).isEqualTo(Side.DEFENDER);
        assertThat(retaliation.target()).isEqualTo(Side.ATTACKER);
        assertThat(retaliation.damage()).isEqualTo(30);
        assertThat(retaliation.killed()).isEqualTo(2);
        assertThat(retaliation.targetAfter()).isSameAs(ATTACKER_SNAP);
    }

    @Test
    void marker_events_carry_actor() {
        assertThat(new BattleEvent.TwoBlows(Side.ATTACKER).actor()).isEqualTo(Side.ATTACKER);
        assertThat(new BattleEvent.TwoShots(Side.DEFENDER).actor()).isEqualTo(Side.DEFENDER);
        assertThat(new BattleEvent.GoodMorale(Side.ATTACKER).actor()).isEqualTo(Side.ATTACKER);
    }

    @Test
    void death_stare_carries_kills_and_snapshot() {
        BattleEvent.DeathStare stare =
                new BattleEvent.DeathStare(Side.ATTACKER, Side.DEFENDER, 2, DEFENDER_SNAP);

        assertThat(stare.kills()).isEqualTo(2);
        assertThat(stare.targetAfter()).isSameAs(DEFENDER_SNAP);
    }

    @Test
    void thunderbolts_carries_damage_and_snapshot() {
        BattleEvent.Thunderbolts bolt =
                new BattleEvent.Thunderbolts(Side.ATTACKER, Side.DEFENDER, 70, DEFENDER_SNAP);

        assertThat(bolt.damage()).isEqualTo(70);
        assertThat(bolt.targetAfter()).isSameAs(DEFENDER_SNAP);
    }

    @Test
    void debuff_skill_events_carry_actor_and_target() {
        BattleEvent.Petrifying petrifying = new BattleEvent.Petrifying(Side.ATTACKER, Side.DEFENDER);
        BattleEvent.Cursing cursing = new BattleEvent.Cursing(Side.DEFENDER, Side.ATTACKER);
        BattleEvent.Poisoning poisoning = new BattleEvent.Poisoning(Side.ATTACKER, Side.DEFENDER);
        BattleEvent.Diseasing diseasing = new BattleEvent.Diseasing(Side.DEFENDER, Side.ATTACKER);
        BattleEvent.Aging aging = new BattleEvent.Aging(Side.ATTACKER, Side.DEFENDER);

        assertThat(petrifying.actor()).isEqualTo(Side.ATTACKER);
        assertThat(petrifying.target()).isEqualTo(Side.DEFENDER);
        assertThat(cursing.actor()).isEqualTo(Side.DEFENDER);
        assertThat(cursing.target()).isEqualTo(Side.ATTACKER);
        assertThat(poisoning.target()).isEqualTo(Side.DEFENDER);
        assertThat(diseasing.target()).isEqualTo(Side.ATTACKER);
        assertThat(aging.target()).isEqualTo(Side.DEFENDER);
    }

    @Test
    void fire_shield_carries_reflected_damage_and_attacker_snapshot() {
        BattleEvent.FireShield shield =
                new BattleEvent.FireShield(Side.DEFENDER, Side.ATTACKER, 12, ATTACKER_SNAP);

        assertThat(shield.shielded()).isEqualTo(Side.DEFENDER);
        assertThat(shield.attacker()).isEqualTo(Side.ATTACKER);
        assertThat(shield.damage()).isEqualTo(12);
        assertThat(shield.attackerAfter()).isSameAs(ATTACKER_SNAP);
    }

    @Test
    void rebirth_carries_restored_count_and_snapshot() {
        BattleEvent.Rebirth rebirth =
                new BattleEvent.Rebirth(Side.ATTACKER, 1, ATTACKER_SNAP);

        assertThat(rebirth.restoredCount()).isEqualTo(1);
        assertThat(rebirth.actorAfter()).isSameAs(ATTACKER_SNAP);
    }

    @Test
    void battle_end_carries_winner_survivors_turns() {
        BattleEvent.BattleEnd end = new BattleEvent.BattleEnd(Winner.DRAW, 0, 0, 50);

        assertThat(end.winner()).isEqualTo(Winner.DRAW);
        assertThat(end.attackerSurvivors()).isZero();
        assertThat(end.defenderSurvivors()).isZero();
        assertThat(end.turns()).isEqualTo(50);
    }
}
