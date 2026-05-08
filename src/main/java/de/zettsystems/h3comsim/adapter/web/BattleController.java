package de.zettsystems.h3comsim.adapter.web;

import de.zettsystems.h3comsim.adapter.web.dto.BattleConfigRequest;
import de.zettsystems.h3comsim.adapter.web.dto.BattleSimulationDto;
import de.zettsystems.h3comsim.adapter.web.dto.UnitDto;
import de.zettsystems.h3comsim.adapter.web.dto.UnitMapper;
import de.zettsystems.h3comsim.application.BattleSimulation;
import de.zettsystems.h3comsim.application.BattleSimulationService;
import de.zettsystems.h3comsim.domain.Faction;
import de.zettsystems.h3comsim.domain.Unit;
import de.zettsystems.h3comsim.domain.UnitCatalog;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@RestController
@RequestMapping("/api")
public class BattleController {

    private final BattleSimulationService simulations;

    public BattleController(BattleSimulationService simulations) {
        this.simulations = simulations;
    }

    @GetMapping("/units")
    public List<UnitDto> listUnits() {
        return UnitCatalog.all().stream()
                .map(UnitMapper::toDto)
                .toList();
    }

    @GetMapping("/factions")
    public List<Faction> listFactions() {
        return Arrays.asList(Faction.values());
    }

    @PostMapping("/battles/simulate")
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
