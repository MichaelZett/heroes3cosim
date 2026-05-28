package de.zettsystems.h3comsim.armybattle.values;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.jspecify.annotations.Nullable;

@Schema(description = "Konfiguration einer Army-vs-Army-Simulation. Bis zu 7 Stacks pro Seite. Unit-Namen müssen exakt einem Eintrag aus /api/units entsprechen.")
public record ArmyBattleRequest(
        @Schema(description = "Armee der Attacker-Seite", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull @Valid ArmySpec attacker,

        @Schema(description = "Armee der Defender-Seite", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull @Valid ArmySpec defender,

        @Schema(description = "Optionaler PRNG-Seed für deterministische Reproduktion. Bei null wird ein Zufalls-Seed gezogen.",
                example = "42", nullable = true)
        @Nullable Long seed) {
}
