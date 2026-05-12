package de.zettsystems.h3comsim.application.experiment;

public interface MatrixExperimentService {

    default MatrixReport run(MatrixRequest request) {
        return run(request, ProgressListener.NOOP);
    }

    MatrixReport run(MatrixRequest request, ProgressListener listener);
}
