package de.zettsystems.h3comsim.battle.domain;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Die Felder der Stadtbefestigung. Grundlage sind die Feldnummern des Spiels, wie sie die freie
 * H3-Engine VCMI verwendet: 17 Spalten je Reihe, Spalte 0 und 16 liegen außerhalb des Felds.
 * Ungerade Reihen sind im Spiel einen halben Hex nach links versetzt; das Simulator-Feld
 * ({@link Battlefield}) entspricht den Spalten 1 bis 15, in geraden Reihen fehlt rechts die
 * Spalte 15. Mauer, Tor und Graben liegen alle weiter links und lassen sich exakt übertragen.
 */
final class SiegeLayout {

    /** Unzerstörbare Mauerstücke („W" auf heroes.thelazy.net, <em>Wall</em>). */
    static final Set<Hex> PERMANENT_WALL = hexes(12, 45, 62, 112, 147, 165);
    /** Die äußere, unzerstörbare Hälfte des Tors; blockiert keine Schüsse. */
    static final Hex GATE_OUTER = h3(95);
    /** Die Zugbrücke selbst (D), das zerstörbare Tor. */
    static final Hex GATE_INNER = h3(96);
    /** Das Feld vor dem Tor (B), auf das die heruntergelassene Brücke reicht. */
    static final Hex GATE_BRIDGE = h3(94);
    /** Graben vor der Mauer, in jeder Stadt außer Fortress und Tower. */
    static final Set<Hex> MOAT = hexes(11, 28, 44, 61, 77, 111, 129, 146, 164, 181);
    /** Der zweireihige Graben von Fortress, einschließlich des Felds vor dem Tor. */
    static final Set<Hex> FORTRESS_MOAT = hexes(10, 11, 27, 28, 43, 44, 60, 61, 76, 77, 94, 110, 111, 128, 129,
            145, 146, 163, 164, 180, 181);
    /** Je Reihe das Mauerfeld: links davon ist draußen, rechts davon drinnen. */
    private static final List<Hex> WALL_LINE = List.of(h3(12), h3(29), h3(45), h3(62), h3(78), GATE_INNER,
            h3(112), h3(130), h3(147), h3(165), h3(182));

    private SiegeLayout() {
    }

    /** Ein Feld des Spiels, übertragen auf das Simulator-Feld. */
    static Hex h3(int index) {
        int row = index / 17;
        int column = index % 17;
        int lateral = 2 * column + 3 + (row % 2 == 0 ? 1 : 0);
        return new Hex((lateral - row) / 2, row);
    }

    private static Set<Hex> hexes(int... indices) {
        return Arrays.stream(indices).mapToObj(SiegeLayout::h3).collect(Collectors.toUnmodifiableSet());
    }

    /** Steht das Feld außerhalb der Mauern (auf der Seite des Angreifers)? */
    static boolean isOutside(Hex hex) {
        if (hex.r() < 0 || hex.r() >= WALL_LINE.size()) {
            return true;
        }
        return hex.q() < WALL_LINE.get(hex.r()).q();
    }
}
