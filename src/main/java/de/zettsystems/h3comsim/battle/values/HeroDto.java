package de.zettsystems.h3comsim.battle.values;

import de.zettsystems.h3comsim.battle.domain.Faction;
import de.zettsystems.h3comsim.battle.domain.HeroClass;
import de.zettsystems.h3comsim.battle.domain.SecondarySkill;
import de.zettsystems.h3comsim.battle.domain.SkillLevel;
import de.zettsystems.h3comsim.battle.domain.Spell;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;

@Schema(description = """
        Held aus dem H3-Catalog. `attack` und `defense` werden auf jede Kreatur der geführten
        Armee addiert (Manual S. 33), `power` und `knowledge` bestimmen Spell Power und Mana
        seiner Zauber. Zauber-Spezialisten bringen ihren Zauber in `spells` mit.
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

        @Schema(description = "Power-Primärwert: Spell Power seiner Zauber.", example = "1")
        int power,

        @Schema(description = "Knowledge-Primärwert: 10 Mana je Punkt.", example = "1")
        int knowledge,

        @Schema(description = "Startfertigkeiten mit Ausbaustufe. Kampfwirksam sind alle außer den Abenteuer-Fertigkeiten, Necromancy, Mysticism, Eagle Eye und Ballistics.")
        Map<SecondarySkill, SkillLevel> skills,

        @Schema(description = "Spezialfähigkeit des Helden")
        HeroSpecialtyDto specialty,

        @Schema(description = "Startzauber: ein Zauber-Spezialist beginnt mit seinem Zauber, auch über dem Wisdom-Gate.")
        List<Spell> spells) {
}
