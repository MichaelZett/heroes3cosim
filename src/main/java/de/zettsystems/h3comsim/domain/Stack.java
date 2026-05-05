package de.zettsystems.h3comsim.domain;

import lombok.extern.slf4j.Slf4j;

import java.util.Set;
import java.util.random.RandomGenerator;

@Slf4j
public class Stack {

    private final Unit unit;
    private int aliveCount;
    private int topUnitCurrentHealth;
    private int shotsRemaining;
    private Hex position;
    private boolean petrified;
    private int petrifiedCounter;
    private boolean cursed;
    private int cursedCounter;

    public Stack(Unit unit, int count) {
        if (count < 0) {
            throw new IllegalArgumentException("count must be >= 0, was " + count);
        }
        this.unit = unit;
        this.aliveCount = count;
        this.topUnitCurrentHealth = unit.health();
        this.shotsRemaining = unit.shots();
    }

    public Unit unit() {
        return unit;
    }

    public Hex position() {
        return position;
    }

    public void setPosition(Hex position) {
        this.position = position;
    }

    public int shotsRemaining() {
        return shotsRemaining;
    }

    public void useShot() {
        if (shotsRemaining > 0) {
            shotsRemaining--;
        }
    }

    public boolean canShoot() {
        return unit.attackType() == AttackType.LONG_RANGE && shotsRemaining > 0;
    }

    public String getName() {
        return unit.name();
    }

    public int getSpeed() {
        return unit.speed();
    }

    public int getDefense() {
        return unit.defense();
    }

    public int getCount() {
        return aliveCount;
    }

    public boolean isAlive() {
        return aliveCount > 0;
    }

    public boolean isPetrified() {
        return petrified;
    }

    public int getCurrentHealth() {
        return aliveCount > 0 ? topUnitCurrentHealth : 0;
    }

    public boolean hasSpeciality(UnitSpeciality speciality) {
        return unit.hasSpeciality(speciality);
    }

    public Set<UnitSpeciality> getAttackerSpecialities() {
        return unit.attackerSpecialities();
    }

    public int calculateCurrentDamage(AttackType usedAttackType, RandomGenerator rng) {
        int baseValue = cursed ? unit.minDamage() : rng.nextInt(unit.minDamage(), unit.maxDamage() + 1);
        if (unit.hasPenality(usedAttackType)) {
            baseValue = (int) Math.round(0.5 * baseValue);
            LOG.info("Stack von {} hat Nachteil, halbiert also den Schaden.", getName());
        }
        if (unit.hasSpeciality(UnitSpeciality.DEATH_BLOW) && rng.nextInt(1, 101) <= 20) {
            baseValue = baseValue * 2;
            LOG.info("Stack von {} nutzt Death Blow, verdoppelt also den Schaden.", getName());
        }
        return baseValue * aliveCount;
    }

    public int calculateAttackBoniMaliPercentage(int defense) {
        int diff = unit.attack() - defense;
        return diff >= 0 ? diff * 5 : diff * 2;
    }

    public boolean hasGoodMorale(RandomGenerator rng) {
        int morale = unit.morale();
        if (morale > 0) {
            int random = rng.nextInt(1000);
            return (morale == 3 && random <= 125)
                    || (morale == 2 && random <= 83)
                    || (morale == 1 && random <= 42);
        }
        return false;
    }

    public void retrieveDamage(int baseDamage, Set<UnitSpeciality> attackerSpecialities) {
        int realDamage = baseDamage;
        boolean angelHate = unit.hasSpeciality(UnitSpeciality.ANGEL_RACE)
                && attackerSpecialities.contains(UnitSpeciality.ANGEL_HATE);
        boolean devilHate = unit.hasSpeciality(UnitSpeciality.DEVIL_RACE)
                && attackerSpecialities.contains(UnitSpeciality.DEVIL_HATE);
        if (angelHate || devilHate) {
            realDamage = (int) Math.round(1.5 * realDamage);
            LOG.info("Stack von {} wird vom Gegner gehasst, bekommt 1,5x Schaden.", getName());
        }
        if (petrified) {
            applyDamage((int) Math.round(0.5 * realDamage));
            unpetrify();
        } else {
            applyDamage(realDamage);
        }
    }

    public void retrieveDamageToDeath() {
        if (aliveCount > 0) {
            aliveCount--;
            topUnitCurrentHealth = aliveCount > 0 ? unit.health() : 0;
        }
    }

    public void petrify() {
        petrified = true;
        petrifiedCounter = 3;
    }

    public void curse() {
        cursed = true;
        cursedCounter = 3;
    }

    public void endTurn() {
        if (petrifiedCounter > 0 && --petrifiedCounter == 0) {
            unpetrify();
        }
        if (cursedCounter > 0 && --cursedCounter == 0) {
            uncurse();
        }
    }

    public boolean isAbleToAct() {
        if (!isAlive()) {
            LOG.info("Stack von {} ist bereits tot und macht nichts.", getName());
            return false;
        }
        if (petrified) {
            LOG.info("Stack von {} ist versteinert und macht nichts.", getName());
            return false;
        }
        return true;
    }

    private void applyDamage(int damage) {
        int rest = damage;
        int countBefore = aliveCount;
        while (rest > 0 && aliveCount > 0) {
            int absorbed = Math.min(topUnitCurrentHealth, rest);
            topUnitCurrentHealth -= absorbed;
            rest -= absorbed;
            if (topUnitCurrentHealth == 0) {
                aliveCount--;
                if (aliveCount > 0) {
                    topUnitCurrentHealth = unit.health();
                }
            }
        }
        LOG.info("Stack von {} erhaelt {} Schaden. {} wurden getoetet.",
                getName(), damage, countBefore - aliveCount);
    }

    private void uncurse() {
        cursed = false;
        cursedCounter = 0;
        LOG.info("{} wurde entflucht.", getName());
    }

    private void unpetrify() {
        petrified = false;
        petrifiedCounter = 0;
        LOG.info("{} wurde entsteinert.", getName());
    }
}
