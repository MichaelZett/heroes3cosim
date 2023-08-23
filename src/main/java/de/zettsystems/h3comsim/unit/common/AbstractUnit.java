package de.zettsystems.h3comsim.unit.common;

import java.util.Set;
import java.util.stream.Collectors;

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
    public int retrieveDamage(int damage) {
        int realDamage = Math.min(this.currentHealth, damage);
        this.currentHealth = this.currentHealth - realDamage;
        return damage - realDamage;
    }

    @Override
    public void retrieveDamageToDeath() {
        this.currentHealth = 0;
    }

    @Override
    public boolean hasSpeciality(UnitSpeciality unitSpeciality) {
        return unitSpecialities.contains(unitSpeciality);
    }

    @Override
    public boolean isDead() {
        return currentHealth <= 0;
    }

    @Override
    public Set<UnitSpeciality> retrieveAttackerSpecialities() {
        return unitSpecialities.stream().filter(UnitSpeciality::isAttack).collect(Collectors.toSet());
    }
}
