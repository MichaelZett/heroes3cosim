package de.zettsystems.h3comsim.unit.values;

import de.zettsystems.h3comsim.unit.common.AttackType;
import de.zettsystems.h3comsim.unit.common.Movement;

public final class ArchMagiValues {

    public final static String NAME = "Arch Magi";
    public final static int ATTACK = 12;
    public final static int DEFENSE = 9;
    public final static int HEALTH = 30;
    public final static int SPEED = 7;
    public final static int MIN_DAMAGE = 7;
    public final static int MAX_DAMAGE = 9;
    public final static Movement MOVEMENT = Movement.GROUND;
    public final static int SHOTS = 24;
    public final static int COST = 350;
    public final static AttackType ATTACK_TYPE = AttackType.LONG_RANGE;

    private ArchMagiValues() {
        //not intended
    }
}
