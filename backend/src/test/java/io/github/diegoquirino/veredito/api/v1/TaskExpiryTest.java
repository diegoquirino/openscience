package io.github.diegoquirino.veredito.api.v1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

import io.github.diegoquirino.veredito.support.ApiTestBase;
import io.restassured.response.Response;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * US5 of spec 007: an expired reservation goes back to the queue without a scheduled job (FR-003 to
 * FR-005). Covers quickstart section 5 over REST, driving expiry with the movable clock instead of
 * waiting in a browser.
 *
 * <p>Policy for the save of the previous holder (A) once the task changed hands: the task now belongs
 * to another participant, so {@code DraftService} answers {@code 404 not-found} (it never reveals
 * foreign tasks). {@code 409 reservation-expired} is only used when A still owns the expired task but
 * already holds a different valid reservation (see {@code TaskDraftTest}).
 */
class TaskExpiryTest extends ApiTestBase {

    private static final String PROBLEM = "urn:veredito:problem:";
    private static final int SHORT_MINUTES = 1;
    private static final int DEFAULT_MINUTES = 20;

    @BeforeEach
    void shortReservationOnMinitest() {
        clearStudyDatabase();
        importMinitestBatch();
        setReservationMinutes(SHORT_MINUTES);
    }

    @AfterEach
    void restoreReservationMinutes() {
        setReservationMinutes(DEFAULT_MINUTES);
    }

    @Test
    void expiredTaskGoesToTheNextParticipantAndTheOldDraftsAreDeleted() {
        String a = eligibleParticipantSession();
        String b = eligibleParticipantSession();
        Response taskA = reserve(a);
        assertThat(taskA.<Integer>path("reservationMinutes")).isEqualTo(SHORT_MINUTES);
        long itemId = itemId(a, taskA, 1);
        put(a, taskA, itemId, "HIGH", "UPDATE", "will be lost").then().statusCode(200);
        assertThat(responseRows(taskA)).isEqualTo(1);

        clock.advance(Duration.ofMinutes(SHORT_MINUTES + 1));
        Response taskB = reserve(b);

        assertThat(taskB.<Integer>path("id")).isEqualTo(taskA.<Integer>path("id"));
        assertThat(taskB.<Integer>path("answeredCount")).isZero();
        assertThat(responseRows(taskA)).isZero();
        assertThat(Instant.parse(taskB.path("reservationExpiresAt"))).isAfter(clock.instant());
        as(b).get(itemUrl(taskB, 1)).then().statusCode(200).body("draft", org.hamcrest.Matchers.nullValue());
    }

    @Test
    void previousHolderCannotSaveAfterTheTaskWasReclaimed() {
        String a = eligibleParticipantSession();
        String b = eligibleParticipantSession();
        Response taskA = reserve(a);
        long itemId = itemId(a, taskA, 1);
        put(a, taskA, itemId, "LOW", "KEEP", "mine").then().statusCode(200);

        clock.advance(Duration.ofMinutes(SHORT_MINUTES + 1));
        reserve(b).then().statusCode(201);

        // Documented policy: the task is no longer A's, so the save is 404, not reservation-expired.
        put(a, taskA, itemId, "HIGH", "FLAG", "too late").then().statusCode(404)
                .body("type", equalTo(PROBLEM + "not-found"));
        as(a).get("/api/v1/tasks/" + taskA.<Integer>path("id")).then().statusCode(404);
        assertThat(responseRows(taskA)).isZero();
    }

    @Test
    void saveWhenNobodyTookTheExpiredTaskRenewsItForTheOwnerAndKeepsDrafts() {
        String a = eligibleParticipantSession();
        Response taskA = reserve(a);
        long itemId = itemId(a, taskA, 1);
        put(a, taskA, itemId, "LOW", "KEEP", "first").then().statusCode(200);

        clock.advance(Duration.ofMinutes(SHORT_MINUTES + 1));
        // No other participant asked, so nothing is reclaimed; there is no scheduled job either.
        assertThat(responseRows(taskA)).isEqualTo(1);

        Response saved = put(a, taskA, itemId, "HIGH", "UPDATE", "second");
        saved.then().statusCode(200)
                .body("id", equalTo(taskA.<Integer>path("id")))
                .body("status", equalTo("reserved"))
                .body("answeredCount", equalTo(1));
        assertThat(Instant.parse(saved.path("reservationExpiresAt"))).isAfter(clock.instant());
        assertThat(responseRows(taskA)).isEqualTo(1);
        as(a).get(itemUrl(taskA, 1)).then().statusCode(200)
                .body("draft.comment", equalTo("second"))
                .body("draft.changeImpact", equalTo("HIGH"));
    }

    @Test
    void expiryNeedsNoJobAndReservationStaysUntilSomebodyAsks() {
        String a = eligibleParticipantSession();
        Response taskA = reserve(a);
        clock.advance(Duration.ofHours(3));

        assertThat(jdbc.sql("SELECT status FROM task WHERE id = ?")
                        .param(taskA.<Integer>path("id"))
                        .query(String.class)
                        .single())
                .isEqualTo("reserved");
    }

    private void setReservationMinutes(int minutes) {
        jdbc.sql("UPDATE config SET reservation_minutes = ?, updated_at = UTC_TIMESTAMP(6) WHERE id = 1")
                .param(minutes)
                .update();
    }

    private long responseRows(Response task) {
        return jdbc.sql("SELECT COUNT(*) FROM response WHERE task_id = ?")
                .param(task.<Integer>path("id"))
                .query(Long.class)
                .single();
    }

    private Response reserve(String token) {
        return as(token).post(href(as(token).get("/api/v1"), "next-task"));
    }

    private static String itemUrl(Response task, int position) {
        return "/api/v1/tasks/" + task.<Integer>path("id") + "/items/" + position;
    }

    private long itemId(String token, Response task, int position) {
        return as(token).get(itemUrl(task, position)).then().statusCode(200).extract().jsonPath().getLong("itemId");
    }

    private Response put(String token, Response task, long itemId, String impact, String operation, String comment) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("changeImpact", impact);
        body.put("maintenanceOperation", operation);
        body.put("comment", comment);
        return as(token).contentType("application/json").body(body)
                .put("/api/v1/tasks/" + task.<Integer>path("id") + "/responses/" + itemId);
    }
}
