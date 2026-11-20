package io.github.diegoquirino.veredito.api.v1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

import io.github.diegoquirino.veredito.domain.onboarding.ConsentDecision;
import io.github.diegoquirino.veredito.domain.onboarding.ScreeningAnswer;
import io.github.diegoquirino.veredito.support.ApiTestBase;
import io.restassured.response.Response;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Spec 011: administrator search and anonymize (keep / discard). */
class ParticipantWithdrawalTest extends ApiTestBase {

    private static final String PROBLEM = "urn:veredito:problem:";

    private String admin;
    private int originalDailyLimit;
    private int participantCounter;

    @BeforeEach
    void setUp() {
        clearStudyDatabase();
        jdbc.sql("DELETE FROM access_session WHERE subject = 'participant'").update();
        jdbc.sql("DELETE FROM withdrawal_log").update();
        jdbc.sql("DELETE FROM participant").update();
        jdbc.sql("DELETE FROM access_code").update();
        originalDailyLimit = jdbc.sql("SELECT daily_email_limit FROM config WHERE id = 1")
                .query(Integer.class)
                .single();
        importMinitestBatch();
        admin = administratorSession();
    }

    @AfterEach
    void restoreLimit() {
        jdbc.sql("UPDATE config SET daily_email_limit = ? WHERE id = 1").param(originalDailyLimit).update();
    }

    @Test
    void administratorFollowsVdParticipantsAndSearchesByEmail() {
        long id = rater("developer", "es-ES");
        String email = emailOf(id);
        String pseudonym = pseudonym(id);

        Response entry = as(admin).get("/api/v1");
        entry.then().statusCode(200).body("_links.'vd:participants'.href", equalTo("/api/v1/participants"));

        as(admin).get(href(entry, "participants") + "?email=" + email)
                .then()
                .statusCode(200)
                .body("participants.size()", equalTo(1))
                .body("participants[0].pseudonym", equalTo(pseudonym))
                .body("participants[0].email", equalTo(email))
                .body("participants[0].preferredLanguage", equalTo("es-ES"))
                .body("participants[0]._links", hasKey("vd:anonymize"));
    }

    @Test
    void keepResponsesErasesPiiReleasesReservationAndKeepsFinals() {
        long id = rater("developer", "en-US");
        String email = emailOf(id);
        completeTask(1, 1, id, "en-US");
        reserveTask(2, 1, id, clock.instant().plus(Duration.ofHours(1)), 2);

        Response result = as(admin)
                .contentType("application/json")
                .body(Map.of("option", "keep_responses"))
                .post("/api/v1/participants/" + id + "/anonymizations");
        result.then()
                .statusCode(201)
                .body("option", equalTo("keep_responses"))
                .body("emailSent", equalTo(true))
                .body("finalResponsesDiscarded", equalTo(0))
                .body("tasksReturned", equalTo(0));

        assertThat(jdbc.sql("SELECT name FROM participant WHERE id = ?").param(id).query(String.class).optional())
                .isEmpty();
        assertThat(jdbc.sql("SELECT email FROM participant WHERE id = ?").param(id).query(String.class).optional())
                .isEmpty();
        assertThat(jdbc.sql("SELECT anonymized_at FROM participant WHERE id = ?").param(id).query(Timestamp.class)
                        .optional())
                .isPresent();
        assertThat(count("SELECT COUNT(*) FROM response WHERE participant_id = ? AND state = 'final'", id))
                .isGreaterThan(0);
        assertThat(count("SELECT COUNT(*) FROM response WHERE participant_id = ? AND state = 'draft'", id))
                .isEqualTo(0);
        assertThat(count("SELECT COUNT(*) FROM task WHERE participant_id = ? AND status = 'reserved'", id))
                .isEqualTo(0);
        assertThat(count("SELECT COUNT(*) FROM task WHERE participant_id = ? AND status = 'completed'", id))
                .isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM access_session WHERE participant_id = ?", id)).isEqualTo(0);
        assertThat(count("SELECT COUNT(*) FROM access_code WHERE email = ?", email)).isEqualTo(0);

        as(admin).get("/api/v1/participants?email=" + email)
                .then()
                .statusCode(200)
                .body("participants.size()", equalTo(0));

        String wide = as(admin).accept("text/csv").get("/api/v1/exports/wide").asString();
        assertThat(wide).contains("anonymized");
        assertThat(wide).doesNotContain(email);
    }

    @Test
    void discardReturnsCompletedTasksToTheQueue() {
        long id = rater("test_analyst", "pt-BR");
        completeTask(1, 1, id, "pt-BR");
        int queuePosition = jdbc.sql("""
                        SELECT queue_position FROM task t JOIN block b ON b.id = t.block_id
                        WHERE b.number = 1 AND t.role = 1""")
                .query(Integer.class)
                .single();

        as(admin)
                .contentType("application/json")
                .body(Map.of("option", "discard_responses"))
                .post("/api/v1/participants/" + id + "/anonymizations")
                .then()
                .statusCode(201)
                .body("option", equalTo("discard_responses"))
                .body("tasksReturned", equalTo(1));

        assertThat(count("SELECT COUNT(*) FROM response WHERE participant_id = ?", id)).isEqualTo(0);
        assertThat(jdbc.sql("""
                        SELECT status, participant_id, queue_position FROM task t
                        JOIN block b ON b.id = t.block_id WHERE b.number = 1 AND t.role = 1""")
                        .query((rs, row) -> Map.of(
                                "status", rs.getString("status"),
                                "participant", String.valueOf(rs.getObject("participant_id")),
                                "queue", rs.getInt("queue_position")))
                        .single())
                .containsEntry("status", "free")
                .containsEntry("participant", "null")
                .containsEntry("queue", queuePosition);
    }

    @Test
    void quotaExhaustedStillWithdrawsWithoutSendingMail() {
        long id = rater("developer", "pt-BR");
        jdbc.sql("UPDATE config SET daily_email_limit = 0 WHERE id = 1").update();

        as(admin)
                .contentType("application/json")
                .body(Map.of("option", "keep_responses"))
                .post("/api/v1/participants/" + id + "/anonymizations")
                .then()
                .statusCode(201)
                .body("emailSent", equalTo(false));

        assertThat(jdbc.sql("SELECT anonymized_at FROM participant WHERE id = ?").param(id).query(Timestamp.class)
                        .optional())
                .isPresent();
    }

    @Test
    void alreadyAnonymizedConflicts() {
        long id = rater("developer", "pt-BR");
        as(admin)
                .contentType("application/json")
                .body(Map.of("option", "keep_responses"))
                .post("/api/v1/participants/" + id + "/anonymizations")
                .then()
                .statusCode(201);

        as(admin)
                .contentType("application/json")
                .body(Map.of("option", "keep_responses"))
                .post("/api/v1/participants/" + id + "/anonymizations")
                .then()
                .statusCode(409)
                .body("type", equalTo(PROBLEM + "already-anonymized"));
    }

    @Test
    void participantCannotSearchOrAnonymize() {
        String participant = participantSession();
        as(participant)
                .get("/api/v1/participants?email=x@example.com")
                .then()
                .statusCode(403);
        as(participant)
                .contentType("application/json")
                .body(Map.of("option", "keep_responses"))
                .post("/api/v1/participants/1/anonymizations")
                .then()
                .statusCode(403);
        as(participant).get("/api/v1").then().body("_links", not(hasKey("vd:participants")));
    }

    @Test
    void listWithoutQueryReturnsAllParticipants() {
        long first = rater("developer", "pt-BR");
        long second = rater("test_analyst", "en-US");

        as(admin)
                .get("/api/v1/participants")
                .then()
                .statusCode(200)
                .body("participants.size()", equalTo(2))
                .body("participants.find { it.id == " + first + " }.email", not(nullValue()))
                .body("participants.find { it.id == " + second + " }.pseudonym", not(nullValue()));
    }

    @Test
    void anonymizeWithChosenPseudonymRenamesWhenUnique() {
        long id = rater("developer", "pt-BR");

        as(admin)
                .contentType("application/json")
                .body(Map.of("option", "keep_responses", "pseudonym", "KEEP-ME"))
                .post("/api/v1/participants/" + id + "/anonymizations")
                .then()
                .statusCode(201)
                .body("pseudonym", equalTo("KEEP-ME"));

        assertThat(jdbc.sql("SELECT pseudonym FROM participant WHERE id = ?").param(id).query(String.class).single())
                .isEqualTo("KEEP-ME");
        assertThat(jdbc.sql("SELECT email FROM participant WHERE id = ?").param(id).query(String.class).optional())
                .isEmpty();
    }

    @Test
    void anonymizeWithTakenPseudonymConflicts() {
        long first = rater("developer", "pt-BR");
        long second = rater("test_analyst", "en-US");
        String taken = pseudonym(first);

        as(admin)
                .contentType("application/json")
                .body(Map.of("option", "keep_responses", "pseudonym", taken))
                .post("/api/v1/participants/" + second + "/anonymizations")
                .then()
                .statusCode(409)
                .body("type", equalTo(PROBLEM + "pseudonym-taken"));

        assertThat(jdbc.sql("SELECT anonymized_at FROM participant WHERE id = ?")
                        .param(second)
                        .query(Timestamp.class)
                        .optional())
                .isEmpty();
    }

    private long rater(String skill, String language) {
        long id = participantIdOf(participantSession());
        participantCounter++;
        jdbc.sql("UPDATE participant SET name = ?, email = ?, preferred_language = ? WHERE id = ?")
                .params(
                        "Withdrawal " + participantCounter,
                        "withdrawal." + participantCounter + "@example.com",
                        language,
                        id)
                .update();
        recordConsent(id, ConsentDecision.ACCEPTED, language);
        seedSkill(id, skill, null);
        seedScreening(id, ScreeningAnswer.YES_PROFESSIONAL);
        return id;
    }

    private void completeTask(int blockNumber, int role, long participantId, String language) {
        long block = blockId(blockNumber);
        Instant reservedAt = clock.instant().minus(Duration.ofMinutes(30));
        jdbc.sql("""
                        UPDATE task SET status = 'completed', participant_id = ?, reserved_at = ?, expires_at = ?,
                            submitted_at = ?
                        WHERE block_id = ? AND role = ?""")
                .params(
                        participantId,
                        Timestamp.from(reservedAt),
                        Timestamp.from(reservedAt.plus(Duration.ofHours(1))),
                        Timestamp.from(clock.instant()),
                        block,
                        role)
                .update();
        insertResponses(block, role, participantId, "final", language, Integer.MAX_VALUE);
    }

    private void reserveTask(int blockNumber, int role, long participantId, Instant expiresAt, int drafts) {
        long block = blockId(blockNumber);
        jdbc.sql("""
                        UPDATE task SET status = 'reserved', participant_id = ?, reserved_at = ?, expires_at = ?
                        WHERE block_id = ? AND role = ?""")
                .params(
                        participantId,
                        Timestamp.from(expiresAt.minus(Duration.ofHours(1))),
                        Timestamp.from(expiresAt),
                        block,
                        role)
                .update();
        insertResponses(block, role, participantId, "draft", "pt-BR", drafts);
    }

    private void insertResponses(long block, int role, long participantId, String state, String language, int limit) {
        long task = jdbc.sql("SELECT id FROM task WHERE block_id = ? AND role = ?")
                .params(block, role)
                .query(Long.class)
                .single();
        var items = jdbc.sql("SELECT id FROM item WHERE block_id = ? ORDER BY position LIMIT ?")
                .params(block, limit)
                .query(Long.class)
                .list();
        for (long item : items) {
            jdbc.sql("""
                            INSERT INTO response (task_id, item_id, participant_id, state, change_impact,
                                maintenance_operation, comment, language, saved_at)
                            VALUES (?, ?, ?, ?, 'LOW', 'KEEP', 'comment', ?, ?)""")
                    .params(task, item, participantId, state, language, Timestamp.from(clock.instant()))
                    .update();
        }
    }

    private long blockId(int number) {
        return jdbc.sql("SELECT id FROM block WHERE number = ?").param(number).query(Long.class).single();
    }

    private String pseudonym(long id) {
        return jdbc.sql("SELECT pseudonym FROM participant WHERE id = ?").param(id).query(String.class).single();
    }

    private String emailOf(long id) {
        return jdbc.sql("SELECT email FROM participant WHERE id = ?").param(id).query(String.class).single();
    }

    private long count(String sql, Object... params) {
        return jdbc.sql(sql).params(params).query(Long.class).single();
    }
}
