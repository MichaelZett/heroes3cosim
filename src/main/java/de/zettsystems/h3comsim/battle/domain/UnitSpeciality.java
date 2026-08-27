package de.zettsystems.h3comsim.battle.domain;

import static de.zettsystems.h3comsim.battle.domain.UnitSpecialityType.AFTER_ATTACK;
import static de.zettsystems.h3comsim.battle.domain.UnitSpecialityType.ATTACK;
import static de.zettsystems.h3comsim.battle.domain.UnitSpecialityType.DEFENSE;
import static de.zettsystems.h3comsim.battle.domain.UnitSpecialityType.SPECIAL;

// https://heroes.thelazy.net/index.php/Special_ability
public enum UnitSpeciality {

    NO_RETALIATION(DEFENSE),
    DEATH_STARE(AFTER_ATTACK),
    THUNDERBOLTS(AFTER_ATTACK),
    PETRYFYING(AFTER_ATTACK),
    POISONOUS(AFTER_ATTACK),
    CURSING(AFTER_ATTACK),
    DISEASES(AFTER_ATTACK),
    AGING(AFTER_ATTACK),
    DEATH_BLOW(AFTER_ATTACK),
    DEVIL_HATE(ATTACK),
    ANGEL_HATE(ATTACK),
    TITAN_HATE(ATTACK),
    NO_HAND_TO_HAND_PENALTY(ATTACK),
    NO_DISTANCE_PENALTY(ATTACK),
    TWO_BLOWS(ATTACK),
    TWO_SHOTS(ATTACK),
    GOOD_MORALE(AFTER_ATTACK),
    REGENERATION(SPECIAL),
    REBIRTH(SPECIAL),
    /** Vampire Lord (Manual S. 101): heilt sich am verursachten Nahkampf-Schaden, kann
     *  bis zur Start-Stack-Größe tote eigene Vampire Lords resurrecten. */
    LIFE_DRAIN(SPECIAL),
    /** Devil/Arch Devil (Manual S. 99): „can teleport to any hex on the battlefield".
     *  Bewegung ignoriert die Speed-Schranke; Lande-Hex muss nur passable und frei sein. */
    TELEPORT_NO_COST(SPECIAL),
    FIRE_SHIELD(DEFENSE),

    // Race markers — set on the defender to enable hate-based damage modifiers from the attacker.
    ANGEL_RACE(SPECIAL),
    DEVIL_RACE(SPECIAL),
    TITAN_RACE(SPECIAL),

    // Evaluated by the engine.
    /** Cavalier/Champion: +5 % damage per hex of run-up, capped at +50 % (Stack). */
    IMPACT_DAMAGE(ATTACK),
    /** Negates the half-damage penalty for an obstacle in the line of fire (Battle). */
    NO_OBSTACLE_PENALTY(SPECIAL),
    /** Harpy: returns to its starting hex after a melee strike (Battle). */
    MOVE_BACK(SPECIAL),
    COUNTERSTRIKE_TWICE(DEFENSE),
    COUNTERSTRIKE_UNLIMITED(DEFENSE),
    /** Behemoth line: cuts the defender's effective defense (Stack). */
    DEFENSE_REDUCTION_40(ATTACK),
    DEFENSE_REDUCTION_80(ATTACK),

    // Not yet evaluated by the engine — defined for unit catalog tagging only.
    // Most of these need the spell/hero system before they can be evaluated.
    IMMUNE_TO_BLIND(DEFENSE),
    RESURRECTION(SPECIAL),
    GOOD_ARMY_MORALE(SPECIAL),
    SPELL_COST_REDUCTION(SPECIAL),
    IMMUNE_TO_SPELLS(SPECIAL),
    IMMUNE_TO_SPELLS_BELOW_4(SPECIAL),
    CASTS_BLOODLUST(SPECIAL),
    ATTACKS_WALLS(SPECIAL),

    // Multi-Stack-Fähigkeiten (greifen erst, wenn mehr als ein Gegner-Stack auf dem Feld steht).
    /** Cerberus: Nahkampf trifft bis zu drei adjazente Hexen der eigenen Position
     *  gleichzeitig — Friendly Fire inklusive, eigene Nachbarn werden mitgetroffen. */
    THREE_HEADED_ATTACK(ATTACK),
    /** Green/Gold/Red/Black/Azure Dragon: Nahkampf trifft zwei inline-Hexe (Ziel +
     *  dahinterliegender). Friendly Fire inklusive, wenn dort ein eigener Stack steht. */
    FIRE_BREATH(ATTACK),
    /** Magog: ranged-Hit als 3-Hex-Splash (Ziel-Hex + zwei seitliche Nachbarn).
     *  Friendly Fire inklusive — eigene Stacks im Radius werden mitgetroffen. */
    SPLASH_SHOT(ATTACK),
    /** Lich: ranged-Hit erzeugt 7-Hex-AoE rund ums Ziel (Death Cloud). Friendly Fire
     *  inklusive, aber Untote sind immun — eine reine Necropolis-Armee trifft sich nicht. */
    DEATH_CLOUD(ATTACK);

    private final UnitSpecialityType unitSpecialityType;

    UnitSpeciality(UnitSpecialityType unitSpecialityType) {
        this.unitSpecialityType = unitSpecialityType;
    }

    public boolean isAttack() {
        return this.unitSpecialityType == UnitSpecialityType.ATTACK;
    }
}
