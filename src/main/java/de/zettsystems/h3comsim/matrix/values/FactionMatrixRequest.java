package de.zettsystems.h3comsim.matrix.values;

import de.zettsystems.h3comsim.battle.domain.Faction;
import de.zettsystems.h3comsim.battle.domain.MagicTerrain;
import de.zettsystems.h3comsim.battle.domain.SolverKind;
import de.zettsystems.h3comsim.battle.domain.Terrain;
import org.jspecify.annotations.Nullable;

import java.util.Set;

/**
 * Eingabe für die Faction-Matrix: die Wochenproduktion jeder gewählten Fraktion kämpft gegen die
 * jeder anderen, in beiden Rollen und mit {@code seedsPerPair} Seeds je Rolle, einmal je Solver.
 * {@code weeks} vervielfacht die Wochenproduktion; ohne {@code heroes} kämpfen die Armeen ohne
 * Helden, sonst führt jede der Standardheld ihrer Fraktion.
 */
public record FactionMatrixRequest(
        Set<Faction> factions,
        Set<SolverKind> solvers,
        int seedsPerPair,
        int weeks,
        boolean heroes,
        @Nullable Terrain terrain,
        @Nullable MagicTerrain magicTerrain
) {
    public FactionMatrixRequest {
        if (factions.size() < 2 || factions.contains(Faction.NEUTRAL)) {
            throw new IllegalArgumentException("factions must name at least two towns, was " + factions);
        }
        if (solvers.isEmpty()) {
            throw new IllegalArgumentException("solvers must not be empty");
        }
        if (seedsPerPair < 1) {
            throw new IllegalArgumentException("seedsPerPair must be >= 1, was " + seedsPerPair);
        }
        if (weeks < 1) {
            throw new IllegalArgumentException("weeks must be >= 1, was " + weeks);
        }
        if (terrain == Terrain.WATER && magicTerrain != null) {
            throw new IllegalArgumentException("There is no magical terrain at sea");
        }
        factions = Set.copyOf(factions);
        solvers = Set.copyOf(solvers);
    }

    /** Einzelsimulationen des ganzen Laufs: jede geordnete Paarung samt Spiegel, je Seed und Solver. */
    public int totalSims() {
        return factions.size() * factions.size() * seedsPerPair * solvers.size();
    }
}
