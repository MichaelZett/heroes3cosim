package de.zettsystems.h3comsim.unit.concrete;

import de.zettsystems.h3comsim.unit.common.AbstractUnit;
import de.zettsystems.h3comsim.unit.common.AttackType;
import de.zettsystems.h3comsim.unit.common.Movement;
import de.zettsystems.h3comsim.unit.values.MedusaQueenValues;

import java.util.Collections;

public class MedusaQueen extends AbstractUnit {
    public MedusaQueen() {
        super(MedusaQueenValues.HEALTH, Collections.emptySet());
    }

    @Override
    public String getName() {
        return MedusaQueenValues.NAME;
    }

    @Override
    public int getAttack() {
        return MedusaQueenValues.ATTACK;
    }

    @Override
    public int getDefense() {
        return MedusaQueenValues.DEFENSE;
    }

    @Override
    public int getHealth() {
        return MedusaQueenValues.HEALTH;
    }

    @Override
    public int getSpeed() {
        return MedusaQueenValues.SPEED;
    }

    @Override
    public int getMinDamage() {
        return MedusaQueenValues.MIN_DAMAGE;
    }

    @Override
    public int getMaxDamage() {
        return MedusaQueenValues.MAX_DAMAGE;
    }

    @Override
    public Movement getMovement() {
        return MedusaQueenValues.MOVEMENT;
    }

    @Override
    public int getShots() {
        return MedusaQueenValues.SHOTS;
    }

    @Override
    public int getCost() {
        return MedusaQueenValues.COST;
    }

    @Override
    public AttackType getAttackType() {
        return MedusaQueenValues.ATTACK_TYPE;
    }
}
