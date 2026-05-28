package de.zettsystems.h3comsim.armybattle.values;

import de.zettsystems.h3comsim.battle.domain.Faction;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Hartkodierte Wochenproduktions-Composition einer Faktion. Slots in Reihenfolge T7 → T1 (stärkster Tier zuerst).")
public record FactionPresetDto(
        @Schema(description = "Faktion") Faction faction,

        @Schema(description = "7 Stacks in Slot-Reihenfolge") List<StackSpec> stacks) {
    public FactionPresetDto {
        stacks = List.copyOf(stacks);
    }
}
