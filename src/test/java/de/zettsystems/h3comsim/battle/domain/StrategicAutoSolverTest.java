package de.zettsystems.h3comsim.battle.domain;

import de.zettsystems.h3comsim.battle.domain.events.Side;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class StrategicAutoSolverTest {

    private final StrategicAutoSolver solver = new StrategicAutoSolver();
    private final Battlefield battlefield = Battlefield.STANDARD;

    @Test
    void ranged_dominant_side_protects_shooters_via_stance() {
        // Attacker: 1 Halberdier (Tank) + 1 Marksman + 1 Zealot (zwei Schützen → Ranged-Power).
        // Defender: 1 Pikeman (Melee only).
        Stack hal = new Stack(UnitCatalog.HALBERDIER, 14, new Hex(0, 4), Side.ATTACKER, 0);
        Stack mark = new Stack(UnitCatalog.MARKSMAN, 9, new Hex(0, 5), Side.ATTACKER, 1);
        Stack zealot = new Stack(UnitCatalog.ZEALOT, 3, new Hex(0, 6), Side.ATTACKER, 2);
        Stack pikeman = new Stack(UnitCatalog.PIKEMAN, 14, new Hex(14, 5), Side.DEFENDER, 0);
        BattleSetup setup = new BattleSetup(List.of(hal, mark, zealot), List.of(pikeman), battlefield);

        solver.planRound(setup);

        assertThat(solver.currentPlan().stanceOf(Side.ATTACKER)).isEqualTo(TeamStance.RANGED_DOMINANT);
        assertThat(solver.currentPlan().protectedShooters()).containsExactlyInAnyOrder(mark, zealot);
    }

    @Test
    void melee_dominant_side_charges_via_stance() {
        // Attacker: 3 Melee-Stacks ohne Schützen. Defender: 1 schwacher Schütze.
        Stack hal = new Stack(UnitCatalog.HALBERDIER, 14, new Hex(0, 4), Side.ATTACKER, 0);
        Stack crusader = new Stack(UnitCatalog.CRUSADER, 4, new Hex(0, 5), Side.ATTACKER, 1);
        Stack archAngel = new Stack(UnitCatalog.ARCH_ANGEL, 1, new Hex(0, 6), Side.ATTACKER, 2);
        Stack marksman = new Stack(UnitCatalog.MARKSMAN, 1, new Hex(14, 5), Side.DEFENDER, 0);
        BattleSetup setup = new BattleSetup(List.of(hal, crusader, archAngel), List.of(marksman), battlefield);

        solver.planRound(setup);

        assertThat(solver.currentPlan().stanceOf(Side.ATTACKER)).isEqualTo(TeamStance.MELEE_DOMINANT);
        // Defender mit einsamem Marksman ist seinerseits RANGED_DOMINANT — der Set enthält ihn.
        // Wichtig ist nur: Attacker hat keine Schützen drin (er chargt geschlossen).
        assertThat(solver.currentPlan().protectedShooters())
                .noneMatch(s -> s.side() == Side.ATTACKER);
    }

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
    void ranged_dominant_melee_tank_moves_towards_shooter_lane() {
        // Attacker: Marksman (Schütze) hinten + Halberdier (Tank, kann sich bewegen).
        // Defender: schneller Pikeman, der den Marksman bedroht.
        Stack hal = new Stack(UnitCatalog.HALBERDIER, 14, new Hex(3, 5), Side.ATTACKER, 0);
        Stack mark = new Stack(UnitCatalog.MARKSMAN, 9, new Hex(0, 5), Side.ATTACKER, 1);
        Stack zealot = new Stack(UnitCatalog.ZEALOT, 3, new Hex(0, 6), Side.ATTACKER, 2);
        Stack threat = new Stack(UnitCatalog.PIKEMAN, 14, new Hex(8, 5), Side.DEFENDER, 0);
        BattleSetup setup = new BattleSetup(List.of(hal, mark, zealot), List.of(threat), battlefield);
        solver.planRound(setup);

        Action action = solver.decide(hal, threat, battlefield);

        // Tank soll sich bewegen statt direkt anzugreifen — Move-Aktion, kein MoveAndMelee.
        assertThat(action).isInstanceOf(Action.Move.class);
    }
}
