package de.zettsystems.h3comsim.battle.domain;

import de.zettsystems.h3comsim.battle.domain.events.BattleEvent;
import de.zettsystems.h3comsim.battle.domain.events.ListEventCollector;
import de.zettsystems.h3comsim.battle.domain.events.Side;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/** Stadtmauern, Tor und Graben auf dem Feld (VCMI-Feldnummern, heroes.thelazy.net). */
class SiegeTest {

    private static final Battlefield FIELD = Battlefield.STANDARD;

    @Test
    void game_hex_numbers_map_onto_the_simulator_field() {
        // Die Aufstellungsfelder des Spiels: Spalte 1 bzw. 15 der Mittelreihe.
        assertThat(SiegeLayout.h3(86)).isEqualTo(new Hex(0, 5));
        assertThat(SiegeLayout.h3(100)).isEqualTo(new Hex(14, 5));
        // Ballista des Angreifers (52) und Munitionswagen des Verteidigers (32).
        assertThat(SiegeLayout.h3(52)).isEqualTo(new Hex(FIELD.leftmostQ(3), 3));
        assertThat(SiegeLayout.h3(32)).isEqualTo(new Hex(FIELD.rightmostQ(1), 1));
        // Gerade Reihe, Spalte 1: die linke Randzelle.
        assertThat(SiegeLayout.h3(69)).isEqualTo(new Hex(FIELD.leftmostQ(4), 4));
        assertThat(SiegeLayout.PERMANENT_WALL).allSatisfy(hex -> assertThat(FIELD.contains(hex)).isTrue());
        assertThat(SiegeLayout.MOAT).hasSize(10).allSatisfy(hex -> assertThat(SiegeLayout.isOutside(hex)).isTrue());
        assertThat(SiegeLayout.isOutside(new Hex(14, 5))).isFalse();
    }

    @Test
    void walls_hold_back_the_attackers_on_foot_but_not_the_defenders_at_the_gate() {
        BattleSetup setup = siege(Fortification.FORT,
                new Stack(UnitCatalog.PIKEMAN, 10, new Hex(0, 5), Side.ATTACKER, 0),
                new Stack(UnitCatalog.PIKEMAN, 10, new Hex(14, 5), Side.DEFENDER, 0));
        Battlefield attackerView = setup.battlefieldFor(Side.ATTACKER);
        Battlefield defenderView = setup.battlefieldFor(Side.DEFENDER);

        assertThat(attackerView.isPassable(SiegeLayout.GATE_INNER)).isFalse();
        assertThat(defenderView.isPassable(SiegeLayout.GATE_INNER)).isTrue();
        assertThat(defenderView.isPassable(WallPart.UPPER_WALL.hex())).isFalse();
        assertThat(attackerView.findPath(new Hex(0, 5), new Hex(14, 5), Movement.GROUND)).isEmpty();
        assertThat(defenderView.findPath(new Hex(14, 5), new Hex(0, 5), Movement.GROUND)).isNotEmpty();
        assertThat(setup.baseBattlefield().obstacles()).as("Mauern sind keine Hindernisse").isEmpty();

        Objects.requireNonNull(setup.siege()).damage(WallPart.MID_UPPER_WALL, 2);
        assertThat(setup.battlefieldFor(Side.ATTACKER).findPath(new Hex(0, 5), new Hex(14, 5), Movement.GROUND))
                .as("durch die Bresche").isNotEmpty();
    }

    @Test
    void a_shot_from_outside_over_a_standing_wall_loses_half() {
        int open = firstShotDamage(UnitCatalog.GRAND_ELF, null);
        int walled = firstShotDamage(UnitCatalog.GRAND_ELF, Fortification.FORT);
        int magi = firstShotDamage(UnitCatalog.MAGI, Fortification.FORT);
        int magiOpen = firstShotDamage(UnitCatalog.MAGI, null);

        assertThat(walled).isBetween(open / 2 - 3, open / 2 + 3);
        assertThat(magi).as("Magi ignorieren die Mauer").isEqualTo(magiOpen);
    }

    @Test
    void the_defender_stays_behind_the_walls_without_tactics_or_ballista() {
        Hero tactician = HeroCatalog.SORSHA.withBuild(0, 0, Map.of(SecondarySkill.TACTICS, SkillLevel.EXPERT));
        Stack defender = new Stack(UnitCatalog.PIKEMAN, 10, new Hex(14, 5), Side.DEFENDER, 0);
        BattleSetup setup = new BattleSetup(List.of(new Stack(UnitCatalog.PIKEMAN, 10, new Hex(0, 5), Side.ATTACKER, 0)),
                List.of(defender), FIELD, null, CollectedBonus.NONE, tactician, CollectedBonus.NONE,
                new Siege(Fortification.CASTLE, Faction.CASTLE))
                .equip(Side.DEFENDER, Set.of(WarMachine.BALLISTA, WarMachine.FIRST_AID_TENT));

        assertThat(setup.tacticsOf(Side.DEFENDER)).isZero();
        assertThat(defender.position()).isEqualTo(new Hex(14, 5));
        assertThat(setup.warMachinesOf(Side.DEFENDER)).containsExactly(WarMachine.FIRST_AID_TENT);
        assertThat(setup.siege().moat()).isEqualTo(SiegeLayout.MOAT);
        assertThat(new Siege(Fortification.CITADEL, Faction.FORTRESS).moat()).hasSize(21);
        assertThat(new Siege(Fortification.FORT, Faction.CASTLE).moat()).isEmpty();

        ListEventCollector collector = new ListEventCollector();
        new Battle(new Random(3), new GreedyAutoSolver(), collector).simulate(setup);
        BattleEvent.BattleStart start = (BattleEvent.BattleStart) collector.events().getFirst();
        assertThat(start.siege()).isNotNull();
        assertThat(start.siege().parts()).hasSize(WallPart.values().length);
        assertThat(start.siege().permanentWall()).hasSize(6);
    }

    @Test
    void castle_walls_take_three_hits_and_the_gate_two() {
        Siege castle = new Siege(Fortification.CASTLE, Faction.CASTLE);
        assertThat(castle.health(WallPart.UPPER_WALL)).isEqualTo(3);
        assertThat(castle.health(WallPart.GATE)).isEqualTo(2);
        assertThat(castle.health(WallPart.KEEP)).isEqualTo(2);
        assertThat(new Siege(Fortification.FORT, Faction.CASTLE).stands(WallPart.KEEP)).isFalse();
        assertThat(castle.damage(WallPart.GATE, 1)).isFalse();
        assertThat(castle.damage(WallPart.GATE, 1)).isTrue();
        assertThat(castle.damage(WallPart.GATE, 1)).as("schon zerstört").isFalse();
    }

    private static BattleSetup siege(Fortification fortification, Stack attacker, Stack defender) {
        return new BattleSetup(List.of(attacker), List.of(defender), FIELD, null, CollectedBonus.NONE, null,
                CollectedBonus.NONE, new Siege(fortification, Faction.CASTLE));
    }

    /** Ein Schütze draußen in der Mittelreihe auf ein Ziel hinter der Mauer, oberhalb des Tors. */
    private static int firstShotDamage(Unit shooter, Fortification fortification) {
        Stack attacker = new Stack(shooter, 5, new Hex(2, 3), Side.ATTACKER, 0);
        Stack target = new Stack(UnitCatalog.PIKEMAN, 100, new Hex(14, 3), Side.DEFENDER, 0);
        BattleSetup setup = fortification == null
                ? new BattleSetup(List.of(attacker), List.of(target), FIELD)
                : siege(fortification, attacker, target);
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
