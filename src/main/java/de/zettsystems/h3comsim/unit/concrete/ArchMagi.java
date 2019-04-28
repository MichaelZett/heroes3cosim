package de.zettsystems.h3comsim.unit.concrete;

import de.zettsystems.h3comsim.unit.common.AbstractUnit;
import de.zettsystems.h3comsim.unit.common.AttackType;
import de.zettsystems.h3comsim.unit.common.Movement;
import de.zettsystems.h3comsim.unit.values.ArchMagiValues;

public class ArchMagi extends AbstractUnit {
    public ArchMagi() {
        super(ArchMagiValues.HEALTH);
    }

    @Override
    public String getName() {
        return ArchMagiValues.NAME;
    }

    @Override
    public int getAttack() {
        return ArchMagiValues.ATTACK;
    }

    @Override
    public int getDefense() {
        return ArchMagiValues.DEFENSE;
    }

    @Override
    public int getHealth() {
        return ArchMagiValues.HEALTH;
    }

    @Override
    public int getSpeed() {
        return ArchMagiValues.SPEED;
    }

    @Override
    public int getMinDamage() {
        return ArchMagiValues.MIN_DAMAGE;
    }

    @Override
    public int getMaxDamage() {
        return ArchMagiValues.MAX_DAMAGE;
    }

    @Override
    public Movement getMovement() {
        return ArchMagiValues.MOVEMENT;
    }

    @Override
    public int getShots() {
        return ArchMagiValues.SHOTS;
    }

    @Override
    public int getCost() {
        return ArchMagiValues.COST;
    }

    @Override
    public AttackType getAttackType() {
        return ArchMagiValues.ATTACK_TYPE;
    }
}
