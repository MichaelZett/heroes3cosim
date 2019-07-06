package de.zettsystems.h3comsim.unit.common;

public interface Unit {
    int calculateCurrentDamage();

    int calculateAttackBoniMaliPercentage(int defense);

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

    void retrieveDamage(int realDamage);

    int getCurrentHealth();

    boolean hasSpeciality(UnitSpeciality unitSpeciality);

    void retrieveDamageToDeath();

    void petrify();

    boolean isPetrified();

    void endTurn();

    void curse();
}
