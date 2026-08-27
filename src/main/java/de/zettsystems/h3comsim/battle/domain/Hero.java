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
 * <p>Das Level fehlt bewusst: Fortschritt gehört auf die Abenteuerkarte, die es hier nicht
 * gibt. Der Katalog führt die Startwerte, mit denen ein Held rekrutiert wird.
 *
 * @param skills Startfertigkeiten mit ihrer Ausbaustufe. Heute nirgends ausgewertet —
 *               siehe {@link SecondarySkill}.
 */
public record Hero(
        String name,
        HeroClass heroClass,
        Faction faction,
        int attack,
        int defense,
        int power,
        int knowledge,
        Map<SecondarySkill, SkillLevel> skills
) {
    public Hero {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(heroClass, "heroClass");
        Objects.requireNonNull(faction, "faction");
        Objects.requireNonNull(skills, "skills");
        requireNonNegative(attack, "attack");
        requireNonNegative(defense, "defense");
        requireNonNegative(power, "power");
        requireNonNegative(knowledge, "knowledge");
        skills = Map.copyOf(skills);
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
}
