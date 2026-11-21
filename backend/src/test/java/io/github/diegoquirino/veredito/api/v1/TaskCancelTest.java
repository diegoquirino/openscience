package io.github.diegoquirino.veredito.api.v1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;

import io.github.diegoquirino.veredito.support.ApiTestBase;
import io.restassured.response.Response;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Spec 023: cancel a held reservation, discard drafts, free the task. */
class TaskCancelTest extends ApiTestBase {

    @BeforeEach
    void minitestImported() {
        clearStudyDatabase();
        importMinitestBatch();
    }

    @Test
    void cancelFreesTaskDeletesDraftsAndAllowsAnotherReserve() {
        String a = eligibleParticipantSession();
        Response task = reserve(a);
        int taskId = task.path("id");
        long itemId = as(a).get("/api/v1/tasks/" + taskId + "/items/1")
                .then().statusCode(200)
                .extract().jsonPath().getLong("itemId");
        putDraft(a, taskId, itemId).then().statusCode(200);
        assertThat(jdbc.sql("SELECT COUNT(*) FROM response WHERE task_id = ?")
                        .param(taskId).query(Long.class).single())
                .isEqualTo(1);
        as(a).get("/api/v1/tasks/" + taskId)
                .then().statusCode(200)
                .body("_links", hasKey("vd:cancel-reservation"));

        as(a).delete("/api/v1/tasks/" + taskId).then().statusCode(204);

        assertThat(jdbc.sql("SELECT status FROM task WHERE id = ?")
                        .param(taskId).query(String.class).single())
                .isEqualTo("free");
        assertThat(jdbc.sql("SELECT COUNT(*) FROM response WHERE task_id = ?")
                        .param(taskId).query(Long.class).single())
                .isZero();

        String b = eligibleSignIn("Carla Dias", "carla-" + System.nanoTime() + "@example.com", "en-US");
        Response again = as(b).post(href(as(b).get("/api/v1"), "next-task"));
        again.then().statusCode(201).body("id", equalTo(taskId));
    }

    @Test
    void foreignOrCompletedTaskCannotBeCancelled() {
        String a = eligibleParticipantSession();
        Response task = reserve(a);
        int taskId = task.path("id");
        String b = eligibleSignIn("Diego Nunes", "diego-" + System.nanoTime() + "@example.com", "en-US");

        as(b).delete("/api/v1/tasks/" + taskId).then().statusCode(404);
        request().delete("/api/v1/tasks/" + taskId).then().statusCode(401);
        as(administratorSession()).delete("/api/v1/tasks/" + taskId).then().statusCode(403);
    }

    private Response reserve(String token) {
        Response response = as(token).post(href(as(token).get("/api/v1"), "next-task"));
        response.then().statusCode(201);
        return response;
    }

    private Response putDraft(String token, int taskId, long itemId) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("changeImpact", "LOW");
        body.put("maintenanceOperation", "KEEP");
        body.put("comment", null);
        return as(token).contentType("application/json").body(body)
                .put("/api/v1/tasks/" + taskId + "/responses/" + itemId);
    }
}
