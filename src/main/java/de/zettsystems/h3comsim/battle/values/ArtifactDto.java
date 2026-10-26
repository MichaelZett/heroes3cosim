package de.zettsystems.h3comsim.battle.values;

import de.zettsystems.h3comsim.battle.domain.Artifact;
import de.zettsystems.h3comsim.battle.domain.ArtifactSlot;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = """
        Kampfwirksames Artefakt (RoE, AB, SoD). Primärwerte kommen auf den Helden, die übrigen
        Wirkungen stehen in `effects`. Ein Kombi-Artefakt führt die Summe seiner Teile und sperrt
        die Plätze in `blocks`.
        """)
public record ArtifactDto(
        @Schema(description = "Identifier für die Requests (`artifacts` der Armee)", example = "SWORD_OF_HELLFIRE")
        Artifact id,

        @Schema(description = "Englischer Name wie im Spiel", example = "Sword of Hellfire")
        String name,

        @Schema(description = "Art des Platzes, den das Artefakt belegt", example = "WEAPON")
        ArtifactSlot.Kind kind,

        @Schema(description = "Seltenheitsklasse", example = "MAJOR")
        Artifact.Rarity rarity,

        @Schema(description = "Goldwert", example = "6000")
        int value,

        @Schema(description = "Attack-Bonus, auch negativ", example = "6")
        int attack,

        @Schema(description = "Defense-Bonus, auch negativ", example = "0")
        int defense,

        @Schema(description = "Power-Bonus, auch negativ", example = "0")
        int power,

        @Schema(description = "Knowledge-Bonus, auch negativ", example = "0")
        int knowledge,

        @Schema(description = "Teile eines Kombi-Artefakts, sonst leer")
        List<Artifact> components,

        @Schema(description = "Zusätzlich gesperrte Platz-Arten eines Kombi-Artefakts, sonst leer")
        List<ArtifactSlot.Kind> blocks,

        @Schema(description = "Kampfwirkungen über die Primärwerte hinaus")
        List<ArtifactEffectDto> effects) {
}
