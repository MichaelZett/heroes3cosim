package de.zettsystems.h3comsim.battle.values;

import de.zettsystems.h3comsim.battle.domain.SecondarySkill;
import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

@Schema(description = """
        Spezialfähigkeit eines Helden. `CREATURE`: +1 Speed und +5 % Attack/Defense je
        angefangene Spanne von (Stufe der Kreatur) Leveln, für Basis- und Ausbaustufe.
        `SKILL`: Effekt der Fertigkeit × (1 + 0,05 × Level). `FIXED_CREATURE`: feste Boni der
        Planeswalker. `OTHER`: ohne Kampfwirkung.
        """)
public record HeroSpecialtyDto(
        @Schema(description = "Art der Spezialität", example = "CREATURE") Kind kind,
        @Schema(description = "Kreatur (Basisstufe) bei CREATURE und FIXED_CREATURE", example = "Swordsman", nullable = true)
        @Nullable String unitName,
        @Schema(description = "Stufe dieser Kreatur (1..7), sonst 0. Bei CREATURE die Spanne in Leveln je +5 %.", example = "4")
        int creatureTier,
        @Schema(description = "Fertigkeit bei SKILL", example = "ARMORER", nullable = true)
        @Nullable SecondarySkill skill,
        @Schema(description = "Fester Attack-Bonus (FIXED_CREATURE)", example = "1") int attack,
        @Schema(description = "Fester Defense-Bonus (FIXED_CREATURE)", example = "2") int defense,
        @Schema(description = "Fester Schadens-Bonus je Kreatur (FIXED_CREATURE)", example = "2") int damage,
        @Schema(description = "Kurzbeschreibung bei OTHER", example = "Spell: Haste", nullable = true)
        @Nullable String description,
        @Schema(description = "Wirkt die Spezialität im Kampf?", example = "true") boolean effective) {

    public enum Kind { CREATURE, SKILL, FIXED_CREATURE, OTHER }
}
