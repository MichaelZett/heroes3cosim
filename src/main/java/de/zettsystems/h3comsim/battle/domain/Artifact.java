package de.zettsystems.h3comsim.battle.domain;

import de.zettsystems.h3comsim.battle.domain.ArtifactEffect.AlignmentMix;
import de.zettsystems.h3comsim.battle.domain.ArtifactEffect.Archery;
import de.zettsystems.h3comsim.battle.domain.ArtifactEffect.BlockMagic;
import de.zettsystems.h3comsim.battle.domain.ArtifactEffect.DragonBonus;
import de.zettsystems.h3comsim.battle.domain.ArtifactEffect.FreeShooting;
import de.zettsystems.h3comsim.battle.domain.ArtifactEffect.GrantsSpell;
import de.zettsystems.h3comsim.battle.domain.ArtifactEffect.Health;
import de.zettsystems.h3comsim.battle.domain.ArtifactEffect.LevelImmunity;
import de.zettsystems.h3comsim.battle.domain.ArtifactEffect.LevelSpells;
import de.zettsystems.h3comsim.battle.domain.ArtifactEffect.LivingVitality;
import de.zettsystems.h3comsim.battle.domain.ArtifactEffect.Luck;
import de.zettsystems.h3comsim.battle.domain.ArtifactEffect.MagicResistance;
import de.zettsystems.h3comsim.battle.domain.ArtifactEffect.MindImmunity;
import de.zettsystems.h3comsim.battle.domain.ArtifactEffect.Morale;
import de.zettsystems.h3comsim.battle.domain.ArtifactEffect.NegateImmunities;
import de.zettsystems.h3comsim.battle.domain.ArtifactEffect.NegateLuck;
import de.zettsystems.h3comsim.battle.domain.ArtifactEffect.NegateMorale;
import de.zettsystems.h3comsim.battle.domain.ArtifactEffect.NoShootingPenalty;
import de.zettsystems.h3comsim.battle.domain.ArtifactEffect.OpeningSpell;
import de.zettsystems.h3comsim.battle.domain.ArtifactEffect.SchoolSpells;
import de.zettsystems.h3comsim.battle.domain.ArtifactEffect.Speed;
import de.zettsystems.h3comsim.battle.domain.ArtifactEffect.SpellDamage;
import de.zettsystems.h3comsim.battle.domain.ArtifactEffect.SpellDuration;
import de.zettsystems.h3comsim.battle.domain.ArtifactEffect.SpellImmunity;
import de.zettsystems.h3comsim.battle.domain.ArtifactSlot.Kind;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Die Artefakte aus RoE, AB und SoD, die im Kampf wirken (heroes.thelazy.net, <em>List of
 * artifacts</em>; Wirkungen nach VCMI {@code config/artifacts.json}). Was nur auf der
 * Abenteuerkarte oder nach der Schlacht wirkt — Bewegung, Ressourcen, Necromancy, Eagle Eye,
 * Rückzug —, fehlt, ebenso die Kombi-Artefakte, die nur solche Teile verbinden.
 *
 * <p>Kombi-Artefakte führen die Summe ihrer Teile plus der eigenen Wirkung, wie das Wiki sie
 * angibt; sie belegen ihren Platz und sperren die Plätze der übrigen Teile ({@link #blocks()}).
 *
 * @see Equipment
 */
@Schema(description = "Artefakt eines Helden.", enumAsRef = true)
// Stats ist ein Record aus ints, die Listen kommen aus List.of/List.copyOf und sind unveränderlich.
@SuppressWarnings("ImmutableEnumChecker")
public enum Artifact {
    // Waffen
    CENTAURS_AXE("Centaur's Axe", Kind.WEAPON, Rarity.TREASURE, 2000, stats(2, 0, 0, 0)),
    BLACKSHARD_OF_THE_DEAD_KNIGHT("Blackshard of the Dead Knight", Kind.WEAPON, Rarity.MINOR, 3000,
            stats(3, 0, 0, 0)),
    GREATER_GNOLLS_FLAIL("Greater Gnoll's Flail", Kind.WEAPON, Rarity.MINOR, 4000, stats(4, 0, 0, 0)),
    OGRES_CLUB_OF_HAVOC("Ogre's Club of Havoc", Kind.WEAPON, Rarity.MAJOR, 5000, stats(5, 0, 0, 0)),
    SWORD_OF_HELLFIRE("Sword of Hellfire", Kind.WEAPON, Rarity.MAJOR, 6000, stats(6, 0, 0, 0)),
    RED_DRAGON_FLAME_TONGUE("Red Dragon Flame Tongue", Kind.WEAPON, Rarity.MINOR, 4000, stats(2, 2, 0, 0)),
    TITANS_GLADIUS("Titan's Gladius", Kind.WEAPON, Rarity.RELIC, 10000, stats(12, -3, 0, 0)),
    SWORD_OF_JUDGEMENT("Sword of Judgement", Kind.WEAPON, Rarity.RELIC, 20000, stats(5, 5, 5, 5)),
    // VCMI und die Artefakt-Seite des Wikis: +3 Power, +6 Knowledge (die Wiki-Tabelle vertauscht sie).
    ARMAGEDDONS_BLADE("Armageddon's Blade", Kind.WEAPON, Rarity.RELIC, 50000, stats(3, 3, 3, 6),
            new GrantsSpell(Spell.ARMAGEDDON, SkillLevel.EXPERT), new SpellImmunity(Set.of(Spell.ARMAGEDDON))),
    // Schilde
    SHIELD_OF_THE_DWARVEN_LORDS("Shield of the Dwarven Lords", Kind.SHIELD, Rarity.TREASURE, 2000,
            stats(0, 2, 0, 0)),
    SHIELD_OF_THE_YAWNING_DEAD("Shield of the Yawning Dead", Kind.SHIELD, Rarity.MINOR, 3000, stats(0, 3, 0, 0)),
    BUCKLER_OF_THE_GNOLL_KING("Buckler of the Gnoll King", Kind.SHIELD, Rarity.MINOR, 4000, stats(0, 4, 0, 0)),
    TARG_OF_THE_RAMPAGING_OGRE("Targ of the Rampaging Ogre", Kind.SHIELD, Rarity.MAJOR, 5000, stats(0, 5, 0, 0)),
    SHIELD_OF_THE_DAMNED("Shield of the Damned", Kind.SHIELD, Rarity.MAJOR, 6000, stats(0, 6, 0, 0)),
    DRAGON_SCALE_SHIELD("Dragon Scale Shield", Kind.SHIELD, Rarity.MAJOR, 6000, stats(3, 3, 0, 0)),
    LIONS_SHIELD_OF_COURAGE("Lion's Shield of Courage", Kind.SHIELD, Rarity.RELIC, 16000, stats(4, 4, 4, 4)),
    SENTINELS_SHIELD("Sentinel's Shield", Kind.SHIELD, Rarity.RELIC, 10000, stats(-3, 12, 0, 0)),
    // Rüstungen
    BREASTPLATE_OF_PETRIFIED_WOOD("Breastplate of Petrified Wood", Kind.TORSO, Rarity.TREASURE, 1000,
            stats(0, 0, 1, 0)),
    RIB_CAGE("Rib Cage", Kind.TORSO, Rarity.MINOR, 3000, stats(0, 0, 2, 0)),
    SCALES_OF_THE_GREATER_BASILISK("Scales of the Greater Basilisk", Kind.TORSO, Rarity.MINOR, 4000,
            stats(0, 0, 3, 0)),
    TUNIC_OF_THE_CYCLOPS_KING("Tunic of the Cyclops King", Kind.TORSO, Rarity.MAJOR, 5000, stats(0, 0, 4, 0)),
    BREASTPLATE_OF_BRIMSTONE("Breastplate of Brimstone", Kind.TORSO, Rarity.MAJOR, 6000, stats(0, 0, 5, 0)),
    ARMOR_OF_WONDER("Armor of Wonder", Kind.TORSO, Rarity.MINOR, 4000, stats(1, 1, 1, 1)),
    DRAGON_SCALE_ARMOR("Dragon Scale Armor", Kind.TORSO, Rarity.RELIC, 8000, stats(4, 4, 0, 0)),
    TITANS_CUIRASS("Titan's Cuirass", Kind.TORSO, Rarity.RELIC, 10000, stats(0, 0, 10, -2)),
    // Helme
    HELM_OF_THE_ALABASTER_UNICORN("Helm of the Alabaster Unicorn", Kind.HELMET, Rarity.TREASURE, 1000,
            stats(0, 0, 0, 1)),
    SKULL_HELMET("Skull Helmet", Kind.HELMET, Rarity.TREASURE, 3000, stats(0, 0, 0, 2)),
    HELM_OF_CHAOS("Helm of Chaos", Kind.HELMET, Rarity.MINOR, 4000, stats(0, 0, 0, 3)),
    CROWN_OF_THE_SUPREME_MAGI("Crown of the Supreme Magi", Kind.HELMET, Rarity.MINOR, 5000, stats(0, 0, 0, 4)),
    HELLSTORM_HELMET("Hellstorm Helmet", Kind.HELMET, Rarity.MAJOR, 6000, stats(0, 0, 0, 5)),
    CROWN_OF_DRAGONTOOTH("Crown of Dragontooth", Kind.HELMET, Rarity.RELIC, 8000, stats(0, 0, 4, 4)),
    THUNDER_HELMET("Thunder Helmet", Kind.HELMET, Rarity.RELIC, 10000, stats(0, 0, -2, 10)),
    HELM_OF_HEAVENLY_ENLIGHTENMENT("Helm of Heavenly Enlightenment", Kind.HELMET, Rarity.RELIC, 24000,
            stats(6, 6, 6, 6)),
    SPELLBINDERS_HAT("Spellbinder's Hat", Kind.HELMET, Rarity.RELIC, 30000, stats(0, 0, 0, 0),
            new LevelSpells(5)),
    // Umhänge
    DRAGON_WING_TABARD("Dragon Wing Tabard", Kind.CAPE, Rarity.MINOR, 4000, stats(0, 0, 2, 2)),
    CAPE_OF_CONJURING("Cape of Conjuring", Kind.CAPE, Rarity.TREASURE, 1500, stats(0, 0, 0, 0),
            new SpellDuration(3)),
    SURCOAT_OF_COUNTERPOISE("Surcoat of Counterpoise", Kind.CAPE, Rarity.MAJOR, 4000, stats(0, 0, 0, 0),
            new MagicResistance(10)),
    RECANTERS_CLOAK("Recanter's Cloak", Kind.CAPE, Rarity.MAJOR, 8000, stats(0, 0, 0, 0), new BlockMagic(2)),
    CAPE_OF_VELOCITY("Cape of Velocity", Kind.CAPE, Rarity.MAJOR, 10000, stats(0, 0, 0, 0), new Speed(2)),
    // Halsketten
    NECKLACE_OF_DRAGONTEETH("Necklace of Dragonteeth", Kind.NECKLACE, Rarity.MAJOR, 6000, stats(0, 0, 3, 3)),
    CELESTIAL_NECKLACE_OF_BLISS("Celestial Necklace of Bliss", Kind.NECKLACE, Rarity.RELIC, 12000,
            stats(3, 3, 3, 3)),
    COLLAR_OF_CONJURING("Collar of Conjuring", Kind.NECKLACE, Rarity.TREASURE, 500, stats(0, 0, 0, 0),
            new SpellDuration(1)),
    PENDANT_OF_DISPASSION("Pendant of Dispassion", Kind.NECKLACE, Rarity.TREASURE, 1000, stats(0, 0, 0, 0),
            new SpellImmunity(Set.of(Spell.BERSERK))),
    PENDANT_OF_FREE_WILL("Pendant of Free Will", Kind.NECKLACE, Rarity.TREASURE, 1000, stats(0, 0, 0, 0),
            new SpellImmunity(Set.of(Spell.HYPNOTIZE))),
    PENDANT_OF_HOLINESS("Pendant of Holiness", Kind.NECKLACE, Rarity.TREASURE, 1000, stats(0, 0, 0, 0),
            new SpellImmunity(Set.of(Spell.CURSE))),
    PENDANT_OF_LIFE("Pendant of Life", Kind.NECKLACE, Rarity.TREASURE, 2500, stats(0, 0, 0, 0),
            new SpellImmunity(Set.of(Spell.DEATH_RIPPLE))),
    PENDANT_OF_DEATH("Pendant of Death", Kind.NECKLACE, Rarity.TREASURE, 2500, stats(0, 0, 0, 0),
            new SpellImmunity(Set.of(Spell.DESTROY_UNDEAD))),
    PENDANT_OF_TOTAL_RECALL("Pendant of Total Recall", Kind.NECKLACE, Rarity.TREASURE, 3000, stats(0, 0, 0, 0),
            new SpellImmunity(Set.of(Spell.FORGETFULNESS))),
    GARNITURE_OF_INTERFERENCE("Garniture of Interference", Kind.NECKLACE, Rarity.MAJOR, 2000, stats(0, 0, 0, 0),
            new MagicResistance(5)),
    PENDANT_OF_SECOND_SIGHT("Pendant of Second Sight", Kind.NECKLACE, Rarity.MAJOR, 5000, stats(0, 0, 0, 0),
            new SpellImmunity(Set.of(Spell.BLIND))),
    PENDANT_OF_NEGATIVITY("Pendant of Negativity", Kind.NECKLACE, Rarity.MAJOR, 5000, stats(0, 0, 0, 0),
            new SpellImmunity(Set.of(Spell.LIGHTNING_BOLT, Spell.CHAIN_LIGHTNING))),
    NECKLACE_OF_SWIFTNESS("Necklace of Swiftness", Kind.NECKLACE, Rarity.TREASURE, 5000, stats(0, 0, 0, 0),
            new Speed(1)),
    PENDANT_OF_COURAGE("Pendant of Courage", Kind.NECKLACE, Rarity.MAJOR, 7000, stats(0, 0, 0, 0),
            new Luck(3), new Morale(3)),
    // Ringe
    STILL_EYE_OF_THE_DRAGON("Still Eye of the Dragon", Kind.RING, Rarity.TREASURE, 2000, stats(0, 0, 0, 0),
            new Luck(1), new Morale(1)),
    QUIET_EYE_OF_THE_DRAGON("Quiet Eye of the Dragon", Kind.RING, Rarity.TREASURE, 2000, stats(1, 1, 0, 0)),
    RING_OF_CONJURING("Ring of Conjuring", Kind.RING, Rarity.TREASURE, 1000, stats(0, 0, 0, 0),
            new SpellDuration(2)),
    RING_OF_THE_WAYFARER("Ring of the Wayfarer", Kind.RING, Rarity.MAJOR, 5000, stats(0, 0, 0, 0), new Speed(1)),
    RING_OF_VITALITY("Ring of Vitality", Kind.RING, Rarity.TREASURE, 5000, stats(0, 0, 0, 0), new Health(1)),
    RING_OF_LIFE("Ring of Life", Kind.RING, Rarity.MINOR, 5000, stats(0, 0, 0, 0), new Health(1)),
    // Stiefel
    DRAGONBONE_GREAVES("Dragonbone Greaves", Kind.FEET, Rarity.TREASURE, 2000, stats(0, 0, 1, 1)),
    SANDALS_OF_THE_SAINT("Sandals of the Saint", Kind.FEET, Rarity.RELIC, 8000, stats(2, 2, 2, 2)),
    BOOTS_OF_POLARITY("Boots of Polarity", Kind.FEET, Rarity.RELIC, 6000, stats(0, 0, 0, 0),
            new MagicResistance(15)),
    // Taschen
    ORB_OF_THE_FIRMAMENT("Orb of the Firmament", Kind.MISC, Rarity.MAJOR, 6000, stats(0, 0, 0, 0),
            new SpellDamage(MagicSchool.AIR, 50)),
    ORB_OF_SILT("Orb of Silt", Kind.MISC, Rarity.MAJOR, 6000, stats(0, 0, 0, 0),
            new SpellDamage(MagicSchool.EARTH, 50)),
    ORB_OF_TEMPESTUOUS_FIRE("Orb of Tempestuous Fire", Kind.MISC, Rarity.MAJOR, 6000, stats(0, 0, 0, 0),
            new SpellDamage(MagicSchool.FIRE, 50)),
    ORB_OF_DRIVING_RAIN("Orb of Driving Rain", Kind.MISC, Rarity.MAJOR, 6000, stats(0, 0, 0, 0),
            new SpellDamage(MagicSchool.WATER, 50)),
    TOME_OF_AIR_MAGIC("Tome of Air Magic", Kind.MISC, Rarity.RELIC, 20000, stats(0, 0, 0, 0),
            new SchoolSpells(MagicSchool.AIR)),
    TOME_OF_EARTH_MAGIC("Tome of Earth Magic", Kind.MISC, Rarity.RELIC, 20000, stats(0, 0, 0, 0),
            new SchoolSpells(MagicSchool.EARTH)),
    TOME_OF_FIRE_MAGIC("Tome of Fire Magic", Kind.MISC, Rarity.RELIC, 20000, stats(0, 0, 0, 0),
            new SchoolSpells(MagicSchool.FIRE)),
    TOME_OF_WATER_MAGIC("Tome of Water Magic", Kind.MISC, Rarity.RELIC, 20000, stats(0, 0, 0, 0),
            new SchoolSpells(MagicSchool.WATER)),
    SPHERE_OF_PERMANENCE("Sphere of Permanence", Kind.MISC, Rarity.MAJOR, 7500, stats(0, 0, 0, 0),
            new SpellImmunity(Set.of(Spell.DISPEL))),
    ORB_OF_INHIBITION("Orb of Inhibition", Kind.MISC, Rarity.RELIC, 20000, stats(0, 0, 0, 0), new BlockMagic(0)),
    ORB_OF_VULNERABILITY("Orb of Vulnerability", Kind.MISC, Rarity.RELIC, 25000, stats(0, 0, 0, 0),
            new NegateImmunities()),
    CLOVER_OF_FORTUNE("Clover of Fortune", Kind.MISC, Rarity.TREASURE, 1000, stats(0, 0, 0, 0), new Luck(1)),
    LADYBIRD_OF_LUCK("Ladybird of Luck", Kind.MISC, Rarity.TREASURE, 1000, stats(0, 0, 0, 0), new Luck(1)),
    CARDS_OF_PROPHECY("Cards of Prophecy", Kind.MISC, Rarity.TREASURE, 1000, stats(0, 0, 0, 0), new Luck(1)),
    HOURGLASS_OF_THE_EVIL_HOUR("Hourglass of the Evil Hour", Kind.MISC, Rarity.TREASURE, 2000, stats(0, 0, 0, 0),
            new NegateLuck()),
    GLYPH_OF_GALLANTRY("Glyph of Gallantry", Kind.MISC, Rarity.TREASURE, 1000, stats(0, 0, 0, 0), new Morale(1)),
    CREST_OF_VALOR("Crest of Valor", Kind.MISC, Rarity.TREASURE, 1000, stats(0, 0, 0, 0), new Morale(1)),
    // In SoD dazu immun gegen Gedankenzauber (Wiki: „Hidden Effect", erst in HotA entfernt; VCMI ebenso).
    BADGE_OF_COURAGE("Badge of Courage", Kind.MISC, Rarity.TREASURE, 1000, stats(0, 0, 0, 0), new Morale(1),
            new MindImmunity()),
    SPIRIT_OF_OPPRESSION("Spirit of Oppression", Kind.MISC, Rarity.TREASURE, 2000, stats(0, 0, 0, 0),
            new NegateMorale()),
    GOLDEN_BOW("Golden Bow", Kind.MISC, Rarity.MAJOR, 8000, stats(0, 0, 0, 0), new NoShootingPenalty()),
    BOW_OF_ELVEN_CHERRYWOOD("Bow of Elven Cherrywood", Kind.MISC, Rarity.TREASURE, 2000, stats(0, 0, 0, 0),
            new Archery(5)),
    BOWSTRING_OF_THE_UNICORNS_MANE("Bowstring of the Unicorn's Mane", Kind.MISC, Rarity.MINOR, 4000,
            stats(0, 0, 0, 0), new Archery(10)),
    ANGEL_FEATHER_ARROWS("Angel Feather Arrows", Kind.MISC, Rarity.MAJOR, 6000, stats(0, 0, 0, 0),
            new Archery(15)),
    VIAL_OF_LIFEBLOOD("Vial of Lifeblood", Kind.MISC, Rarity.MAJOR, 10000, stats(0, 0, 0, 0), new Health(2)),
    VIAL_OF_DRAGON_BLOOD("Vial of Dragon Blood", Kind.MISC, Rarity.RELIC, 20000, stats(0, 0, 0, 0),
            new DragonBonus(5)),
    // Kombi-Artefakte (SoD): Wirkung der Teile plus eigene, Platz des ersten Teils laut Wiki.
    ANGELIC_ALLIANCE("Angelic Alliance", Kind.WEAPON, 84000, stats(21, 21, 21, 21),
            List.of(SWORD_OF_JUDGEMENT, HELM_OF_HEAVENLY_ENLIGHTENMENT, CELESTIAL_NECKLACE_OF_BLISS, ARMOR_OF_WONDER,
                    SANDALS_OF_THE_SAINT, LIONS_SHIELD_OF_COURAGE),
            new OpeningSpell(Spell.PRAYER, 10), new AlignmentMix()),
    TITANS_THUNDER("Titan's Thunder", Kind.WEAPON, 40000, stats(9, 9, 8, 8),
            List.of(TITANS_GLADIUS, THUNDER_HELMET, TITANS_CUIRASS, SENTINELS_SHIELD),
            new GrantsSpell(Spell.TITANS_LIGHTNING_BOLT, SkillLevel.EXPERT)),
    ARMOR_OF_THE_DAMNED("Armor of the Damned", Kind.TORSO, 12000, stats(3, 3, 2, 2),
            List.of(RIB_CAGE, BLACKSHARD_OF_THE_DEAD_KNIGHT, SHIELD_OF_THE_YAWNING_DEAD, SKULL_HELMET),
            new OpeningSpell(Spell.SLOW, 50), new OpeningSpell(Spell.CURSE, 50), new OpeningSpell(Spell.WEAKNESS, 50),
            new OpeningSpell(Spell.MISFORTUNE, 50)),
    POWER_OF_THE_DRAGON_FATHER("Power of the Dragon Father", Kind.TORSO, 42000, stats(16, 16, 16, 16),
            List.of(DRAGON_SCALE_ARMOR, CROWN_OF_DRAGONTOOTH, NECKLACE_OF_DRAGONTEETH, DRAGON_WING_TABARD,
                    DRAGONBONE_GREAVES, RED_DRAGON_FLAME_TONGUE, DRAGON_SCALE_SHIELD, QUIET_EYE_OF_THE_DRAGON,
                    STILL_EYE_OF_THE_DRAGON),
            new Luck(1), new Morale(1), new LevelImmunity(4)),
    BOW_OF_THE_SHARPSHOOTER("Bow of the Sharpshooter", Kind.MISC, 12000, stats(0, 0, 0, 0),
            List.of(BOW_OF_ELVEN_CHERRYWOOD, BOWSTRING_OF_THE_UNICORNS_MANE, ANGEL_FEATHER_ARROWS),
            new Archery(30), new NoShootingPenalty(), new FreeShooting()),
    ELIXIR_OF_LIFE("Elixir of Life", Kind.MISC, 20000, stats(0, 0, 0, 0),
            List.of(VIAL_OF_LIFEBLOOD, RING_OF_VITALITY, RING_OF_LIFE),
            new Health(4), new LivingVitality(25)),
    RING_OF_THE_MAGI("Ring of the Magi", Kind.RING, 3000, stats(0, 0, 0, 0),
            List.of(RING_OF_CONJURING, COLLAR_OF_CONJURING, CAPE_OF_CONJURING),
            new SpellDuration(56));

    /** Seltenheit, wie das Spiel sie führt; Kombi-Artefakte sind {@link #RELIC}. */
    @Schema(description = "Seltenheitsklasse eines Artefakts.", enumAsRef = true)
    public enum Rarity {
        TREASURE, MINOR, MAJOR, RELIC
    }

    /** Primärwerte eines Artefakts, auch negative (Titan's Gladius: −3 Defense). */
    public record Stats(int attack, int defense, int power, int knowledge) {
    }

    private final String displayName;
    private final Kind kind;
    private final Rarity rarity;
    private final int value;
    private final Stats stats;
    private final List<Artifact> components;
    private final List<ArtifactEffect> effects;

    Artifact(String displayName, Kind kind, Rarity rarity, int value, Stats stats, ArtifactEffect... effects) {
        this.displayName = displayName;
        this.kind = kind;
        this.rarity = rarity;
        this.value = value;
        this.stats = stats;
        this.components = List.of();
        this.effects = List.of(effects);
    }

    Artifact(String displayName, Kind kind, int value, Stats stats, List<Artifact> components,
             ArtifactEffect... effects) {
        this.displayName = displayName;
        this.kind = kind;
        this.rarity = Rarity.RELIC;
        this.value = value;
        this.stats = stats;
        this.components = List.copyOf(components);
        this.effects = List.of(effects);
    }

    private static Stats stats(int attack, int defense, int power, int knowledge) {
        return new Stats(attack, defense, power, knowledge);
    }

    /** Englischer Name wie im Spiel. */
    public String displayName() {
        return displayName;
    }

    /** Art des Platzes, den das Artefakt belegt. */
    public Kind kind() {
        return kind;
    }

    public Rarity rarity() {
        return rarity;
    }

    /** Goldwert (Wiki, <em>cost</em>). */
    public int value() {
        return value;
    }

    public Stats stats() {
        return stats;
    }

    /** Teile eines Kombi-Artefakts, leer für ein einfaches. */
    public List<Artifact> components() {
        return components;
    }

    public boolean isCombination() {
        return !components.isEmpty();
    }

    public List<ArtifactEffect> effects() {
        return effects;
    }

    /**
     * Plätze, die ein Kombi-Artefakt zusätzlich zu seinem eigenen sperrt: die Arten seiner Teile,
     * ohne den Teil auf dem eigenen Platz.
     */
    public List<Kind> blocks() {
        List<Kind> kinds = new ArrayList<>(components.stream().map(Artifact::kind).toList());
        kinds.remove(kind);
        return List.copyOf(kinds);
    }
}
