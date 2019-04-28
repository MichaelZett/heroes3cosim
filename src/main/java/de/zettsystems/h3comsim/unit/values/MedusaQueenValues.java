package de.zettsystems.h3comsim.unit.values;

import de.zettsystems.h3comsim.unit.common.AttackType;
import de.zettsystems.h3comsim.unit.common.Movement;

public final class MedusaQueenValues {
    public final static String NAME = "Medusa Queen";
    public final static int ATTACK = 10;
    public final static int DEFENSE = 10;
    public final static int HEALTH = 30;
    public final static int SPEED = 6;
    public final static int MIN_DAMAGE = 6;
    public final static int MAX_DAMAGE = 8;
    public final static Movement MOVEMENT = Movement.GROUND;
    public final static int SHOTS = 24;
    public final static int COST = 330;
    public final static AttackType ATTACK_TYPE = AttackType.LONG_RANGE;

    private MedusaQueenValues() {
        //not intended
    }
}
