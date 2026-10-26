package de.zettsystems.h3comsim.battle.domain;

/**
 * Glück und Moral, die ein Held außerhalb der Schlacht gesammelt hat — Stadtgebäude und besuchte
 * Abenteuer-Orte (Manual S. 44: „Various spells, hero skills, town buildings, adventure sites, and
 * artifacts can permanently or temporarily affect the luck of armies"). Der Simulator kennt diese
 * Quellen nicht einzeln, er nimmt nur ihre Summe je Armee entgegen; Artefakte zählen einzeln
 * ({@link Equipment}).
 *
 * <p>Beide Werte hängen am Helden: ohne Helden gibt es keine Artefakte und keine besuchten Orte,
 * die Boni bleiben dann wirkungslos.
 *
 * @param luck   gesammeltes Glück, 0..3 — negatives Glück gibt es in SoD nicht
 * @param morale gesammelte Moral, −3..+3
 */
public record CollectedBonus(int luck, int morale) {

    /** Kein gesammelter Bonus. */
    public static final CollectedBonus NONE = new CollectedBonus(0, 0);

    public CollectedBonus {
        if (luck < ArmyLuck.MIN || luck > ArmyLuck.MAX) {
            throw new IllegalArgumentException("luck must be 0..3, was " + luck);
        }
        if (morale < ArmyMorale.MIN || morale > ArmyMorale.MAX) {
            throw new IllegalArgumentException("morale must be -3..3, was " + morale);
        }
    }
}
