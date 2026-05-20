package de.zettsystems.h3comsim.matrix.ui;

import de.zettsystems.h3comsim.matrix.application.MatrixJobService;
import de.zettsystems.h3comsim.matrix.values.MatrixJobSnapshot;
import de.zettsystems.h3comsim.matrix.values.MatrixRequestDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/experiments")
@Tag(name = "Matrix Experiments",
        description = "Asynchrone All-vs-All-Experimente: jede zugelassene Einheit kämpft gegen jede andere mehrfach mit getauschten Rollen; das Resultat ist ein aggregierter Report inkl. Tier-Anomalien.")
public class ExperimentController {

    private final MatrixJobService jobs;

    public ExperimentController(MatrixJobService jobs) {
        this.jobs = jobs;
    }

    @Operation(
            summary = "Matrix-Lauf starten",
            description = """
                    Startet einen asynchronen Matrix-Job und liefert sofort einen Snapshot mit
                    der vergebenen `jobId` und Status `RUNNING`. Der Lauf-Fortschritt wird über
                    `GET /matrix/{jobId}` gepollt. Alle Felder im Request sind optional —
                    nicht gesetzte Felder fallen auf Defaults (`unitCount=20`,
                    `mode=EQUAL_COUNT`, `seedsPerMatchup=20`, keine Excludes).
                    """,
            operationId = "startMatrix")
    @ApiResponse(responseCode = "200",
            description = "Job angenommen — Snapshot mit jobId und initialem Status.")
    @ApiResponse(responseCode = "400",
            description = "Validierungsfehler (z.B. unitCount außerhalb 1..200, seedsPerMatchup außerhalb 1..100)",
            content = @Content)
    @PostMapping("/matrix")
    public MatrixJobSnapshot runMatrix(@Valid @RequestBody MatrixRequestDto request) {
        return jobs.start(request.toApplication());
    }

    @Operation(
            summary = "Matrix-Job pollen",
            description = """
                    Liefert den aktuellen Snapshot eines laufenden oder abgeschlossenen
                    Matrix-Jobs. `completed`/`total` sind Einzel-Simulationen (nicht Match-ups).
                    Im Status `COMPLETED` ist `report` gesetzt, im Status `FAILED` ist `error`
                    gesetzt.
                    """,
            operationId = "getMatrixJob")
    @ApiResponse(responseCode = "200", description = "Snapshot des Jobs.")
    @ApiResponse(responseCode = "404",
            description = "Keine Job-ID mit diesem Wert bekannt (Server-Restart oder Tippfehler).",
            content = @Content)
    @GetMapping("/matrix/{jobId}")
    public MatrixJobSnapshot getMatrixJob(
            @Parameter(description = "Job-ID, wie zuvor von `POST /matrix` zurückgegeben.",
                    required = true, example = "1a2b3c4d-5e6f-7890-abcd-ef1234567890")
            @PathVariable String jobId) {
        return jobs.get(jobId).orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Unknown matrix job: " + jobId));
    }
}
