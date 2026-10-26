package de.zettsystems.h3comsim.battle.values;

import de.zettsystems.h3comsim.battle.domain.MagicSchool;
import de.zettsystems.h3comsim.battle.domain.SkillLevel;
import de.zettsystems.h3comsim.battle.domain.Spell;
import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

import java.util.List;

@Schema(description = "Eine Kampfwirkung eines Artefakts; welche Felder gesetzt sind, hängt von `type` ab.")
public record ArtifactEffectDto(
        @Schema(description = "Art der Wirkung", example = "LUCK")
        EffectType type,

        @Schema(description = """
                Zahlwert: Glück/Moral, Runden, Prozent, Speed, Lebenspunkte, Zauberstufe oder
                Attack/Defense je nach `type`.
                """, example = "1", nullable = true)
        @Nullable Integer value,

        @Schema(description = "Schule bei SPELL_DAMAGE und SCHOOL_SPELLS", nullable = true)
        @Nullable MagicSchool school,

        @Schema(description = "Zauber bei GRANTS_SPELL, OPENING_SPELL und SPELL_IMMUNITY")
        List<Spell> spells,

        @Schema(description = "Stufe bei GRANTS_SPELL", nullable = true)
        @Nullable SkillLevel level) {

    /** Art einer Artefakt-Wirkung, siehe {@code ArtifactEffect}. */
    @Schema(description = "Art einer Artefakt-Wirkung.", enumAsRef = true)
    public enum EffectType {
        LUCK, MORALE, SPELL_DURATION, MAGIC_RESISTANCE, ARCHERY, SPEED, HEALTH, LIVING_VITALITY, SPELL_DAMAGE,
        SCHOOL_SPELLS, LEVEL_SPELLS, GRANTS_SPELL, SPELL_IMMUNITY, LEVEL_IMMUNITY, MIND_IMMUNITY, BLOCK_MAGIC,
        NEGATE_IMMUNITIES, NEGATE_LUCK, NEGATE_MORALE, NO_SHOOTING_PENALTY, FREE_SHOOTING, DRAGON_BONUS,
        OPENING_SPELL, ALIGNMENT_MIX
    }
}
