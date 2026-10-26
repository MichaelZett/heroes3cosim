package de.zettsystems.h3comsim.battle.domain;

import de.zettsystems.h3comsim.battle.domain.ArtifactSlot.Kind;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Die Artefakte, die ein Held trägt, je Platz wie im Heldenfenster. Geprüft wird, was das Spiel
 * erzwingt: jedes Artefakt auf einem Platz seiner Art, und ein Kombi-Artefakt braucht freie Plätze
 * für seine übrigen Teile.
 *
 * <p>Jedes Artefakt höchstens einmal, und kein Teil neben seinem Kombi-Artefakt. Im Spiel kann
 * ein Held zwei gleiche Artefakte finden; ob ihre Wirkungen sich dann addieren, ist je Artefakt
 * verschieden und kaum belegt, darum lässt der Simulator es nicht zu.
 */
public final class Equipment {

    /** Keine Artefakte. */
    public static final Equipment NONE = new Equipment(Map.of());

    private final Map<ArtifactSlot, Artifact> slots;
    // Vorberechnet: Stack und Held fragen diese Summen in jedem Zug ab.
    private final Artifact.Stats stats;
    private final int luck;
    private final int morale;
    private final int spellDuration;
    private final int magicResistance;
    private final int archery;
    private final int speed;
    private final int health;
    private final int livingHealthPercent;
    private final int dragonBonus;

    public Equipment(Map<ArtifactSlot, Artifact> slots) {
        Map<ArtifactSlot, Artifact> copy = new EnumMap<>(ArtifactSlot.class);
        copy.putAll(slots);
        validate(copy);
        this.slots = Collections.unmodifiableMap(copy);
        this.stats = sumStats(copy.values());
        this.luck = sum(ArtifactEffect.Luck.class, ArtifactEffect.Luck::value);
        this.morale = sum(ArtifactEffect.Morale.class, ArtifactEffect.Morale::value);
        this.spellDuration = sum(ArtifactEffect.SpellDuration.class, ArtifactEffect.SpellDuration::rounds);
        this.magicResistance = sum(ArtifactEffect.MagicResistance.class, ArtifactEffect.MagicResistance::percent);
        this.archery = sum(ArtifactEffect.Archery.class, ArtifactEffect.Archery::percent);
        this.speed = sum(ArtifactEffect.Speed.class, ArtifactEffect.Speed::value);
        this.health = sum(ArtifactEffect.Health.class, ArtifactEffect.Health::value);
        this.livingHealthPercent = sum(ArtifactEffect.LivingVitality.class, ArtifactEffect.LivingVitality::percent);
        this.dragonBonus = sum(ArtifactEffect.DragonBonus.class, ArtifactEffect.DragonBonus::value);
    }

    /** Die belegten Plätze. */
    public Map<ArtifactSlot, Artifact> slots() {
        return slots;
    }

    /**
     * Legt die Artefakte auf die ersten freien Plätze ihrer Art.
     *
     * @throws IllegalArgumentException wenn sie nicht zusammen passen
     */
    public static Equipment of(Collection<Artifact> artifacts) {
        Map<ArtifactSlot, Artifact> slots = new EnumMap<>(ArtifactSlot.class);
        for (Artifact artifact : artifacts) {
            ArtifactSlot free = ArtifactSlot.of(artifact.kind()).stream()
                    .filter(slot -> !slots.containsKey(slot))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("no free " + artifact.kind() + " slot for "
                            + artifact.displayName()));
            slots.put(free, artifact);
        }
        return new Equipment(slots);
    }

    public static Equipment of(Artifact... artifacts) {
        return of(List.of(artifacts));
    }

    private static void validate(Map<ArtifactSlot, Artifact> slots) {
        Set<Artifact> seen = EnumSet.noneOf(Artifact.class);
        Map<Kind, Integer> needed = new EnumMap<>(Kind.class);
        slots.forEach((slot, artifact) -> {
            if (slot.kind() != artifact.kind()) {
                throw new IllegalArgumentException(artifact.displayName() + " does not fit " + slot);
            }
            if (!seen.add(artifact)) {
                throw new IllegalArgumentException(artifact.displayName() + " is worn twice");
            }
            needed.merge(slot.kind(), 1, Integer::sum);
            artifact.blocks().forEach(kind -> needed.merge(kind, 1, Integer::sum));
        });
        for (Artifact artifact : seen) {
            for (Artifact part : artifact.components()) {
                if (seen.contains(part)) {
                    throw new IllegalArgumentException(part.displayName() + " is part of the worn "
                            + artifact.displayName());
                }
            }
        }
        needed.forEach((kind, count) -> {
            if (count > ArtifactSlot.of(kind).size()) {
                throw new IllegalArgumentException("not enough " + kind + " slots: " + count + " needed");
            }
        });
    }

    public Collection<Artifact> artifacts() {
        return slots.values();
    }

    public boolean isEmpty() {
        return slots.isEmpty();
    }

    public boolean has(Artifact artifact) {
        return slots.containsValue(artifact);
    }

    /** Summe der Primärwerte. */
    public Artifact.Stats stats() {
        return stats;
    }

    private static Artifact.Stats sumStats(Collection<Artifact> artifacts) {
        int attack = 0;
        int defense = 0;
        int power = 0;
        int knowledge = 0;
        for (Artifact artifact : artifacts) {
            attack += artifact.stats().attack();
            defense += artifact.stats().defense();
            power += artifact.stats().power();
            knowledge += artifact.stats().knowledge();
        }
        return new Artifact.Stats(attack, defense, power, knowledge);
    }

    /** Goldwert aller Artefakte. */
    public int value() {
        return slots.values().stream().mapToInt(Artifact::value).sum();
    }

    /** Alle Wirkungen dieser Art. */
    public <T extends ArtifactEffect> Stream<T> effects(Class<T> type) {
        return slots.values().stream()
                .flatMap(artifact -> artifact.effects().stream())
                .filter(type::isInstance)
                .map(type::cast);
    }

    public boolean has(Class<? extends ArtifactEffect> type) {
        return effects(type).findAny().isPresent();
    }

    /** Summe eines Zahlwerts über alle Wirkungen dieser Art. */
    private <T extends ArtifactEffect> int sum(Class<T> type, Function<T, Integer> value) {
        return effects(type).mapToInt(value::apply).sum();
    }

    public int luck() {
        return luck;
    }

    public int morale() {
        return morale;
    }

    public int spellDuration() {
        return spellDuration;
    }

    public int magicResistance() {
        return magicResistance;
    }

    public int archery() {
        return archery;
    }

    public int speed() {
        return speed;
    }

    public int health() {
        return health;
    }

    public int livingHealthPercent() {
        return livingHealthPercent;
    }

    public int dragonBonus() {
        return dragonBonus;
    }

    /** Prozent auf den Schaden eines Zaubers seiner Schule; bei Magic Arrow zählt der beste Orb. */
    public int spellDamagePercent(Spell spell) {
        return effects(ArtifactEffect.SpellDamage.class)
                .filter(orb -> spell.schools().contains(orb.school()))
                .mapToInt(ArtifactEffect.SpellDamage::percent)
                .max().orElse(0);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        return o instanceof Equipment other && slots.equals(other.slots);
    }

    @Override
    public int hashCode() {
        return slots.hashCode();
    }

    @Override
    public String toString() {
        return "Equipment" + slots;
    }

    /** Höchste Zauberstufe, die dieses Artefakt beiden Helden noch erlaubt; 5 ohne Sperre. */
    public int maxSpellLevel() {
        return effects(ArtifactEffect.BlockMagic.class)
                .mapToInt(ArtifactEffect.BlockMagic::maxLevel)
                .min().orElse(5);
    }

    /** Ist die Armee gegen diesen Zauber immun? */
    public boolean grantsImmunity(Spell spell) {
        return effects(ArtifactEffect.SpellImmunity.class).anyMatch(immunity -> immunity.spells().contains(spell))
                || effects(ArtifactEffect.LevelImmunity.class).anyMatch(immunity -> spell.level() <= immunity.maxLevel())
                || (spell.mind() && has(ArtifactEffect.MindImmunity.class));
    }
}
