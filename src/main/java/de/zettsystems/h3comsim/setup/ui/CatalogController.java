package de.zettsystems.h3comsim.setup.ui;

import de.zettsystems.h3comsim.battle.domain.Faction;
import de.zettsystems.h3comsim.battle.domain.HeroCatalog;
import de.zettsystems.h3comsim.battle.domain.UnitCatalog;
import de.zettsystems.h3comsim.battle.values.HeroDto;
import de.zettsystems.h3comsim.battle.values.HeroMapper;
import de.zettsystems.h3comsim.battle.values.UnitDto;
import de.zettsystems.h3comsim.battle.values.UnitMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "Catalog",
        description = "Read-only Listen für den Frontend-Konfigurator: alle bekannten Einheiten, Helden und Faktionen.")
public class CatalogController {

    @Operation(summary = "Alle Einheiten auflisten",
            description = """
                    Gibt den vollständigen Unit-Catalog zurück — Stats, Faction, Tier, Upgrade-Flag,
                    Bewegungsart, Kosten und besondere Skills (`specialities`). Die Reihenfolge ist
                    stabil (Faction × Tier × upgrade) und kann direkt im UI-Dropdown verwendet werden.
                    """,
            operationId = "listUnits")
    @ApiResponse(responseCode = "200", description = "Vollständige Unit-Liste.")
    @GetMapping("/units")
    public List<UnitDto> listUnits() {
        return UnitCatalog.all().stream()
                .map(UnitMapper::toDto)
                .toList();
    }

    @Operation(summary = "Alle Faktionen auflisten",
            description = "Gibt alle H3-Faktionen plus `NEUTRAL` zurück.",
            operationId = "listFactions")
    @ApiResponse(responseCode = "200", description = "Vollständige Faction-Liste.")
    @GetMapping("/factions")
    public List<Faction> listFactions() {
        return Arrays.asList(Faction.values());
    }

    @Operation(summary = "Alle Helden auflisten",
            description = """
                    Gibt den Hero-Catalog zurück — derzeit genau einen Helden je Fraktion
                    (`NEUTRAL` hat keinen). Von den Primärwerten wirken nur `attack` und
                    `defense`: sie werden auf jede Kreatur der geführten Armee addiert
                    (Manual S. 33). `power`, `knowledge` und `skills` werden geführt, aber
                    noch nicht ausgewertet.
                    """,
            operationId = "listHeroes")
    @ApiResponse(responseCode = "200", description = "Vollständige Hero-Liste.")
    @GetMapping("/heroes")
    public List<HeroDto> listHeroes() {
        return HeroCatalog.all().stream()
                .map(HeroMapper::toDto)
                .toList();
    }
}
