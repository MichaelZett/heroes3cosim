package de.zettsystems.h3comsim.matrix.application;

import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Ein Matrix-Lauf zur Zeit, gleich welcher Art: jeder Lauf parallelisiert intern, mehrere Läufe
 * sollen nicht um dieselben Worker konkurrieren.
 */
@Component
class MatrixJobRunner {

    private final ExecutorService runner = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "matrix-job");
        t.setDaemon(true);
        return t;
    });

    void execute(Runnable job) {
        runner.execute(job);
    }

    @PreDestroy
    void shutdown() {
        runner.shutdownNow();
    }
}
