package de.zettsystems.h3comsim.battle.domain;

import de.zettsystems.h3comsim.battle.domain.events.Side;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class StrategicAutoSolverTest {

    private final StrategicAutoSolver solver = new StrategicAutoSolver();
    private final Battlefield battlefield = Battlefield.STANDARD;

    @Test
    void focus_target_picks_highest_threat_enemy() {
        // Defender hat zwei Stacks: schwache Pikemen + harte Arch Angels. Focus muss Arch Angels sein.
        Stack hal = new Stack(UnitCatalog.HALBERDIER, 14, new Hex(0, 5), Side.ATTACKER, 0);
        Stack pikemen = new Stack(UnitCatalog.PIKEMAN, 14, new Hex(14, 3), Side.DEFENDER, 0);
        Stack angels = new Stack(UnitCatalog.ARCH_ANGEL, 1, new Hex(14, 7), Side.DEFENDER, 1);
        BattleSetup setup = new BattleSetup(List.of(hal), List.of(pikemen, angels), battlefield);

        solver.planRound(setup);

        assertThat(solver.currentPlan().focusOf(Side.ATTACKER)).isSameAs(angels);
    }

    @Test
    void aoe_shooter_picks_target_with_most_adjacent_enemies() {
        // Magog auf Distanz; zwei Pikemen-Stacks rechts isoliert, eine Dreier-Gruppe links eng.
        Stack magog = new Stack(UnitCatalog.MAGOG, 10, new Hex(0, 5), Side.ATTACKER, 0);
        Stack isolated = new Stack(UnitCatalog.PIKEMAN, 50, new Hex(10, 0), Side.DEFENDER, 0);
        Stack center = new Stack(UnitCatalog.PIKEMAN, 10, new Hex(10, 5), Side.DEFENDER, 1);
        Stack near1 = new Stack(UnitCatalog.PIKEMAN, 10, new Hex(10, 4), Side.DEFENDER, 2);
        Stack near2 = new Stack(UnitCatalog.PIKEMAN, 10, new Hex(10, 6), Side.DEFENDER, 3);
        BattleSetup setup = new BattleSetup(
                List.of(magog),
                List.of(isolated, center, near1, near2),
                battlefield);
        solver.planRound(setup);

        Stack target = solver.pickTarget(magog, setup.opponentsOf(magog), battlefield);

        // center hat 2 adjazente Verbündete → soll von Splash-Shot bevorzugt werden.
        assertThat(target).isSameAs(center);
    }

    @Test
    void fire_breath_picks_inline_pair() {
        // Black Dragon auf (5,5). Pikemen direkt davor auf (6,5) + Inferno-Goblin auf (7,5) inline dahinter.
        Stack dragon = new Stack(UnitCatalog.BLACK_DRAGON, 1, new Hex(5, 5), Side.ATTACKER, 0);
        Stack inlinePair = new Stack(UnitCatalog.PIKEMAN, 10, new Hex(6, 5), Side.DEFENDER, 0);
        Stack inlineBehind = new Stack(UnitCatalog.GOBLIN, 50, new Hex(7, 5), Side.DEFENDER, 1);
        Stack isolated = new Stack(UnitCatalog.PIKEMAN, 10, new Hex(4, 3), Side.DEFENDER, 2);
        BattleSetup setup = new BattleSetup(
                List.of(dragon),
                List.of(inlinePair, inlineBehind, isolated),
                battlefield);
        solver.planRound(setup);

        Stack target = solver.pickTarget(dragon, setup.opponentsOf(dragon), battlefield);

        assertThat(target).isSameAs(inlinePair);
    }

    @Test
    void an_engaged_shooter_strikes_its_neighbour_instead_of_the_focus() {
        Stack marksmen = new Stack(UnitCatalog.MARKSMAN, 9, new Hex(5, 5), Side.ATTACKER, 0);
        Stack pikemen = new Stack(UnitCatalog.PIKEMAN, 2, new Hex(6, 5), Side.DEFENDER, 0);
        Stack angels = new Stack(UnitCatalog.ARCH_ANGEL, 1, new Hex(14, 7), Side.DEFENDER, 1);
        BattleSetup setup = new BattleSetup(List.of(marksmen), List.of(pikemen, angels), battlefield);
        solver.planRound(setup);
        assertThat(solver.currentPlan().focusOf(Side.ATTACKER)).isSameAs(angels);

        assertThat(solver.pickTarget(marksmen, setup.opponentsOf(marksmen), battlefield)).isSameAs(pikemen);
    }

    @Test
    void a_walker_only_follows_the_focus_it_can_strike_this_turn() {
        // Halberdiere (Speed 5): die Arch Angels ganz hinten sind der Fokus, aber unerreichbar.
        Stack hal = new Stack(UnitCatalog.HALBERDIER, 14, new Hex(2, 5), Side.ATTACKER, 0);
        Stack pikemen = new Stack(UnitCatalog.PIKEMAN, 14, new Hex(7, 5), Side.DEFENDER, 0);
        Stack angels = new Stack(UnitCatalog.ARCH_ANGEL, 1, new Hex(14, 7), Side.DEFENDER, 1);
        BattleSetup setup = new BattleSetup(List.of(hal), List.of(pikemen, angels), battlefield);
        solver.planRound(setup);
        assertThat(solver.currentPlan().focusOf(Side.ATTACKER)).isSameAs(angels);

        assertThat(solver.pickTarget(hal, setup.opponentsOf(hal), battlefield)).isSameAs(pikemen);

        // Steht der Fokus in Reichweite, gilt er.
        angels.moveTo(new Hex(5, 3));
        assertThat(solver.pickTarget(hal, setup.opponentsOf(hal), battlefield)).isSameAs(angels);
    }

    @Test
    void an_engaged_walker_leaves_its_neighbour_for_a_focus_in_reach() {
        Stack hal = new Stack(UnitCatalog.HALBERDIER, 14, new Hex(5, 5), Side.ATTACKER, 0);
        Stack pikemen = new Stack(UnitCatalog.PIKEMAN, 2, new Hex(6, 5), Side.DEFENDER, 0);
        Stack angels = new Stack(UnitCatalog.ARCH_ANGEL, 1, new Hex(7, 3), Side.DEFENDER, 1);
        BattleSetup setup = new BattleSetup(List.of(hal), List.of(pikemen, angels), battlefield);
        solver.planRound(setup);

        assertThat(solver.pickTarget(hal, setup.opponentsOf(hal), battlefield)).isSameAs(angels);
    }

    @Test
    void stale_focus_target_falls_back_to_greedy_when_dead() {
        // Defender: schwache Pikemen + harte Arch Angels → Focus = Arch Angels.
        // Wir töten die Arch Angels zwischen planRound() und pickTarget(): der Solver darf
        // keinen toten Stack zurückgeben, sondern muss auf den lebenden Pikeman fallen.
        Stack hal = new Stack(UnitCatalog.HALBERDIER, 14, new Hex(0, 5), Side.ATTACKER, 0);
        Stack pikemen = new Stack(UnitCatalog.PIKEMAN, 14, new Hex(14, 3), Side.DEFENDER, 0);
        Stack angels = new Stack(UnitCatalog.ARCH_ANGEL, 1, new Hex(14, 7), Side.DEFENDER, 1);
        BattleSetup setup = new BattleSetup(List.of(hal), List.of(pikemen, angels), battlefield);
        solver.planRound(setup);
        assertThat(solver.currentPlan().focusOf(Side.ATTACKER)).isSameAs(angels);

        angels.loseTopCreatures(1);

        Stack target = solver.pickTarget(hal, setup.opponentsOf(hal), battlefield);
        assertThat(target).isSameAs(pikemen);
    }

    @Test
    void aoe_shooter_without_cluster_falls_back_to_danger_pick() {
        // Magog mit drei isolierten Gegnern (alle adjazenten Allies = 0) → AoE-Pick liefert null.
        // Greedy-Fallback: gefährlichster Stack = avgDmg × count, hier der große Pikeman-Stack.
        Stack magog = new Stack(UnitCatalog.MAGOG, 10, new Hex(0, 5), Side.ATTACKER, 0);
        Stack weak = new Stack(UnitCatalog.PIKEMAN, 5, new Hex(10, 0), Side.DEFENDER, 0);
        Stack heavy = new Stack(UnitCatalog.PIKEMAN, 60, new Hex(10, 5), Side.DEFENDER, 1);
        Stack medium = new Stack(UnitCatalog.PIKEMAN, 20, new Hex(10, 10), Side.DEFENDER, 2);
        BattleSetup setup = new BattleSetup(List.of(magog), List.of(weak, heavy, medium), battlefield);
        solver.planRound(setup);

        Stack target = solver.pickTarget(magog, setup.opponentsOf(magog), battlefield);

        assertThat(target).isSameAs(heavy);
    }

    @Test
    void plan_round_refreshes_setup_and_focus() {
        // Erst Setup A → Focus = Pikemen (einziger Feind).
        Stack halA = new Stack(UnitCatalog.HALBERDIER, 14, new Hex(0, 5), Side.ATTACKER, 0);
        Stack pikemenA = new Stack(UnitCatalog.PIKEMAN, 14, new Hex(14, 5), Side.DEFENDER, 0);
        BattleSetup setupA = new BattleSetup(List.of(halA), List.of(pikemenA), battlefield);
        solver.planRound(setupA);
        assertThat(solver.currentPlan().focusOf(Side.ATTACKER)).isSameAs(pikemenA);

        // Setup B → andere Stacks.
        Stack markB = new Stack(UnitCatalog.MARKSMAN, 9, new Hex(0, 5), Side.ATTACKER, 0);
        Stack pikemenB = new Stack(UnitCatalog.PIKEMAN, 14, new Hex(14, 5), Side.DEFENDER, 0);
        BattleSetup setupB = new BattleSetup(List.of(markB), List.of(pikemenB), battlefield);
        solver.planRound(setupB);

        // Focus zeigt auf den NEUEN Pikeman, nicht den aus Setup A.
        assertThat(solver.currentPlan().focusOf(Side.ATTACKER)).isSameAs(pikemenB);
        assertThat(solver.currentPlan().focusOf(Side.ATTACKER)).isNotSameAs(pikemenA);
    }

    @Test
    void focus_score_ability_bonus_outweighs_higher_raw_damage() {
        // Pikeman 28 → roher Threat 56 (avgDmg 2 × 28), kein Special-Bonus.
        // Vampire 5  → roher Threat 32.5 (avgDmg 6.5 × 5) + 30 (NO_RETALIATION) = 62.5.
        // Trotz ~40 % weniger rohem Damage gewinnt der Vampire wegen Ability-Gewichtung.
        Stack hal = new Stack(UnitCatalog.HALBERDIER, 14, new Hex(0, 5), Side.ATTACKER, 0);
        Stack pikemen = new Stack(UnitCatalog.PIKEMAN, 28, new Hex(14, 3), Side.DEFENDER, 0);
        Stack vampires = new Stack(UnitCatalog.VAMPIRE, 5, new Hex(14, 7), Side.DEFENDER, 1);
        BattleSetup setup = new BattleSetup(List.of(hal), List.of(pikemen, vampires), battlefield);

        solver.planRound(setup);

        assertThat(solver.currentPlan().focusOf(Side.ATTACKER)).isSameAs(vampires);
    }

    @Test
    void flyer_pickTarget_prioritizes_unguarded_enemy_shooter() {
        // Black Dragon (Flieger, speed 15) gegen Defender mit Marksman (T2 ranged) + Swordsman
        // (T4 melee). Standard-Focus-Score gibt Swordsman höhere Priorität (mehr Rohschaden).
        // Mit Flieger-Heuristik: Dragon priorisiert Marksman, weil dieser ungeschützt ist
        // (Adjacents frei + in Reichweite). Sonst chargt der Dragon den Tank und der Schütze
        // schießt ungestört weiter.
        Stack dragon = new Stack(UnitCatalog.BLACK_DRAGON, 1, new Hex(0, 5), Side.ATTACKER, 0);
        Stack marksman = new Stack(UnitCatalog.MARKSMAN, 5, new Hex(14, 0), Side.DEFENDER, 0);
        Stack swordsman = new Stack(UnitCatalog.SWORDSMAN, 10, new Hex(14, 5), Side.DEFENDER, 1);
        BattleSetup setup = new BattleSetup(List.of(dragon), List.of(marksman, swordsman),
                battlefield);
        solver.planRound(setup);

        Stack target = solver.pickTarget(dragon, setup.opponentsOf(dragon), battlefield);

        assertThat(target).isSameAs(marksman);
    }

    @Test
    void flyer_pickTarget_falls_back_when_shooter_is_fully_guarded() {
        // Marksman im Eck, Tank-Wall vor ihm (alle in-board Adjacents besetzt von Defendern).
        // Flieger-Heuristik findet keinen freien Lande-Hex → fallback zum default Focus
        // (hier: Swordsman wegen höherem Rohschaden).
        // Eck-Schütze auf (16,0), die drei Nachbarn auf dem Feld sind besetzt.
        Stack dragon = new Stack(UnitCatalog.BLACK_DRAGON, 1, new Hex(2, 5), Side.ATTACKER, 0);
        Stack marksman = new Stack(UnitCatalog.MARKSMAN, 5, new Hex(16, 0), Side.DEFENDER, 0);
        Stack tank1 = new Stack(UnitCatalog.HALBERDIER, 14, new Hex(15, 0), Side.DEFENDER, 1);
        Stack tank2 = new Stack(UnitCatalog.HALBERDIER, 14, new Hex(15, 1), Side.DEFENDER, 2);
        Stack tank3 = new Stack(UnitCatalog.HALBERDIER, 14, new Hex(16, 1), Side.DEFENDER, 3);
        Stack swordsman = new Stack(UnitCatalog.SWORDSMAN, 10, new Hex(8, 5), Side.DEFENDER, 4);
        BattleSetup setup = new BattleSetup(List.of(dragon),
                List.of(marksman, tank1, tank2, tank3, swordsman), battlefield);
        solver.planRound(setup);

        Stack target = solver.pickTarget(dragon, setup.opponentsOf(dragon), battlefield);

        // Marksman komplett umstellt → Heuristik überspringt ihn → Default-Focus
        // (Swordsman, weil höchster threat-Score auf der Defender-Seite ohne Marksman).
        assertThat(target).isNotSameAs(marksman);
    }

    @Test
    void flyer_picks_free_adjacent_when_straight_line_to_shooter_is_blocked() {
        // Angel (ATTACKER, FLYING, speed 12, einfeldig) gegen einen Eck-Schützen (Marksman auf
        // (16,0)). Defender baut Tank-Wall vor dem Schützen — Halberdier auf (15,0),
        // Swordsman auf (15,1). Nur (16,1) bleibt als freier Adjacent.
        //
        // Ohne Flieger-Heuristik: Greedy.moveToward straight-line landet auf einem besetzten
        // Hex, Engine erkennt Belegung → Wait. Mit Heuristik: findFlyerLanding wählt (16,1).
        Stack dragon = new Stack(UnitCatalog.ANGEL, 1, new Hex(6, 5), Side.ATTACKER, 0);
        Stack tank1 = new Stack(UnitCatalog.HALBERDIER, 14, new Hex(15, 0), Side.DEFENDER, 0);
        Stack tank2 = new Stack(UnitCatalog.SWORDSMAN, 10, new Hex(15, 1), Side.DEFENDER, 1);
        Stack marksman = new Stack(UnitCatalog.MARKSMAN, 9, new Hex(16, 0), Side.DEFENDER, 2);
        BattleSetup setup = new BattleSetup(List.of(dragon),
                List.of(tank1, tank2, marksman), battlefield);
        solver.planRound(setup);

        Action act = solver.decide(dragon, marksman, battlefield);

        assertThat(act).isInstanceOf(Action.MoveAndMelee.class);
        Action.MoveAndMelee mm = (Action.MoveAndMelee) act;
        assertThat(mm.destination()).isEqualTo(new Hex(16, 1));
        assertThat(mm.target()).isSameAs(marksman);
    }

    @Test
    void a_two_hex_flyer_finds_no_room_beside_the_cornered_shooter() {
        // Wie oben, aber ein Black Dragon: auf (16,1) läge sein Heck auf (15,1), dort steht der
        // Swordsman. Ein anderes Feld, an dem nur das Heck anläge, gibt es in der Ecke nicht.
        Stack dragon = new Stack(UnitCatalog.BLACK_DRAGON, 1, new Hex(2, 5), Side.ATTACKER, 0);
        Stack tank1 = new Stack(UnitCatalog.HALBERDIER, 14, new Hex(15, 0), Side.DEFENDER, 0);
        Stack tank2 = new Stack(UnitCatalog.SWORDSMAN, 10, new Hex(15, 1), Side.DEFENDER, 1);
        Stack marksman = new Stack(UnitCatalog.MARKSMAN, 9, new Hex(16, 0), Side.DEFENDER, 2);
        BattleSetup setup = new BattleSetup(List.of(dragon), List.of(tank1, tank2, marksman), battlefield);
        solver.planRound(setup);

        assertThat(setup.attackHeads(dragon, marksman)).isEmpty();
        assertThat(solver.decide(dragon, marksman, battlefield)).isNotInstanceOf(Action.MoveAndMelee.class);
    }

    @Test
    void the_safe_approach_is_inherited_from_greedy_and_can_be_switched_off() {
        // Pikeman in der Wait-Phase, Gegner-Pikeman noch am Zug: mit der Regel bleibt er auf
        // (3,5) außer Reichweite, ohne zieht er voll nach (4,5).
        assertThat(lateApproach(new StrategicAutoSolver(true, true))).isEqualTo(new Hex(3, 5));
        assertThat(lateApproach(new StrategicAutoSolver(true, false))).isEqualTo(new Hex(4, 5));
    }

    private static Hex lateApproach(StrategicAutoSolver solver) {
        Stack active = new Stack(UnitCatalog.PIKEMAN, 10, new Hex(0, 5), Side.ATTACKER, 0);
        active.markWaited();
        Stack enemy = new Stack(UnitCatalog.PIKEMAN, 10, new Hex(9, 5), Side.DEFENDER, 0);
        solver.planRound(new BattleSetup(List.of(active), List.of(enemy), Battlefield.STANDARD));
        Action action = solver.decide(active, enemy, Battlefield.STANDARD);
        return ((Action.Move) action).destination();
    }
}
