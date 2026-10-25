package de.zettsystems.h3comsim.battle.domain;

import de.zettsystems.h3comsim.battle.domain.events.BattleEvent;
import de.zettsystems.h3comsim.battle.domain.events.ListEventCollector;
import org.junit.jupiter.api.Test;

import java.util.Random;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class RangedPenaltiesTest {

    /**
     * Dummy mit Speed 0 und HP 1: lebt genau einen Schuss lang. So lässt sich der erste
     * Shoot-Event-Damage zwischen verschiedenen Setups vergleichen, ohne dass die zweite
     * Sim-Schleife weiter laufen kann.
     */
    private static final Unit STATIONARY_TARGET = new Unit(
            "Sandbag",
            new Stats(1, 1, 1, 0),
            new Combat(0, 0, 0, AttackType.HAND_TO_HAND),
            Movement.GROUND,
            0,
            Faction.NEUTRAL,
            1,
            false,
            Set.of());

    // Toleranz ±3: Integer-Division beim Halbieren kann pro Stufe ~1-2 Schaden "verschlucken",
    // verstärkt durch den anschließenden ×1.x-Bonus aus dem Attack/Defense-Diff.
    @Test
    void distance_penalty_halves_damage_above_ten_hexes() {
        int closeDamage = firstShotDamage(new Hex(8, 5), Battlefield.STANDARD); // distance 6
        int farDamage = firstShotDamage(new Hex(0, 5), Battlefield.STANDARD);  // distance 14
        assertThat(farDamage).isBetween(closeDamage / 2 - 3, closeDamage / 2 + 3);
    }

    /** Die Hindernis-Strafe gilt nur für Schüsse über Stadtmauern, nicht für Felsen auf dem Feld. */
    @Test
    void an_ordinary_obstacle_in_the_line_costs_nothing() {
        int clearShot = firstShotDamage(new Hex(8, 5), Battlefield.STANDARD); // distance 6, clear
        Battlefield blocked = Battlefield.STANDARD.withObstacles(Set.of(new Hex(11, 5)));
        assertThat(firstShotDamage(new Hex(8, 5), blocked)).isEqualTo(clearShot);
    }

    private static int firstShotDamage(Hex attackerPos, Battlefield battlefield) {
        BattleSetup setup = new BattleSetup(
                UnitCatalog.GRAND_ELF, 5,
                STATIONARY_TARGET, 1,
                battlefield, attackerPos, new Hex(14, 5));
        ListEventCollector collector = new ListEventCollector();
        new Battle(new Random(42L), new GreedyAutoSolver(), collector).simulate(setup);
        return collector.events().stream()
                .filter(BattleEvent.Shoot.class::isInstance)
                .map(BattleEvent.Shoot.class::cast)
                .mapToInt(BattleEvent.Shoot::damage)
                .findFirst()
                .orElseThrow();
    }
}
