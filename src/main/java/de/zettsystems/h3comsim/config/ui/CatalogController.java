package de.zettsystems.h3comsim.config.ui;

import de.zettsystems.h3comsim.battle.domain.Faction;
import de.zettsystems.h3comsim.battle.domain.UnitCatalog;
import de.zettsystems.h3comsim.battle.values.UnitDto;
import de.zettsystems.h3comsim.battle.values.UnitMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api")
public class CatalogController {

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
}
