package de.zettsystems.h3comsim.battle.values;

import de.zettsystems.h3comsim.battle.domain.Faction;
import de.zettsystems.h3comsim.battle.domain.Movement;

import java.util.Set;

public record UnitDto(
        String id,
        String name,
        Faction faction,
        int tier,
        boolean upgrade,
        int attack,
        int defense,
        int health,
        int speed,
        int minDamage,
        int maxDamage,
        int shots,
        Movement movement,
        int cost,
        Set<String> specialities
) {
}
