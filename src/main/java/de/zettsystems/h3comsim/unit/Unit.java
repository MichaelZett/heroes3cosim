package de.zettsystems.h3comsim.unit;

import java.util.concurrent.ThreadLocalRandom;

public abstract class Unit {
    private String name;
    private int attack;
    private int defense;
    private int health;
    private int speed;
    private int minDamage;
    private int maxDamage;
    private Movement movement;
    private int shots;
    private int cost;
    private AttackType attackType;

    public Unit(String name, int attack, int defense, int health, int speed, int minDamage, int maxDamage,
                Movement movement, int shots, int cost, AttackType attackType) {
        this.name = name;
        this.attack = attack;
        this.defense = defense;
        this.health = health;
        this.speed = speed;
        this.minDamage = minDamage;
        this.maxDamage = maxDamage;
        this.movement = movement;
        this.shots = shots;
        this.cost = cost;
        this.attackType = attackType;
    }

    public int calculateCurrentDamage() {
        return ThreadLocalRandom.current().nextInt(minDamage, maxDamage + 1);
    }

    public int calculateAttackBoniMaliPercentage(int defense) {
        int difference = this.attack - defense;
        if (difference >= 0) {
            return difference * 5;
        } else {
            return difference * 2;
        }
    }

    public String getName() {
        return name;
    }

    public int getAttack() {
        return attack;
    }

    public int getDefense() {
        return defense;
    }

    public int getHealth() {
        return health;
    }

    public int getSpeed() {
        return speed;
    }

    public int getMinDamage() {
        return minDamage;
    }
    public int getMaxDamage() {
        return maxDamage;
    }

    public Movement getMovement() {
        return movement;
    }

    public int getShots() {
        return shots;
    }

    public int getCost() {
        return cost;
    }

    public AttackType getAttackType() {
        return attackType;
    }

    public void retrieveDamage(int realDamage) {
        this.health = this.getHealth()  - realDamage;
    }
}
