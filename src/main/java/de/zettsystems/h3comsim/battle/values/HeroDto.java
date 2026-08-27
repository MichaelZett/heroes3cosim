package de.zettsystems.h3comsim.battle.values;

import de.zettsystems.h3comsim.battle.domain.Faction;
import de.zettsystems.h3comsim.battle.domain.HeroClass;
import de.zettsystems.h3comsim.battle.domain.SecondarySkill;
import de.zettsystems.h3comsim.battle.domain.SkillLevel;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

@Schema(description = """
        Held aus dem H3-Catalog. Von den vier Primärwerten wirken derzeit nur `attack` und
        `defense` — sie werden auf jede Kreatur der geführten Armee addiert (Manual S. 33).
        `power` und `knowledge` steuern ausschließlich das Zaubern und bleiben ohne
        Zaubersystem folgenlos; `skills` wird geführt, aber noch nicht ausgewertet.
        """)
public record HeroDto(
        @Schema(description = "Anzeigename — wird auch als Identifier in den Battle-Requests verwendet.",
                example = "Crag Hack")
        String name,

        @Schema(description = "Heldenklasse. Bestimmt die Primärwerte.", example = "BARBARIAN")
        HeroClass heroClass,

        @Schema(description = "Fraktion, deren Armee dieser Held führt.", example = "STRONGHOLD")
        Faction faction,

        @Schema(description = "Attack-Primärwert. Wird auf jede eigene Kreatur addiert.", example = "4")
        int attack,

        @Schema(description = "Defense-Primärwert. Wird auf jede eigene Kreatur addiert.", example = "0")
        int defense,

        @Schema(description = "Power-Primärwert. Ohne Zaubersystem derzeit ohne Wirkung.", example = "1")
        int power,

        @Schema(description = "Knowledge-Primärwert. Ohne Zaubersystem derzeit ohne Wirkung.", example = "1")
        int knowledge,

        @Schema(description = "Startfertigkeiten mit Ausbaustufe. Derzeit ohne Wirkung im Kampf.")
        Map<SecondarySkill, SkillLevel> skills) {
}
