package de.zettsystems.h3comsim.battle.domain;

import java.util.Map;
import java.util.Objects;

/**
 * Ein Held, der eine Armee in die Schlacht führt (Manual S. 33).
 *
 * <p>Von den vier Primärwerten wirken heute nur zwei:
 * <ul>
 *   <li><strong>Attack</strong> — „A hero's Attack skill number is added to each of their
 *       creature's attack rating"; ausgewertet in {@link Stack#getAttack()}.</li>
 *   <li><strong>Defense</strong> — „is added to each of their army creature's defense rating";
 *       ausgewertet in {@link Stack#getDefense()}.</li>
 *   <li><strong>Power</strong> und <strong>Knowledge</strong> steuern ausschließlich das
 *       Zaubern (Spell Power bzw. 10 Mana-Punkte je Punkt Knowledge). Ohne Zaubersystem sind
 *       sie reine Daten — sie werden geführt und ausgeliefert, aber nirgends gelesen.</li>
 * </ul>
 *
 * <p>Der Katalog führt die Startwerte, mit denen ein Held rekrutiert wird, auf Level 1. Das
 * Level wirkt nur über die Spezialität ({@link HeroSpecialty}); was ein Held beim Aufstieg an
 * Primärwerten und Fertigkeiten dazugewinnt, ist Zufall auf der Abenteuerkarte und wird nicht
 * nachgewürfelt.
 *
 * @param skills    Startfertigkeiten mit ihrer Ausbaustufe. Welche davon wirken, steht bei
 *                  {@link SecondarySkill}.
 * @param specialty Spezialfähigkeit („Unique Ability")
 * @param level     Heldenlevel, ab 1
 */
public record Hero(
        String name,
        HeroClass heroClass,
        Faction faction,
        int attack,
        int defense,
        int power,
        int knowledge,
        Map<SecondarySkill, SkillLevel> skills,
        HeroSpecialty specialty,
        int level
) {
    public Hero {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(heroClass, "heroClass");
        Objects.requireNonNull(faction, "faction");
        Objects.requireNonNull(skills, "skills");
        Objects.requireNonNull(specialty, "specialty");
        if (level < 1) {
            throw new IllegalArgumentException("level must be at least 1, was " + level);
        }
        requireNonNegative(attack, "attack");
        requireNonNegative(defense, "defense");
        requireNonNegative(power, "power");
        requireNonNegative(knowledge, "knowledge");
        skills = Map.copyOf(skills);
    }

    /** Held ohne Spezialität auf Level 1 — für Tests, die eine Wirkung isolieren. */
    public Hero(String name, HeroClass heroClass, Faction faction, int attack, int defense,
                int power, int knowledge, Map<SecondarySkill, SkillLevel> skills) {
        this(name, heroClass, faction, attack, defense, power, knowledge, skills,
                HeroSpecialty.Other.NONE, 1);
    }

    /** Derselbe Held auf einem anderen Level. */
    public Hero withLevel(int newLevel) {
        return new Hero(name, heroClass, faction, attack, defense, power, knowledge, skills,
                specialty, newLevel);
    }

    private static void requireNonNegative(int value, String field) {
        if (value < 0) {
            throw new IllegalArgumentException(field + " must not be negative, was " + value);
        }
    }

    /** Ausbaustufe der Fertigkeit, {@link SkillLevel#NONE} wenn der Held sie nicht hat. */
    public SkillLevel levelOf(SecondarySkill skill) {
        return skills.getOrDefault(skill, SkillLevel.NONE);
    }

    /**
     * Offense, Manual S. 38: „Increases the amount of hand-to-hand damage the hero's troops
     * inflict in combat" — 10/20/30 %. Gilt ausdrücklich nur für Nahkampf.
     */
    public int offenseBonusPercent() {
        return switch (levelOf(SecondarySkill.OFFENSE)) {
            case NONE -> 0;
            case BASIC -> 10;
            case ADVANCED -> 20;
            case EXPERT -> 30;
        };
    }

    /**
     * Archery, Manual S. 35: „Increases the damage done by ranged attackers in the hero's army"
     * — 10/25/50 %. Die Stufen springen weiter als bei Offense; das ist so im Manual.
     */
    public int archeryBonusPercent() {
        return switch (levelOf(SecondarySkill.ARCHERY)) {
            case NONE -> 0;
            case BASIC -> 10;
            case ADVANCED -> 25;
            case EXPERT -> 50;
        };
    }

    /**
     * Offense in Promille, mit Spezialität (+5 % je Level auf den Effekt, siehe
     * {@link HeroSpecialty.Skill}). Crag Hack auf Level 1 mit Advanced Offense: 210 ‰.
     */
    public int offenseBonusPerMille() {
        return offenseBonusPercent() * specialty.skillFactorPerMille(SecondarySkill.OFFENSE, level) / 100;
    }

    /** Archery in Promille, mit Spezialität. */
    public int archeryBonusPerMille() {
        return archeryBonusPercent() * specialty.skillFactorPerMille(SecondarySkill.ARCHERY, level) / 100;
    }

    /** Armorer in Promille, mit Spezialität. */
    public int armorerReductionPerMille() {
        return armorerReductionPercent() * specialty.skillFactorPerMille(SecondarySkill.ARMORER, level) / 100;
    }

    /** Attack-Aufschlag der Spezialität für diese Kreatur. */
    public int specialtyAttackBonus(Unit unit) {
        return specialty.attackBonus(unit, level);
    }

    /** Defense-Aufschlag der Spezialität für diese Kreatur. */
    public int specialtyDefenseBonus(Unit unit) {
        return specialty.defenseBonus(unit, level);
    }

    /**
     * Armorer, Manual S. 35: „Reduces the amount of damage received by the hero's troops in
     * combat" — 5/10/15 %. Gilt für jeden eingehenden Schaden, egal ob Nah- oder Fernkampf.
     */
    public int armorerReductionPercent() {
        return switch (levelOf(SecondarySkill.ARMORER)) {
            case NONE -> 0;
            case BASIC -> 5;
            case ADVANCED -> 10;
            case EXPERT -> 15;
        };
    }

    /**
     * Leadership, Manual S. 37: „The hero's troops gain a morale rating bonus" — +1/2/3.
     * Anders als die drei Schadens-Fertigkeiten wirkt Leadership nicht auf den Schaden,
     * sondern auf das Armee-Moral-Rating; ausgewertet in {@link ArmyMorale}.
     *
     * <p>Das Manual schließt Necromancer und Death Knights von der Fertigkeit aus — der
     * Katalog bildet das über die Daten ab, nicht über eine Prüfung hier: kein
     * Necropolis-Held bringt Leadership mit.
     */
    public int leadershipMoraleBonus() {
        return switch (levelOf(SecondarySkill.LEADERSHIP)) {
            case NONE -> 0;
            case BASIC -> 1;
            case ADVANCED -> 2;
            case EXPERT -> 3;
        };
    }

    /**
     * Luck, Manual S. 37: „The hero's troops gain a bonus to their luck rating" — +1/2/3, geht in
     * das Armee-Glück ein ({@link ArmyLuck}).
     */
    public int luckBonus() {
        return switch (levelOf(SecondarySkill.LUCK)) {
            case NONE -> 0;
            case BASIC -> 1;
            case ADVANCED -> 2;
            case EXPERT -> 3;
        };
    }
}
