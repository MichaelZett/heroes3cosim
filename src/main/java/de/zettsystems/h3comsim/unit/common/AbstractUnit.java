package de.zettsystems.h3comsim.unit.common;

import java.util.Set;

public abstract class AbstractUnit implements Unit {

    protected int currentHealth;
    protected Set<UnitSpeciality> unitSpecialities;

    public AbstractUnit(int currentHealth, Set<UnitSpeciality> unitSpecialities) {
        this.currentHealth = currentHealth;
        this.unitSpecialities = unitSpecialities;
    }

    @Override
    public int getCurrentHealth() {
        return currentHealth;
    }

    @Override
    public void retrieveDamage(int damage) {
        this.currentHealth = this.getCurrentHealth() - damage;
    }

    @Override
    public void retrieveDamageToDeath() {
        this.currentHealth = 0;
    }

    @Override
    public boolean hasSpeciality(UnitSpeciality unitSpeciality) {
        return unitSpecialities.contains(unitSpeciality);
    }

}
