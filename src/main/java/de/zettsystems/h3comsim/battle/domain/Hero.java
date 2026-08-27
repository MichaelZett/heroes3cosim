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
}
