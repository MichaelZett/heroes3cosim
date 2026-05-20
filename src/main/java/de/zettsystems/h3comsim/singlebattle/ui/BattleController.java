package de.zettsystems.h3comsim.singlebattle.ui;

import de.zettsystems.h3comsim.battle.domain.Unit;
import de.zettsystems.h3comsim.battle.domain.UnitCatalog;
import de.zettsystems.h3comsim.singlebattle.application.BattleSimulationService;
import de.zettsystems.h3comsim.singlebattle.values.BattleConfigRequest;
import de.zettsystems.h3comsim.singlebattle.values.BattleSimulation;
import de.zettsystems.h3comsim.singlebattle.values.BattleSimulationDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.concurrent.ThreadLocalRandom;

@RestController
@RequestMapping("/api/battles")
@Tag(name = "Single Battle",
        description = "Deterministische Einzel-Simulation Attacker-Stack vs Defender-Stack inklusive vollem Event-Stream zum Replay.")
public class BattleController {

    private final BattleSimulationService simulations;

    public BattleController(BattleSimulationService simulations) {
        this.simulations = simulations;
    }

    @Operation(
            summary = "Eine Einzelschlacht simulieren",
            description = """
                    Löst genau ein Attacker-vs-Defender-Matchup auf. Bei gleichem Seed
                    deterministisch reproduzierbar; ohne Seed wird ein zufälliger gewählt
                    und im Result-Stream nicht erneut ausgegeben. Liefert das aggregierte
                    {@code BattleResult} plus den geordneten Event-Stream (Movement,
                    Shoot, Melee, Retaliation, Skills, BattleEnd …) für die UI-Replay-View.
                    """,
            operationId = "simulateBattle")
    @ApiResponse(responseCode = "200", description = "Simulation abgeschlossen")
    @ApiResponse(responseCode = "400",
            description = "Unbekannter Unit-Name oder Validierungsfehler (Count < 1, Pflichtfeld leer)",
            content = @Content)
    @PostMapping("/simulate")
    public BattleSimulationDto simulate(@Valid @RequestBody BattleConfigRequest request) {
        Unit attacker = lookupUnit(request.attackerUnit());
        Unit defender = lookupUnit(request.defenderUnit());
        long seed = request.seed() != null ? request.seed() : ThreadLocalRandom.current().nextLong();

        BattleSimulation simulation = simulations.simulate(
                attacker, request.attackerCount(),
                defender, request.defenderCount(),
                seed);
        return new BattleSimulationDto(simulation.result(), simulation.events());
    }

    private static Unit lookupUnit(String name) {
        return UnitCatalog.byName(name).orElseThrow(() -> new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Unknown unit: " + name));
    }
}
