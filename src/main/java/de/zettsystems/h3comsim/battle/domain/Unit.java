package de.zettsystems.h3comsim.battle.domain;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public record Unit(
        String name,
        Stats stats,
        Combat combat,
        Movement movement,
        int cost,
        Faction faction,
        int tier,
        boolean upgrade,
        Set<UnitSpeciality> specialities
) {
    public Unit {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(stats, "stats");
        Objects.requireNonNull(combat, "combat");
        Objects.requireNonNull(movement, "movement");
        Objects.requireNonNull(faction, "faction");
        Objects.requireNonNull(specialities, "specialities");
        if (tier < 1 || tier > 7) {
            throw new IllegalArgumentException("tier must be 1..7, was " + tier);
        }
        specialities = Set.copyOf(specialities);
    }

    public int attack() {
        return stats.attack();
    }

    public int defense() {
        return stats.defense();
    }

    public int health() {
        return stats.health();
    }

    public int speed() {
        return stats.speed();
    }

    public int minDamage() {
        return combat.minDamage();
    }

    public int maxDamage() {
        return combat.maxDamage();
    }

    public int shots() {
        return combat.shots();
    }

    public AttackType attackType() {
        return combat.attackType();
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
            return attackType() == AttackType.LONG_RANGE
                    && !hasSpeciality(UnitSpeciality.NO_HAND_TO_HAND_PENALTY);
        }
        return false;
    }
}
