package de.zettsystems.h3comsim.battle.domain;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Kriegsmaschinen (Manual S. 117, <em>War Machines</em>): je Held höchstens eine jeder Art. Das
 * Katapult fehlt — es gehört zur Belagerung, die der Simulator nicht kennt.
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
 * <p>Die Maschinen stehen im Simulator nicht auf dem Feld und lassen sich nicht angreifen. Im
 * Spiel können Gegner sie zerstören; die Auto-Kampf-KI tut das selten, und ohne Ziel-Logik für
 * Maschinen bliebe es ein Zufallsprodukt der Solver.
 */
@Schema(description = "Kriegsmaschine eines Helden. Das Katapult fehlt (nur Belagerung).", enumAsRef = true)
public enum WarMachine {
    BALLISTA(10, 10, 250),
    AMMO_CART(0, 5, 100),
    FIRST_AID_TENT(0, 0, 75);

    private final int attack;
    private final int defense;
    private final int health;

    WarMachine(int attack, int defense, int health) {
        this.attack = attack;
        this.defense = defense;
        this.health = health;
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
}
