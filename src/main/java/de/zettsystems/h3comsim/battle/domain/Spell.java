package de.zettsystems.h3comsim.battle.domain;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Set;

/**
 * Kampfzauber (Manual, <em>Spell Reference</em> S. 64–79; Schule, Stufe und Kosten von dort).
 *
 * <p>Aufgenommen sind die Zauber, die Kreaturen wirken oder auslösen, und die Heldenzauber, die im
 * Simulator eine Wirkung haben: Schaden, Stärkungen, Schwächungen, Schutz und Wiederbelebung.
 * Nicht aufgenommen sind Zauber, die Feldelemente oder Kontrolle über Kreaturen brauchen
 * (Force Field, Quicksand, Land Mine, Fire Wall, Hypnotize, Berserk, Teleport, Clone, Sacrifice,
 * Beschwörungen) sowie Magic Mirror.
 *
 * <p>{@code mind} markiert die Gedankenzauber (heroes.thelazy.net, <em>Mind spells</em>), gegen
 * die Untote, Elementare, Golems, Gargoyles, Giants und Titans immun sind. {@code massAtExpert}:
 * auf Expert wirkt der Zauber auf alle verbündeten bzw. feindlichen Stacks.
 */
@Schema(description = "Kampfzauber eines Helden oder einer Kreatur.", enumAsRef = true)
public enum Spell {
    // Stärkungen
    BLESS(MagicSchool.WATER, 1, 5, Kind.EFFECT, true, false, true),
    BLOODLUST(MagicSchool.FIRE, 1, 5, Kind.EFFECT, true, false, true),
    HASTE(MagicSchool.AIR, 1, 6, Kind.EFFECT, true, false, true),
    SHIELD(MagicSchool.EARTH, 1, 5, Kind.EFFECT, true, false, true),
    STONE_SKIN(MagicSchool.EARTH, 1, 5, Kind.EFFECT, true, false, true),
    PROTECTION_FROM_FIRE(MagicSchool.FIRE, 1, 5, Kind.EFFECT, true, false, true),
    PROTECTION_FROM_WATER(MagicSchool.WATER, 1, 5, Kind.EFFECT, true, false, true),
    FORTUNE(MagicSchool.AIR, 2, 7, Kind.EFFECT, true, false, true),
    PRECISION(MagicSchool.AIR, 2, 8, Kind.EFFECT, true, false, true),
    PROTECTION_FROM_AIR(MagicSchool.AIR, 2, 7, Kind.EFFECT, true, false, true),
    AIR_SHIELD(MagicSchool.AIR, 3, 12, Kind.EFFECT, true, false, true),
    ANTI_MAGIC(MagicSchool.EARTH, 3, 15, Kind.EFFECT, true, false, false),
    MIRTH(MagicSchool.WATER, 3, 12, Kind.EFFECT, true, true, true),
    PROTECTION_FROM_EARTH(MagicSchool.EARTH, 3, 12, Kind.EFFECT, true, false, true),
    COUNTERSTRIKE(MagicSchool.AIR, 4, 24, Kind.EFFECT, true, false, true),
    FIRE_SHIELD(MagicSchool.FIRE, 4, 16, Kind.EFFECT, true, false, false),
    FRENZY(MagicSchool.FIRE, 4, 16, Kind.EFFECT, true, true, false),
    PRAYER(MagicSchool.WATER, 4, 16, Kind.EFFECT, true, false, true),
    SLAYER(MagicSchool.FIRE, 4, 16, Kind.EFFECT, true, false, false),
    // Schwächungen
    CURSE(MagicSchool.FIRE, 1, 6, Kind.EFFECT, false, false, true),
    SLOW(MagicSchool.EARTH, 1, 6, Kind.EFFECT, false, false, true),
    BLIND(MagicSchool.FIRE, 2, 10, Kind.EFFECT, false, true, false),
    DISRUPTING_RAY(MagicSchool.AIR, 2, 10, Kind.EFFECT, false, false, false),
    WEAKNESS(MagicSchool.WATER, 2, 8, Kind.EFFECT, false, false, true),
    FORGETFULNESS(MagicSchool.WATER, 3, 12, Kind.EFFECT, false, true, true),
    MISFORTUNE(MagicSchool.FIRE, 3, 12, Kind.EFFECT, false, false, true),
    SORROW(MagicSchool.EARTH, 4, 16, Kind.EFFECT, false, true, true),
    // Sofortzauber auf Verbündete
    CURE(MagicSchool.WATER, 1, 6, Kind.INSTANT, true, false, true),
    DISPEL(MagicSchool.WATER, 1, 5, Kind.INSTANT, false, false, false),
    ANIMATE_DEAD(MagicSchool.EARTH, 3, 15, Kind.INSTANT, true, false, false),
    RESURRECTION(MagicSchool.EARTH, 4, 20, Kind.INSTANT, true, false, false),
    // Schaden
    MAGIC_ARROW(MagicSchool.AIR, 1, 5, Kind.DAMAGE, false, false, false),
    DEATH_RIPPLE(MagicSchool.EARTH, 2, 10, Kind.DAMAGE, false, false, false),
    ICE_BOLT(MagicSchool.WATER, 2, 8, Kind.DAMAGE, false, false, false),
    LIGHTNING_BOLT(MagicSchool.AIR, 2, 10, Kind.DAMAGE, false, false, false),
    DESTROY_UNDEAD(MagicSchool.AIR, 3, 15, Kind.DAMAGE, false, false, false),
    FIREBALL(MagicSchool.FIRE, 3, 15, Kind.DAMAGE, false, false, false),
    FROST_RING(MagicSchool.WATER, 3, 12, Kind.DAMAGE, false, false, false),
    ARMAGEDDON(MagicSchool.FIRE, 4, 24, Kind.DAMAGE, false, false, false),
    CHAIN_LIGHTNING(MagicSchool.AIR, 4, 24, Kind.DAMAGE, false, false, false),
    INFERNO(MagicSchool.FIRE, 4, 16, Kind.DAMAGE, false, false, false),
    METEOR_SHOWER(MagicSchool.EARTH, 4, 16, Kind.DAMAGE, false, false, false),
    IMPLOSION(MagicSchool.EARTH, 5, 30, Kind.DAMAGE, false, false, false);

    /** Wirkungsart. */
    public enum Kind {
        /** Liegt für eine Zahl von Runden auf dem Stack. */
        EFFECT,
        /** Wirkt sofort und bleibt nicht liegen. */
        INSTANT,
        /** Fügt Schaden zu. */
        DAMAGE
    }

    /** Zauber, die nur Kreaturen auslösen und ein Held nicht wirkt. */
    private static final Set<Spell> CREATURE_ONLY = Set.of(FRENZY);

    private final MagicSchool school;
    private final int level;
    private final int cost;
    private final Kind kind;
    private final boolean beneficial;
    private final boolean mind;
    private final boolean massAtExpert;

    Spell(MagicSchool school, int level, int cost, Kind kind, boolean beneficial, boolean mind,
          boolean massAtExpert) {
        this.school = school;
        this.level = level;
        this.cost = cost;
        this.kind = kind;
        this.beneficial = beneficial;
        this.mind = mind;
        this.massAtExpert = massAtExpert;
    }

    /**
     * Schule. Magic Arrow gehört allen vier Schulen an (Manual S. 64 ff.) und wird mit der besten
     * Schul-Fertigkeit des Helden gewirkt, siehe {@link #schools()}.
     */
    public MagicSchool school() {
        return school;
    }

    /** Alle Schulen, zu denen der Zauber gehört. */
    public Set<MagicSchool> schools() {
        return this == MAGIC_ARROW ? Set.of(MagicSchool.values()) : Set.of(school);
    }

    public int level() {
        return level;
    }

    /** Mana-Kosten (Manual). */
    public int cost() {
        return cost;
    }

    public Kind kind() {
        return kind;
    }

    /** Nützlicher Zauber für das Ziel; feindliche Zauber lassen sich per Magieresistenz abwehren. */
    public boolean beneficial() {
        return beneficial;
    }

    public boolean mind() {
        return mind;
    }

    public boolean massAtExpert() {
        return massAtExpert;
    }

    /** Kann ein Held den Zauber im Simulator wirken? */
    public boolean heroCastable() {
        return !CREATURE_ONLY.contains(this);
    }
}
