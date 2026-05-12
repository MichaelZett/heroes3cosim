package de.zettsystems.h3comsim.adapter.web;

import de.zettsystems.h3comsim.adapter.web.dto.MatrixRequestDto;
import de.zettsystems.h3comsim.application.experiment.MatrixJobService;
import de.zettsystems.h3comsim.application.experiment.MatrixJobSnapshot;
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
public class ExperimentController {

    private final MatrixJobService jobs;

    public ExperimentController(MatrixJobService jobs) {
        this.jobs = jobs;
    }

    @PostMapping("/matrix")
    public MatrixJobSnapshot runMatrix(@Valid @RequestBody MatrixRequestDto request) {
        return jobs.start(request.toApplication());
    }

    @GetMapping("/matrix/{jobId}")
    public MatrixJobSnapshot getMatrixJob(@PathVariable String jobId) {
        return jobs.get(jobId).orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Unknown matrix job: " + jobId));
    }
}
