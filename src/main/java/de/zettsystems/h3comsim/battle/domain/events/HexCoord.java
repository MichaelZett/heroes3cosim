package de.zettsystems.h3comsim.battle.domain.events;

/**
 * Reine (q, r)-Hex-Koordinate für Event-Snapshots — entkoppelt vom domain.Hex-Record.
 */
public record HexCoord(int q, int r) {
}
