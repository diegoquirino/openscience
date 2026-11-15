package io.github.diegoquirino.veredito.api.v1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.startsWith;

import io.github.diegoquirino.veredito.support.ApiTestBase;
import io.restassured.response.Response;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

/** US1 of spec 007: reserve the next free task. */
class TaskReservationTest extends ApiTestBase {

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
    void firstAndSecondCallersGetDifferentRolesThenResume() throws IOException {
        importMinitest();
        String a = participantSession();
        String b = signIn("Bruno Lima", "bruno-" + System.nanoTime() + "@example.com", "en-US");

        Response first = as(a).post(href(as(a).get("/api/v1"), "next-task"));
        first.then().statusCode(201)
                .header("Location", startsWith("/api/v1/tasks/"))
                .body("blockNumber", equalTo(1))
                .body("role", equalTo(1))
                .body("status", equalTo("reserved"))
                .body("size", equalTo(10));

        Response second = as(b).post(href(as(b).get("/api/v1"), "next-task"));
        second.then().statusCode(201)
                .body("blockNumber", equalTo(1))
                .body("role", equalTo(2));

        as(a).post(EntryPointController.TASK_RESERVATIONS)
                .then().statusCode(200)
                .body("id", equalTo(first.<Integer>path("id")))
                .body("role", equalTo(1));

        as(a).get(first.<String>path("_links.self.href"))
                .then().statusCode(200)
                .body("id", equalTo(first.<Integer>path("id")));
    }

    @Test
    void emptyDatabaseReturnsNoItemsYet() {
        String token = participantSession();
        as(token).post(href(as(token).get("/api/v1"), "next-task"))
                .then().statusCode(200)
                .body("outcome", equalTo("no-items-yet"));
    }

    @Test
    void visitorAndAdministratorAreRefused() throws IOException {
        importMinitest();
        request().post(EntryPointController.TASK_RESERVATIONS)
                .then().statusCode(401);
        as(administratorSession()).post(EntryPointController.TASK_RESERVATIONS)
                .then().statusCode(403);
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
        assertThat(jdbc.sql("SELECT COUNT(*) FROM task").query(Long.class).single()).isEqualTo(8);
    }
}
