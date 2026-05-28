package de.zettsystems.h3comsim.armybattle.application;

import de.zettsystems.h3comsim.battle.domain.Battlefield;
import de.zettsystems.h3comsim.battle.domain.Hex;
import de.zettsystems.h3comsim.battle.domain.events.Side;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Spawn-Positionen für Army-Battles. Die 7 möglichen Slots werden auf 7 Reihen verteilt,
 * symmetrisch um die Mittelreihe r=5 auf {@link Battlefield#STANDARD} (Höhe 11).
 *
 * <p>Reihenfolge {@code {0, 2, 4, 5, 6, 8, 10}}: Mitte besetzt, 1-Hex-Lücken oben/unten
 * vermeiden Wand-an-Wand-Adjazenz und geben dem A* Pfad-Spielraum.
 *
 * <p>Attacker spawnt auf q=0, Defender auf q=14.
 */
public final class SpawnLayout {

    static final int[] ROWS_7 = {0, 2, 4, 5, 6, 8, 10};
    static final int ATTACKER_COLUMN = 0;
    static final int DEFENDER_COLUMN = 14;

    private SpawnLayout() {
    }

    /**
     * Liefert die Spawn-Position für einen Stack an Slot {@code slotIndex} (0-based) einer
     * Armee mit {@code totalSlots} Stacks (1..7). Bei < 7 Stacks wird die Reihenauswahl
     * symmetrisch um r=5 zentriert.
     */
    public static Hex positionFor(Side side, int slotIndex, int totalSlots) {
        if (slotIndex < 0 || slotIndex >= totalSlots) {
            throw new IllegalArgumentException(
                    "slotIndex " + slotIndex + " out of range for totalSlots " + totalSlots);
        }
        if (totalSlots < 1 || totalSlots > 7) {
            throw new IllegalArgumentException("totalSlots must be 1..7, was " + totalSlots);
        }
        int q = side == Side.ATTACKER ? ATTACKER_COLUMN : DEFENDER_COLUMN;
        int r = rowsFor(totalSlots)[slotIndex];
        return new Hex(q, r);
    }

    /**
     * Liefert die Set aller Spawn-Hexen, die für die gegebene Armee-Größe benutzt werden,
     * über beide Seiten — fürs Filtern von ObstacleGenerator-Output.
     */
    public static Set<Hex> spawnHexesFor(int attackerStacks, int defenderStacks) {
        List<Hex> all = new ArrayList<>(attackerStacks + defenderStacks);
        for (int i = 0; i < attackerStacks; i++) {
            all.add(positionFor(Side.ATTACKER, i, attackerStacks));
        }
        for (int i = 0; i < defenderStacks; i++) {
            all.add(positionFor(Side.DEFENDER, i, defenderStacks));
        }
        return all.stream().collect(Collectors.toUnmodifiableSet());
    }

    private static int[] rowsFor(int totalSlots) {
        if (totalSlots == 7) {
            return ROWS_7;
        }
        // Zentriere {0,2,4,5,6,8,10} um r=5: nimm die mittleren totalSlots Einträge.
        int start = (ROWS_7.length - totalSlots) / 2;
        return Arrays.copyOfRange(ROWS_7, start, start + totalSlots);
    }
}
