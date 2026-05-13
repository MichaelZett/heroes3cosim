package de.zettsystems.h3comsim.battle.domain.events;

public record StackSnapshot(
        Side side,
        String unitName,
        int count,
        int topHp,
        int q,
        int r
) {
}
