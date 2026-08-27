package de.zettsystems.h3comsim.armybattle.values;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.Nullable;

import java.util.List;

@Schema(description = "Armee einer Seite: 1..7 Stacks. Reihenfolge in der Liste entspricht dem Slot-Index 0..6.")
public record ArmySpec(
        @Schema(description = "Stack-Slots dieser Seite", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull @Valid @Size(min = 1, max = 7) List<StackSpec> stacks,

        @Schema(description = """
                Optionaler Held, exakter Name aus `/api/heroes` (z.B. `Crag Hack`). Sein
                Attack- und Defense-Wert wird auf jede Kreatur dieser Armee addiert. Bei
                null kämpft die Armee führerlos.
                """, example = "Crag Hack", nullable = true)
        @Nullable String heroName) {
    public ArmySpec {
        stacks = List.copyOf(stacks);
    }

    /** Rückwärtskompatibler Konstruktor für heldenlose Armeen. */
    public ArmySpec(List<StackSpec> stacks) {
        this(stacks, null);
    }
}
