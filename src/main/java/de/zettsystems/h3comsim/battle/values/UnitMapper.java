package de.zettsystems.h3comsim.battle.values;

import de.zettsystems.h3comsim.battle.domain.Unit;
import de.zettsystems.h3comsim.battle.domain.UnitSpeciality;

import java.util.stream.Collectors;

public final class UnitMapper {

    private UnitMapper() {
    }

    public static UnitDto toDto(Unit unit) {
        return new UnitDto(
                unit.name(),
                unit.name(),
                unit.faction(),
                unit.tier(),
                unit.upgrade(),
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
