package de.zettsystems.h3comsim.battle.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.random.RandomGenerator;

import static de.zettsystems.h3comsim.battle.domain.UnitCatalog.BATTLE_DWARF;
import static de.zettsystems.h3comsim.battle.domain.UnitCatalog.BLACK_DRAGON;
import static de.zettsystems.h3comsim.battle.domain.UnitCatalog.CYCLOPS;
import static de.zettsystems.h3comsim.battle.domain.UnitCatalog.CYCLOPS_KING;
import static de.zettsystems.h3comsim.battle.domain.UnitCatalog.DRAGON_FLY;
import static de.zettsystems.h3comsim.battle.domain.UnitCatalog.DWARF;
import static de.zettsystems.h3comsim.battle.domain.UnitCatalog.FAMILIAR;
import static de.zettsystems.h3comsim.battle.domain.UnitCatalog.GOLD_DRAGON;
import static de.zettsystems.h3comsim.battle.domain.UnitCatalog.GREEN_DRAGON;
import static de.zettsystems.h3comsim.battle.domain.UnitCatalog.GRIFFIN;
import static de.zettsystems.h3comsim.battle.domain.UnitCatalog.IMP;
import static de.zettsystems.h3comsim.battle.domain.UnitCatalog.MEDUSA;
import static de.zettsystems.h3comsim.battle.domain.UnitCatalog.MEDUSA_QUEEN;
import static de.zettsystems.h3comsim.battle.domain.UnitCatalog.NAGA;
import static de.zettsystems.h3comsim.battle.domain.UnitCatalog.NAGA_QUEEN;
import static de.zettsystems.h3comsim.battle.domain.UnitCatalog.RED_DRAGON;
import static de.zettsystems.h3comsim.battle.domain.UnitCatalog.ROYAL_GRIFFIN;
import static de.zettsystems.h3comsim.battle.domain.UnitCatalog.SKELETON;
import static de.zettsystems.h3comsim.battle.domain.UnitCatalog.VAMPIRE;
import static de.zettsystems.h3comsim.battle.domain.UnitCatalog.WALKING_DEAD;
import static de.zettsystems.h3comsim.battle.domain.UnitCatalog.WATER_ELEMENTAL;
import static de.zettsystems.h3comsim.battle.domain.UnitCatalog.WIGHT;

/**
 * Die Kreaturenbanken aus SoD mit ihren vier Ausbaustufen (VCMI {@code config/objects/creatureBanks.json}):
 * je Stufe die Wächter und wie oft die Stufe auf der Karte vorkommt (30/30/30/10 %). Ein Wächter-Stack
 * je Bank ist mit 50 % die ausgebaute Kreatur.
 *
 * <p>Gekämpft wird in eigener Aufstellung ({@link #attackerHex}, {@link #guardHex}): die Armee des
 * Helden in der Mitte, die Wächter an den Rändern ringsum, ohne Tactics, ohne Hindernisse und ohne
 * Kriegsmaschinen (VCMI {@code gameConfig.json}, Layouts {@code creatureBankNarrow/Wide}).
 */
@Schema(description = "Kreaturenbank (SoD).", enumAsRef = true)
// Die Listen kommen aus List.of und sind unveränderlich, Level und Guard sind Records.
@SuppressWarnings("ImmutableEnumChecker")
public enum CreatureBank {
    CYCLOPS_STOCKPILE("Cyclops Stockpile", Layout.NARROW,
            level(30, guard(CYCLOPS, 4), guard(CYCLOPS, 4), upgradable(CYCLOPS, CYCLOPS_KING, 4), guard(CYCLOPS, 4), guard(CYCLOPS, 4)),
            level(30, guard(CYCLOPS, 6), guard(CYCLOPS, 6), upgradable(CYCLOPS, CYCLOPS_KING, 6), guard(CYCLOPS, 6), guard(CYCLOPS, 6)),
            level(30, guard(CYCLOPS, 8), guard(CYCLOPS, 8), upgradable(CYCLOPS, CYCLOPS_KING, 8), guard(CYCLOPS, 8), guard(CYCLOPS, 8)),
            level(10, guard(CYCLOPS, 10), guard(CYCLOPS, 10), upgradable(CYCLOPS, CYCLOPS_KING, 10), guard(CYCLOPS, 10), guard(CYCLOPS, 10))),
    DWARVEN_TREASURY("Dwarven Treasury", Layout.NARROW,
            level(30, guard(DWARF, 10), guard(DWARF, 10), upgradable(DWARF, BATTLE_DWARF, 10), guard(DWARF, 10), guard(DWARF, 10)),
            level(30, guard(DWARF, 15), guard(DWARF, 15), upgradable(DWARF, BATTLE_DWARF, 15), guard(DWARF, 15), guard(DWARF, 15)),
            level(30, guard(DWARF, 20), guard(DWARF, 20), upgradable(DWARF, BATTLE_DWARF, 20), guard(DWARF, 20), guard(DWARF, 20)),
            level(10, guard(DWARF, 30), guard(DWARF, 30), upgradable(DWARF, BATTLE_DWARF, 30), guard(DWARF, 30), guard(DWARF, 30))),
    GRIFFIN_CONSERVATORY("Griffin Conservatory", Layout.WIDE,
            level(30, guard(GRIFFIN, 10), guard(GRIFFIN, 10), upgradable(GRIFFIN, ROYAL_GRIFFIN, 10), guard(GRIFFIN, 10), guard(GRIFFIN, 10)),
            level(30, guard(GRIFFIN, 20), guard(GRIFFIN, 20), upgradable(GRIFFIN, ROYAL_GRIFFIN, 20), guard(GRIFFIN, 20), guard(GRIFFIN, 20)),
            level(30, guard(GRIFFIN, 30), guard(GRIFFIN, 30), upgradable(GRIFFIN, ROYAL_GRIFFIN, 30), guard(GRIFFIN, 30), guard(GRIFFIN, 30)),
            level(10, guard(GRIFFIN, 40), guard(GRIFFIN, 40), upgradable(GRIFFIN, ROYAL_GRIFFIN, 40), guard(GRIFFIN, 40), guard(GRIFFIN, 40))),
    IMP_CACHE("Imp Cache", Layout.NARROW,
            level(30, guard(IMP, 20), guard(IMP, 20), upgradable(IMP, FAMILIAR, 20), guard(IMP, 20), guard(IMP, 20)),
            level(30, guard(IMP, 30), guard(IMP, 30), upgradable(IMP, FAMILIAR, 30), guard(IMP, 30), guard(IMP, 30)),
            level(30, guard(IMP, 40), guard(IMP, 40), upgradable(IMP, FAMILIAR, 40), guard(IMP, 40), guard(IMP, 40)),
            level(10, guard(IMP, 60), guard(IMP, 60), upgradable(IMP, FAMILIAR, 60), guard(IMP, 60), guard(IMP, 60))),
    MEDUSA_STORES("Medusa Stores", Layout.WIDE,
            level(30, guard(MEDUSA, 4), guard(MEDUSA, 4), upgradable(MEDUSA, MEDUSA_QUEEN, 4), guard(MEDUSA, 4), guard(MEDUSA, 4)),
            level(30, guard(MEDUSA, 6), guard(MEDUSA, 6), upgradable(MEDUSA, MEDUSA_QUEEN, 6), guard(MEDUSA, 6), guard(MEDUSA, 6)),
            level(30, guard(MEDUSA, 8), guard(MEDUSA, 8), upgradable(MEDUSA, MEDUSA_QUEEN, 8), guard(MEDUSA, 8), guard(MEDUSA, 8)),
            level(10, guard(MEDUSA, 10), guard(MEDUSA, 10), upgradable(MEDUSA, MEDUSA_QUEEN, 10), guard(MEDUSA, 10), guard(MEDUSA, 10))),
    NAGA_BANK("Naga Bank", Layout.WIDE,
            level(30, guard(NAGA, 2), guard(NAGA, 2), upgradable(NAGA, NAGA_QUEEN, 2), guard(NAGA, 2), guard(NAGA, 2)),
            level(30, guard(NAGA, 3), guard(NAGA, 3), upgradable(NAGA, NAGA_QUEEN, 3), guard(NAGA, 3), guard(NAGA, 3)),
            level(30, guard(NAGA, 4), guard(NAGA, 4), upgradable(NAGA, NAGA_QUEEN, 4), guard(NAGA, 4), guard(NAGA, 4)),
            level(10, guard(NAGA, 6), guard(NAGA, 6), upgradable(NAGA, NAGA_QUEEN, 6), guard(NAGA, 6), guard(NAGA, 6))),
    DRAGON_FLY_HIVE("Dragon Fly Hive", Layout.NARROW,
            level(30, guard(DRAGON_FLY, 6), guard(DRAGON_FLY, 6), guard(DRAGON_FLY, 6), guard(DRAGON_FLY, 6), guard(DRAGON_FLY, 6)),
            level(30, guard(DRAGON_FLY, 9), guard(DRAGON_FLY, 9), guard(DRAGON_FLY, 9), guard(DRAGON_FLY, 9), guard(DRAGON_FLY, 9)),
            level(30, guard(DRAGON_FLY, 12), guard(DRAGON_FLY, 12), guard(DRAGON_FLY, 12), guard(DRAGON_FLY, 12), guard(DRAGON_FLY, 12)),
            level(10, guard(DRAGON_FLY, 18), guard(DRAGON_FLY, 18), guard(DRAGON_FLY, 18), guard(DRAGON_FLY, 18), guard(DRAGON_FLY, 18))),
    SHIPWRECK("Shipwreck", Layout.NARROW,
            level(30, guard(WIGHT, 2), guard(WIGHT, 2), guard(WIGHT, 2), guard(WIGHT, 2), guard(WIGHT, 2)),
            level(30, guard(WIGHT, 3), guard(WIGHT, 3), guard(WIGHT, 3), guard(WIGHT, 3), guard(WIGHT, 3)),
            level(30, guard(WIGHT, 5), guard(WIGHT, 5), guard(WIGHT, 5), guard(WIGHT, 5), guard(WIGHT, 5)),
            level(10, guard(WIGHT, 10), guard(WIGHT, 10), guard(WIGHT, 10), guard(WIGHT, 10), guard(WIGHT, 10))),
    DERELICT_SHIP("Derelict Ship", Layout.WIDE,
            level(30, guard(WATER_ELEMENTAL, 4), guard(WATER_ELEMENTAL, 4), guard(WATER_ELEMENTAL, 4), guard(WATER_ELEMENTAL, 4), guard(WATER_ELEMENTAL, 4)),
            level(30, guard(WATER_ELEMENTAL, 6), guard(WATER_ELEMENTAL, 6), guard(WATER_ELEMENTAL, 6), guard(WATER_ELEMENTAL, 6), guard(WATER_ELEMENTAL, 6)),
            level(30, guard(WATER_ELEMENTAL, 8), guard(WATER_ELEMENTAL, 8), guard(WATER_ELEMENTAL, 8), guard(WATER_ELEMENTAL, 8), guard(WATER_ELEMENTAL, 8)),
            level(10, guard(WATER_ELEMENTAL, 12), guard(WATER_ELEMENTAL, 12), guard(WATER_ELEMENTAL, 12), guard(WATER_ELEMENTAL, 12), guard(WATER_ELEMENTAL, 12))),
    CRYPT("Crypt", Layout.NARROW,
            level(30, guard(SKELETON, 10), guard(WALKING_DEAD, 10), guard(WALKING_DEAD, 10), guard(SKELETON, 10), guard(SKELETON, 10)),
            level(30, guard(SKELETON, 13), guard(WALKING_DEAD, 10), guard(WIGHT, 5), guard(WALKING_DEAD, 10), guard(SKELETON, 12)),
            level(30, guard(SKELETON, 20), guard(WALKING_DEAD, 20), guard(WIGHT, 10), guard(VAMPIRE, 5)),
            level(10, guard(SKELETON, 20), guard(WALKING_DEAD, 20), guard(WIGHT, 10), guard(VAMPIRE, 10))),
    DRAGON_UTOPIA("Dragon Utopia", Layout.WIDE,
            level(30, guard(GREEN_DRAGON, 8), guard(RED_DRAGON, 5), guard(GOLD_DRAGON, 2), guard(BLACK_DRAGON, 1)),
            level(30, guard(GREEN_DRAGON, 8), guard(RED_DRAGON, 6), guard(GOLD_DRAGON, 3), guard(BLACK_DRAGON, 2)),
            level(30, guard(GREEN_DRAGON, 8), guard(RED_DRAGON, 6), guard(GOLD_DRAGON, 4), guard(BLACK_DRAGON, 3)),
            level(10, guard(GREEN_DRAGON, 8), guard(RED_DRAGON, 7), guard(GOLD_DRAGON, 6), guard(BLACK_DRAGON, 5)));

    /**
     * Aufstellung der Wächter: {@link #WIDE} rückt die beiden linken Eck-Stacks und den linken
     * mittleren um ein Feld an den Rand, damit zweifeldrige Wächter Platz haben.
     */
    @SuppressWarnings("ImmutableEnumChecker")
    public enum Layout {
        NARROW(15, 185, 172, 2, 100, 87, 8),
        WIDE(15, 185, 171, 1, 100, 86, 8);

        private final List<Integer> guardIndices;

        Layout(Integer... guardIndices) {
            this.guardIndices = List.of(guardIndices);
        }
    }

    /** Ein Wächter-Stack; mit {@code upgrade} ist er zu 50 % die ausgebaute Kreatur. */
    public record Guard(Unit unit, int count, @Nullable Unit upgrade) {
    }

    /** Eine Ausbaustufe: Anteil auf der Karte in Prozent und die Wächter. */
    public record Level(int chance, List<Guard> guards) {
        public Level {
            guards = List.copyOf(guards);
        }
    }

    /** Felder der Armee des Helden je Slot (H3-Felder 57, 61, 90, 93, 96, 125, 129). */
    private static final List<Integer> ATTACKER_INDICES = List.of(57, 61, 90, 93, 96, 125, 129);
    private static final int UPGRADE_PERCENT = 50;

    private final String displayName;
    private final Layout layout;
    private final List<Level> levels;

    CreatureBank(String displayName, Layout layout, Level... levels) {
        this.displayName = displayName;
        this.layout = layout;
        this.levels = List.of(levels);
    }

    private static Level level(int chance, Guard... guards) {
        return new Level(chance, Arrays.asList(guards));
    }

    private static Guard guard(Unit unit, int count) {
        return new Guard(unit, count, null);
    }

    private static Guard upgradable(Unit unit, Unit upgrade, int count) {
        return new Guard(unit, count, upgrade);
    }

    /** Englischer Name wie im Spiel. */
    public String displayName() {
        return displayName;
    }

    public Layout layout() {
        return layout;
    }

    /** Die Ausbaustufen, Stufe 1 zuerst. */
    public List<Level> levels() {
        return levels;
    }

    /** Die Wächter der Stufe 1..4. */
    public Level level(int level) {
        if (level < 1 || level > levels.size()) {
            throw new IllegalArgumentException(displayName + " has levels 1.." + levels.size() + ", not " + level);
        }
        return levels.get(level - 1);
    }

    /** Würfelt die Stufe nach ihrem Anteil auf der Karte. */
    public int rollLevel(RandomGenerator rng) {
        int roll = rng.nextInt(100);
        for (int i = 0; i < levels.size(); i++) {
            roll -= levels.get(i).chance();
            if (roll < 0) {
                return i + 1;
            }
        }
        return levels.size();
    }

    /** Die Kreatur eines Wächters; ausbaufähige sind zu 50 % die ausgebaute. */
    public static Unit roll(Guard guard, RandomGenerator rng) {
        Unit upgrade = guard.upgrade();
        return upgrade != null && rng.nextInt(100) < UPGRADE_PERCENT ? upgrade : guard.unit();
    }

    /** Feld der Armee des Helden in diesem Slot. */
    public static Hex attackerHex(int slot) {
        return onField(SiegeLayout.h3(ATTACKER_INDICES.get(slot)));
    }

    /** Feld des Wächters mit diesem Index. */
    public Hex guardHex(int index) {
        return onField(SiegeLayout.h3(layout.guardIndices.get(index)));
    }

    /**
     * Die äußerste Spalte des Spiels fehlt dem Simulator-Feld in den geraden Reihen; ein Wächter dort
     * rückt ein Feld nach innen.
     */
    private static Hex onField(Hex hex) {
        return Battlefield.STANDARD.contains(hex) ? hex : new Hex(hex.q() - 1, hex.r());
    }
}
