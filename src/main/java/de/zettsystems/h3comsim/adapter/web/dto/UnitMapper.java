package de.zettsystems.h3comsim.adapter.web.dto;

import de.zettsystems.h3comsim.domain.Unit;
import de.zettsystems.h3comsim.domain.UnitSpeciality;

import java.util.stream.Collectors;

public final class UnitMapper {

    private UnitMapper() {
    }

    public static UnitDto toDto(Unit unit) {
        return new UnitDto(
                unit.name(),
                unit.name(),
                unit.faction(),
                unit.attack(),
                unit.defense(),
                unit.health(),
                unit.speed(),
                unit.minDamage(),
                unit.maxDamage(),
                unit.shots(),
                unit.movement(),
                unit.cost(),
                unit.specialities().stream()
                        .map(UnitSpeciality::name)
                        .collect(Collectors.toUnmodifiableSet())
        );
    }
}
