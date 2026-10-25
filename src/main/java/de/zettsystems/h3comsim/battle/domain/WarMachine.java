package de.zettsystems.h3comsim.battle.domain;

import de.zettsystems.h3comsim.battle.domain.events.Side;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Kriegsmaschinen (Manual S. 117, <em>War Machines</em>): je Held höchstens eine jeder Art. Das
 * Katapult ({@link #CATAPULT}) hat jeder angreifende Held bei einer Belagerung von selbst; es
 * schießt auf die Mauern ({@link Catapult}) und lässt sich wählen, wirkt aber nur dort.
 *
 * <ul>
 *   <li>{@link #BALLISTA} — schießt am Ende jeder Runde auf einen gegnerischen Stack, Schaden
 *       (2–3) × (Attack des Helden + 1). Artillery gibt einen zweiten Schuss (ab Advanced) und
 *       50/75/100 % Chance auf doppelten Grundschaden (SoD, heroes.thelazy.net).</li>
 *   <li>{@link #AMMO_CART} — die eigenen Schützen haben unbegrenzt Munition.</li>
 *   <li>{@link #FIRST_AID_TENT} — heilt am Ende jeder Runde die oberste Kreatur eines eigenen
 *       Stacks um 1–25 Lebenspunkte; First Aid hebt die Obergrenze auf 50/75/100 (Manual
 *       S. 37). Tote holt es nicht zurück.</li>
 * </ul>
 *
 * <p>Auf dem Feld stehen die Maschinen am eigenen Rand, in den Reihen 1 (Munitionswagen), 3
 * (Ballista) und 9 (Zelt) — die Felder 18, 52 und 154 bzw. 32, 66 und 168 des Spiels, wie sie
 * die freie H3-Engine VCMI verwendet. Ballista und Zelt sind im Spiel zwei Felder breit; der
 * Simulator kennt keine zweifeldrigen Einheiten, jede Maschine belegt ein Feld. Sie bewegen sich
 * nicht, haben keine Moral und schlagen nicht zurück (heroes.thelazy.net, <em>War Machine</em>);
 * die Primärwerte des Helden bekommen sie hier nicht. Zerstört, hört die Maschine auf zu wirken,
 * nach dem Munitionswagen verbrauchen Schützen wieder Munition. Die Lebenspunkte und Werte
 * stammen von dort, die Kosten aus SoD.
 */
@Schema(description = "Kriegsmaschine eines Helden. Das Katapult fehlt (nur Belagerung).", enumAsRef = true)
public enum WarMachine {
    // Slots ab 20: die Kreaturen haben 0..6, beschworene Stacks folgen ab 7.
    BALLISTA("Ballista", 10, 10, 250, 2, 3, 2500, 3, 20),
    AMMO_CART("Ammo Cart", 0, 5, 100, 0, 0, 1000, 1, 21),
    FIRST_AID_TENT("First Aid Tent", 0, 0, 75, 0, 0, 750, 9, 22),
    // Nur bei Belagerungen, und dort hat jeder angreifende Held eines (Feld 120 des Spiels).
    CATAPULT("Catapult", 10, 10, 1000, 0, 0, 0, 7, 23);

    private static final Map<WarMachine, Unit> UNITS = units();

    private final String displayName;
    private final int attack;
    private final int defense;
    private final int health;
    private final int minDamage;
    private final int maxDamage;
    private final int cost;
    private final int row;
    private final int slot;

    WarMachine(String displayName, int attack, int defense, int health, int minDamage, int maxDamage, int cost,
               int row, int slot) {
        this.displayName = displayName;
        this.attack = attack;
        this.defense = defense;
        this.health = health;
        this.minDamage = minDamage;
        this.maxDamage = maxDamage;
        this.cost = cost;
        this.row = row;
        this.slot = slot;
    }

    private static Map<WarMachine, Unit> units() {
        Map<WarMachine, Unit> units = new EnumMap<>(WarMachine.class);
        for (WarMachine m : values()) {
            units.put(m, new Unit(m.displayName, new Stats(m.attack, m.defense, m.health, 0),
                    new Combat(m.minDamage, m.maxDamage, 0, AttackType.HAND_TO_HAND), Movement.GROUND, m.cost,
                    Faction.NEUTRAL, 1, false, Set.of()));
        }
        return units;
    }

    public int attack() {
        return attack;
    }

    public int defense() {
        return defense;
    }

    public int health() {
        return health;
    }

    /** Goldkosten beim Schmied (SoD). */
    public int cost() {
        return cost;
    }

    /** Die Einheit, als die die Maschine auf dem Feld steht. Steht nicht im {@link UnitCatalog}. */
    Unit unit() {
        return Objects.requireNonNull(UNITS.get(this));
    }

    int slot() {
        return slot;
    }

    /** Das Feld der Maschine: am Rand der eigenen Seite, in ihrer festen Reihe. */
    Hex hex(Side side, Battlefield battlefield) {
        int q = side == Side.ATTACKER ? battlefield.leftmostQ(row) : battlefield.rightmostQ(row);
        return new Hex(q, row);
    }

    /** Steht diese Einheit für eine Kriegsmaschine? */
    static boolean isMachine(Unit unit) {
        return UNITS.containsValue(unit);
    }
}
