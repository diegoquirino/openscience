package io.github.diegoquirino.veredito.api.v1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;

import io.github.diegoquirino.veredito.domain.onboarding.ConsentDecision;
import io.github.diegoquirino.veredito.domain.onboarding.ScreeningAnswer;
import io.github.diegoquirino.veredito.support.ApiTestBase;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Spec 010: the administrator follows {@code vd:dashboard} and reads the progress snapshot. Fixtures are the
 * Minitest import (4 blocks, 8 tasks) with tasks, drafts and participants written straight in the tables.
 */
class DashboardTest extends ApiTestBase {

    private static final String PROBLEM = "urn:veredito:problem:";
    private static final String SECRET_NAME = "Secret Rater";
    private static final String SECRET_EMAIL = "secret.rater@example.com";

    private String admin;
    private int originalDailyLimit;
    private int participantCounter;

    @BeforeEach
    void minitestImported() {
        clearStudyDatabase();
        jdbc.sql("DELETE FROM access_session WHERE subject = 'participant'").update();
        jdbc.sql("DELETE FROM participant").update();
        jdbc.sql("DELETE FROM access_code").update();
        originalDailyLimit = jdbc.sql("SELECT daily_email_limit FROM config WHERE id = 1")
                .query(Integer.class).single();
        importMinitestBatch();
        admin = administratorSession();
    }

    @AfterEach
    void restoreTheDailyLimit() {
        jdbc.sql("UPDATE config SET daily_email_limit = ? WHERE id = 1").param(originalDailyLimit).update();
    }

    // access

    @Test
    void theAdministratorFollowsTheEntryPointLinkToTheSnapshot() {
        Response entry = as(admin).get("/api/v1");
        entry.then().statusCode(200).body("_links.'vd:dashboard'.href", equalTo("/api/v1/dashboard"));

        as(admin).get(href(entry, "dashboard"))
                .then().statusCode(200)
                .contentType(startsWith("application/prs.hal-forms+json"))
                .body("_links.self.href", equalTo("/api/v1/dashboard"))
                .body("_links.curies[0].name", equalTo("vd"))
                .body("_links", not(hasKey("vd:batches")));
    }

    @Test
    void visitorsAndParticipantsAreRefusedAndNeverOfferedTheLink() {
        String participant = participantSession();

        request().get("/api/v1/dashboard")
                .then().statusCode(401).body("type", equalTo(PROBLEM + "authentication-required"));
        as(participant).get("/api/v1/dashboard")
                .then().statusCode(403).body("type", equalTo(PROBLEM + "forbidden"));
        entryPoint().then().body("_links", not(hasKey("vd:dashboard")));
        as(participant).get("/api/v1").then().body("_links", not(hasKey("vd:dashboard")));
    }

    @Test
    void theRelationIsDocumented() {
        request().get("/api/v1/rels/dashboard").then().statusCode(200);
    }

    // US1: progress

    @Test
    void emptyStudyHasZerosAndLinksTheImport() {
        clearStudyDatabase();

        Response response = dashboard();

        response.then().statusCode(200)
                .body("progress.size()", equalTo(1))
                .body("progress[0].items", equalTo(0))
                .body("progress[0].blocks", equalTo(0))
                .body("progress[0].role1.free", equalTo(0))
                .body("progress[0].role1.completedPercent", equalTo(0.0f))
                .body("firstFreeQueuePosition", equalTo(null))
                .body("reservations.size()", equalTo(0))
                .body("participants.completedByPseudonym.size()", equalTo(0))
                .body("languages.size()", equalTo(3))
                .body("_links.'vd:batches'.href", equalTo("/api/v1/batches"));
    }

    @Test
    void overallRowIsFirstWithNullSystemAndBatchThenOneRowPerSystemAndBatch() {
        JsonPath json = dashboard().jsonPath();

        assertThat(json.getList("progress")).hasSize(2);
        Map<String, Object> overall = json.getMap("progress[0]");
        assertThat(overall).containsEntry("system", null).containsEntry("batchId", null)
                .containsEntry("batchFileName", null);
        assertThat(overall).containsEntry("items", 40).containsEntry("blocks", 4);
        assertThat(json.getString("progress[1].system")).isEqualTo("minitest");
        assertThat(json.getString("progress[1].batchFileName")).isEqualTo("veredito_minitest_vazio.csv");
        assertThat(json.getLong("progress[1].batchId")).isEqualTo(batchId());
        assertThat(json.getInt("progress[1].items")).isEqualTo(40);
        assertThat(json.getInt("progress[1].blocks")).isEqualTo(4);
        assertThat(json.getInt("progress[1].role1.free")).isEqualTo(4);
        assertThat(json.getInt("progress[1].role2.free")).isEqualTo(4);
        assertThat(json.getInt("firstFreeQueuePosition")).isEqualTo(1);
    }

    @Test
    void bucketsMatchTheTablesAndAnExpiredReservationCountsAsFree() {
        long ana = rater("test_analyst", "pt-BR");
        long bruno = rater("test_analyst", "pt-BR");
        long carla = rater("test_analyst", "pt-BR");
        Instant now = clock.instant();
        completeTask(1, 1, ana, "pt-BR");
        reserveTask(1, 2, bruno, now.plus(Duration.ofMinutes(30)), 0);
        reserveTask(2, 1, carla, now.minus(Duration.ofMinutes(1)), 0);

        JsonPath json = dashboard().jsonPath();

        // role 1: block 1 completed, block 2 reserved but expired (free), blocks 3 and 4 free
        assertThat(json.getInt("progress[0].role1.completed")).isEqualTo(1);
        assertThat(json.getInt("progress[0].role1.inProgress")).isZero();
        assertThat(json.getInt("progress[0].role1.free")).isEqualTo(3);
        assertThat(json.getDouble("progress[0].role1.completedPercent")).isEqualTo(25.0);
        // role 2: block 2 reserved and active, the other three free
        assertThat(json.getInt("progress[0].role2.completed")).isZero();
        assertThat(json.getInt("progress[0].role2.inProgress")).isEqualTo(1);
        assertThat(json.getInt("progress[0].role2.free")).isEqualTo(3);
        assertThat(json.getDouble("progress[0].role2.completedPercent")).isEqualTo(0.0);
        // SC-001: the totals equal the task table under the research section 3 mapping.
        long completed = count("SELECT COUNT(*) FROM task WHERE status = 'completed'");
        long active = count("SELECT COUNT(*) FROM task WHERE status = 'reserved' AND expires_at > ?", now);
        long free = count("SELECT COUNT(*) FROM task WHERE status = 'free' OR (status = 'reserved' AND expires_at <= ?)", now);
        assertThat(json.getInt("progress[0].role1.completed") + json.getInt("progress[0].role2.completed"))
                .isEqualTo(completed);
        assertThat(json.getInt("progress[0].role1.inProgress") + json.getInt("progress[0].role2.inProgress"))
                .isEqualTo(active);
        assertThat(json.getInt("progress[0].role1.free") + json.getInt("progress[0].role2.free")).isEqualTo(free);
        assertThat(completed + active + free).isEqualTo(count("SELECT COUNT(*) FROM task"));
        // The per system and batch row repeats the overall numbers (a single batch).
        assertThat(json.getMap("progress[1].role1")).isEqualTo(json.getMap("progress[0].role1"));
        // The expired reservation is reclaimable: queue position of block 2 role 1 is the first free one.
        long firstFree = jdbc.sql("""
                        SELECT MIN(queue_position) FROM task
                        WHERE status = 'free' OR (status = 'reserved' AND expires_at <= ?)""")
                .param(Timestamp.from(now)).query(Long.class).single();
        assertThat(json.getLong("firstFreeQueuePosition")).isEqualTo(firstFree);
    }

    @Test
    void firstFreeQueuePositionIsNullWhenEveryTaskIsTakenOrDone() {
        long ana = rater("test_analyst", "pt-BR");
        Instant later = clock.instant().plus(Duration.ofHours(1));
        for (int block = 1; block <= 4; block++) {
            for (int role = 1; role <= 2; role++) {
                long who = block == 1 && role == 1 ? ana : rater("test_analyst", "pt-BR");
                if ((block + role) % 2 == 0) {
                    completeTask(block, role, who, "pt-BR");
                } else {
                    reserveTask(block, role, who, later, 0);
                }
            }
        }

        dashboard().then().statusCode(200)
                .body("firstFreeQueuePosition", equalTo(null))
                .body("progress[0].role1.free", equalTo(0))
                .body("progress[0].role2.free", equalTo(0));
    }

    // US2: reservations

    @Test
    void reservationsListPseudonymBlockRoleDraftsAndTheExpiredFlagByExpiry() {
        long ana = rater("test_analyst", "pt-BR");
        long bruno = rater("test_analyst", "pt-BR");
        Instant now = clock.instant();
        reserveTask(2, 1, ana, now.plus(Duration.ofMinutes(40)), 3);
        reserveTask(3, 2, bruno, now.minus(Duration.ofMinutes(5)), 1);

        JsonPath json = dashboard().jsonPath();

        assertThat(json.getList("reservations")).hasSize(2);
        assertThat(json.getString("reservations[0].pseudonym")).isEqualTo(pseudonym(bruno));
        assertThat(json.getInt("reservations[0].block")).isEqualTo(3);
        assertThat(json.getInt("reservations[0].role")).isEqualTo(2);
        assertThat(json.getInt("reservations[0].draftCount")).isEqualTo(1);
        assertThat(json.getBoolean("reservations[0].expired")).isTrue();
        assertThat(json.getString("reservations[0].system")).isEqualTo("minitest");
        assertThat(json.getString("reservations[1].pseudonym")).isEqualTo(pseudonym(ana));
        assertThat(json.getInt("reservations[1].block")).isEqualTo(2);
        assertThat(json.getInt("reservations[1].role")).isEqualTo(1);
        assertThat(json.getInt("reservations[1].draftCount")).isEqualTo(3);
        assertThat(json.getBoolean("reservations[1].expired")).isFalse();
        assertThat(Instant.parse(json.getString("reservations[1].expiresAt")))
                .isCloseTo(now.plus(Duration.ofMinutes(40)), within(2, ChronoUnit.SECONDS));
    }

    @Test
    void completedTasksAreNotListedAsReservations() {
        completeTask(1, 1, rater("test_analyst", "pt-BR"), "pt-BR");

        dashboard().then().body("reservations.size()", equalTo(0));
    }

    @Test
    void noNameOrEmailAppearsAnywhereInTheDocument() {
        long ana = rater("test_analyst", "pt-BR");
        completeTask(1, 1, ana, "pt-BR");
        reserveTask(2, 1, rater("test_analyst", "pt-BR"), clock.instant().plus(Duration.ofHours(1)), 2);

        Response response = dashboard();
        String body = response.asString();

        assertThat(body).doesNotContain(SECRET_NAME).doesNotContain(SECRET_EMAIL).doesNotContain("@");
        assertThat(allKeys(response.jsonPath().get("$"))).doesNotContain("name", "email", "e-mail");
        assertThat(body).contains(pseudonym(ana));
    }

    // US3: participants, languages, quota

    @Test
    void participantsFallInExactlyOneStatusBucketAndSkillsAreTallied() {
        rater("test_analyst", "pt-BR");
        rater("test_analyst", "en-US");
        rater("developer", "pt-BR");
        long ineligible = participant("test_analyst", "es-ES", ConsentDecision.ACCEPTED, ScreeningAnswer.NO);
        participant(null, "pt-BR", ConsentDecision.REFUSED, null);
        participant(null, "en-US", null, null);
        participant("developer", "pt-BR", ConsentDecision.ACCEPTED, null);
        long anonymized = rater("test_analyst", "pt-BR");
        jdbc.sql("UPDATE participant SET name = NULL, email = NULL, anonymized_at = ? WHERE id = ?")
                .params(Timestamp.from(clock.instant()), anonymized).update();

        JsonPath json = dashboard().jsonPath();

        assertThat(json.getInt("participants.eligible")).isEqualTo(3);
        assertThat(json.getInt("participants.ineligible")).isEqualTo(1);
        assertThat(json.getInt("participants.declinedConsent")).isEqualTo(1);
        assertThat(json.getInt("participants.incomplete")).isEqualTo(2);
        // The anonymized participant is in no bucket and no skill count.
        Map<String, Integer> bySkill = json.getMap("participants.bySkill");
        assertThat(bySkill).containsOnlyKeys("test_analyst", "developer")
                .containsEntry("test_analyst", 3).containsEntry("developer", 2);
        assertThat(ineligible).isPositive();
    }

    @Test
    void completedByPseudonymIsOrderedWithSharesOfTheCompletedTasks() {
        long ana = rater("test_analyst", "pt-BR");
        long bruno = rater("test_analyst", "pt-BR");
        rater("test_analyst", "pt-BR");
        completeTask(1, 1, ana, "pt-BR");
        completeTask(2, 1, ana, "pt-BR");
        completeTask(3, 1, ana, "pt-BR");
        completeTask(1, 2, bruno, "pt-BR");

        JsonPath json = dashboard().jsonPath();

        assertThat(json.getList("participants.completedByPseudonym")).hasSize(2);
        assertThat(json.getString("participants.completedByPseudonym[0].pseudonym")).isEqualTo(pseudonym(ana));
        assertThat(json.getInt("participants.completedByPseudonym[0].completedTasks")).isEqualTo(3);
        assertThat(json.getDouble("participants.completedByPseudonym[0].shareOfCompleted")).isEqualTo(0.75);
        assertThat(json.getString("participants.completedByPseudonym[1].pseudonym")).isEqualTo(pseudonym(bruno));
        assertThat(json.getDouble("participants.completedByPseudonym[1].shareOfCompleted")).isEqualTo(0.25);
    }

    @Test
    void languagesAlwaysListThreeLocalesAndFinalCountsMatchTheLongExport() throws IOException {
        long ana = rater("test_analyst", "pt-BR");
        long bruno = rater("test_analyst", "en-US");
        rater("test_analyst", "pt-BR");
        completeTask(1, 1, ana, "es-ES");
        completeTask(1, 2, bruno, "en-US");
        completeTask(2, 1, bruno, "pt-BR");
        reserveTask(3, 1, ana, clock.instant().plus(Duration.ofHours(1)), 4);

        JsonPath json = dashboard().jsonPath();

        assertThat(json.getList("languages.language")).containsExactly("pt-BR", "en-US", "es-ES");
        assertThat(json.getList("languages.participantsPreferred")).containsExactly(2, 1, 0);
        // SC-003: finals per language equal the response_language tally of the long export; drafts never count.
        Map<String, Long> exported = new HashMap<>();
        for (CSVRecord record : longExport()) {
            exported.merge(record.get("response_language"), 1L, Long::sum);
        }
        List<String> languages = json.getList("languages.language");
        List<Integer> finals = json.getList("languages.finalResponses");
        for (int i = 0; i < languages.size(); i++) {
            assertThat(finals.get(i).longValue()).as(languages.get(i)).isEqualTo(exported.getOrDefault(languages.get(i), 0L));
        }
        assertThat(finals).containsExactly(10, 10, 10);
        assertThat(exported.values().stream().mapToLong(Long::longValue).sum()).isEqualTo(30L);
    }

    @Test
    void languagesKeepAllThreeLocalesWithZerosWhenNothingIsStored() {
        clearStudyDatabase();

        dashboard().then()
                .body("languages.language", contains("pt-BR", "en-US", "es-ES"))
                .body("languages.participantsPreferred", contains(0, 0, 0))
                .body("languages.finalResponses", contains(0, 0, 0));
    }

    @Test
    void emailQuotaCountsTheRollingLast24HoursAgainstTheLimit() {
        Instant now = clock.instant();
        for (int i = 0; i < 6; i++) {
            insertAccessCode(now.minus(Duration.ofHours(1)));
        }
        for (int i = 0; i < 3; i++) {
            insertAccessCode(now.minus(Duration.ofHours(25)));
        }
        jdbc.sql("UPDATE config SET daily_email_limit = 15 WHERE id = 1").update();

        dashboard().then().statusCode(200)
                .body("emailQuota.emailsSentLast24h", equalTo(6))
                .body("emailQuota.dailyEmailLimit", equalTo(15))
                .body("emailQuota.emailsRemaining", equalTo(9))
                .body("emailQuota.lowRemaining", equalTo(true));

        jdbc.sql("UPDATE config SET daily_email_limit = 450 WHERE id = 1").update();

        dashboard().then()
                .body("emailQuota.emailsRemaining", equalTo(444))
                .body("emailQuota.lowRemaining", equalTo(false));

        jdbc.sql("UPDATE config SET daily_email_limit = 4 WHERE id = 1").update();

        dashboard().then()
                .body("emailQuota.emailsRemaining", equalTo(0))
                .body("emailQuota.lowRemaining", equalTo(true));
    }

    // read-only

    @Test
    void readingTheDashboardChangesNoRow() {
        long ana = rater("test_analyst", "pt-BR");
        completeTask(1, 1, ana, "pt-BR");
        reserveTask(2, 1, rater("test_analyst", "pt-BR"), clock.instant().minus(Duration.ofMinutes(5)), 1);
        List<String> tables = List.of("task", "response", "participant", "item", "block", "access_code", "access_session");
        Map<String, Long> before = rowCounts(tables);
        String expiredStatus = jdbc.sql("SELECT status FROM task WHERE block_id = ? AND role = 1")
                .param(blockId(2)).query(String.class).single();

        dashboard().then().statusCode(200);
        dashboard().then().statusCode(200);

        assertThat(rowCounts(tables)).isEqualTo(before);
        assertThat(jdbc.sql("SELECT status FROM task WHERE block_id = ? AND role = 1")
                .param(blockId(2)).query(String.class).single()).isEqualTo(expiredStatus).isEqualTo("reserved");
    }

    @Test
    void refreshReflectsNewDataOnTheSameLink() {
        assertThat(dashboard().jsonPath().getInt("progress[0].role1.completed")).isZero();

        completeTask(1, 1, rater("test_analyst", "pt-BR"), "pt-BR");

        Response again = dashboard();
        assertThat(again.jsonPath().getInt("progress[0].role1.completed")).isEqualTo(1);
        assertThat(again.jsonPath().getString("generatedAt")).isNotBlank();
        again.then().body(containsString("generatedAt"));
    }

    // helpers

    private Response dashboard() {
        Response response = as(admin).get(href(as(admin).get("/api/v1"), "dashboard"));
        response.then().statusCode(200);
        return response;
    }

    private long count(String sql, Object... params) {
        Object[] converted = new Object[params.length];
        for (int i = 0; i < params.length; i++) {
            converted[i] = params[i] instanceof Instant instant ? Timestamp.from(instant) : params[i];
        }
        return jdbc.sql(sql).params(converted).query(Long.class).single();
    }

    private Map<String, Long> rowCounts(List<String> tables) {
        Map<String, Long> counts = new HashMap<>();
        tables.forEach(table -> counts.put(table, count("SELECT COUNT(*) FROM `" + table + "`")));
        return counts;
    }

    private long batchId() {
        return jdbc.sql("SELECT id FROM import_batch ORDER BY id LIMIT 1").query(Long.class).single();
    }

    private long blockId(int number) {
        return jdbc.sql("SELECT id FROM block WHERE number = ?").param(number).query(Long.class).single();
    }

    private String pseudonym(long participantId) {
        return jdbc.sql("SELECT pseudonym FROM participant WHERE id = ?").param(participantId)
                .query(String.class).single();
    }

    /** An eligible participant (consent accepted, skill, screening yes) with the given preferred language. */
    private long rater(String skill, String language) {
        return participant(skill, language, ConsentDecision.ACCEPTED, ScreeningAnswer.YES_PROFESSIONAL);
    }

    /** A new participant with the onboarding steps given ({@code null} leaves a step undone). */
    private long participant(String skill, String language, ConsentDecision consent, ScreeningAnswer screening) {
        long id = participantIdOf(participantSession());
        participantCounter++;
        jdbc.sql("UPDATE participant SET name = ?, email = ?, preferred_language = ? WHERE id = ?")
                .params(SECRET_NAME + " " + participantCounter, "secret.rater." + participantCounter + "@example.com",
                        language, id)
                .update();
        if (consent != null) {
            recordConsent(id, consent, language);
        }
        if (skill != null) {
            seedSkill(id, skill, null);
        }
        if (screening != null) {
            seedScreening(id, screening);
        }
        return id;
    }

    /** A completed task with a final response in {@code language} for each item of the block. */
    private void completeTask(int blockNumber, int role, long participantId, String language) {
        long block = blockId(blockNumber);
        Instant reservedAt = clock.instant().minus(Duration.ofMinutes(30));
        jdbc.sql("""
                        UPDATE task SET status = 'completed', participant_id = ?, reserved_at = ?, expires_at = ?,
                            submitted_at = ?
                        WHERE block_id = ? AND role = ?""")
                .params(participantId, Timestamp.from(reservedAt), Timestamp.from(reservedAt.plus(Duration.ofHours(1))),
                        Timestamp.from(clock.instant()), block, role)
                .update();
        insertResponses(block, role, participantId, "final", language, Integer.MAX_VALUE);
    }

    /** A reserved task with {@code drafts} draft responses. */
    private void reserveTask(int blockNumber, int role, long participantId, Instant expiresAt, int drafts) {
        long block = blockId(blockNumber);
        jdbc.sql("""
                        UPDATE task SET status = 'reserved', participant_id = ?, reserved_at = ?, expires_at = ?
                        WHERE block_id = ? AND role = ?""")
                .params(participantId, Timestamp.from(expiresAt.minus(Duration.ofHours(1))), Timestamp.from(expiresAt),
                        block, role)
                .update();
        insertResponses(block, role, participantId, "draft", "pt-BR", drafts);
    }

    private void insertResponses(long block, int role, long participantId, String state, String language, int limit) {
        long task = jdbc.sql("SELECT id FROM task WHERE block_id = ? AND role = ?").params(block, role)
                .query(Long.class).single();
        List<Long> items = jdbc.sql("SELECT id FROM item WHERE block_id = ? ORDER BY position LIMIT ?")
                .params(block, limit).query(Long.class).list();
        for (long item : items) {
            jdbc.sql("""
                            INSERT INTO response (task_id, item_id, participant_id, state, change_impact,
                                maintenance_operation, comment, language, saved_at)
                            VALUES (?, ?, ?, ?, 'LOW', 'KEEP', 'private comment', ?, ?)""")
                    .params(task, item, participantId, state, language, Timestamp.from(clock.instant()))
                    .update();
        }
    }

    private void insertAccessCode(Instant createdAt) {
        jdbc.sql("""
                        INSERT INTO access_code (email, code_hash, created_at, expires_at)
                        VALUES (?, ?, ?, ?)""")
                .params("quota-" + System.nanoTime() + "@example.com", "hash-" + System.nanoTime(),
                        Timestamp.from(createdAt), Timestamp.from(createdAt.plus(Duration.ofMinutes(10))))
                .update();
    }

    private List<CSVRecord> longExport() throws IOException {
        Response response = as(admin).accept("text/csv").get("/api/v1/exports/long");
        response.then().statusCode(200);
        String text = new String(response.asByteArray(), StandardCharsets.UTF_8);
        try (CSVParser parser = CSVParser.parse(
                new StringReader(text), CSVFormat.RFC4180.builder().setHeader().setSkipHeaderRecord(true).get())) {
            return parser.getRecords();
        }
    }

    /** Every JSON object key found anywhere in the document. */
    @SuppressWarnings("unchecked")
    private static List<String> allKeys(Object node) {
        List<String> keys = new ArrayList<>();
        if (node instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                keys.add(String.valueOf(entry.getKey()));
                keys.addAll(allKeys(entry.getValue()));
            }
        } else if (node instanceof List<?> list) {
            list.forEach(element -> keys.addAll(allKeys(element)));
        }
        return keys;
    }
}
