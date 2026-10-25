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
    /** Elementar-Marker (Manual S. 43): „Undead creatures and elementals have a morale of zero,
     *  independent of their army's morale rating." Gesetzt auf die zehn Conflux-Elementale —
     *  Firebird, Phoenix, Pixie und Sprite gehören ausdrücklich <em>nicht</em> dazu.
     *  Bewusst ein eigener Marker statt einer Namensprüfung auf „… Elemental": die Zugehörigkeit
     *  ist eine Eigenschaft der Kreatur, keine ihrer Schreibweise. */
    ELEMENTAL_RACE(SPECIAL),

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
    /** Bone/Ghost Dragon (Manual S. 103): „lower the morale of enemy units by one when they
     *  appear on the battlefield". Wirkt auf das Moral-Rating der Gegenseite (ArmyMorale),
     *  einmal pro Armee — zwei Drachen-Stacks senken nicht um zwei. */
    LOWERS_ENEMY_MORALE(SPECIAL),
    /** Arch Angel (Manual S. 87): „able to resurrect dead allied troops once per combat".
     *  Die Menge steht nicht im Manual; SoD-Vanilla laut heroes.thelazy.net: 100 HP je Arch
     *  Angel im Stack, dauerhaft. Regeln und Ziel-Wahl in {@link Resurrection}. */
    RESURRECTION(SPECIAL),
    /** Golems und Gargoyles: keine Lebewesen. Wie Untote und Elementare nicht
     *  wiederbelebbar (heroes.thelazy.net, Archangel: „Golems, Gargoyles, Elementals …
     *  can never be resurrected"). */
    NON_LIVING(SPECIAL),
    /** Pit Lord (Manual S. 98): beschwört einmal pro Kampf statt einer Aktion aus einem ganz
     *  gefallenen verbündeten Stack Dämonen. Regeln und Ziel-Wahl in {@link DemonRaising}. */
    RAISE_DEMONS(SPECIAL),
    /** Devil/Arch Devil (Manual S. 99): „reduce enemy Luck by one when they appear on the
     *  battlefield". Wirkt auf das Glücks-Rating der Gegenseite ({@link ArmyLuck}), einmal pro
     *  Armee. */
    LOWERS_ENEMY_LUCK(SPECIAL),

    // Zauber (siehe SpellResistance und CreatureSpellcasting).
    /** Troglodytes (Manual S. 89): „immune to blinding effects". */
    IMMUNE_TO_BLIND(DEFENSE),
    /** Black Dragon, Magic Elemental: gegen alle Zauber immun. */
    IMMUNE_TO_SPELLS(SPECIAL),
    /** Red Dragon (Manual S. 107): „immune to spells below the fourth level". */
    IMMUNE_TO_SPELLS_BELOW_4(SPECIAL),
    /** Gold Dragon (Manual S. 107): „only affected by spells of level five". */
    IMMUNE_TO_SPELLS_BELOW_5(SPECIAL),
    /** Efreet, Fire/Energy Elemental, Firebird, Phoenix: immun gegen die Schule des Feuers. */
    IMMUNE_TO_FIRE_SPELLS(SPECIAL),
    /** Giants und Titans (Manual S. 115): „immune to spells affecting the mind". Untote,
     *  Elementare und nicht lebende Kreaturen sind es ohne eigenen Marker. */
    IMMUNE_TO_MIND_SPELLS(SPECIAL),
    /** Dwarf (Manual S. 105): 20 % Chance, einen feindlichen Zauber abzuwehren. */
    MAGIC_RESISTANCE_20(DEFENSE),
    /** Battle Dwarf (Manual S. 105): 40 %. */
    MAGIC_RESISTANCE_40(DEFENSE),
    /** Unicorns (Manual S. 106): benachbarte Verbündete wehren feindliche Zauber zu 20 % ab. */
    MAGIC_RESISTANCE_AURA(SPECIAL),
    /** Ogre Magi (Manual S. 110): Bloodlust auf Verbündete, dreimal pro Kampf. */
    CASTS_BLOODLUST(SPECIAL),
    /** Storm, Ice, Energy und Magma Elemental (heroes.thelazy.net): Protection from Air, Water,
     *  Fire bzw. Earth auf Verbündete, Advanced mit Power 6, dreimal pro Kampf. */
    CASTS_PROTECTION(SPECIAL),
    /** Master Genie (Manual S. 114): zufälliger nützlicher Zauber, dreimal pro Kampf. */
    CASTS_RANDOM_BENEFICIAL(SPECIAL),
    /** Unicorns (Manual S. 106): 20 % Chance, das Ziel des Angriffs zu blenden. */
    BLINDING(AFTER_ATTACK),
    /** Serpent und Dragon Fly (Manual S. 93): heben nützliche Zauber des Ziels auf. */
    DISPELS_BENEFICIAL(AFTER_ATTACK),
    /** Dragon Fly (SoD, heroes.thelazy.net): Weakness auf Advanced nach jedem Angriff. */
    CASTS_WEAKNESS(AFTER_ATTACK),
    /** Magi und Arch Magi (Manual S. 114): Zauber des eigenen Helden kosten 2 Mana weniger. */
    SPELL_COST_REDUCTION(SPECIAL),
    /** Pegasi und Silver Pegasi (Manual S. 106): Zauber des gegnerischen Helden kosten 2 Mana mehr. */
    SPELL_COST_INCREASE(SPECIAL),

    // Not yet evaluated by the engine — defined for unit catalog tagging only.
    GOOD_ARMY_MORALE(SPECIAL),
    /** Stone Golem (Manual S. 113): halber Schaden durch Zauber. */
    SPELL_DAMAGE_HALVED(DEFENSE),
    /** Iron Golem (Manual S. 113): ein Viertel des Zauberschadens. */
    SPELL_DAMAGE_QUARTERED(DEFENSE),
    ATTACKS_WALLS(SPECIAL),

    // Multi-Stack-Fähigkeiten (greifen erst, wenn mehr als ein Gegner-Stack auf dem Feld steht).
    /** Cerberus (Manual S. 97): „can attack three adjacent enemies in a single attack" — das
     *  Ziel und die beiden Felder links und rechts davon, aus der Angriffsrichtung gesehen
     *  (heroes.thelazy.net). Trifft nur Gegner. */
    THREE_HEADED_ATTACK(ATTACK),
    /** Green/Gold/Red/Black/Azure Dragon: Nahkampf trifft zwei inline-Hexe (Ziel +
     *  dahinterliegender). Friendly Fire inklusive, wenn dort ein eigener Stack steht. */
    FIRE_BREATH(ATTACK),
    /** Magog (Manual S. 97): „fireblasts affect the target hex and its surrounding hexes like
     *  a Fireball spell" — Ziel plus alle sechs Nachbarn. Friendly Fire inklusive, eigene
     *  Stacks im Radius werden mitgetroffen. */
    SPLASH_SHOT(ATTACK),
    /** Lich: ranged-Hit erzeugt 7-Hex-AoE rund ums Ziel (Death Cloud). Friendly Fire
     *  inklusive, aber Untote sind immun — eine reine Necropolis-Armee trifft sich nicht. */
    DEATH_CLOUD(ATTACK),
    /** Hydra, Chaos Hydra, Psychic und Magic Elemental: der Nahkampf trifft alle benachbarten
     *  Gegner (heroes.thelazy.net, „Attacks all adjacent enemies"). */
    ATTACKS_ALL_ADJACENT(ATTACK),
    /** Dendroid Guard und Soldier: ihr Angriff fesselt das Ziel an seinen Platz, bis die
     *  Dendroiden sich bewegen oder fallen (heroes.thelazy.net, <em>Dendroid Guard</em>). */
    BINDS(AFTER_ATTACK),
    /** Wraith (Manual S. 102): zieht dem gegnerischen Helden je Runde 2 Mana ab, je Stack. */
    MANA_DRAIN(SPECIAL),
    /** Familiar (Manual S. 96): 20 % des Manas, das der gegnerische Held ausgibt, abgerundet,
     *  fließen dem eigenen zu — einmal, egal wie viele Familiar-Stacks. */
    MANA_CHANNEL(SPECIAL),
    /** Pikeman und Halberdier: der Anlauf-Bonus von Cavalier und Champion wirkt gegen sie nicht
     *  (heroes.thelazy.net, „Immune to jousting bonus"). */
    JOUSTING_IMMUNE(DEFENSE),

    // Hass-Paare (heroes.thelazy.net): Genie und Efreet, Titan und Black Dragon +50 %, Elementare gegen ihr Gegenelement
    // +100 % — Hass-Marker am Angreifer, Volks-Marker am Ziel, ausgewertet in Stack.takeDamage.
    GENIE_HATE(ATTACK),
    GENIE_RACE(SPECIAL),
    EFREET_HATE(ATTACK),
    EFREET_RACE(SPECIAL),
    AIR_HATE(ATTACK),
    AIR_RACE(SPECIAL),
    EARTH_HATE(ATTACK),
    EARTH_RACE(SPECIAL),
    FIRE_HATE(ATTACK),
    FIRE_RACE(SPECIAL),
    WATER_HATE(ATTACK),
    WATER_RACE(SPECIAL),
    /** Titan gegen Black Dragon (+50 %); umgekehrt gilt {@link #TITAN_HATE}. */
    BLACK_DRAGON_HATE(ATTACK),
    BLACK_DRAGON_RACE(SPECIAL);

    private final UnitSpecialityType unitSpecialityType;

    UnitSpeciality(UnitSpecialityType unitSpecialityType) {
        this.unitSpecialityType = unitSpecialityType;
    }

    public boolean isAttack() {
        return this.unitSpecialityType == UnitSpecialityType.ATTACK;
    }
}
