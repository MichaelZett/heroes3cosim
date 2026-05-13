package de.zettsystems.h3comsim.matrix.application;

import de.zettsystems.h3comsim.matrix.values.MatrixJobSnapshot;
import de.zettsystems.h3comsim.matrix.values.MatrixJobStatus;
import de.zettsystems.h3comsim.matrix.values.MatrixRequest;

import java.util.Optional;

public interface MatrixJobService {

    /**
     * Startet einen asynchronen Matrix-Lauf. Der Snapshot enthält den frisch erzeugten
     * {@code jobId} und {@code total} (Anzahl Einzelsimulationen), Status ist initial
     * {@link MatrixJobStatus#RUNNING}.
     */
    MatrixJobSnapshot start(MatrixRequest request);

    Optional<MatrixJobSnapshot> get(String jobId);
}
