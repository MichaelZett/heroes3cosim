package de.zettsystems.h3comsim.armybattle.values;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "Armee einer Seite: 1..7 Stacks. Reihenfolge in der Liste entspricht dem Slot-Index 0..6.")
public record ArmySpec(
        @Schema(description = "Stack-Slots dieser Seite", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull @Valid @Size(min = 1, max = 7) List<StackSpec> stacks) {
    public ArmySpec {
        stacks = List.copyOf(stacks);
    }
}
