package de.zettsystems.h3comsim.setup.ui;

import de.zettsystems.h3comsim.battle.domain.Artifact;
import de.zettsystems.h3comsim.battle.domain.CreatureBank;
import de.zettsystems.h3comsim.battle.domain.Faction;
import de.zettsystems.h3comsim.battle.domain.HeroCatalog;
import de.zettsystems.h3comsim.battle.domain.UnitCatalog;
import de.zettsystems.h3comsim.battle.values.ArtifactDto;
import de.zettsystems.h3comsim.battle.values.ArtifactMapper;
import de.zettsystems.h3comsim.battle.values.CreatureBankDto;
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
        description = "Read-only Listen für den Frontend-Konfigurator: alle bekannten Einheiten, Helden, Artefakte, Kreaturenbanken und Faktionen.")
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

    @Operation(summary = "Alle Artefakte auflisten",
            description = """
                    Gibt die kampfwirksamen Artefakte aus RoE, AB und SoD zurück — Platz-Art, Seltenheit,
                    Goldwert, Primärwerte und Wirkungen. Was nur auf der Abenteuerkarte oder nach der
                    Schlacht wirkt, fehlt. Die Reihenfolge ist stabil (nach Platz-Art, Kombi-Artefakte
                    zuletzt).
                    """,
            operationId = "listArtifacts")
    @ApiResponse(responseCode = "200", description = "Vollständige Artefakt-Liste.")
    @GetMapping("/artifacts")
    public List<ArtifactDto> listArtifacts() {
        return Arrays.stream(Artifact.values())
                .map(ArtifactMapper::toDto)
                .toList();
    }

    @Operation(summary = "Alle Kreaturenbanken auflisten",
            description = """
                    Gibt die Kreaturenbanken aus SoD zurück (VCMI `config/objects/creatureBanks.json`): je
                    Ausbaustufe ihr Anteil auf der Karte und die Wächter, ausbaufähige Stacks mit ihrer
                    ausgebauten Kreatur.
                    """,
            operationId = "listCreatureBanks")
    @ApiResponse(responseCode = "200", description = "Vollständige Liste der Kreaturenbanken.")
    @GetMapping("/creature-banks")
    public List<CreatureBankDto> listCreatureBanks() {
        return Arrays.stream(CreatureBank.values())
                .map(CreatureBankDto::of)
                .toList();
    }

    @Operation(summary = "Alle Helden auflisten",
            description = """
                    Gibt den Hero-Catalog zurück: alle Helden der neun Fraktionen mit Klasse,
                    Primärwerten, Startfertigkeiten, Spezialität und Startzaubern auf Level 1.
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
