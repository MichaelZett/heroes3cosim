package de.zettsystems.h3comsim.battle.domain;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.random.RandomGenerator;
import java.util.stream.Collectors;

import static de.zettsystems.h3comsim.battle.domain.MagicTerrain.CLOVER_FIELD;
import static de.zettsystems.h3comsim.battle.domain.MagicTerrain.CURSED_GROUND;
import static de.zettsystems.h3comsim.battle.domain.MagicTerrain.EVIL_FOG;
import static de.zettsystems.h3comsim.battle.domain.MagicTerrain.FIERY_FIELDS;
import static de.zettsystems.h3comsim.battle.domain.MagicTerrain.HOLY_GROUND;
import static de.zettsystems.h3comsim.battle.domain.MagicTerrain.LUCID_POOLS;
import static de.zettsystems.h3comsim.battle.domain.MagicTerrain.MAGIC_CLOUDS;
import static de.zettsystems.h3comsim.battle.domain.MagicTerrain.MAGIC_PLAINS;
import static de.zettsystems.h3comsim.battle.domain.MagicTerrain.ROCKLANDS;
import static de.zettsystems.h3comsim.battle.domain.Terrain.DIRT;
import static de.zettsystems.h3comsim.battle.domain.Terrain.GRASS;
import static de.zettsystems.h3comsim.battle.domain.Terrain.LAVA;
import static de.zettsystems.h3comsim.battle.domain.Terrain.ROUGH;
import static de.zettsystems.h3comsim.battle.domain.Terrain.SAND;
import static de.zettsystems.h3comsim.battle.domain.Terrain.SNOW;
import static de.zettsystems.h3comsim.battle.domain.Terrain.SUBTERRANEAN;
import static de.zettsystems.h3comsim.battle.domain.Terrain.SWAMP;

/**
 * Die Hindernisse eines Felds nach Gelände, wie sie die freie H3-Engine VCMI aus den Spieldaten
 * führt ({@code config/obstacles.json}) und setzt ({@code BattleInfo::setupBattle}): 5–12 Felder
 * sollen blockiert werden; mit 40 % steht zuerst ein großes Hindernis an fester Stelle (es zählt
 * nur die Hälfte seiner Felder), dann folgen zufällige gewöhnliche Hindernisse, bis die Zahl erreicht
 * ist oder keins mehr passt. Auf einem magischen Gelände gelten nur dessen eigene Hindernisse.
 *
 * <p>Felder zählen wie im Spiel: Index = Reihe × 17 + Spalte, Spalte 0 und 16 liegen außerhalb.
 * Gewöhnliche Hindernisse geben ihre Felder relativ zur linken unteren Ecke an; liegt diese in
 * einer ungeraden Reihe und das Feld in einer geraden, rückt es eins nach links. Blockiert werden
 * nur die Spalten 3–13, die beiden Spalten an jedem Rand bleiben für die Aufstellung frei.
 */
public final class TerrainObstacles {

    private static final int ROW = 17;
    private static final int FIRST_POSITION = 18;
    private static final int LAST_POSITION = 168;

    private record Obstacle(boolean absolute, int width, int height, Set<Terrain> terrains,
                            Set<MagicTerrain> fields, List<Integer> tiles) {

        boolean fits(@Nullable Terrain terrain, @Nullable MagicTerrain magic) {
            return magic != null ? fields.contains(magic) : terrain != null && terrains.contains(terrain);
        }

        List<Integer> blockedAt(int position) {
            if (absolute) {
                return tiles;
            }
            List<Integer> blocked = new ArrayList<>(tiles.size());
            for (int offset : tiles) {
                int tile = position + offset;
                if ((position / ROW) % 2 != 0 && Math.floorMod(Math.floorDiv(tile, ROW), 2) == 0) {
                    tile--;
                }
                blocked.add(tile);
            }
            return blocked;
        }
    }

    private static final List<Obstacle> ALL = List.of(
            usual(2, 1, terrains(DIRT), fields(), 0, 1),
            usual(3, 2, terrains(DIRT, SAND, ROUGH, SUBTERRANEAN), fields(), 0, 1, 2),
            usual(4, 2, terrains(DIRT), fields(), 0, 1, -14, -15, -16),
            usual(2, 1, terrains(DIRT, ROUGH), fields(CURSED_GROUND), 0, 1),
            usual(2, 1, terrains(DIRT, ROUGH, SUBTERRANEAN), fields(CURSED_GROUND), 0, 1),
            usual(4, 2, terrains(DIRT), fields(), 1, 2, 3),
            usual(3, 1, terrains(DIRT), fields(), 0, 1),
            usual(2, 2, terrains(DIRT), fields(), 0, 1),
            usual(2, 2, terrains(DIRT), fields(), -16),
            usual(2, 2, terrains(DIRT), fields(), 0, 1),
            usual(2, 2, terrains(DIRT), fields(), 0, 1),
            usual(2, 1, terrains(DIRT), fields(), 0, 1),
            usual(4, 3, terrains(DIRT, ROUGH), fields(CURSED_GROUND), 0, 1, 2, 3),
            usual(3, 2, terrains(DIRT, ROUGH), fields(CURSED_GROUND), 1, 2, -15),
            usual(3, 2, terrains(DIRT, ROUGH), fields(CURSED_GROUND), 2, -15, -16),
            usual(3, 3, terrains(DIRT, ROUGH), fields(CURSED_GROUND), 1, -16, -33),
            usual(4, 4, terrains(SAND), fields(), -15, -16, -32, -33, -48, -49),
            usual(3, 2, terrains(SAND), fields(), 1, -15, -16),
            usual(4, 2, terrains(SAND), fields(), 1, 2, 3, -15, -16),
            usual(2, 1, terrains(GRASS, SWAMP), fields(), 0, 1),
            usual(2, 2, terrains(GRASS, SWAMP), fields(MAGIC_PLAINS), 0, 1),
            usual(1, 1, terrains(GRASS, SWAMP), fields(), 0),
            usual(6, 2, terrains(GRASS), fields(MAGIC_PLAINS), 1, 2, 3, 4, -13, -14, -15, -16),
            usual(7, 1, terrains(GRASS), fields(), 1, 2),
            usual(3, 1, terrains(SNOW), fields(), 0, 1, 2),
            usual(5, 1, terrains(SNOW), fields(), 1, 2, 3, 4),
            usual(3, 3, terrains(SNOW), fields(), 0, -16, -33),
            usual(3, 1, terrains(SNOW), fields(), 0, 1, 2),
            usual(3, 1, terrains(SNOW), fields(), 1),
            usual(3, 2, terrains(SNOW), fields(), 1, 2),
            usual(2, 1, terrains(SNOW), fields(), 0, 1),
            usual(3, 2, terrains(SNOW), fields(), 0, 1, 2),
            usual(7, 2, terrains(SNOW), fields(), 2, 3, 4, 5, -13, -14, -15, -16),
            usual(5, 5, terrains(SNOW), fields(), 3, -13, -14, -15, -33, -49, -66),
            usual(2, 2, terrains(SWAMP), fields(), 0),
            usual(8, 3, terrains(SWAMP), fields(), -10, -11, -12, -13, -14, -15, -16),
            usual(2, 1, terrains(SWAMP), fields(), 0, 1),
            usual(3, 1, terrains(SWAMP), fields(), 0, 1, 2),
            usual(5, 4, terrains(SWAMP), fields(), -13, -14, -15, -16, -30, -31, -32, -33),
            usual(4, 3, terrains(SWAMP), fields(), -16, -17, -31, -32, -33, -34),
            usual(2, 2, terrains(ROUGH), fields(CURSED_GROUND), 0, 1, -16),
            usual(4, 3, terrains(ROUGH), fields(CURSED_GROUND), -14, -15, -16, -32, -33),
            usual(3, 2, terrains(ROUGH), fields(CURSED_GROUND), 1, 2, -15, -16),
            usual(3, 3, terrains(ROUGH), fields(CURSED_GROUND), -16, -32, -33),
            usual(3, 3, terrains(ROUGH), fields(CURSED_GROUND), -15, -16, -32),
            usual(3, 3, terrains(SUBTERRANEAN), fields(), 0, 1, 2, -15, -16),
            usual(3, 2, terrains(SUBTERRANEAN), fields(), 0, 1, 2),
            usual(4, 3, terrains(SUBTERRANEAN), fields(), 0, 1, 2, 3, -14, -15, -16),
            usual(4, 3, terrains(LAVA), fields(), -14, -32, -33),
            usual(4, 2, terrains(LAVA), fields(), 0, 1, 2, -14, -15, -16),
            usual(5, 3, terrains(LAVA), fields(), -13, -14, -15, -30, -31, -32, -33),
            usual(3, 2, terrains(LAVA), fields(), 0, 1, 2),
            usual(4, 4, terrains(LAVA), fields(), -14, -15, -32, -33, -49, -50),
            usual(5, 3, terrains(LAVA), fields(), -13, -14, -15, -16, -30, -31),
            usual(5, 3, terrains(LAVA), fields(), -13, -14, -15, -16, -31, -32, -33),
            usual(1, 1, terrains(), fields(HOLY_GROUND), 0),
            usual(2, 1, terrains(), fields(HOLY_GROUND), 0, 1),
            usual(3, 3, terrains(), fields(HOLY_GROUND), 1),
            usual(3, 2, terrains(), fields(HOLY_GROUND), 0, 1, 2),
            usual(4, 3, terrains(), fields(HOLY_GROUND), 0, 1, 2, 3),
            usual(1, 1, terrains(), fields(EVIL_FOG), 0),
            usual(2, 1, terrains(), fields(EVIL_FOG), 0, 1),
            usual(3, 2, terrains(), fields(EVIL_FOG), 0, 1, 2),
            usual(4, 2, terrains(), fields(EVIL_FOG), 1, 2),
            usual(6, 2, terrains(), fields(EVIL_FOG), 1, 2, 3, -12, -13),
            usual(1, 1, terrains(), fields(CLOVER_FIELD), 0),
            usual(3, 1, terrains(), fields(CLOVER_FIELD), 0, 1, 2),
            usual(3, 2, terrains(), fields(CLOVER_FIELD), 1, 2, -15, -16),
            usual(4, 2, terrains(), fields(CLOVER_FIELD), 0, 1, 2, -14, -15, -16),
            usual(1, 1, terrains(), fields(LUCID_POOLS), 0),
            usual(2, 1, terrains(), fields(LUCID_POOLS), 0, 1),
            usual(3, 2, terrains(), fields(LUCID_POOLS), 0, -15, -16),
            usual(5, 2, terrains(), fields(LUCID_POOLS), 1, 2, 3, -13, -14, -15, -16),
            usual(1, 1, terrains(), fields(FIERY_FIELDS), 0),
            usual(2, 1, terrains(), fields(FIERY_FIELDS), 0, 1),
            usual(3, 2, terrains(), fields(FIERY_FIELDS), 0, 1, 2, -15),
            usual(4, 2, terrains(), fields(FIERY_FIELDS), 1, 2, 3, -15, -16),
            usual(3, 3, terrains(), fields(FIERY_FIELDS), 0, 1, 2, 3, -14, -15, -16),
            usual(1, 1, terrains(), fields(ROCKLANDS), 0),
            usual(2, 1, terrains(), fields(ROCKLANDS), 0, 1),
            usual(3, 1, terrains(), fields(ROCKLANDS), 0, 1, 2),
            usual(4, 2, terrains(), fields(ROCKLANDS), 1, 2, 3, -15, -16),
            usual(1, 1, terrains(), fields(MAGIC_CLOUDS), 0),
            usual(2, 2, terrains(), fields(MAGIC_CLOUDS), 1, -16),
            usual(4, 2, terrains(), fields(MAGIC_CLOUDS), 0, 1, -14, -15),
            absolute(terrains(DIRT), fields(), 80, 94, 95, 96, 97, 105, 106, 107, 108, 109, 110),
            absolute(terrains(DIRT), fields(), 73, 91, 108, 109, 110, 111, 112, 113),
            absolute(terrains(DIRT), fields(), 60, 61, 62, 63, 64, 72, 73, 74, 75, 76, 140, 141, 142, 143, 144, 145, 146, 147, 148, 149),
            absolute(terrains(DIRT), fields(), 88, 89, 90, 91, 92, 93, 94, 95, 96, 97, 98),
            absolute(terrains(DIRT), fields(), 76, 77, 78, 79, 80, 89, 90, 91, 92, 93),
            absolute(terrains(GRASS), fields(), 55, 56, 57, 58, 75, 76, 77, 95, 112, 113, 131),
            absolute(terrains(GRASS), fields(), 81, 91, 92, 93, 94, 95, 96, 97, 98, 106, 107, 123),
            absolute(terrains(SNOW), fields(), 76, 77, 78, 79, 91, 92, 93, 97, 98, 106, 107, 108),
            absolute(terrains(SNOW), fields(), 41, 42, 43, 58, 75, 92, 108, 126, 143),
            absolute(terrains(SWAMP), fields(), 40, 41, 58, 59, 74, 75, 92, 93, 109, 110, 111, 127, 128, 129, 130),
            absolute(terrains(SWAMP), fields(), 43, 60, 61, 77, 93, 94, 95, 109, 110, 126, 127),
            absolute(terrains(SWAMP), fields(), 74, 75, 76, 77, 91, 92, 93, 94, 95, 109, 110, 111, 112),
            absolute(terrains(LAVA), fields(), 77, 78, 79, 80, 81, 91, 92, 93, 94, 105, 106, 107),
            absolute(terrains(LAVA), fields(), 43, 60, 61, 76, 77, 93, 109, 126, 127, 142, 143),
            absolute(terrains(ROUGH), fields(CURSED_GROUND), 55, 72, 90, 107, 125, 126, 127, 128, 129, 130, 131, 132),
            absolute(terrains(ROUGH), fields(CURSED_GROUND), 41, 59, 76, 94, 111, 129, 143, 144, 145),
            absolute(terrains(ROUGH), fields(CURSED_GROUND), 40, 41, 42, 43, 58, 75, 93, 110, 128, 145),
            absolute(terrains(ROUGH), fields(CURSED_GROUND), 72, 73, 74, 75, 76, 77, 78, 79, 80, 81, 89, 105),
            absolute(terrains(ROUGH), fields(CURSED_GROUND), 72, 73, 74, 75, 76, 77, 78, 79, 80, 90, 91, 92, 93, 94, 95, 96, 97),
            absolute(terrains(ROUGH), fields(CURSED_GROUND), 73, 74, 75, 76, 77, 78, 91, 92, 93, 94),
            absolute(terrains(ROUGH), fields(CURSED_GROUND), 23, 40, 58, 75, 93, 110, 128, 145, 163),
            absolute(terrains(ROUGH), fields(CURSED_GROUND), 72, 73, 74, 75, 76, 77, 78, 79, 80, 90, 91, 92, 93, 94, 95, 96, 97),
            absolute(terrains(ROUGH), fields(CURSED_GROUND), 73, 74, 75, 76, 77, 78, 79, 90, 91, 92, 93, 94, 95, 96, 97, 106, 107, 108, 109, 110, 111, 112),
            absolute(terrains(), fields(CLOVER_FIELD), 76, 77, 92, 93, 94, 95, 109, 110, 111),
            absolute(terrains(), fields(LUCID_POOLS), 76, 77, 78, 92, 93, 94, 107, 108, 109),
            absolute(terrains(), fields(FIERY_FIELDS), 76, 77, 91, 92, 93, 94, 95, 108, 109, 110, 111),
            absolute(terrains(), fields(ROCKLANDS), 60, 61, 75, 76, 77, 91, 92, 93, 94, 95),
            absolute(terrains(), fields(MAGIC_CLOUDS), 59, 60, 74, 75, 76, 93, 94, 95, 111, 112));

    private TerrainObstacles() {
    }

    /** Die blockierten Felder und die einzeln entfernbaren Hindernisse (ohne Klippen). */
    public record Layout(Set<Hex> blocked, List<Set<Hex>> removable) {
        public Layout {
            blocked = Set.copyOf(blocked);
            removable = List.copyOf(removable);
        }
    }

    /** Die blockierten Felder für ein Gelände; ohne Grund- und magisches Gelände keine. */
    public static Set<Hex> generate(@Nullable Terrain terrain, @Nullable MagicTerrain magic, RandomGenerator rng) {
        return layout(terrain, magic, rng).blocked();
    }

    /** Wie {@link #generate}, dazu die Hindernisse, die Remove Obstacle als Ganzes wegnimmt. */
    public static Layout layout(@Nullable Terrain terrain, @Nullable MagicTerrain magic, RandomGenerator rng) {
        List<Obstacle> absolutes = ALL.stream().filter(o -> o.absolute() && o.fits(terrain, magic)).toList();
        List<Obstacle> usual = ALL.stream().filter(o -> !o.absolute() && o.fits(terrain, magic)).toList();
        Set<Integer> blocked = new HashSet<>();
        List<Set<Hex>> removable = new ArrayList<>();
        int tilesToBlock = rng.nextInt(5, 13);
        if (!absolutes.isEmpty() && rng.nextInt(100) < 40) {
            Obstacle cliff = absolutes.get(rng.nextInt(absolutes.size()));
            blocked.addAll(cliff.blockedAt(0));
            tilesToBlock -= cliff.tiles().size() / 2;
        }
        while (tilesToBlock > 0 && !usual.isEmpty()) {
            Obstacle obstacle = usual.get(rng.nextInt(usual.size()));
            List<Integer> positions = new ArrayList<>();
            for (int position = FIRST_POSITION; position <= LAST_POSITION; position++) {
                if (valid(obstacle, position, blocked)) {
                    positions.add(position);
                }
            }
            if (positions.isEmpty()) {
                break;
            }
            List<Integer> placed = obstacle.blockedAt(positions.get(rng.nextInt(positions.size())));
            blocked.addAll(placed);
            removable.add(placed.stream().map(SiegeLayout::h3).collect(Collectors.toUnmodifiableSet()));
            tilesToBlock -= obstacle.tiles().size();
        }
        return new Layout(blocked.stream().map(SiegeLayout::h3).collect(Collectors.toUnmodifiableSet()), removable);
    }

    private static boolean valid(Obstacle obstacle, int position, Set<Integer> blocked) {
        int column = position % ROW;
        if (obstacle.height() >= position / ROW || column == 0 || column + obstacle.width() > 15
                || blocked.contains(position)) {
            return false;
        }
        for (int tile : obstacle.blockedAt(position)) {
            int tileColumn = Math.floorMod(tile, ROW);
            if (blocked.contains(tile) || tileColumn <= 2 || tileColumn >= 14) {
                return false;
            }
        }
        return true;
    }

    private static Obstacle usual(int width, int height, Set<Terrain> terrains, Set<MagicTerrain> fields, int... tiles) {
        return new Obstacle(false, width, height, terrains, fields, Arrays.stream(tiles).boxed().toList());
    }

    private static Obstacle absolute(Set<Terrain> terrains, Set<MagicTerrain> fields, int... tiles) {
        return new Obstacle(true, 0, 0, terrains, fields, Arrays.stream(tiles).boxed().toList());
    }

    private static Set<Terrain> terrains(Terrain... terrains) {
        return terrains.length == 0 ? Set.of() : EnumSet.copyOf(Arrays.asList(terrains));
    }

    private static Set<MagicTerrain> fields(MagicTerrain... fields) {
        return fields.length == 0 ? Set.of() : EnumSet.copyOf(Arrays.asList(fields));
    }
}
