package de.zettsystems.h3comsim.matrix.application;

import de.zettsystems.h3comsim.matrix.values.MatrixReport;
import de.zettsystems.h3comsim.matrix.values.MatrixRequest;

public interface MatrixExperimentService {

    default MatrixReport run(MatrixRequest request) {
        return run(request, ProgressListener.NOOP);
    }

    MatrixReport run(MatrixRequest request, ProgressListener listener);
}
