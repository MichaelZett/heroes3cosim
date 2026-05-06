package de.zettsystems.h3comsim.domain;

public record Battlefield(int width, int height) {

    public static final Battlefield STANDARD = new Battlefield(15, 11);

    public boolean contains(Hex hex) {
        return hex.q() >= 0 && hex.q() < width && hex.r() >= 0 && hex.r() < height;
    }

    /**
     * Liefert eine Position auf einer Linie zwischen {@code from} und {@code target}, höchstens
     * {@code maxHexes} Schritte von {@code from} entfernt. Stoppt mindestens einen Hex vor dem
     * Ziel — Bewegung schließt nie auf das Zielfeld auf, weil dort der Gegner steht.
     * Keine Hindernisse — gerade Linie reicht für das MVP.
     */
    public Hex moveToward(Hex from, Hex target, int maxHexes) {
        int distance = from.distanceTo(target);
        int hexesToMove = Math.clamp(distance - 1L, 0, maxHexes);
        if (hexesToMove == 0) {
            return from;
        }
        double t = (double) hexesToMove / distance;
        int q = (int) Math.round(from.q() + t * (target.q() - from.q()));
        int r = (int) Math.round(from.r() + t * (target.r() - from.r()));
        return new Hex(q, r);
    }
}
