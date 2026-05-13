package de.zettsystems.h3comsim.singlebattle.ui;

import de.zettsystems.h3comsim.battle.domain.Unit;
import de.zettsystems.h3comsim.battle.domain.UnitCatalog;
import de.zettsystems.h3comsim.singlebattle.application.BattleSimulationService;
import de.zettsystems.h3comsim.singlebattle.values.BattleConfigRequest;
import de.zettsystems.h3comsim.singlebattle.values.BattleSimulation;
import de.zettsystems.h3comsim.singlebattle.values.BattleSimulationDto;
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
public class BattleController {

    private final BattleSimulationService simulations;

    public BattleController(BattleSimulationService simulations) {
        this.simulations = simulations;
    }

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
