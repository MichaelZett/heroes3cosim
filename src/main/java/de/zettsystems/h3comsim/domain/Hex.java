package de.zettsystems.h3comsim.domain;

import java.util.List;

public record Hex(int q, int r) {

    private static final int[][] NEIGHBOR_OFFSETS = {
            {1, 0}, {1, -1}, {0, -1}, {-1, 0}, {-1, 1}, {0, 1}
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
        List<Hex> result = new java.util.ArrayList<>(6);
        for (int[] offset : NEIGHBOR_OFFSETS) {
            result.add(new Hex(q + offset[0], r + offset[1]));
        }
        return result;
    }
}
