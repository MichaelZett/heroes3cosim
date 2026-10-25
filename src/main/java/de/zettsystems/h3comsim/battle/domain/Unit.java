package de.zettsystems.h3comsim.battle.domain;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public record Unit(
        String name,
        Stats stats,
        Combat combat,
        Movement movement,
        int cost,
        Faction faction,
        int tier,
        boolean upgrade,
        Set<UnitSpeciality> specialities
) {
    /**
     * Die zweifeldrigen Kreaturen und Kriegsmaschinen, wie sie die freie H3-Engine VCMI führt
     * ({@code doubleWide} in {@code config/creatures}). Angel ist einfeldig, Arch Angel nicht.
     */
    private static final Set<String> DOUBLE_WIDE = Set.of(
            "Griffin", "Royal Griffin", "Cavalier", "Champion", "Arch Angel",
            "Centaur", "Centaur Captain", "Pegasus", "Silver Pegasus", "Unicorn", "War Unicorn",
            "Green Dragon", "Gold Dragon", "Naga", "Naga Queen", "Hell Hound", "Cerberus",
            "Black Knight", "Dread Knight", "Bone Dragon", "Ghost Dragon",
            "Medusa", "Medusa Queen", "Manticore", "Scorpicore", "Red Dragon", "Black Dragon",
            "Wolf Rider", "Wolf Raider", "Roc", "Thunderbird", "Behemoth", "Ancient Behemoth",
            "Basilisk", "Greater Basilisk", "Gorgon", "Mighty Gorgon", "Wyvern", "Wyvern Monarch",
            "Hydra", "Chaos Hydra", "Water Elemental", "Ice Elemental", "Firebird", "Phoenix",
            "Ballista", "First Aid Tent", "Catapult");

    public Unit {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(stats, "stats");
        Objects.requireNonNull(combat, "combat");
        Objects.requireNonNull(movement, "movement");
        Objects.requireNonNull(faction, "faction");
        Objects.requireNonNull(specialities, "specialities");
        if (tier < 1 || tier > 7) {
            throw new IllegalArgumentException("tier must be 1..7, was " + tier);
        }
        specialities = Set.copyOf(specialities);
    }

    public int attack() {
        return stats.attack();
    }

    /** Belegt die Kreatur zwei Felder nebeneinander? */
    public boolean doubleWide() {
        return DOUBLE_WIDE.contains(name);
    }

    public int defense() {
        return stats.defense();
    }

    public int health() {
        return stats.health();
    }

    public int speed() {
        return stats.speed();
    }

    public int minDamage() {
        return combat.minDamage();
    }

    public int maxDamage() {
        return combat.maxDamage();
    }

    public int shots() {
        return combat.shots();
    }

    public AttackType attackType() {
        return combat.attackType();
    }

    /**
     * Individueller Moral-Bonus der Kreatur selbst, additiv zum Armee-Rating.
     * In RoE tragen ihn nur Minotaur und Minotaur King (Manual S. 89: „have good morale").
     */
    public int morale() {
        return specialities.contains(UnitSpeciality.GOOD_MORALE) ? 1 : 0;
    }

    /**
     * Manual S. 43: „Undead creatures and elementals have a morale of zero, independent of
     * their army's morale rating." Weder Leadership noch eine Armee-Zusammenstellung bewegt
     * diese Kreaturen — sie haben nie eine zweite Aktion und frieren nie ein.
     */
    public boolean hasFixedZeroMorale() {
        return isUndead() || specialities.contains(UnitSpeciality.ELEMENTAL_RACE);
    }

    /**
     * Ob eine Resurrection diese Kreatur zurückholen kann. Ausgenommen sind Untote, Elementare
     * und nicht-lebende Kreaturen (Golems, Gargoyles), dazu wer gegen Resurrection als Zauber
     * der vierten Stufe (Manual S. 71) immun ist: Magic Elemental und Black Dragon ganz, Gold
     * Dragon unterhalb Stufe 5. Der Red Dragon ist nur gegen Stufe 1–3 immun und bleibt
     * wiederbelebbar.
     */
    public boolean isResurrectable() {
        return isLiving()
                && !specialities.contains(UnitSpeciality.IMMUNE_TO_SPELLS)
                && !specialities.contains(UnitSpeciality.IMMUNE_TO_SPELLS_BELOW_5);
    }

    /** Lebewesen: weder untot noch Elementar noch Golem oder Gargoyle. */
    public boolean isLiving() {
        return !isUndead()
                && !specialities.contains(UnitSpeciality.ELEMENTAL_RACE)
                && !specialities.contains(UnitSpeciality.NON_LIVING);
    }

    public boolean hasSpeciality(UnitSpeciality speciality) {
        return specialities.contains(speciality);
    }

    public Set<UnitSpeciality> attackerSpecialities() {
        return specialities.stream()
                .filter(UnitSpeciality::isAttack)
                .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Undead-Klassifikation für engine-seitige Immunitäten (z.B. DEATH_CLOUD-Splash
     * überspringt Undead — Manual S. 101). H3 RoE: alle Necropolis-Creatures sind Undead.
     */
    public boolean isUndead() {
        return faction == Faction.NECROPOLIS;
    }

    public boolean hasPenality(AttackType usedAttackType) {
        if (usedAttackType == AttackType.HAND_TO_HAND) {
            return attackType() == AttackType.LONG_RANGE
                    && !hasSpeciality(UnitSpeciality.NO_HAND_TO_HAND_PENALTY);
        }
        return false;
    }
}
