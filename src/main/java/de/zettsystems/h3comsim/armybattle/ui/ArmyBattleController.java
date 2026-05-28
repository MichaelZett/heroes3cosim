package de.zettsystems.h3comsim.armybattle.ui;

import de.zettsystems.h3comsim.armybattle.application.ArmyBattleService;
import de.zettsystems.h3comsim.armybattle.application.FactionPresetCatalog;
import de.zettsystems.h3comsim.armybattle.values.ArmyBattleRequest;
import de.zettsystems.h3comsim.armybattle.values.ArmyBattleSimulation;
import de.zettsystems.h3comsim.armybattle.values.ArmyPresetsResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.ThreadLocalRandom;

@RestController
@RequestMapping("/api/army-battles")
@Tag(name = "Army Battle",
        description = "Deterministische Army-vs-Army-Simulation: bis zu 7 Stacks pro Seite, vollständiger Event-Stream zum Replay.")
public class ArmyBattleController {

    private final ArmyBattleService service;
    private final FactionPresetCatalog presets;

    public ArmyBattleController(ArmyBattleService service, FactionPresetCatalog presets) {
        this.service = service;
        this.presets = presets;
    }

    @Operation(
            summary = "Eine Army-Battle simulieren",
            description = """
                    Löst genau eine Army-vs-Army-Schlacht auf (bis zu 7 Stacks pro Seite).
                    Bei gleichem Seed deterministisch reproduzierbar; ohne Seed wird ein
                    zufälliger gewählt. Liefert das aggregierte BattleResult plus den
                    geordneten Event-Stream mit `actorSlot`/`targetSlot`-Disambiguierung.
                    """,
            operationId = "simulateArmyBattle")
    @ApiResponse(responseCode = "200", description = "Simulation abgeschlossen")
    @ApiResponse(responseCode = "400",
            description = "Unbekannter Unit-Name oder Validierungsfehler",
            content = @Content)
    @PostMapping("/simulate")
    public ArmyBattleSimulation simulate(@Valid @RequestBody ArmyBattleRequest request) {
        long seed = request.seed() != null ? request.seed() : ThreadLocalRandom.current().nextLong();
        return service.simulate(request, seed);
    }

    @Operation(
            summary = "Wochenproduktions-Compositions aller Faktionen abrufen",
            description = "Liefert die hartkodierten Faction-Presets (CASTLE, RAMPART, …, CONFLUX). Slot-Reihenfolge T7 → T1.",
            operationId = "listArmyPresets")
    @GetMapping("/presets")
    public ArmyPresetsResponse listPresets() {
        return new ArmyPresetsResponse(presets.all());
    }
}
