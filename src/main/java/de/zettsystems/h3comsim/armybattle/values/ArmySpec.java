package de.zettsystems.h3comsim.armybattle.values;

import de.zettsystems.h3comsim.battle.domain.SecondarySkill;
import de.zettsystems.h3comsim.battle.domain.SkillLevel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

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
        @Min(-3) @Max(3) @Nullable Integer moraleBonus,

        @Schema(description = """
                Level des Helden, 1..99. Wirkt über die Spezialität: Kreatur-Spezialisten +5 %
                Attack/Defense je angefangene Spanne von (Stufe der Kreatur) Leveln,
                Fertigkeits-Spezialisten +5 % je Level auf den Effekt. Fehlt das Feld, gilt 1.
                """, example = "10", minimum = "1", maximum = "99", nullable = true)
        @Min(1) @Max(MAX_HERO_LEVEL) @Nullable Integer heroLevel,

        @Schema(description = """
                Zusätzliche Attack-Punkte des Helden (Level-Aufstiege, Artefakte), 0..99. Werden wie
                der Grundwert auf jede Kreatur addiert. Fehlt das Feld, gilt 0.
                """, example = "3", minimum = "0", maximum = "99", nullable = true)
        @Min(0) @Max(99) @Nullable Integer attackBonus,

        @Schema(description = "Zusätzliche Defense-Punkte des Helden, analog zu `attackBonus`.",
                example = "2", minimum = "0", maximum = "99", nullable = true)
        @Min(0) @Max(99) @Nullable Integer defenseBonus,

        @Schema(description = """
                Gelernte oder verbesserte Sekundärfertigkeiten, zusätzlich zu den Startfertigkeiten.
                Eine Startfertigkeit lässt sich nur heben, nicht senken; insgesamt höchstens acht.
                """, example = "{\"OFFENSE\": \"EXPERT\"}", nullable = true)
        @Nullable Map<SecondarySkill, SkillLevel> skills) {

    /** Obergrenze des Simulators, kein Spielwert — Kreatur-Boni wachsen linear weiter. */
    public static final int MAX_HERO_LEVEL = 99;

    public ArmySpec {
        stacks = List.copyOf(stacks);
        luckBonus = Objects.requireNonNullElse(luckBonus, 0);
        moraleBonus = Objects.requireNonNullElse(moraleBonus, 0);
        heroLevel = Objects.requireNonNullElse(heroLevel, 1);
        attackBonus = Objects.requireNonNullElse(attackBonus, 0);
        defenseBonus = Objects.requireNonNullElse(defenseBonus, 0);
        // Ein JSON-Eintrag mit null als Stufe bedeutet „nichts gelernt", kein Fehler.
        skills = skills == null ? Map.of() : skills.entrySet().stream()
                .filter(e -> e.getKey() != null && e.getValue() != null)
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    /** Armee mit Helden, ohne gesammelte Boni. */
    public ArmySpec(List<StackSpec> stacks, @Nullable String heroName) {
        this(stacks, heroName, 0, 0, 1);
    }

    /** Armee mit Helden, gesammeltem Glück/Moral und Level, ohne weiteren Aufbau. */
    public ArmySpec(List<StackSpec> stacks, @Nullable String heroName, int luckBonus,
                    int moraleBonus, int heroLevel) {
        this(stacks, heroName, luckBonus, moraleBonus, heroLevel, 0, 0, Map.of());
    }

    /** Armee mit dem Standardhelden ihrer Hauptfraktion. */
    public ArmySpec(List<StackSpec> stacks) {
        this(stacks, null);
    }
}
