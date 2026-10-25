package de.zettsystems.h3comsim.battle.domain.events;

import de.zettsystems.h3comsim.battle.domain.Faction;
import de.zettsystems.h3comsim.battle.domain.Fortification;
import de.zettsystems.h3comsim.battle.domain.WallPart;
import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

import java.util.List;

/** Die Befestigung zu Kampfbeginn. */
@Schema(description = "Stadtbefestigung bei einer Belagerung: unzerstörbare Mauerstücke, die zerstörbaren Teile mit ihren Treffern und der Graben.")
public record SiegeSnapshot(
        @Schema(description = "Ausbau der Befestigung") Fortification fortification,
        @Schema(description = "Stadt des Verteidigers; bestimmt Graben und Turmschaden") Faction town,
        @Schema(description = "Unzerstörbare Mauerfelder") List<HexCoord> permanentWall,
        @Schema(description = "Die zerstörbaren Teile mit Feld und verbleibenden Treffern; der Hauptturm steht außerhalb des Felds (hex = null)") List<Part> parts,
        @Schema(description = "Grabenfelder (bei Tower die Landminen); leer ohne Citadel") List<HexCoord> moat) {

    public SiegeSnapshot {
        permanentWall = List.copyOf(permanentWall);
        parts = List.copyOf(parts);
        moat = List.copyOf(moat);
    }

    @Schema(name = "SiegePart", description = "Ein zerstörbarer Teil der Befestigung.")
    public record Part(
            @Schema(description = "Welcher Teil") WallPart part,
            @Schema(description = "Feld des Teils, null für den Hauptturm", nullable = true) @Nullable HexCoord hex,
            @Schema(description = "Verbleibende Treffer, 0 = zerstört oder nicht gebaut") int health) {
    }
}
