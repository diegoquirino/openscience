package io.github.diegoquirino.veredito.api.v1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

import io.github.diegoquirino.veredito.support.ApiTestBase;
import io.restassured.response.Response;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** SC-001 of spec 005: 500 lines, preview and confirm under 2 minutes. */
class ImportVolumeTest extends ApiTestBase {

    private static final int ROWS = 500;
    private static final Duration LIMIT = Duration.ofMinutes(2);

    @BeforeEach
    void clearStudyData() {
        jdbc.sql("DELETE FROM task").update();
        jdbc.sql("DELETE FROM item").update();
        jdbc.sql("DELETE FROM block").update();
        jdbc.sql("DELETE FROM import_batch").update();
        jdbc.sql("DELETE FROM import_preview").update();
    }

    @Test
    void fiveHundredRowsPreviewAndConfirmUnderTwoMinutes() {
        String token = administratorSession();
        byte[] csv = volumeCsv().getBytes(StandardCharsets.UTF_8);

        Instant started = Instant.now();
        Response preview = as(token)
                .multiPart("file", "volume-500.csv", csv, "text/csv")
                .post(href(as(token).get("/api/v1"), "request-preview"));
        preview.then().statusCode(201)
                .body("includedCount", equalTo(ROWS))
                .body("errorCount", equalTo(0))
                .body("ignoredCount", equalTo(0));

        Response confirmed = as(token)
                .contentType("application/json")
                .body(Map.of("previewId", preview.<Integer>path("id")))
                .post(href(preview, "confirm"));
        confirmed.then().statusCode(201)
                .body("includedCount", equalTo(ROWS))
                .body("blockCount", equalTo(50));
        Duration elapsed = Duration.between(started, Instant.now());

        assertThat(elapsed).isLessThan(LIMIT);
        assertThat(jdbc.sql("SELECT COUNT(*) FROM item").query(Long.class).single()).isEqualTo(ROWS);
        assertThat(jdbc.sql("SELECT COUNT(*) FROM block").query(Long.class).single()).isEqualTo(50);
        assertThat(jdbc.sql("SELECT COUNT(*) FROM task").query(Long.class).single()).isEqualTo(100);
    }

    private static String volumeCsv() {
        String header = String.join(",",
                "system", "edit_key", "uc", "kind", "field", "origin", "target",
                "origin_version", "target_version", "origin_usecase", "target_usecase");
        String rows = IntStream.range(0, ROWS)
                .mapToObj(i -> "minitest,vol-" + i + ",UC,MOD,field,o,t,1,2,ou,tu")
                .collect(Collectors.joining("\n"));
        return header + "\n" + rows + "\n";
    }
}
