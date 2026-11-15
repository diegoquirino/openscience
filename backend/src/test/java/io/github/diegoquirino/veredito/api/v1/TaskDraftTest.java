package io.github.diegoquirino.veredito.api.v1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.nullValue;

import io.github.diegoquirino.veredito.support.ApiTestBase;
import io.restassured.response.Response;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** US2 of spec 007: read items and save drafts, renewing the reservation. */
class TaskDraftTest extends ApiTestBase {

    private static final String PROBLEM = "urn:veredito:problem:";

    @BeforeEach
    void minitestImported() {
        clearStudyDatabase();
        importMinitestBatch();
    }

    @Test
    void itemCarriesFieldsAndNoDraftUntilSaved() {
        String a = participantSession();
        Response task = reserve(a);

        as(a).get(itemUrl(task, 1))
                .then().statusCode(200)
                .body("position", equalTo(1))
                .body("itemId", org.hamcrest.Matchers.notNullValue())
                .body("system", equalTo("minitest"))
                .body("$", hasKey("origin"))
                .body("$", hasKey("target"))
                .body("draft", nullValue())
                .body("_links.self.href", equalTo(itemUrl(task, 1)))
                .body("_links.'vd:save-response'.href", org.hamcrest.Matchers.startsWith(
                        "/api/v1/tasks/" + task.<Integer>path("id") + "/responses/"));
    }

    @Test
    void savingDraftRenewsReservationAndIsShownOnTheItem() {
        String a = participantSession();
        Response task = reserve(a);
        Instant before = Instant.parse(task.path("reservationExpiresAt"));
        long itemId = itemId(a, task, 1);

        clock.advance(Duration.ofMinutes(5));
        Response saved = put(a, task, itemId, "HIGH", "UPDATE", "needs rework");
        saved.then().statusCode(200)
                .body("id", equalTo(task.<Integer>path("id")))
                .body("answeredCount", equalTo(1))
                .body("status", equalTo("reserved"));
        Instant after = Instant.parse(saved.path("reservationExpiresAt"));
        // The movable clock also follows real time, so allow a little drift on top of the 5 minutes.
        assertThat(Duration.between(before, after))
                .isBetween(Duration.ofMinutes(5), Duration.ofMinutes(5).plusSeconds(30));

        as(a).get(itemUrl(task, 1))
                .then().statusCode(200)
                .body("draft.changeImpact", equalTo("HIGH"))
                .body("draft.maintenanceOperation", equalTo("UPDATE"))
                .body("draft.comment", equalTo("needs rework"))
                .body("draft.language", equalTo("pt-BR"));
    }

    @Test
    void draftLanguageFollowsAcceptLanguage() {
        String a = participantSession();
        Response task = reserve(a);
        long itemId = itemId(a, task, 1);

        as(a).header("Accept-Language", "es-ES")
                .contentType("application/json")
                .body(Map.of("changeImpact", "LOW", "maintenanceOperation", "KEEP"))
                .put(responseUrl(task, itemId))
                .then().statusCode(200);

        as(a).get(itemUrl(task, 1)).then().body("draft.language", equalTo("es-ES"));
    }

    @Test
    void savingTwiceKeepsOneRowAndReplacesTheValues() {
        String a = participantSession();
        Response task = reserve(a);
        long itemId = itemId(a, task, 1);

        put(a, task, itemId, "LOW", "KEEP", null).then().statusCode(200);
        put(a, task, itemId, "HIGH", "FLAG", "second").then().statusCode(200)
                .body("answeredCount", equalTo(1));

        assertThat(jdbc.sql("SELECT COUNT(*) FROM response WHERE task_id = ?")
                        .param(task.<Integer>path("id"))
                        .query(Long.class)
                        .single())
                .isEqualTo(1);
        as(a).get(itemUrl(task, 1))
                .then().body("draft.changeImpact", equalTo("HIGH"))
                .body("draft.maintenanceOperation", equalTo("FLAG"))
                .body("draft.comment", equalTo("second"));
    }

    @Test
    void invalidCodesAreInvalidRequest() {
        String a = participantSession();
        Response task = reserve(a);
        long itemId = itemId(a, task, 1);

        put(a, task, itemId, "MEDIUM", "KEEP", null).then().statusCode(400)
                .body("type", equalTo(PROBLEM + "invalid-request"))
                .body("errors[0].field", equalTo("changeImpact"))
                .body("errors[0].code", equalTo("invalid-change-impact"));
        put(a, task, itemId, "LOW", null, null).then().statusCode(400)
                .body("type", equalTo(PROBLEM + "invalid-request"))
                .body("errors[0].field", equalTo("maintenanceOperation"))
                .body("errors[0].code", equalTo("invalid-maintenance-operation"));
        put(a, task, itemId, "LOW", "KEEP", "x".repeat(1001)).then().statusCode(400)
                .body("errors[0].field", equalTo("comment"))
                .body("errors[0].code", equalTo("comment-too-long"));
        as(a).contentType("application/json").body("{}").put(responseUrl(task, itemId))
                .then().statusCode(400)
                .body("type", equalTo(PROBLEM + "invalid-request"));

        assertThat(jdbc.sql("SELECT COUNT(*) FROM response").query(Long.class).single()).isZero();
    }

    @Test
    void foreignTaskIsNotFoundAndOtherRoleDataNeverAppears() {
        String a = participantSession();
        String b = signIn("Bruno Lima", "bruno-" + System.nanoTime() + "@example.com", "en-US");
        Response taskA = reserve(a);
        Response taskB = reserve(b);
        long itemId = itemId(a, taskA, 1);
        assertThat(taskB.<Integer>path("id")).isNotEqualTo(taskA.<Integer>path("id"));

        // B can neither read nor write A's task
        as(b).get(itemUrl(taskA, 1)).then().statusCode(404).body("type", equalTo(PROBLEM + "not-found"));
        put(b, taskA, itemId, "LOW", "KEEP", null).then().statusCode(404)
                .body("type", equalTo(PROBLEM + "not-found"));

        // A's secret comment never shows on B's item for the same block position
        put(a, taskA, itemId, "HIGH", "REMOVE", "secret of A").then().statusCode(200);
        put(b, taskB, itemId, "LOW", "KEEP", "note of B").then().statusCode(200);
        Response itemB = as(b).get(itemUrl(taskB, 1));
        itemB.then().statusCode(200).body("draft.comment", equalTo("note of B"));
        assertThat(itemB.asString()).doesNotContain("secret of A");
        assertThat(as(a).get(itemUrl(taskA, 1)).asString()).doesNotContain("note of B");
    }

    @Test
    void positionOutsideTheBlockAndItemOfAnotherBlockAreNotFound() {
        String a = participantSession();
        Response task = reserve(a);
        long otherBlockItem = jdbc.sql("""
                        SELECT id FROM item
                        WHERE block_id <> (SELECT block_id FROM task WHERE id = ?) LIMIT 1""")
                .param(task.<Integer>path("id"))
                .query(Long.class)
                .single();

        as(a).get(itemUrl(task, 0)).then().statusCode(404);
        as(a).get(itemUrl(task, 11)).then().statusCode(404);
        put(a, task, otherBlockItem, "LOW", "KEEP", null).then().statusCode(404)
                .body("type", equalTo(PROBLEM + "not-found"));
    }

    @Test
    void ownExpiredReservationNobodyTookIsRenewedAndKeepsDrafts() {
        String a = participantSession();
        Response task = reserve(a);
        long itemId = itemId(a, task, 1);
        put(a, task, itemId, "LOW", "KEEP", "kept").then().statusCode(200);

        clock.advance(Duration.ofMinutes(task.<Integer>path("reservationMinutes") + 1));
        Response saved = put(a, task, itemId, "HIGH", "UPDATE", "kept");
        saved.then().statusCode(200).body("answeredCount", equalTo(1));
        assertThat(Instant.parse(saved.path("reservationExpiresAt"))).isAfter(clock.instant());
    }

    @Test
    void expiredReservationIsRefusedWhenTheCallerAlreadyHoldsAnotherBlock() {
        String a = participantSession();
        Response first = reserve(a);
        long itemId = itemId(a, first, 1);

        clock.advance(Duration.ofMinutes(first.<Integer>path("reservationMinutes") + 1));
        Response second = reserve(a);
        assertThat(second.<Integer>path("id")).isNotEqualTo(first.<Integer>path("id"));

        put(a, first, itemId, "LOW", "KEEP", null).then().statusCode(409)
                .body("type", equalTo(PROBLEM + "reservation-expired"));
    }

    @Test
    void completedTaskLocksItsAnswers() {
        String a = participantSession();
        Response task = reserve(a);
        long itemId = itemId(a, task, 1);
        put(a, task, itemId, "LOW", "KEEP", null).then().statusCode(200);
        jdbc.sql("UPDATE task SET status = 'completed', submitted_at = ? WHERE id = ?")
                .params(java.sql.Timestamp.from(clock.instant()), task.<Integer>path("id"))
                .update();

        put(a, task, itemId, "HIGH", "FLAG", null).then().statusCode(409)
                .body("type", equalTo(PROBLEM + "task-already-submitted"));
    }

    @Test
    void visitorsAndAdministratorsAreRefused() {
        String a = participantSession();
        Response task = reserve(a);
        long itemId = itemId(a, task, 1);

        request().get(itemUrl(task, 1)).then().statusCode(401);
        request().contentType("application/json").body("{}").put(responseUrl(task, itemId))
                .then().statusCode(401);
        String administrator = administratorSession();
        as(administrator).get(itemUrl(task, 1)).then().statusCode(403);
        as(administrator).contentType("application/json").body("{}").put(responseUrl(task, itemId))
                .then().statusCode(403);
    }

    private Response reserve(String token) {
        Response response = as(token).post(href(as(token).get("/api/v1"), "next-task"));
        response.then().statusCode(201);
        return response;
    }

    private static String itemUrl(Response task, int position) {
        return "/api/v1/tasks/" + task.<Integer>path("id") + "/items/" + position;
    }

    private static String responseUrl(Response task, long itemId) {
        return "/api/v1/tasks/" + task.<Integer>path("id") + "/responses/" + itemId;
    }

    private long itemId(String token, Response task, int position) {
        return as(token).get(itemUrl(task, position)).then().statusCode(200).extract().jsonPath().getLong("itemId");
    }

    private Response put(String token, Response task, long itemId, String impact, String operation, String comment) {
        Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("changeImpact", impact);
        body.put("maintenanceOperation", operation);
        body.put("comment", comment);
        return as(token).contentType("application/json").body(body).put(responseUrl(task, itemId));
    }
}
