package de.zettsystems.h3comsim.armybattle.values;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Antwort des Preset-Endpunkts: alle verfügbaren Faktions-Compositions.")
public record ArmyPresetsResponse(
        @Schema(description = "Liste aller Faktions-Presets") List<FactionPresetDto> presets) {
    public ArmyPresetsResponse {
        presets = List.copyOf(presets);
    }
}
