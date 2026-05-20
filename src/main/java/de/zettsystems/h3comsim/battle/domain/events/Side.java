package de.zettsystems.h3comsim.battle.domain.events;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Seite eines Stacks in einer Einzelschlacht.", enumAsRef = true)
public enum Side {
    ATTACKER,
    DEFENDER
}
