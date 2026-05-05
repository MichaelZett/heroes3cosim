package de.zettsystems.h3comsim.domain;

public record Hex(int q, int r) {

    public int distanceTo(Hex other) {
        int dq = q - other.q;
        int dr = r - other.r;
        int ds = (q + r) - (other.q + other.r);
        return (Math.abs(dq) + Math.abs(dr) + Math.abs(ds)) / 2;
    }

    public boolean isAdjacent(Hex other) {
        return !this.equals(other) && distanceTo(other) == 1;
    }
}
