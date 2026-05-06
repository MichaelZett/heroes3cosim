package de.zettsystems.h3comsim.domain;

import static de.zettsystems.h3comsim.domain.UnitSpecialityType.AFTER_ATTACK;
import static de.zettsystems.h3comsim.domain.UnitSpecialityType.ATTACK;
import static de.zettsystems.h3comsim.domain.UnitSpecialityType.DEFENSE;
import static de.zettsystems.h3comsim.domain.UnitSpecialityType.SPECIAL;

// https://heroes.thelazy.net/index.php/Special_ability
public enum UnitSpeciality {

    NO_RETALIATION(DEFENSE),
    DEATH_STARE(AFTER_ATTACK),
    THUNDERBOLTS(AFTER_ATTACK),
    PETRYFYING(AFTER_ATTACK),
    POISONOUS(AFTER_ATTACK),
    CURSING(AFTER_ATTACK),
    DEATH_BLOW(AFTER_ATTACK),
    DEVIL_HATE(ATTACK),
    ANGEL_HATE(ATTACK),
    TITAN_HATE(ATTACK),
    NO_HAND_TO_HAND_PENALTY(ATTACK),
    TWO_BLOWS(ATTACK),
    TWO_SHOTS(ATTACK),
    GOOD_MORALE(AFTER_ATTACK),

    // Race markers — set on the defender to enable hate-based damage modifiers from the attacker.
    ANGEL_RACE(SPECIAL),
    DEVIL_RACE(SPECIAL),
    TITAN_RACE(SPECIAL),

    // Not yet evaluated by the engine — defined for unit catalog tagging only.
    IMMUNE_TO_BLIND(DEFENSE),
    IMPACT_DAMAGE(ATTACK),
    NO_OBSTACLE_PENALTY(SPECIAL),
    MOVE_BACK(SPECIAL),
    RESURRECTION(SPECIAL),
    COUNTERSTRIKE_TWICE(DEFENSE),
    COUNERSTRIKE_UNLIMITED(DEFENSE),
    GOOD_ARMY_MORALE(SPECIAL),
    SPELL_COST_REDUCTION(SPECIAL),
    IMMUNE_TO_SPELLS(SPECIAL),
    IMMUNE_TO_SPELLS_BELOW_4(SPECIAL);

    private final UnitSpecialityType unitSpecialityType;

    UnitSpeciality(UnitSpecialityType unitSpecialityType) {
        this.unitSpecialityType = unitSpecialityType;
    }

    public boolean isAttack() {
        return this.unitSpecialityType == UnitSpecialityType.ATTACK;
    }
}
