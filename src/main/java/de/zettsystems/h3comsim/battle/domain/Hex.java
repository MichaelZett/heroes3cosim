package de.zettsystems.h3comsim.battle.domain;

import java.util.List;

public record Hex(int q, int r) {

    /** Ost, Nordost, Nordwest, West, Südwest, Südost. */
    private static final int[][] NEIGHBOR_OFFSETS = {
            {1, 0}, {1, -1}, {0, -1}, {-1, 0}, {-1, 1}, {0, 1}
    };
    /** Dieselbe Reihenfolge links-rechts gespiegelt: West, Nordwest, Nordost, Ost, Südost, Südwest. */
    private static final int[][] MIRRORED_NEIGHBOR_OFFSETS = {
            {-1, 0}, {0, -1}, {1, -1}, {1, 0}, {0, 1}, {-1, 1}
    };

    public int distanceTo(Hex other) {
        int dq = q - other.q;
        int dr = r - other.r;
        int ds = (q + r) - (other.q + other.r);
        return (Math.abs(dq) + Math.abs(dr) + Math.abs(ds)) / 2;
    }

    public boolean isAdjacent(Hex other) {
        return !this.equals(other) && distanceTo(other) == 1;
    }

    public List<Hex> neighbors() {
        return neighbors(false);
    }

    /**
     * Nachbarn in einer Reihenfolge, die zur Blickrichtung passt. Wo Engine oder Solver bei
     * Gleichstand den ersten Nachbarn nehmen, darf das nicht davon abhängen, ob man von links nach
     * rechts schaut oder umgekehrt — sonst ist eine Seite im Vorteil. Wer nach links blickt
     * ({@link #isLeftward}), bekommt die gespiegelte Reihenfolge; ein Verteidiger, der nach links
     * zieht, verhält sich damit genau wie das Spiegelbild eines Angreifers, der nach rechts zieht.
     */
    public List<Hex> neighbors(boolean mirrored) {
        List<Hex> result = new java.util.ArrayList<>(6);
        for (int[] offset : mirrored ? MIRRORED_NEIGHBOR_OFFSETS : NEIGHBOR_OFFSETS) {
            result.add(new Hex(q + offset[0], r + offset[1]));
        }
        return result;
    }

    /** Ob {@code to} von {@code from} aus gesehen links liegt (waagerechte Lage {@code q + r/2}). */
    public static boolean isLeftward(Hex from, Hex to) {
        return 2 * to.q + to.r < 2 * from.q + from.r;
    }
}
