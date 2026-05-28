package de.zettsystems.h3comsim.armybattle.values;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Eine einzelne Stack-Position in einer Armee. Unit-Name muss exakt einem Eintrag aus /api/units entsprechen.")
public record StackSpec(
        @Schema(description = "Name der Einheit (Case-sensitive, vgl. /api/units)",
                example = "Halberdier", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank String unitName,

        @Schema(description = "Stack-Größe (Anzahl Einheiten)", example = "14", minimum = "1",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @Min(1) int count) {
}
