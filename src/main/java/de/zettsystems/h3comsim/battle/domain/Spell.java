package de.zettsystems.h3comsim.battle.domain;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Die Zauber, die Kreaturen im Kampf wirken oder auslösen (Manual, <em>Spell Reference</em>,
 * Schule und Stufe von dort). Aufgenommen sind die Zauber der Ogre Magi (Bloodlust), Unicorns
 * (Blind), Serpent und Dragon Flies (Dispel, Weakness) und die nützlichen Zauber, die ein Master
 * Genie wirken kann und die im Simulator etwas bewirken. Nicht aufgenommen: die vier
 * Protection-Zauber und Magic Mirror (es gibt keine gegnerischen Zauber, gegen die sie wirken)
 * sowie Anti-Magic.
 *
 * <p>{@code mind} markiert die Gedankenzauber (heroes.thelazy.net, <em>Mind spells</em>), gegen
 * die Untote, Elementare, Golems, Gargoyles, Giants und Titans immun sind.
 */
@Schema(description = "Zauber, den eine Kreatur wirkt oder auslöst.", enumAsRef = true)
public enum Spell {
    BLESS(MagicSchool.WATER, 1, true, false),
    BLOODLUST(MagicSchool.FIRE, 1, true, false),
    CURE(MagicSchool.WATER, 1, true, false),
    DISPEL(MagicSchool.WATER, 1, false, false),
    HASTE(MagicSchool.AIR, 1, true, false),
    SHIELD(MagicSchool.EARTH, 1, true, false),
    STONE_SKIN(MagicSchool.EARTH, 1, true, false),
    BLIND(MagicSchool.FIRE, 2, false, true),
    FORTUNE(MagicSchool.AIR, 2, true, false),
    PRECISION(MagicSchool.AIR, 2, true, false),
    WEAKNESS(MagicSchool.WATER, 2, false, false),
    AIR_SHIELD(MagicSchool.AIR, 3, true, false),
    MIRTH(MagicSchool.WATER, 3, true, true),
    COUNTERSTRIKE(MagicSchool.AIR, 4, true, false),
    FIRE_SHIELD(MagicSchool.FIRE, 4, true, false),
    FRENZY(MagicSchool.FIRE, 4, true, true),
    PRAYER(MagicSchool.WATER, 4, true, false),
    SLAYER(MagicSchool.FIRE, 4, true, false);

    private final MagicSchool school;
    private final int level;
    private final boolean beneficial;
    private final boolean mind;

    Spell(MagicSchool school, int level, boolean beneficial, boolean mind) {
        this.school = school;
        this.level = level;
        this.beneficial = beneficial;
        this.mind = mind;
    }

    public MagicSchool school() {
        return school;
    }

    public int level() {
        return level;
    }

    /** Nützlicher Zauber für das Ziel; feindliche Zauber lassen sich per Magieresistenz abwehren. */
    public boolean beneficial() {
        return beneficial;
    }

    public boolean mind() {
        return mind;
    }
}
