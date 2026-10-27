package de.zettsystems.h3comsim.armybattle.values;

import de.zettsystems.h3comsim.battle.domain.CreatureBank;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.jspecify.annotations.Nullable;

@Schema(description = "Kampf um eine Kreaturenbank: die Wächter ersetzen die Armee des Verteidigers.")
public record CreatureBankSpec(
        @Schema(description = "Die Bank", example = "CYCLOPS_STOCKPILE", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull CreatureBank bank,

        @Schema(description = """
                Ausbaustufe 1..4. Bei null gewürfelt nach dem Anteil auf der Karte (30/30/30/10 %),
                reproduzierbar über den Seed.
                """, example = "2", minimum = "1", maximum = "4", nullable = true)
        @Min(1) @Max(4) @Nullable Integer level) {
}
