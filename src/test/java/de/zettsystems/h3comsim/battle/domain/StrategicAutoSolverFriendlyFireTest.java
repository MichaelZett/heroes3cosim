package de.zettsystems.h3comsim.battle.domain;

import de.zettsystems.h3comsim.battle.domain.events.Side;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Charakterisierungstests für die Friendly-Fire-Awareness der AoE-Targeting-Heuristiken.
 *
 * <p>Die Engine verteilt Splash an beide Seiten ({@code Battle.findStackAt} iteriert Attacker-
 * und Defender-Stacks). Ein Solver, der nur Gegner-Cluster zählt, optimiert deshalb auf
 * Eigentore. Diese Tests nageln fest, dass eigene Stacks im Splash-Radius das Ziel abwerten.
 *
 * <p>Aufbau aller Splash-Szenarien: zwei gleichwertige, zueinander adjazente Gegner
 * {@code enemyNearOwn} und {@code enemyClear}. Nur der erste hat eigene Stacks im Radius.
 * Die alte Heuristik zählte für beide genau einen Gegner-Nachbarn, blieb also im Gleichstand
 * und nahm den zuerst geprüften — {@code enemyNearOwn}. Mit Friendly-Fire-Bewertung kippt die
 * Wahl auf {@code enemyClear}.
 *
 * <p>Hex-Nachbar-Reihenfolge (relevant für den Zwei-Kollateral-Deckel von SPLASH_SHOT):
 * {@code (q+1,r), (q+1,r-1), (q,r-1), (q-1,r), (q-1,r+1), (q,r+1)}. Die eigenen Stacks liegen
 * bewusst auf den beiden ERSTEN Nachbar-Hexen, belegen den Deckel also vollständig.
 */
class StrategicAutoSolverFriendlyFireTest {

    private static final Hex NEAR_OWN = new Hex(10, 5);
    private static final Hex CLEAR = new Hex(9, 6);
    private static final Hex OWN_FIRST_NEIGHBOUR = new Hex(11, 5);
    private static final Hex OWN_SECOND_NEIGHBOUR = new Hex(11, 4);
    private static final Hex SHOOTER_SPOT = new Hex(0, 5);

    private final StrategicAutoSolver solver = new StrategicAutoSolver();
    private final Battlefield battlefield = Battlefield.STANDARD;

    @Test
    void splash_shooter_prefers_the_target_without_own_stacks_in_radius() {
        Stack magog = new Stack(UnitCatalog.MAGOG, 12, SHOOTER_SPOT, Side.ATTACKER, 0);
        Stack ownImpA = new Stack(UnitCatalog.IMP, 15, OWN_FIRST_NEIGHBOUR, Side.ATTACKER, 1);
        Stack ownImpB = new Stack(UnitCatalog.IMP, 15, OWN_SECOND_NEIGHBOUR, Side.ATTACKER, 2);

        Stack enemyNearOwn = new Stack(UnitCatalog.PIKEMAN, 14, NEAR_OWN, Side.DEFENDER, 0);
        Stack enemyClear = new Stack(UnitCatalog.PIKEMAN, 14, CLEAR, Side.DEFENDER, 1);

        List<Stack> defenders = List.of(enemyNearOwn, enemyClear);
        BattleSetup setup = new BattleSetup(List.of(magog, ownImpA, ownImpB), defenders, battlefield);
        solver.planRound(setup);

        // enemyNearOwn: Deckel von zwei Kollateralen komplett durch eigene Imps belegt → −2.
        // enemyClear: ein Gegner-Nachbar, kein eigener → +1.
        assertThat(solver.pickTarget(magog, defenders, battlefield)).isSameAs(enemyClear);
    }

    @Test
    void splash_shooter_falls_through_to_focus_fire_when_no_splash_has_positive_net_value() {
        Stack magog = new Stack(UnitCatalog.MAGOG, 12, SHOOTER_SPOT, Side.ATTACKER, 0);
        Stack ownImpA = new Stack(UnitCatalog.IMP, 15, OWN_FIRST_NEIGHBOUR, Side.ATTACKER, 1);
        Stack ownImpB = new Stack(UnitCatalog.IMP, 15, OWN_SECOND_NEIGHBOUR, Side.ATTACKER, 2);
        // Dritter eigener Stack neben enemyClear — damit ist auch dort der Netto-Splash 0.
        Stack ownImpC = new Stack(UnitCatalog.IMP, 15, new Hex(8, 7), Side.ATTACKER, 3);

        Stack enemyNearOwn = new Stack(UnitCatalog.PIKEMAN, 14, NEAR_OWN, Side.DEFENDER, 0);
        Stack enemyClear = new Stack(UnitCatalog.PIKEMAN, 14, CLEAR, Side.DEFENDER, 1);
        Stack archAngel = new Stack(UnitCatalog.ARCH_ANGEL, 2, new Hex(13, 9), Side.DEFENDER, 2);

        List<Stack> defenders = List.of(enemyNearOwn, enemyClear, archAngel);
        BattleSetup setup = new BattleSetup(
                List.of(magog, ownImpA, ownImpB, ownImpC), defenders, battlefield);
        solver.planRound(setup);

        // Kein Ziel mit positivem Netto-Splash → die AoE-Heuristik steigt aus und das
        // Focus-Fire des Team-Plans übernimmt: gefährlichster Gegner statt Eigentor-Cluster.
        assertThat(solver.pickTarget(magog, defenders, battlefield)).isSameAs(archAngel);
    }

    @Test
    void death_cloud_ignores_own_undead_stacks_in_radius() {
        // DEATH_CLOUD verschont Untote — eigene Skelette dürfen das Ziel nicht abwerten.
        // Beide Ziele stehen damit bei +1 und die Wahl bleibt beim zuerst geprüften.
        Stack lich = new Stack(UnitCatalog.LICH, 6, SHOOTER_SPOT, Side.ATTACKER, 0);
        Stack ownSkeletonA = new Stack(UnitCatalog.SKELETON, 30, OWN_FIRST_NEIGHBOUR, Side.ATTACKER, 1);
        Stack ownSkeletonB = new Stack(UnitCatalog.SKELETON, 30, OWN_SECOND_NEIGHBOUR, Side.ATTACKER, 2);

        Stack enemyNearOwn = new Stack(UnitCatalog.PIKEMAN, 14, NEAR_OWN, Side.DEFENDER, 0);
        Stack enemyClear = new Stack(UnitCatalog.PIKEMAN, 14, CLEAR, Side.DEFENDER, 1);

        List<Stack> defenders = List.of(enemyNearOwn, enemyClear);
        BattleSetup setup = new BattleSetup(
                List.of(lich, ownSkeletonA, ownSkeletonB), defenders, battlefield);
        solver.planRound(setup);

        assertThat(solver.pickTarget(lich, defenders, battlefield)).isSameAs(enemyNearOwn);
    }

    @Test
    void death_cloud_avoids_own_living_stacks_in_radius() {
        // Gegenprobe: lebende eigene Stacks im Radius trifft die Death Cloud sehr wohl.
        // Identisches Layout wie oben, nur Pikemen statt Skelette → die Wahl muss kippen.
        Stack lich = new Stack(UnitCatalog.LICH, 6, SHOOTER_SPOT, Side.ATTACKER, 0);
        Stack ownPikemanA = new Stack(UnitCatalog.PIKEMAN, 14, OWN_FIRST_NEIGHBOUR, Side.ATTACKER, 1);
        Stack ownPikemanB = new Stack(UnitCatalog.PIKEMAN, 14, OWN_SECOND_NEIGHBOUR, Side.ATTACKER, 2);

        Stack enemyNearOwn = new Stack(UnitCatalog.PIKEMAN, 14, NEAR_OWN, Side.DEFENDER, 0);
        Stack enemyClear = new Stack(UnitCatalog.PIKEMAN, 14, CLEAR, Side.DEFENDER, 1);

        List<Stack> defenders = List.of(enemyNearOwn, enemyClear);
        BattleSetup setup = new BattleSetup(
                List.of(lich, ownPikemanA, ownPikemanB), defenders, battlefield);
        solver.planRound(setup);

        assertThat(solver.pickTarget(lich, defenders, battlefield)).isSameAs(enemyClear);
    }
}
