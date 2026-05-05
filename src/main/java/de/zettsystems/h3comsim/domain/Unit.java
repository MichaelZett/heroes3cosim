package de.zettsystems.h3comsim.domain;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public record Unit(
        String name,
        int attack,
        int defense,
        int health,
        int speed,
        int minDamage,
        int maxDamage,
        Movement movement,
        int shots,
        int cost,
        AttackType attackType,
        Set<UnitSpeciality> specialities
) {
    public Unit {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(movement, "movement");
        Objects.requireNonNull(attackType, "attackType");
        Objects.requireNonNull(specialities, "specialities");
        specialities = Set.copyOf(specialities);
    }

    public int morale() {
        return specialities.contains(UnitSpeciality.GOOD_MORALE) ? 1 : 0;
    }

    public boolean hasSpeciality(UnitSpeciality speciality) {
        return specialities.contains(speciality);
    }

    public Set<UnitSpeciality> attackerSpecialities() {
        return specialities.stream()
                .filter(UnitSpeciality::isAttack)
                .collect(Collectors.toUnmodifiableSet());
    }

    public boolean hasPenality(AttackType usedAttackType) {
        if (usedAttackType == AttackType.HAND_TO_HAND) {
            return attackType == AttackType.LONG_RANGE
                    && !hasSpeciality(UnitSpeciality.NO_HAND_TO_HAND_PENALTY);
        }
        return false;
    }
}
