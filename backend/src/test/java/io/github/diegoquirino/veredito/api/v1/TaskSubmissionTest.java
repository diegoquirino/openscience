package io.github.diegoquirino.veredito.api.v1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

import io.github.diegoquirino.veredito.support.ApiTestBase;
import io.restassured.response.Response;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** US3 of spec 007: submit the block, locking its answers. */
class TaskSubmissionTest extends ApiTestBase {

    private static final String PROBLEM = "urn:veredito:problem:";

    @BeforeEach
    void minitestImported() {
        clearStudyDatabase();
        importMinitestBatch();
    }

    @Test
    void completeBlockIsSubmittedAndFinalsAreStored() {
        String a = participantSession();
        Response task = reserve(a);
        answerAll(a, task, 10);

        Response submitted = as(a).post(submissionUrl(task));
        submitted.then().statusCode(200)
                .body("id", equalTo(task.<Integer>path("id")))
                .body("status", equalTo("completed"))
                .body("answeredCount", equalTo(10))
                .body("outcome", equalTo("more-available"))
                .body("$", not(hasKey("reservationExpiresAt")))
                .body("_links", not(hasKey("vd:submit")))
                .body("_links", not(hasKey("vd:save-response")))
                .body("_links.'vd:next-task'.href", equalTo("/api/v1/tasks/reservations"));

        assertThat(count("SELECT COUNT(*) FROM response WHERE task_id = ? AND state = 'final'", task))
                .isEqualTo(10);
        assertThat(count("SELECT COUNT(*) FROM response WHERE task_id = ? AND state = 'draft'", task))
                .isZero();
        assertThat(count("SELECT COUNT(*) FROM task WHERE id = ? AND status = 'completed'"
                        + " AND submitted_at IS NOT NULL", task))
                .isEqualTo(1);

        as(a).get("/api/v1/tasks/" + task.<Integer>path("id"))
                .then().statusCode(200)
                .body("status", equalTo("completed"))
                .body("outcome", equalTo("more-available"));
    }

    @Test
    void incompleteBlockIsRefusedWithTheFirstPendingPosition() {
        String a = participantSession();
        Response task = reserve(a);
        answerAll(a, task, 10, 3);

        as(a).post(submissionUrl(task)).then().statusCode(409)
                .body("type", equalTo(PROBLEM + "block-incomplete"))
                .body("firstPendingPosition", equalTo(3));

        assertThat(count("SELECT COUNT(*) FROM response WHERE task_id = ? AND state = 'draft'", task))
                .isEqualTo(9);
        assertThat(count("SELECT COUNT(*) FROM task WHERE id = ? AND status = 'reserved'", task))
                .isEqualTo(1);
    }

    @Test
    void blockWithoutAnyAnswerPointsAtPositionOne() {
        String a = participantSession();
        Response task = reserve(a);

        as(a).post(submissionUrl(task)).then().statusCode(409)
                .body("type", equalTo(PROBLEM + "block-incomplete"))
                .body("firstPendingPosition", equalTo(1));
    }

    @Test
    void resubmittingIsAnUnchangedNoOp() {
        String a = participantSession();
        Response task = reserve(a);
        answerAll(a, task, 10);
        as(a).post(submissionUrl(task)).then().statusCode(200);
        java.sql.Timestamp submittedAt = jdbc.sql("SELECT submitted_at FROM task WHERE id = ?")
                .param(task.<Integer>path("id"))
                .query(java.sql.Timestamp.class)
                .single();

        clock.advance(Duration.ofMinutes(3));
        as(a).post(submissionUrl(task)).then().statusCode(200)
                .body("status", equalTo("completed"))
                .body("answeredCount", equalTo(10))
                .body("outcome", equalTo("more-available"));

        assertThat(jdbc.sql("SELECT submitted_at FROM task WHERE id = ?")
                        .param(task.<Integer>path("id"))
                        .query(java.sql.Timestamp.class)
                        .single())
                .isEqualTo(submittedAt);
        assertThat(count("SELECT COUNT(*) FROM response WHERE task_id = ? AND state = 'final'", task))
                .isEqualTo(10);
    }

    @Test
    void savingAfterSubmitIsRefused() {
        String a = participantSession();
        Response task = reserve(a);
        answerAll(a, task, 10);
        as(a).post(submissionUrl(task)).then().statusCode(200);

        put(a, task, itemId(a, task, 1), "HIGH", "FLAG", "late").then().statusCode(409)
                .body("type", equalTo(PROBLEM + "task-already-submitted"));
    }

    @Test
    void lastTaskOfTheDatabaseReportsDatabaseComplete() {
        String a = participantSession();
        Response task = reserve(a);
        answerAll(a, task, 10);
        // Everything else is already done (by raters outside this test).
        participantSession();
        participantSession();
        List<Long> others = jdbc.sql("SELECT id FROM participant ORDER BY id DESC LIMIT 2")
                .query(Long.class)
                .list();
        // One rater per role, so no participant holds both roles of a block.
        for (int role = 1; role <= 2; role++) {
            jdbc.sql("""
                            UPDATE task SET status = 'completed', participant_id = ?, reserved_at = ?,
                                submitted_at = ?
                            WHERE id <> ? AND status = 'free' AND role = ?""")
                    .params(
                            others.get(role - 1),
                            java.sql.Timestamp.from(clock.instant()),
                            java.sql.Timestamp.from(clock.instant()),
                            task.<Integer>path("id"),
                            role)
                    .update();
        }

        as(a).post(submissionUrl(task)).then().statusCode(200)
                .body("status", equalTo("completed"))
                .body("outcome", equalTo("database-complete"))
                .body("_links", not(hasKey("vd:next-task")));
    }

    @Test
    void personWhoLabeledOneRoleOfEveryBlockGetsNoneAvailableForYou() {
        String a = participantSession();
        // Four blocks: the person may take one role of each, never both roles of a block.
        for (int block = 1; block <= 4; block++) {
            Response task = reserve(a);
            assertThat(task.<Integer>path("blockNumber")).isEqualTo(block);
            answerAll(a, task, task.<Integer>path("size"));
            Response submitted = as(a).post(submissionUrl(task));
            submitted.then().statusCode(200)
                    .body("status", equalTo("completed"))
                    .body("outcome", equalTo(block < 4 ? "more-available" : "none-available-for-you"));
            assertThat(submitted.<Object>path("_links.'vd:next-task'") != null).isEqualTo(block < 4);
        }

        as(a).post("/api/v1/tasks/reservations").then().statusCode(200)
                .body("outcome", equalTo("none-available-for-you"));
        assertThat(jdbc.sql("SELECT COUNT(*) FROM task WHERE status = 'free'").query(Long.class).single())
                .isEqualTo(4);
    }

    @Test
    void reserveOnACompletedDatabaseReportsDatabaseComplete() {
        // Two raters (one per role) already answered everything; the caller arrives afterwards.
        participantSession();
        signIn("Bruno Lima", "bruno-" + System.nanoTime() + "@example.com", "en-US");
        List<Long> raters = jdbc.sql("SELECT id FROM participant ORDER BY id DESC LIMIT 2")
                .query(Long.class)
                .list();
        String a = signIn("Carla Dias", "carla-" + System.nanoTime() + "@example.com", "en-US");
        for (int role = 1; role <= 2; role++) {
            jdbc.sql("""
                            UPDATE task SET status = 'completed', participant_id = ?, reserved_at = ?,
                                submitted_at = ?
                            WHERE role = ?""")
                    .params(
                            raters.get(role - 1),
                            java.sql.Timestamp.from(clock.instant()),
                            java.sql.Timestamp.from(clock.instant()),
                            role)
                    .update();
        }

        as(a).post("/api/v1/tasks/reservations").then().statusCode(200)
                .body("outcome", equalTo("database-complete"));
    }

    @Test
    void foreignTaskIsNotFoundAndOtherRoleKeepsItsOwnState() {
        String a = participantSession();
        String b = signIn("Bruno Lima", "bruno-" + System.nanoTime() + "@example.com", "en-US");
        Response taskA = reserve(a);
        Response taskB = reserve(b);
        answerAll(a, taskA, 10);

        as(b).post(submissionUrl(taskA)).then().statusCode(404)
                .body("type", equalTo(PROBLEM + "not-found"));
        as(a).post("/api/v1/tasks/999999999/submission").then().statusCode(404);

        as(a).post(submissionUrl(taskA)).then().statusCode(200);
        as(b).get("/api/v1/tasks/" + taskB.<Integer>path("id"))
                .then().statusCode(200)
                .body("status", equalTo("reserved"))
                .body("outcome", nullValue());
    }

    @Test
    void expiredReservationNobodyTookStillSubmits() {
        String a = participantSession();
        Response task = reserve(a);
        answerAll(a, task, 10);

        clock.advance(Duration.ofMinutes(task.<Integer>path("reservationMinutes") + 1));
        as(a).post(submissionUrl(task)).then().statusCode(200)
                .body("status", equalTo("completed"));
    }

    @Test
    void expiredReservationIsRefusedWhenTheCallerAlreadyHoldsAnotherBlock() {
        String a = participantSession();
        Response first = reserve(a);
        answerAll(a, first, 10);

        clock.advance(Duration.ofMinutes(first.<Integer>path("reservationMinutes") + 1));
        Response second = reserve(a);
        assertThat(second.<Integer>path("id")).isNotEqualTo(first.<Integer>path("id"));

        as(a).post(submissionUrl(first)).then().statusCode(409)
                .body("type", equalTo(PROBLEM + "reservation-expired"));
        assertThat(count("SELECT COUNT(*) FROM response WHERE task_id = ? AND state = 'final'", first))
                .isZero();
    }

    @Test
    void visitorsAndAdministratorsAreRefused() {
        String a = participantSession();
        Response task = reserve(a);
        answerAll(a, task, 10);

        request().post(submissionUrl(task)).then().statusCode(401);
        as(administratorSession()).post(submissionUrl(task)).then().statusCode(403);
        assertThat(count("SELECT COUNT(*) FROM task WHERE id = ? AND status = 'reserved'", task))
                .isEqualTo(1);
    }

    @Test
    void submitLinkAppearsOnlyWhenEveryItemIsAnswered() {
        String a = participantSession();
        Response task = reserve(a);
        answerAll(a, task, 10, 10);

        as(a).get("/api/v1/tasks/" + task.<Integer>path("id"))
                .then().statusCode(200)
                .body("_links", not(hasKey("vd:submit")));

        Response saved = put(a, task, itemId(a, task, 10), "LOW", "KEEP", null);
        saved.then().statusCode(200).body("_links.'vd:submit'.href", equalTo(submissionUrl(task)));
    }

    // helpers

    private Response reserve(String token) {
        Response response = as(token).post(href(as(token).get("/api/v1"), "next-task"));
        response.then().statusCode(201);
        return response;
    }

    private static String submissionUrl(Response task) {
        return "/api/v1/tasks/" + task.<Integer>path("id") + "/submission";
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
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("changeImpact", impact);
        body.put("maintenanceOperation", operation);
        body.put("comment", comment);
        return as(token).contentType("application/json").body(body).put(responseUrl(task, itemId));
    }

    /** Drafts for positions 1..size, leaving out {@code skipped} (none by default). */
    private void answerAll(String token, Response task, int size, Integer... skipped) {
        List<Integer> skip = List.of(skipped);
        for (int position = 1; position <= size; position++) {
            if (skip.contains(position)) {
                continue;
            }
            put(token, task, itemId(token, task, position), position % 2 == 0 ? "HIGH" : "LOW", "KEEP", null)
                    .then().statusCode(200);
        }
    }

    private long count(String sql, Response task) {
        return jdbc.sql(sql).param(task.<Integer>path("id")).query(Long.class).single();
    }
}
