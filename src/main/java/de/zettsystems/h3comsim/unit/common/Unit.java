package de.zettsystems.h3comsim.unit.common;

import java.util.Set;

public interface Unit {
    String getName();

    int getAttack();

    int getDefense();

    int getHealth();

    int getSpeed();

    int getMinDamage();

    int getMaxDamage();

    Movement getMovement();

    int getShots();

    int getCost();

    AttackType getAttackType();

    int retrieveDamage(int realDamage);

    int getCurrentHealth();

    boolean hasSpeciality(UnitSpeciality unitSpeciality);

    void retrieveDamageToDeath();

    boolean isDead();

    Set<UnitSpeciality> retrieveAttackerSpecialities();

    boolean hasPenality(AttackType usedAttackType);

    int getMorale();
}
