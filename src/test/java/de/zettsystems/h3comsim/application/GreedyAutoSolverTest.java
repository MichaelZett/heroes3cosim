package de.zettsystems.h3comsim.application;

import de.zettsystems.h3comsim.domain.AttackType;
import de.zettsystems.h3comsim.domain.Battlefield;
import de.zettsystems.h3comsim.domain.Combat;
import de.zettsystems.h3comsim.domain.Faction;
import de.zettsystems.h3comsim.domain.Hex;
import de.zettsystems.h3comsim.domain.Movement;
import de.zettsystems.h3comsim.domain.Stack;
import de.zettsystems.h3comsim.domain.Stats;
import de.zettsystems.h3comsim.domain.Unit;
import de.zettsystems.h3comsim.domain.UnitCatalog;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class GreedyAutoSolverTest {

    private final GreedyAutoSolver solver = new GreedyAutoSolver();
    private final Battlefield battlefield = Battlefield.STANDARD;

    @Test
    void melee_when_opponent_is_adjacent() {
        Stack active = new Stack(UnitCatalog.PIKEMAN, 10, new Hex(4, 5));
        Stack opponent = new Stack(UnitCatalog.PIKEMAN, 10, new Hex(5, 5));

        Action action = solver.decide(active, opponent, battlefield);

        assertThat(action).isInstanceOfSatisfying(Action.Melee.class,
                melee -> assertThat(melee.target()).isSameAs(opponent));
    }

    @Test
    void shooter_with_shots_left_fires_instead_of_moving() {
        Stack shooter = new Stack(UnitCatalog.GRAND_ELF, 10, new Hex(0, 5));
        Stack opponent = new Stack(UnitCatalog.PIKEMAN, 10, new Hex(7, 5));

        Action action = solver.decide(shooter, opponent, battlefield);

        assertThat(action).isInstanceOfSatisfying(Action.Shoot.class,
                shoot -> assertThat(shoot.target()).isSameAs(opponent));
    }

    @Test
    void shooter_without_shots_falls_back_to_movement() {
        Stack shooter = new Stack(UnitCatalog.GRAND_ELF, 10, new Hex(0, 5));
        Stack opponent = new Stack(UnitCatalog.PIKEMAN, 10, new Hex(14, 5));
        while (shooter.shotsRemaining() > 0) {
            shooter.useShot();
        }

        Action action = solver.decide(shooter, opponent, battlefield);

        assertThat(action).isInstanceOf(Action.Move.class);
    }

    @Test
    void movement_into_melee_range_is_combined_with_attack() {
        Stack active = new Stack(UnitCatalog.PIKEMAN, 10, new Hex(0, 5));
        Stack opponent = new Stack(UnitCatalog.PIKEMAN, 10, new Hex(5, 5));

        Action action = solver.decide(active, opponent, battlefield);

        assertThat(action).isInstanceOfSatisfying(Action.MoveAndMelee.class, mm -> {
            assertThat(mm.target()).isSameAs(opponent);
            assertThat(mm.destination().distanceTo(opponent.position())).isEqualTo(1);
        });
    }

    @Test
    void plain_move_when_arrival_is_still_out_of_reach() {
        Stack active = new Stack(UnitCatalog.PIKEMAN, 10, new Hex(0, 5));
        Stack opponent = new Stack(UnitCatalog.PIKEMAN, 10, new Hex(14, 5));

        Action action = solver.decide(active, opponent, battlefield);

        assertThat(action).isInstanceOfSatisfying(Action.Move.class,
                move -> assertThat(move.destination().distanceTo(opponent.position())).isGreaterThan(1));
    }

    @Test
    void faster_shooter_kites_when_engagement_is_imminent_next_turn() {
        // Marksman speed 6 vs Pikeman speed 4. Pikeman bei (2,5) → Distanz 2, Pikeman würde
        // nächste Runde adjacent sein. Marksman ist schneller → kite, statt zu schießen.
        Stack shooter = new Stack(UnitCatalog.MARKSMAN, 10, new Hex(0, 5));
        Stack opponent = new Stack(UnitCatalog.PIKEMAN, 10, new Hex(2, 5));

        Action action = solver.decide(shooter, opponent, battlefield);

        assertThat(action).isInstanceOf(Action.Move.class);
        Action.Move move = (Action.Move) action;
        // Nach der Kite-Bewegung muss die Distanz wieder > opponent.speed sein.
        assertThat(move.destination().distanceTo(opponent.position()))
                .isGreaterThan(opponent.getSpeed());
    }

    @Test
    void shooter_shoots_when_incoming_damage_is_not_lethal() {
        // Cyclops (HP 70, Speed 6) vs Imp (HP 4, melee 1-2, Speed 5) auf Distanz 2.
        // Cyclops ist schneller, Engagement droht — aber 10 Imps machen avg 15 Damage,
        // das one-shottet die 70-HP-Top-Cyclops nicht. → schießen statt kiten.
        Stack shooter = new Stack(UnitCatalog.CYCLOPS, 10, new Hex(0, 5));
        Stack opponent = new Stack(UnitCatalog.IMP, 10, new Hex(2, 5));

        Action action = solver.decide(shooter, opponent, battlefield);

        assertThat(action).isInstanceOf(Action.Shoot.class);
    }

    @Test
    void slower_shooter_shoots_even_when_engagement_is_imminent() {
        // Wood Elf (speed 6) wäre langsamer als ein speed-7-Melee. Wenn Kiten nicht reicht,
        // soll der Schütze trotzdem schießen, statt nutzlos zu fliehen.
        Stack shooter = new Stack(UnitCatalog.WOOD_ELF, 10, new Hex(0, 5));
        // Cavalier hat Speed 7 — schneller als Wood Elf.
        Stack opponent = new Stack(UnitCatalog.CAVALIER, 10, new Hex(2, 5));

        Action action = solver.decide(shooter, opponent, battlefield);

        assertThat(action).isInstanceOf(Action.Shoot.class);
    }

    @Test
    void waits_when_unit_cannot_advance_and_cannot_shoot() {
        Unit immobile = new Unit(
                "Test Immobile",
                new Stats(1, 1, 1, 0),
                new Combat(1, 1, 0, AttackType.HAND_TO_HAND),
                Movement.GROUND,
                0,
                Faction.NEUTRAL,
                1,
                false,
                Set.of());
        Stack active = new Stack(immobile, 1, new Hex(0, 5));
        Stack opponent = new Stack(UnitCatalog.PIKEMAN, 10, new Hex(14, 5));

        Action action = solver.decide(active, opponent, battlefield);

        assertThat(action).isInstanceOf(Action.Wait.class);
    }
}
