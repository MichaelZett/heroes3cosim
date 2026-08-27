package de.zettsystems.h3comsim.battle.domain;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Heldenklassen (Manual S. 24-25). Jede Fraktion stellt zwei — eine martialische und eine
 * magische. Aufgenommen ist bisher nur die martialische je Fraktion, weil Power und Knowledge
 * ohne Zaubersystem folgenlos bleiben und ein Magier damit ein Held ohne Wirkung wäre.
 *
 * <p>Die Primärwerte hängen an der Klasse, nicht am einzelnen Helden: alle Knights starten mit
 * 2/2/1/1, alle Barbaren mit 4/0/1/1. Helden derselben Klasse unterscheiden sich ausschließlich
 * über Startfertigkeiten und Spezialfähigkeit.
 */
@Schema(description = "Heldenklasse. Bisher nur die martialische Klasse je Fraktion.",
        enumAsRef = true)
public enum HeroClass {
    KNIGHT(Faction.CASTLE),
    RANGER(Faction.RAMPART),
    ALCHEMIST(Faction.TOWER),
    DEMONIAC(Faction.INFERNO),
    DEATH_KNIGHT(Faction.NECROPOLIS),
    OVERLORD(Faction.DUNGEON),
    BARBARIAN(Faction.STRONGHOLD),
    BEASTMASTER(Faction.FORTRESS),
    PLANESWALKER(Faction.CONFLUX);

    private final Faction faction;

    HeroClass(Faction faction) {
        this.faction = faction;
    }

    public Faction faction() {
        return faction;
    }
}
