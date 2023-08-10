package de.zettsystems.h3comsim.unit.concrete;

import de.zettsystems.h3comsim.unit.common.AbstractUnit;
import de.zettsystems.h3comsim.unit.common.AttackType;
import de.zettsystems.h3comsim.unit.common.Movement;

import java.util.Collections;

public class Champion extends AbstractUnit {

    private final static String NAME = "Champion";
    private final static int ATTACK = 16;
    private final static int DEFENSE = 16;
    private final static int HEALTH = 100;
    private final static int SPEED = 9;
    private final static int MIN_DAMAGE = 20;
    private final static int MAX_DAMAGE = 25;
    private final static Movement MOVEMENT = Movement.GROUND;
    private final static int SHOTS = 0;
    private final static int COST = 1200;
    private final static AttackType ATTACK_TYPE = AttackType.HAND_TO_HAND;

    public Champion() {
        super(HEALTH, Collections.emptySet());
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public int getAttack() {
        return ATTACK;
    }

    @Override
    public int getDefense() {
        return DEFENSE;
    }

    @Override
    public int getHealth() {
        return HEALTH;
    }

    @Override
    public int getSpeed() {
        return SPEED;
    }

    @Override
    public int getMinDamage() {
        return MIN_DAMAGE;
    }

    @Override
    public int getMaxDamage() {
        return MAX_DAMAGE;
    }

    @Override
    public Movement getMovement() {
        return MOVEMENT;
    }

    @Override
    public int getShots() {
        return SHOTS;
    }

    @Override
    public int getCost() {
        return COST;
    }

    @Override
    public AttackType getAttackType() {
        return ATTACK_TYPE;
    }
}
