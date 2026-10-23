package de.zettsystems.h3comsim.battle.domain;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Heldenklassen (Manual S. 24-25, Conflux im Armageddon's-Blade-Manual). Jede Fraktion stellt
 * zwei — eine martialische und eine magische; die martialische steht je Fraktion zuerst.
 *
 * <p>Die Primärwerte hängen an der Klasse, nicht am einzelnen Helden: alle Knights starten mit
 * 2/2/1/1, alle Barbaren mit 4/0/1/1 (abgeglichen mit jedem Eintrag in <em>Individual
 * Heroes</em>). Helden derselben Klasse unterscheiden sich ausschließlich über Startfertigkeiten
 * und Spezialfähigkeit.
 *
 * <p>Power und Knowledge steuern nur das Zaubern, das es hier nicht gibt — ein Held einer
 * magischen Klasse wirkt im Kampf daher fast nur über seine niedrigen Attack-/Defense-Werte.
 */
@Schema(description = "Heldenklasse. Zwei je Fraktion; bestimmt die Primärwerte.", enumAsRef = true)
public enum HeroClass {
    KNIGHT(Faction.CASTLE, 2, 2, 1, 1),
    CLERIC(Faction.CASTLE, 1, 0, 2, 2),
    RANGER(Faction.RAMPART, 1, 3, 1, 1),
    DRUID(Faction.RAMPART, 0, 2, 1, 2),
    ALCHEMIST(Faction.TOWER, 1, 1, 2, 2),
    WIZARD(Faction.TOWER, 0, 0, 2, 3),
    DEMONIAC(Faction.INFERNO, 2, 2, 1, 1),
    HERETIC(Faction.INFERNO, 1, 1, 2, 1),
    DEATH_KNIGHT(Faction.NECROPOLIS, 1, 2, 2, 1),
    NECROMANCER(Faction.NECROPOLIS, 1, 0, 2, 2),
    OVERLORD(Faction.DUNGEON, 2, 2, 1, 1),
    WARLOCK(Faction.DUNGEON, 0, 0, 3, 2),
    BARBARIAN(Faction.STRONGHOLD, 4, 0, 1, 1),
    BATTLE_MAGE(Faction.STRONGHOLD, 2, 1, 1, 1),
    BEASTMASTER(Faction.FORTRESS, 0, 4, 1, 1),
    WITCH(Faction.FORTRESS, 0, 1, 2, 2),
    PLANESWALKER(Faction.CONFLUX, 3, 1, 1, 1),
    ELEMENTALIST(Faction.CONFLUX, 0, 0, 3, 3);

    private final Faction faction;
    private final int attack;
    private final int defense;
    private final int power;
    private final int knowledge;

    HeroClass(Faction faction, int attack, int defense, int power, int knowledge) {
        this.faction = faction;
        this.attack = attack;
        this.defense = defense;
        this.power = power;
        this.knowledge = knowledge;
    }

    public Faction faction() {
        return faction;
    }

    public int attack() {
        return attack;
    }

    public int defense() {
        return defense;
    }

    public int power() {
        return power;
    }

    public int knowledge() {
        return knowledge;
    }
}
