package de.zettsystems.h3comsim.armybattle.values;

import de.zettsystems.h3comsim.battle.domain.Faction;
import io.swagger.v3.oas.annotations.media.Schema;

import org.jspecify.annotations.Nullable;

import java.util.List;

@Schema(description = "Hartkodierte Wochenproduktions-Composition einer Faktion. Slots in Reihenfolge T7 → T1 (stärkster Tier zuerst).")
public record FactionPresetDto(
        @Schema(description = "Faktion") Faction faction,

        @Schema(description = "7 Stacks in Slot-Reihenfolge") List<StackSpec> stacks,

        @Schema(description = """
                Vorgeschlagener Held dieser Faktion (Name aus `/api/heroes`). Das UI übernimmt
                ihn als Default; die Simulation läuft auch ohne, dann führerlos.
                """, example = "Crag Hack", nullable = true)
        @Nullable String heroName) {
    public FactionPresetDto {
        stacks = List.copyOf(stacks);
    }
}
