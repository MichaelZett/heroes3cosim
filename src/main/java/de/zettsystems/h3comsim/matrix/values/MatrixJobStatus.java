package de.zettsystems.h3comsim.matrix.values;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Status eines Matrix-Jobs. `RUNNING` direkt nach Start und während der Ausführung, `COMPLETED` mit gefülltem Report nach Abschluss, `FAILED` bei Abbruch mit gefülltem `error`.",
        enumAsRef = true)
public enum MatrixJobStatus {
    RUNNING,
    COMPLETED,
    FAILED
}
