package de.zettsystems.h3comsim.battle.values;

import de.zettsystems.h3comsim.battle.domain.Faction;
import de.zettsystems.h3comsim.battle.domain.Movement;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Set;

@Schema(description = "Einheit aus dem H3-Catalog inklusive Combat-Stats und besonderen Skills.")
public record UnitDto(
        @Schema(description = "Stabile interne ID (z.B. für Frontend-Lookups).", example = "halberdier")
        String id,

        @Schema(description = "Anzeigename der Einheit (Case-sensitive — wird auch als Identifier in BattleConfigRequest verwendet).",
                example = "Halberdier")
        String name,

        @Schema(description = "Faction dieser Einheit")
        Faction faction,

        @Schema(description = "Tier 1..7. Bestimmt Stadt-Stufe und implizite Stärke-Klasse.",
                example = "1", minimum = "1", maximum = "7")
        int tier,

        @Schema(description = "True wenn es sich um die Upgrade-Variante handelt (z.B. Halberdier statt Pikeman).",
                example = "true")
        boolean upgrade,

        @Schema(description = "Attack-Stat", example = "6")
        int attack,

        @Schema(description = "Defense-Stat", example = "5")
        int defense,

        @Schema(description = "HP pro Einheit", example = "10")
        int health,

        @Schema(description = "Initiative/Geschwindigkeit (bestimmt Move-Order)", example = "5")
        int speed,

        @Schema(description = "Minimaler Damage pro Einheit", example = "2")
        int minDamage,

        @Schema(description = "Maximaler Damage pro Einheit", example = "3")
        int maxDamage,

        @Schema(description = "Anzahl Schüsse (0 = Nahkämpfer)", example = "0")
        int shots,

        @Schema(description = "Bewegungsart — GROUND ignoriert keine Obstacles, FLYING überspringt sie.")
        Movement movement,

        @Schema(description = "Gold-Kosten pro Einheit (Castle-Rekrutierung).", example = "75")
        int cost,

        @Schema(description = "Liste der Spezial-Skills dieser Einheit als String-IDs (z.B. \"TWO_SHOTS\", \"FLYING\", \"NO_MELEE_PENALTY\", \"DEATH_STARE\").",
                example = "[\"TWO_SHOTS\", \"NO_MELEE_PENALTY\"]")
        Set<String> specialities
) {
}
