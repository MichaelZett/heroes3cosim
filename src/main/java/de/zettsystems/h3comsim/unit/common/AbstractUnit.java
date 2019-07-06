package de.zettsystems.h3comsim.unit.common;

import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public abstract class AbstractUnit implements Unit {
    protected boolean petrified;
    protected int petrifiedCounter;
    protected boolean cursed;
    protected int cursedCounter;
    protected int currentHealth;
    protected Set<UnitSpeciality> unitSpecialities;

    public AbstractUnit(int currentHealth, Set<UnitSpeciality> unitSpecialities) {
        this.currentHealth = currentHealth;
        this.unitSpecialities = unitSpecialities;
        this.petrifiedCounter = 0;
        this.petrified = false;
        this.cursedCounter = 0;
        this.cursed = false;
    }

    @Override
    public int calculateCurrentDamage() {
        int baseValue;
        if (cursed) {
            baseValue = getMinDamage();
        } else {
            baseValue = ThreadLocalRandom.current().nextInt(getMinDamage(), getMaxDamage() + 1);
        }
        if (this.hasSpeciality(UnitSpeciality.DEATH_BLOW)) {
            int value = ThreadLocalRandom.current().nextInt(1, 101);
            if (value <= 20) {
                baseValue = baseValue * 2;
                System.out.println(this.getName() + " nutzt Death Blow.");
            }
        }
        return baseValue;
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
        if (this.petrified) {
            int reducedDamage = (int) Math.round(0.5 * realDamage);
            this.currentHealth = this.getCurrentHealth() - reducedDamage;
            System.out.println(this.getName() + " erhaelt " + reducedDamage + " Schaden.");
            unpetrify();
        } else {
            this.currentHealth = this.getCurrentHealth() - realDamage;
            System.out.println(this.getName() + " erhaelt " + realDamage + " Schaden.");
        }
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
    public void petrify() {
        this.petrifiedCounter = 3;
        this.petrified = true;
    }

    @Override
    public boolean isPetrified() {
        return this.petrified;
    }

    @Override
    public void curse() {
        this.cursedCounter = 3;
        this.cursed = true;
    }

    @Override
    public void endTurn() {
        if (this.petrifiedCounter > 0) {
            this.petrifiedCounter--;
            if (this.petrifiedCounter == 0) {
                unpetrify();
            }
        }
        if (this.cursedCounter > 0) {
            this.cursedCounter--;
            if (this.cursedCounter == 0) {
                uncurse();
            }
        }
    }

    private void uncurse() {
        this.cursedCounter = 0;
        this.cursed = false;
        System.out.println(this.getName() + " wurde entflucht.");
    }

    private void unpetrify() {
        this.petrifiedCounter = 0;
        this.petrified = false;
        System.out.println(this.getName() + " wurde entsteinert.");
    }
}
