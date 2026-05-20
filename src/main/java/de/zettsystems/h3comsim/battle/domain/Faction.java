package de.zettsystems.h3comsim.battle.domain;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Faction einer Heroes-3-Einheit — die neun Stadt-Faktionen plus `NEUTRAL` (Peasants, Halflings, Gold Golem, …).",
        enumAsRef = true)
public enum Faction {
    CASTLE,
    RAMPART,
    TOWER,
    INFERNO,
    NECROPOLIS,
    DUNGEON,
    STRONGHOLD,
    FORTRESS,
    CONFLUX,
    NEUTRAL
}
