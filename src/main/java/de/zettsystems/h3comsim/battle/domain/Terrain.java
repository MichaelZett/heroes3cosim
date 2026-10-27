package de.zettsystems.h3comsim.battle.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

/**
 * Die acht Grundgelände eines Kampfs (heroes.thelazy.net, <em>Terrain</em>). Kreaturen auf ihrem
 * Heimatgelände bekommen +1 Attack, Defense und Speed (<em>Native terrain</em>); neutrale
 * Kreaturen haben keins. Eine Belagerung wird immer auf dem Heimatgelände der Stadt geschlagen.
 *
 * <p>{@link #WATER} ist die Seeschlacht (VCMI {@code battlefields.json}): treffen zwei Helden in
 * Booten aufeinander, kämpfen sie auf zwei Schiffen mit Wasser dazwischen
 * ({@link TerrainObstacles#shipToShip()}), sonst auf einem Schiffsdeck mit Masten. Auf Wasser ist
 * niemand zu Hause, magisches Gelände gibt es dort nicht.
 */
@Schema(description = "Grundgelände des Kampfs; bestimmt Heimatboni und Hindernisse.", enumAsRef = true)
public enum Terrain {
    DIRT,
    SAND,
    GRASS,
    SNOW,
    SWAMP,
    ROUGH,
    SUBTERRANEAN,
    LAVA,
    WATER;

    /** Das Heimatgelände einer Fraktion, für neutrale Kreaturen {@code null}. */
    public static @Nullable Terrain nativeOf(Faction faction) {
        return switch (faction) {
            case CASTLE, RAMPART, CONFLUX -> GRASS;
            case TOWER -> SNOW;
            case INFERNO -> LAVA;
            case NECROPOLIS -> DIRT;
            case DUNGEON -> SUBTERRANEAN;
            case STRONGHOLD -> ROUGH;
            case FORTRESS -> SWAMP;
            case NEUTRAL -> null;
        };
    }
}
