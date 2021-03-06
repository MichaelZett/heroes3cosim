package de.zettsystems.h3comsim.unit.common;

import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class Stack {
    private final String name;
    private final int speed;
    private final int minDamage;
    private final int maxDamage;
    private final int defense;
    private final int attack;
    private boolean petrified;
    private int petrifiedCounter;
    private boolean cursed;
    private int cursedCounter;
    private Deque<Unit> units;
    private List<Unit> deadUnits;

    private static Deque<Unit> createDequeOfUnits(Unit unit, int number) {
        Deque<Unit> deque = new LinkedList<>();
        for (int i = 0; i < number; i++) {
            try {
                deque.offerLast(unit.getClass().getConstructor().newInstance());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return deque;
    }

    public static Stack createStack(Unit unit, int number) {
        return new Stack(createDequeOfUnits(unit, number));
    }

    private Stack(Deque<Unit> stackOfUnit) {
        this.units = stackOfUnit;
        this.petrifiedCounter = 0;
        this.petrified = false;
        this.cursedCounter = 0;
        this.cursed = false;
        this.deadUnits = new LinkedList<>();
        this.name = this.units.getFirst().getName();
        this.speed = this.units.getFirst().getSpeed();
        this.minDamage = this.units.getFirst().getMinDamage();
        this.maxDamage = this.units.getFirst().getMaxDamage();
        this.defense = this.units.getFirst().getDefense();
        this.attack = this.units.getFirst().getAttack();
    }

    public String getName() {
        return this.name;
    }

    public int getSpeed() {
        return this.speed;
    }

    public int getDefense() {
        return this.defense;
    }

    public boolean isPetrified() {
        return petrified;
    }

    public boolean isAlive() {
        return !units.isEmpty();
    }

    public int getCurrentHealth() {
        if (this.units.isEmpty()) {
            return 0;
        } else {
            return this.units.getFirst().getCurrentHealth();
        }
    }


    public int calculateCurrentDamage() {
        int baseValue;
        if (cursed) {
            baseValue = minDamage;
        } else {
            baseValue = ThreadLocalRandom.current().nextInt(minDamage, maxDamage + 1);
        }
        if (this.hasSpeciality(UnitSpeciality.DEATH_BLOW)) {
            int value = ThreadLocalRandom.current().nextInt(1, 101);
            if (value <= 20) {
                baseValue = baseValue * 2;
                System.out.println("Stack von " + this.getName() + " nutzt Death Blow, verdoppelt also den Schaden.");
            }
        }
        return baseValue;
    }

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

    public boolean hasSpeciality(UnitSpeciality unitSpeciality) {
        return this.units.getFirst().hasSpeciality(unitSpeciality);
    }

    public int calculateAttackBoniMaliPercentage(int defense) {
        int difference = attack - defense;
        if (difference >= 0) {
            return difference * 5;
        } else {
            return difference * 2;
        }
    }

    public void retrieveDamage(int realDamage) {
        if (this.petrified) {
            int reducedDamage = (int) Math.round(0.5 * realDamage);
            doDamageRetrieving(reducedDamage);
            unpetrify();
        } else {
            doDamageRetrieving(realDamage);
        }
    }

    private void doDamageRetrieving(int damage) {
        int restDamage = damage;
        int countBefore = this.units.size();
        while (restDamage > 0) {
            restDamage = this.units.getFirst().retrieveDamage(damage);
            if (this.units.getFirst().isDead()) {
                deadUnits.add(this.units.pop());
                if (this.units.isEmpty()) {
                    break;
                }
            }
        }
        int countAfter = this.units.size();
        System.out.println("Stack von " + this.getName() + " erhaelt " + damage + " Schaden. " + (countBefore - countAfter) + " wurden getoetet.");
    }

    public void retrieveDamageToDeath() {
        Unit unitToKill = this.units.pop();
        unitToKill.retrieveDamageToDeath();
        deadUnits.add(unitToKill);
    }

    public void petrify() {
        this.petrifiedCounter = 3;
        this.petrified = true;
    }

    public void curse() {
        this.cursedCounter = 3;
        this.cursed = true;
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

    public int getCount() {
        return this.units.size();
    }
}
