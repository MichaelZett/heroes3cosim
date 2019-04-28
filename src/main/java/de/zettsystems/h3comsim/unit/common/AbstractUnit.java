package de.zettsystems.h3comsim.unit.common;

import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public abstract class AbstractUnit implements Unit {
    protected int currentHealth;
    protected Set<UnitSpeciality> unitSpecialities;

    public AbstractUnit(int currentHealth, Set<UnitSpeciality> unitSpecialities) {
        this.currentHealth = currentHealth;
        this.unitSpecialities = unitSpecialities;
    }

    @Override
    public int calculateCurrentDamage() {
        return ThreadLocalRandom.current().nextInt(getMinDamage(), getMaxDamage() + 1);
    }

    @Override
    public int calculateAttackBoniMaliPercentage(int defense) {
        int difference = getAttack() - defense;
        if (difference >= 0) {
            return difference * 5;
        } else {
            return difference * 2;
        }
    }

    @Override
    public int getCurrentHealth() {
        return currentHealth;
    }

    @Override
    public void retrieveDamage(int realDamage) {
        this.currentHealth = this.getCurrentHealth() - realDamage;
    }

    @Override
    public boolean hasSpeciality(UnitSpeciality unitSpeciality) {
        return unitSpecialities.contains(unitSpeciality);
    }
}
