package de.zettsystems.h3comsim.armybattle.application;

import de.zettsystems.h3comsim.battle.domain.AttackType;
import de.zettsystems.h3comsim.battle.domain.Battlefield;
import de.zettsystems.h3comsim.battle.domain.Hex;
import de.zettsystems.h3comsim.battle.domain.Unit;
import de.zettsystems.h3comsim.battle.domain.events.Side;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
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
 *
 * <p><strong>Taktische Aufstellung</strong>: {@link #assignPositions(Side, List)} ordnet die
 * Stacks anhand ihrer Eigenschaften auf die Reihen — Schützen nach außen (am weitesten von
 * der Frontlinie), schnellste Melees ins Zentrum (für Charge-Reichweite), langsame Melees
 * dazwischen. Der User-Slot in {@code ArmySpec} bestimmt nur noch die Anzeige-Reihenfolge,
 * nicht die Battle-Position. {@link #positionFor(Side, int, int)} bleibt als
 * Slot-Index→Reihe-Direktabbildung für Tests/Edge-Cases erhalten.
 */
public final class SpawnLayout {

    static final int[] ROWS_7 = {0, 2, 4, 5, 6, 8, 10};
    static final int CENTER_ROW = 5;
    static final int ATTACKER_COLUMN = 0;
    static final int DEFENDER_COLUMN = 14;

    private SpawnLayout() {
    }

    /**
     * Taktische Reihen-Zuteilung für eine Armee. Liefert für jede Eingabe-Unit (in derselben
     * Reihenfolge wie {@code units}) die Spawn-Position. Heuristik:
     * <ol>
     *   <li>Reihen werden nach Distanz zur Mitte sortiert — die äußersten zuerst.</li>
     *   <li>Schützen ({@link AttackType#LONG_RANGE}) bekommen die äußersten Reihen, sortiert
     *       nach {@link Unit#health()} aufsteigend (Tiebreak: Original-Slot). Begründung:
     *       Ecken haben strukturell weniger Adjazenz-Hexen und sind durch eine Tank-Wall
     *       vollständig abdeckbar — der zerbrechlichste Schütze profitiert davon am meisten;
     *       robuste Schützen wie Titan oder Arch Magi stehen weiter innen und können sich im
     *       Notfall im Nahkampf wehren.</li>
     *   <li>Melees werden nach Speed absteigend sortiert (Tiebreak: Original-Slot) und auf
     *       die restlichen Reihen verteilt, beginnend in der Mitte (schnellster ins Zentrum,
     *       um Charge-Reichweite zu maximieren), nach außen abnehmend.</li>
     * </ol>
     *
     * <p>Beispiel Castle (Marksman, Monk, Halberdier, Swordsman, Griffin, Champion, Angel):
     * Marksman+Monk außen (r=0/r=10), Halberdier+Swordsman als Frontline (r=2/r=8),
     * Griffin+Champion+Angel in der Mitte (r=4/5/6).
     *
     * @return Liste mit derselben Länge und Reihenfolge wie {@code units}, jedem Element
     *         seine zugewiesene Hex-Position.
     */
    public static List<Hex> assignPositions(Side side, List<Unit> units) {
        int total = units.size();
        if (total < 1 || total > 7) {
            throw new IllegalArgumentException("units size must be 1..7, was " + total);
        }
        int q = side == Side.ATTACKER ? ATTACKER_COLUMN : DEFENDER_COLUMN;
        int[] rows = rowsFor(total);

        // Reihen-Indices in zwei Reihenfolgen: außen→innen (für Shooter) und innen→außen
        // (für Melee). Tiebreak bei gleicher Distanz zur Mitte: kleinere r-Koordinate zuerst,
        // damit die Wahl deterministisch ist.
        List<Integer> outsideIn = sortRows(rows,
                Comparator.<Integer>comparingInt(r -> -Math.abs(r - CENTER_ROW))
                        .thenComparingInt(r -> r));
        List<Integer> centerOut = sortRows(rows,
                Comparator.<Integer>comparingInt(r -> Math.abs(r - CENTER_ROW))
                        .thenComparingInt(r -> r));

        List<Integer> shooterSlots = new ArrayList<>();
        List<Integer> meleeSlots = new ArrayList<>();
        for (int i = 0; i < total; i++) {
            (units.get(i).attackType() == AttackType.LONG_RANGE ? shooterSlots : meleeSlots).add(i);
        }
        // Schützen nach unit.health() asc — schwächster zuerst → äußerste Ecke. Stabiler
        // Sort sichert Slot-Tiebreak bei gleichen HP (z.B. Castle Marksman & Archer beide 10).
        shooterSlots.sort(Comparator.comparingInt((Integer slot) -> units.get(slot).health()));
        // Melee nach Speed desc; bei Gleichstand Slot-Reihenfolge (stabiler Sort).
        meleeSlots.sort(Comparator.comparingInt((Integer slot) -> -units.get(slot).speed()));

        Hex[] result = new Hex[total];
        for (int i = 0; i < shooterSlots.size(); i++) {
            result[shooterSlots.get(i)] = new Hex(q, outsideIn.get(i));
        }
        // Melees verbrauchen die Reihen vom Zentrum nach außen, überspringen aber jene, die
        // bereits an Schützen vergeben sind (= die äußersten shooterCount Einträge).
        Set<Integer> usedRows = outsideIn.subList(0, shooterSlots.size()).stream()
                .collect(Collectors.toUnmodifiableSet());
        int meleeRowIdx = 0;
        for (int slot : meleeSlots) {
            while (usedRows.contains(centerOut.get(meleeRowIdx))) {
                meleeRowIdx++;
            }
            result[slot] = new Hex(q, centerOut.get(meleeRowIdx++));
        }
        return Arrays.asList(result);
    }

    private static List<Integer> sortRows(int[] rows, Comparator<Integer> cmp) {
        return Arrays.stream(rows).boxed().sorted(cmp).toList();
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
