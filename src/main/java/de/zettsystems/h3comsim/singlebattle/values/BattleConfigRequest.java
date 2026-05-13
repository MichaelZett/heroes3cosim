package de.zettsystems.h3comsim.singlebattle.values;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.jspecify.annotations.Nullable;

public record BattleConfigRequest(
        @NotBlank String attackerUnit,
        @Min(1) int attackerCount,
        @NotBlank String defenderUnit,
        @Min(1) int defenderCount,
        @Nullable Long seed
) {
}
