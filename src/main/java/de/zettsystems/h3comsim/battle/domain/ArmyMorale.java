package de.zettsystems.h3comsim.battle.domain;

import org.jspecify.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Das Moral-Rating einer Armee (Manual S. 43).
 *
 * <p>Moral ist in H3 keine Eigenschaft der einzelnen Kreatur, sondern der Armee: „Each army has
 * a morale rating which acts as a bonus to the individual morale of its troops." Das Rating
 * startet bei 0 und wird von der Zusammenstellung der Armee bewegt:
 *
 * <pre>
 *   -1  wenn Untote mit Lebenden gemischt sind
 *   +1  wenn alle Kreaturen aus derselben Stadt stammen (außer Necropolis)
 *   -1  je Stadt-Typ jenseits des zweiten
 * </pre>
 *
 * <p>Dazu kommen die beiden Quellen, die es in diesem Simulator gibt: die Leadership-Fertigkeit
 * des Helden (+1/2/3, Manual S. 37) und {@link UnitSpeciality#GOOD_ARMY_MORALE} — Angel und
 * Arch Angel heben laut Manual S. 96 die Moral der gesamten Armee, nicht nur ihre eigene.
 *
 * <p>Bewusst <strong>nicht</strong> abgebildet: Artefakte, Gebäude, Abenteuer-Orte und Zauber.
 * Sie alle stehen außerhalb der Schlacht, die dieser Simulator kennt.
 *
 * <p>Die Kappung auf [-3, +3] passiert nicht hier, sondern erst in {@link Stack#getMorale()} —
 * dort kommt der individuelle Bonus der Kreatur dazu, und erst diese Summe ist der Wert, den
 * die Tabelle des Manuals kennt.
 *
 * <p>{@link Faction#NEUTRAL} zählt als eigener Stadt-Typ. Das ist eine Auslegung: Peasants und
 * Halflings haben in H3 keine Heimatstadt, eine Armee aus lauter Peasants ist also keine Armee
 * „of the same town type". Damit bekommt sie folgerichtig keinen Bonus.
 */
public final class ArmyMorale {

    /** Manual S. 43: Moral außerhalb von [-3, +3] gibt es nicht — die Tabelle endet dort. */
    public static final int MIN = -3;
    public static final int MAX = 3;

    private ArmyMorale() {
    }

    /**
     * Moral-Rating der Armee, ohne den individuellen Bonus der einzelnen Kreatur.
     *
     * @param stacks alle Stacks einer Seite, auch die bereits gefallenen — die Zusammenstellung
     *               wird zu Kampfbeginn bewertet und ändert sich während der Schlacht nicht
     * @param hero   Anführer der Armee, {@code null} bei führerloser Armee
     */
    public static int ratingOf(List<Stack> stacks, @Nullable Hero hero) {
        Set<Faction> towns = EnumSet.noneOf(Faction.class);
        boolean undead = false;
        boolean living = false;
        boolean goodArmyMorale = false;
        for (Stack stack : stacks) {
            Unit unit = stack.unit();
            towns.add(unit.faction());
            undead |= unit.isUndead();
            living |= !unit.isUndead();
            goodArmyMorale |= unit.hasSpeciality(UnitSpeciality.GOOD_ARMY_MORALE);
        }

        int rating = 0;
        if (undead && living) {
            rating--;
        }
        if (towns.size() == 1 && !towns.contains(Faction.NECROPOLIS)) {
            rating++;
        }
        if (towns.size() > 2) {
            rating -= towns.size() - 2;
        }
        if (goodArmyMorale) {
            rating++;
        }
        if (hero != null) {
            rating += hero.leadershipMoraleBonus();
        }
        return rating;
    }

    /**
     * Kappt eine Moral auf den Bereich, den die Tabelle des Manuals kennt. Die Summe aus
     * Armee-Rating und individuellem Bonus kann darüber hinauslaufen — ein Held mit Expert
     * Leadership über einer Ein-Stadt-Armee mit Arch Angels kommt auf +5.
     */
    static int clamp(int morale) {
        return Math.clamp(morale, MIN, MAX);
    }

    /**
     * Wahrscheinlichkeit in Promille, dass Moral oder Glück dieser Stärke auslösen
     * (Manual S. 43/44): 4.2 %, 8.3 %, 12.5 % — ein Mal, zwei Mal, drei Mal ein Zwölftel.
     *
     * @param level Betrag der Moral, 1..3
     */
    static int triggerChancePerMille(int level) {
        return switch (level) {
            case 1 -> 42;
            case 2 -> 83;
            case 3 -> 125;
            default -> throw new IllegalArgumentException("Moral-Betrag muss 1..3 sein, war " + level);
        };
    }
}
