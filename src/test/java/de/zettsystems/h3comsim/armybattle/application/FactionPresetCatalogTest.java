package de.zettsystems.h3comsim.armybattle.application;

import de.zettsystems.h3comsim.armybattle.values.FactionPresetDto;
import de.zettsystems.h3comsim.armybattle.values.StackSpec;
import de.zettsystems.h3comsim.battle.domain.Faction;
import de.zettsystems.h3comsim.battle.domain.UnitCatalog;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FactionPresetCatalogTest {

    private final FactionPresetCatalog catalog = new FactionPresetCatalog();

    @ParameterizedTest
    @EnumSource(value = Faction.class, names = "NEUTRAL", mode = EnumSource.Mode.EXCLUDE)
    void every_faction_has_seven_valid_unit_slots(Faction faction) {
        FactionPresetDto preset = catalog.byFaction(faction);

        assertThat(preset.stacks()).hasSize(7);
        for (StackSpec spec : preset.stacks()) {
            assertThat(UnitCatalog.byName(spec.unitName()))
                    .as("unit %s in preset %s muss in UnitCatalog existieren", spec.unitName(), faction)
                    .isPresent();
            assertThat(spec.count()).as("count > 0 for %s", spec.unitName()).isPositive();
        }
    }

    @Test
    void all_returns_nine_presets() {
        List<FactionPresetDto> all = catalog.all();
        assertThat(all).hasSize(9);
        assertThat(all).extracting(FactionPresetDto::faction)
                .containsExactlyInAnyOrder(Faction.CASTLE, Faction.RAMPART, Faction.TOWER,
                        Faction.INFERNO, Faction.NECROPOLIS, Faction.DUNGEON,
                        Faction.STRONGHOLD, Faction.FORTRESS, Faction.CONFLUX);
    }

    @Test
    void neutral_has_no_preset() {
        assertThatThrownBy(() -> catalog.byFaction(Faction.NEUTRAL))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void castle_preset_matches_documented_backlog_composition() {
        FactionPresetDto castle = catalog.byFaction(Faction.CASTLE);

        assertThat(castle.stacks()).extracting(StackSpec::unitName).containsExactly(
                "Arch Angel", "Champion", "Zealot", "Crusader", "Royal Griffin", "Marksman", "Halberdier");
        assertThat(castle.stacks()).extracting(StackSpec::count).containsExactly(
                1, 2, 3, 4, 7, 9, 14);
    }
}
