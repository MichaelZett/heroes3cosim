package de.zettsystems.h3comsim.unit.common;

import java.util.Set;
import java.util.stream.Collectors;

import static de.zettsystems.h3comsim.unit.common.UnitSpeciality.GOOD_MORALE;

public abstract class AbstractUnit implements Unit {

    protected int currentHealth;
    protected int morale;
    protected Set<UnitSpeciality> unitSpecialities;

    public AbstractUnit(int currentHealth, Set<UnitSpeciality> unitSpecialities) {
        this.currentHealth = currentHealth;
        this.unitSpecialities = unitSpecialities;
        this.morale = 0;
        if (unitSpecialities.contains(GOOD_MORALE)) {
            this.morale = 1;
        }
    }

    @Override
    public int getCurrentHealth() {
        return currentHealth;
    }

    @Override
    public int getMorale() {
        return morale;
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

    @Override
    public boolean hasPenality(AttackType usedAttackType) {
        if (AttackType.HAND_TO_HAND == usedAttackType) {
            return this.getAttackType() == AttackType.LONG_RANGE && !this.hasSpeciality(UnitSpeciality.NO_HAND_TO_HAND_PENALTY);
        } else {
            // TODO check range
            return false;
        }
    }
}
