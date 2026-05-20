package de.zettsystems.h3comsim.singlebattle.values;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.jspecify.annotations.Nullable;

@Schema(description = "Konfiguration für eine Einzel-Simulation. Unit-Namen müssen exakt einem Eintrag aus /api/units entsprechen.")
public record BattleConfigRequest(
        @Schema(description = "Name der Attacker-Einheit (Case-sensitive, vgl. /api/units)",
                example = "Halberdier", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank String attackerUnit,

        @Schema(description = "Stack-Größe der Attacker-Seite", example = "20", minimum = "1",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @Min(1) int attackerCount,

        @Schema(description = "Name der Defender-Einheit (Case-sensitive, vgl. /api/units)",
                example = "Marksman", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank String defenderUnit,

        @Schema(description = "Stack-Größe der Defender-Seite", example = "15", minimum = "1",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @Min(1) int defenderCount,

        @Schema(description = "Optionaler PRNG-Seed für deterministische Reproduktion. Bei null wird ein Zufalls-Seed gezogen.",
                example = "42", nullable = true)
        @Nullable Long seed
) {
}
