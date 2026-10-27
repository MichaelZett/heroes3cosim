package de.zettsystems.h3comsim.matrix.application;

import de.zettsystems.h3comsim.matrix.values.MatrixJobStatus;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class JobRegistryTest {

    private final MatrixJobRunner runner = new MatrixJobRunner();
    private final JobRegistry<String> jobs = new JobRegistry<>(runner);

    @AfterEach
    void stopRunner() {
        runner.shutdown();
    }

    private JobRegistry.View<String> awaitEnd(String jobId) {
        Awaitility.await().atMost(Duration.ofSeconds(10))
                .until(() -> jobs.get(jobId).orElseThrow().status() != MatrixJobStatus.RUNNING);
        return jobs.get(jobId).orElseThrow();
    }

    @Test
    void a_completed_job_carries_its_result_and_full_progress() {
        CountDownLatch release = new CountDownLatch(1);
        String jobId = jobs.start(4, progress -> {
            progress.onProgress(3, 4);
            progress.onProgress(1, 4);
            await(release);
            return "report";
        });

        Awaitility.await().atMost(Duration.ofSeconds(10))
                .until(() -> jobs.get(jobId).orElseThrow().completed() == 3);
        // Fortschritt aus mehreren Threads läuft nie rückwärts: die spätere 1 ändert nichts.
        JobRegistry.View<String> running = jobs.get(jobId).orElseThrow();
        assertThat(running.status()).isEqualTo(MatrixJobStatus.RUNNING);
        assertThat(running.completed()).isEqualTo(3);
        release.countDown();

        JobRegistry.View<String> view = awaitEnd(jobId);
        assertThat(view.status()).isEqualTo(MatrixJobStatus.COMPLETED);
        assertThat(view.completed()).isEqualTo(4);
        assertThat(view.report()).isEqualTo("report");
        assertThat(view.error()).isNull();
    }

    @Test
    void a_failing_job_reports_the_message_or_else_the_exception_type() {
        String withMessage = jobs.start(1, progress -> {
            throw new IllegalStateException("boom");
        });
        String withoutMessage = jobs.start(1, progress -> {
            throw new UnsupportedOperationException();
        });

        JobRegistry.View<String> first = awaitEnd(withMessage);
        JobRegistry.View<String> second = awaitEnd(withoutMessage);

        assertThat(first.status()).isEqualTo(MatrixJobStatus.FAILED);
        assertThat(first.error()).isEqualTo("boom");
        assertThat(first.report()).isNull();
        assertThat(second.error()).isEqualTo("UnsupportedOperationException");
    }

    @Test
    void an_unknown_job_is_empty() {
        assertThat(jobs.get("does-not-exist")).isEmpty();
    }

    private static void await(CountDownLatch latch) {
        try {
            assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
