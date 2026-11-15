package io.github.diegoquirino.veredito.api.v1;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.diegoquirino.veredito.support.ApiTestBase;
import io.restassured.response.Response;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

/**
 * SC-001 skeleton: many participants reserve at once; no task is given twice and nobody holds both
 * roles of a block.
 */
class TaskConcurrencyTest extends ApiTestBase {

    @BeforeEach
    void clearStudyData() {
        jdbc.sql("DELETE FROM response").update();
        jdbc.sql("DELETE FROM task").update();
        jdbc.sql("DELETE FROM item").update();
        jdbc.sql("DELETE FROM block").update();
        jdbc.sql("DELETE FROM import_batch").update();
        jdbc.sql("DELETE FROM import_preview").update();
    }

    @Test
    void thirtyConcurrentReservesNeverShareATaskOrBothRoles() throws Exception {
        importMinitest();
        List<String> tokens = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            tokens.add(signIn("Worker " + i, "worker-" + i + "-" + System.nanoTime() + "@example.com", "en-US"));
        }

        ExecutorService pool = Executors.newFixedThreadPool(30);
        try {
            List<Callable<Integer>> jobs = tokens.stream()
                    .<Callable<Integer>>map(token -> () -> {
                        Response response = as(token).post(EntryPointController.TASK_RESERVATIONS);
                        int status = response.statusCode();
                        if (status == 201 || (status == 200 && response.path("id") != null)) {
                            return response.<Integer>path("id");
                        }
                        return null;
                    })
                    .toList();
            List<Future<Integer>> futures = pool.invokeAll(jobs, 2, TimeUnit.MINUTES);
            List<Integer> taskIds = new ArrayList<>();
            for (Future<Integer> future : futures) {
                Integer id = future.get();
                if (id != null) {
                    taskIds.add(id);
                }
            }
            assertThat(taskIds).hasSize(8);
            assertThat(taskIds).doesNotHaveDuplicates();

            List<Map<String, Object>> rows = jdbc.sql("""
                            SELECT block_id, participant_id, COUNT(*) AS n
                            FROM task WHERE status = 'reserved'
                            GROUP BY block_id, participant_id""")
                    .query()
                    .listOfRows();
            assertThat(rows).allSatisfy(row -> assertThat(((Number) row.get("n")).intValue()).isEqualTo(1));
        } finally {
            pool.shutdownNow();
        }
    }

    private void importMinitest() throws IOException {
        String token = administratorSession();
        try (InputStream in = new ClassPathResource("minitest/veredito_minitest_vazio.csv").getInputStream()) {
            Response preview = as(token)
                    .multiPart("file", "veredito_minitest_vazio.csv", in.readAllBytes(), "text/csv")
                    .post(EntryPointController.REQUEST_PREVIEW);
            preview.then().statusCode(201);
            as(token)
                    .contentType("application/json")
                    .body(Map.of("previewId", preview.<Integer>path("id")))
                    .post(EntryPointController.BATCHES)
                    .then().statusCode(201);
        }
    }
}
