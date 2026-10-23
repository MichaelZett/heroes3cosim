package de.zettsystems.h3comsim.armybattle.values;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;

@Schema(description = "Armee einer Seite: 1..7 Stacks. Reihenfolge in der Liste entspricht dem Slot-Index 0..6.")
public record ArmySpec(
        @Schema(description = "Stack-Slots dieser Seite", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull @Valid @Size(min = 1, max = 7) List<StackSpec> stacks,

        @Schema(description = """
                Held, exakter Name aus `/api/heroes` (z.B. `Crag Hack`). Sein Attack- und
                Defense-Wert wird auf jede Kreatur dieser Armee addiert. Bei null führt der
                Standardheld der Fraktion, die den größten Goldwert der Armee stellt.
                """, example = "Crag Hack", nullable = true)
        @Nullable String heroName,

        @Schema(description = """
                Gesammeltes Glück des Helden (Artefakte, Gebäude, Abenteuer-Orte), 0..3. Addiert
                sich zur Luck-Fertigkeit; das Armee-Glück wird auf 0..3 gekappt. Fehlt das Feld
                oder ist es null, gilt 0.
                """, example = "1", minimum = "0", maximum = "3", nullable = true)
        @Min(0) @Max(3) @Nullable Integer luckBonus,

        @Schema(description = """
                Gesammelte Moral des Helden (Artefakte, Gebäude, Abenteuer-Orte), -3..+3.
                Addiert sich zum Moral-Rating der Armee. Fehlt das Feld oder ist es null, gilt 0.
                """, example = "0", minimum = "-3", maximum = "3", nullable = true)
        @Min(-3) @Max(3) @Nullable Integer moraleBonus) {
    public ArmySpec {
        stacks = List.copyOf(stacks);
        luckBonus = Objects.requireNonNullElse(luckBonus, 0);
        moraleBonus = Objects.requireNonNullElse(moraleBonus, 0);
    }

    /** Armee mit Helden, ohne gesammelte Boni. */
    public ArmySpec(List<StackSpec> stacks, @Nullable String heroName) {
        this(stacks, heroName, 0, 0);
    }

    /** Armee mit dem Standardhelden ihrer Hauptfraktion. */
    public ArmySpec(List<StackSpec> stacks) {
        this(stacks, null);
    }
}
