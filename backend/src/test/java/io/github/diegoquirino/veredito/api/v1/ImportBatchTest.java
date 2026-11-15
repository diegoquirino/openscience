package io.github.diegoquirino.veredito.api.v1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;

import io.github.diegoquirino.veredito.support.ApiTestBase;
import io.restassured.response.Response;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

/** US2 of spec 005: confirm preview into blocks and free tasks. */
class ImportBatchTest extends ApiTestBase {

    @BeforeEach
    void clearStudyData() {
        jdbc.sql("DELETE FROM task").update();
        jdbc.sql("DELETE FROM item").update();
        jdbc.sql("DELETE FROM block").update();
        jdbc.sql("DELETE FROM import_batch").update();
        jdbc.sql("DELETE FROM import_preview").update();
    }

    @Test
    void confirmMinitestStoresBlocksAndQueuePositions() throws IOException {
        String token = administratorSession();
        long itemsBefore = count("item");
        Response preview = upload(token, "minitest/veredito_minitest_vazio.csv");
        preview.then().statusCode(201);
        int previewId = preview.path("id");

        Response confirmed = as(token)
                .contentType("application/json")
                .body(Map.of("previewId", previewId))
                .post(href(preview, "confirm"));
        confirmed.then().statusCode(201)
                .header("Location", startsWith("/api/v1/batches/"))
                .body("includedCount", equalTo(38))
                .body("blockCount", equalTo(4))
                .body("blockSizes", contains(10, 10, 10, 8))
                .body("queuePositions.from", equalTo(1))
                .body("queuePositions.to", equalTo(8))
                .body("systems", contains("minitest"));

        assertThat(count("item")).isEqualTo(itemsBefore + 38);
        assertThat(count("block")).isEqualTo(4);
        assertThat(count("task")).isEqualTo(8);

        String self = confirmed.path("_links.self.href");
        as(token).get(self)
                .then().statusCode(200)
                .body("includedCount", equalTo(38))
                .body("blockSizes", contains(10, 10, 10, 8));

        List<Map<String, Object>> stored = jdbc.sql("""
                        SELECT `system`, edit_key, uc, kind, field FROM item ORDER BY id""")
                .query()
                .listOfRows();
        assertThat(stored).hasSize(38);
        assertThat(stored.getFirst().get("system")).isEqualTo("minitest");
    }

    @Test
    void secondConfirmIgnoresEverything() throws IOException {
        String token = administratorSession();
        Response firstPreview = upload(token, "minitest/veredito_minitest_vazio.csv");
        firstPreview.then().statusCode(201);
        as(token)
                .contentType("application/json")
                .body(Map.of("previewId", firstPreview.<Integer>path("id")))
                .post(href(firstPreview, "confirm"))
                .then().statusCode(201);

        Response secondPreview = upload(token, "minitest/veredito_minitest_vazio.csv");
        secondPreview.then().statusCode(201).body("includedCount", equalTo(0));
        as(token)
                .contentType("application/json")
                .body(Map.of("previewId", secondPreview.<Integer>path("id")))
                .post(EntryPointController.BATCHES)
                .then().statusCode(200)
                .body("includedCount", equalTo(0))
                .body("id", nullValue());
    }

    private Response upload(String token, String classpath) throws IOException {
        try (InputStream in = new ClassPathResource(classpath).getInputStream()) {
            byte[] bytes = in.readAllBytes();
            String name = classpath.substring(classpath.lastIndexOf('/') + 1);
            return as(token).multiPart("file", name, bytes, "text/csv")
                    .post(href(as(token).get("/api/v1"), "request-preview"));
        }
    }

    private long count(String table) {
        return jdbc.sql("SELECT COUNT(*) FROM " + table).query(Long.class).single();
    }
}
