package de.zettsystems.h3comsim.matrix.application;

import de.zettsystems.h3comsim.matrix.values.FactionMatrixJobSnapshot;
import de.zettsystems.h3comsim.matrix.values.FactionMatrixRequest;

import java.util.Optional;

public interface FactionMatrixJobService {

    /** Startet einen asynchronen Lauf; der Snapshot trägt die Job-ID und die Zahl der Kämpfe. */
    FactionMatrixJobSnapshot start(FactionMatrixRequest request);

    Optional<FactionMatrixJobSnapshot> get(String jobId);
}
