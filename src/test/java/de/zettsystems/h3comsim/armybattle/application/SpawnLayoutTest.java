package de.zettsystems.h3comsim.armybattle.application;

import de.zettsystems.h3comsim.battle.domain.Hex;
import de.zettsystems.h3comsim.battle.domain.Unit;
import de.zettsystems.h3comsim.battle.domain.UnitCatalog;
import de.zettsystems.h3comsim.battle.domain.events.Side;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SpawnLayoutTest {

    @Test
    void seven_slot_layout_uses_rows_0_2_4_5_6_8_10() {
        Set<Integer> attackerRows = new HashSet<>();
        for (int slot = 0; slot < 7; slot++) {
            Hex pos = SpawnLayout.positionFor(Side.ATTACKER, slot, 7);
            assertThat(pos.q()).isZero();
            attackerRows.add(pos.r());
        }
        assertThat(attackerRows).containsExactlyInAnyOrder(0, 2, 4, 5, 6, 8, 10);
    }

    @Test
    void defender_spawns_on_column_14() {
        for (int slot = 0; slot < 7; slot++) {
            Hex pos = SpawnLayout.positionFor(Side.DEFENDER, slot, 7);
            assertThat(pos.q()).isEqualTo(14);
        }
    }

    @Test
    void smaller_armies_centre_around_row_5() {
        // 3 Stacks → mittlere Auswahl {4, 5, 6}.
        Hex slot0 = SpawnLayout.positionFor(Side.ATTACKER, 0, 3);
        Hex slot1 = SpawnLayout.positionFor(Side.ATTACKER, 1, 3);
        Hex slot2 = SpawnLayout.positionFor(Side.ATTACKER, 2, 3);
        assertThat(slot0.r()).isEqualTo(4);
        assertThat(slot1.r()).isEqualTo(5);
        assertThat(slot2.r()).isEqualTo(6);
    }

    @Test
    void single_stack_spawns_in_middle() {
        Hex single = SpawnLayout.positionFor(Side.ATTACKER, 0, 1);
        assertThat(single.r()).isEqualTo(5);
    }

    @Test
    void spawn_hexes_cover_both_sides() {
        Set<Hex> hexes = SpawnLayout.spawnHexesFor(7, 7);
        assertThat(hexes).hasSize(14);
        assertThat(hexes.stream().filter(h -> h.q() == 0).count()).isEqualTo(7);
        assertThat(hexes.stream().filter(h -> h.q() == 14).count()).isEqualTo(7);
    }

    @Test
    void out_of_range_slot_index_throws() {
        assertThatThrownBy(() -> SpawnLayout.positionFor(Side.ATTACKER, 7, 7))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SpawnLayout.positionFor(Side.ATTACKER, -1, 7))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void out_of_range_total_slots_throws() {
        assertThatThrownBy(() -> SpawnLayout.positionFor(Side.ATTACKER, 0, 8))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SpawnLayout.positionFor(Side.ATTACKER, 0, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void assign_positions_castle_army_matches_canonical_layout() {
        // Klassische Castle-Aufstellung: Schützen außen, Charge-Trio (Griffin/Champion/Angel)
        // in der Mitte, Tank-Frontline (Halberdier/Swordsman) dazwischen.
        List<Unit> units = List.of(
                UnitCatalog.HALBERDIER,
                UnitCatalog.MARKSMAN,
                UnitCatalog.GRIFFIN,
                UnitCatalog.SWORDSMAN,
                UnitCatalog.MONK,
                UnitCatalog.CHAMPION,
                UnitCatalog.ARCH_ANGEL);

        List<Hex> positions = SpawnLayout.assignPositions(Side.ATTACKER, units);

        Map<Unit, Integer> rowByUnit = new java.util.HashMap<>();
        for (int i = 0; i < units.size(); i++) {
            rowByUnit.put(units.get(i), positions.get(i).r());
        }
        // Schützen außen.
        assertThat(rowByUnit.get(UnitCatalog.MARKSMAN)).isEqualTo(0);
        assertThat(rowByUnit.get(UnitCatalog.MONK)).isEqualTo(10);
        // Schnellster Melee (Arch Angel, speed 18) zentriert.
        assertThat(rowByUnit.get(UnitCatalog.ARCH_ANGEL)).isEqualTo(5);
        // Charge-Trio um die Mitte (r=4 und r=6).
        assertThat(rowByUnit.get(UnitCatalog.CHAMPION)).isIn(4, 6);
        assertThat(rowByUnit.get(UnitCatalog.GRIFFIN)).isIn(4, 6);
        assertThat(rowByUnit.get(UnitCatalog.CHAMPION))
                .isNotEqualTo(rowByUnit.get(UnitCatalog.GRIFFIN));
        // Tank-Frontline (gleiche Speed=5, stabiler Sort → Halberdier vor Swordsman).
        assertThat(rowByUnit.get(UnitCatalog.HALBERDIER)).isIn(2, 8);
        assertThat(rowByUnit.get(UnitCatalog.SWORDSMAN)).isIn(2, 8);
        assertThat(rowByUnit.get(UnitCatalog.HALBERDIER))
                .isNotEqualTo(rowByUnit.get(UnitCatalog.SWORDSMAN));
    }

    @Test
    void assign_positions_input_order_is_preserved() {
        // Output-Liste muss elementweise zur Input-Liste passen (Service ruft danach pairwise auf).
        List<Unit> units = List.of(
                UnitCatalog.ARCH_ANGEL,
                UnitCatalog.MARKSMAN,
                UnitCatalog.HALBERDIER);

        List<Hex> positions = SpawnLayout.assignPositions(Side.ATTACKER, units);

        assertThat(positions).hasSize(3);
        // Marksman ist der einzige Schütze → äußerste Reihe (r=4 oder r=6 — bei N=3 sind das
        // die Außen-Plätze in der zentrierten Auswahl {4, 5, 6}).
        assertThat(positions.get(1).r()).isIn(4, 6);
        // Arch Angel (speed 18) ist schnellster Melee → Zentrum.
        assertThat(positions.get(0).r()).isEqualTo(5);
    }

    @Test
    void assign_positions_pure_melee_sorts_fastest_to_center() {
        // Reine Melee-Armee Stronghold: Wolf Rider(s=6), Behemoth(s=6), Ancient Behemoth(s=9),
        // Cyclops(ranged, s=6). Aber für reines Melee: nehme nur Melees.
        // Speeds: Goblin 5, Wolf Rider 6, Ogre 4, Roc 7, Behemoth 6, Ancient Behemoth 9.
        List<Unit> units = List.of(
                UnitCatalog.GOBLIN,
                UnitCatalog.WOLF_RIDER,
                UnitCatalog.OGRE,
                UnitCatalog.ROC,
                UnitCatalog.BEHEMOTH,
                UnitCatalog.ANCIENT_BEHEMOTH);

        List<Hex> positions = SpawnLayout.assignPositions(Side.ATTACKER, units);

        Map<Unit, Integer> rowByUnit = new java.util.HashMap<>();
        for (int i = 0; i < units.size(); i++) {
            rowByUnit.put(units.get(i), positions.get(i).r());
        }
        // N=6 → rows {2,4,5,6,8} + eine von {0,10}. rowsFor(6) = subList(0.5..6.5) → {0,2,4,5,6,8}.
        // Zentrum (r=5) → schnellster (Ancient Behemoth speed 9).
        assertThat(rowByUnit.get(UnitCatalog.ANCIENT_BEHEMOTH)).isEqualTo(5);
        // Roc (speed 7) → nächstinnerster Platz (r=4 oder r=6).
        assertThat(rowByUnit.get(UnitCatalog.ROC)).isIn(4, 6);
        // Langsamster (Ogre speed 4) → außen (r=0).
        assertThat(rowByUnit.get(UnitCatalog.OGRE)).isEqualTo(0);
    }

    @Test
    void assign_positions_tower_puts_fragile_shooters_in_corners() {
        // Tower-Preset T7→T1: Titan (T7, HP 300), Arch Magi (T4, HP 30), Master Gremlin
        // (T1, HP 4) sind die Schützen. Heuristik: schwächster in die Ecke.
        List<Unit> units = List.of(
                UnitCatalog.TITAN,
                UnitCatalog.NAGA_QUEEN,
                UnitCatalog.MASTER_GENIE,
                UnitCatalog.ARCH_MAGI,
                UnitCatalog.IRON_GOLEM,
                UnitCatalog.OBSIDIAN_GARGOYLE,
                UnitCatalog.MASTER_GREMLIN);

        List<Hex> positions = SpawnLayout.assignPositions(Side.ATTACKER, units);

        Map<Unit, Integer> rowByUnit = new java.util.HashMap<>();
        for (int i = 0; i < units.size(); i++) {
            rowByUnit.put(units.get(i), positions.get(i).r());
        }
        // Schwächster Schütze (Master Gremlin HP 4) → äußerste Ecke r=0.
        assertThat(rowByUnit.get(UnitCatalog.MASTER_GREMLIN)).isEqualTo(0);
        // Mittlerer Schütze (Arch Magi HP 30) → zweite Ecke r=10.
        assertThat(rowByUnit.get(UnitCatalog.ARCH_MAGI)).isEqualTo(10);
        // Robustester Schütze (Titan HP 300) wandert ins Schützen-Inland (r=2 oder r=8).
        assertThat(rowByUnit.get(UnitCatalog.TITAN)).isIn(2, 8);
    }

    @Test
    void assign_positions_pure_shooters_use_all_outer_rows() {
        // Drei Schützen, keine Melees → Shooter belegen alle drei aktiven Reihen, von außen
        // nach innen in Slot-Reihenfolge.
        List<Unit> units = List.of(
                UnitCatalog.ARCHER,
                UnitCatalog.MARKSMAN,
                UnitCatalog.MONK);

        List<Hex> positions = SpawnLayout.assignPositions(Side.ATTACKER, units);

        // rowsFor(3) = {4, 5, 6}. outsideIn (|r-5| desc, r asc): {4, 6, 5}.
        assertThat(positions.get(0).r()).isEqualTo(4);
        assertThat(positions.get(1).r()).isEqualTo(6);
        assertThat(positions.get(2).r()).isEqualTo(5);
    }

    @Test
    void assign_positions_single_unit_lands_in_center() {
        List<Hex> shooterAlone = SpawnLayout.assignPositions(Side.ATTACKER, List.of(UnitCatalog.MARKSMAN));
        List<Hex> meleeAlone = SpawnLayout.assignPositions(Side.ATTACKER, List.of(UnitCatalog.ARCH_ANGEL));

        assertThat(shooterAlone.get(0)).isEqualTo(new Hex(0, 5));
        assertThat(meleeAlone.get(0)).isEqualTo(new Hex(0, 5));
    }

    @Test
    void assign_positions_defender_uses_right_column() {
        List<Hex> positions = SpawnLayout.assignPositions(Side.DEFENDER,
                List.of(UnitCatalog.MARKSMAN, UnitCatalog.HALBERDIER));

        assertThat(positions).allMatch(h -> h.q() == 14);
    }

    @Test
    void assign_positions_more_shooters_than_outer_rows_overflows_inward() {
        // 5 Schützen + 2 Melees in 7 Slots → Schützen-Pool (5) übersteigt die "klassischen
        // Außen-Plätze" (2). Heuristik wandert die überzähligen Schützen Richtung Mitte;
        // die 2 Melees übernehmen die innersten verbleibenden Reihen.
        List<Unit> units = List.of(
                UnitCatalog.ARCHER,
                UnitCatalog.MARKSMAN,
                UnitCatalog.MONK,
                UnitCatalog.ZEALOT,
                UnitCatalog.WOOD_ELF,
                UnitCatalog.HALBERDIER,
                UnitCatalog.ARCH_ANGEL);

        List<Hex> positions = SpawnLayout.assignPositions(Side.ATTACKER, units);

        // Schützen belegen outsideIn[0..4] = {0, 10, 2, 8, 4}.
        Set<Integer> shooterRows = Set.of(
                positions.get(0).r(), positions.get(1).r(), positions.get(2).r(),
                positions.get(3).r(), positions.get(4).r());
        assertThat(shooterRows).containsExactlyInAnyOrder(0, 10, 2, 8, 4);
        // Arch Angel (schnellster Melee) bekommt das Zentrum r=5.
        assertThat(positions.get(6).r()).isEqualTo(5);
        // Halberdier bekommt die einzige verbleibende Reihe r=6.
        assertThat(positions.get(5).r()).isEqualTo(6);
    }

    @Test
    void assign_positions_rejects_invalid_sizes() {
        assertThatThrownBy(() -> SpawnLayout.assignPositions(Side.ATTACKER, List.of()))
                .isInstanceOf(IllegalArgumentException.class);
        List<Unit> tooMany = java.util.Collections.nCopies(8, UnitCatalog.PIKEMAN);
        assertThatThrownBy(() -> SpawnLayout.assignPositions(Side.ATTACKER, tooMany))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
