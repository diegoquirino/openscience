package io.github.diegoquirino.veredito.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.diegoquirino.veredito.support.MySqlContainerConfig;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.mysql.MySQLContainer;

/** Spec 003 US3 and SC-003, and the V3 of spec 004: MySQL 9.7, repeatable, every constraint of the data models. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import(MySqlContainerConfig.class)
@Transactional
class MigrationTest {

    private static final List<String> TABLES = List.of(
            "access_code", "access_session", "block", "config", "consent_form", "consent_form_text",
            "import_batch", "item", "participant", "response", "task");

    @Autowired
    JdbcClient jdbc;

    @Autowired
    Flyway flyway;

    @Autowired
    DataSource dataSource;

    @Test
    void createsEveryTableAndRecordsEachMigrationOnce() {
        List<String> tables = jdbc.sql("""
                        SELECT table_name FROM information_schema.tables
                        WHERE table_schema = DATABASE() AND table_name <> 'flyway_schema_history'
                        ORDER BY table_name""")
                .query(String.class).list();
        assertThat(tables).containsExactlyElementsOf(TABLES);

        List<String> versions = jdbc.sql("""
                        SELECT version FROM flyway_schema_history
                        WHERE success = 1 AND version IS NOT NULL ORDER BY installed_rank""")
                .query(String.class).list();
        assertThat(versions).containsExactly("1", "2", "3");
    }

    @Test
    void v3AddsTheNameColumnsAndTheCleanupIndexes() {
        List<Map<String, Object>> columns = jdbc.sql("""
                        SELECT table_name AS t, column_name AS c, column_type AS type, is_nullable AS nullable
                        FROM information_schema.columns
                        WHERE table_schema = DATABASE()
                          AND ((table_name = 'config' AND column_name = 'admin_name')
                            OR (table_name = 'access_code' AND column_name = 'name'))
                        ORDER BY table_name""")
                .query().listOfRows();
        assertThat(columns).extracting(row -> row.get("t") + "." + row.get("c") + " " + row.get("type") + " "
                        + row.get("nullable"))
                .containsExactly("access_code.name varchar(120) YES", "config.admin_name varchar(120) YES");

        List<String> indexes = jdbc.sql("""
                        SELECT DISTINCT CONCAT(table_name, '.', index_name, '(', column_name, ')')
                        FROM information_schema.statistics
                        WHERE table_schema = DATABASE()
                          AND index_name IN ('ix_access_code_created_at', 'ix_access_session_expires_at')
                        ORDER BY 1""")
                .query(String.class).list();
        assertThat(indexes).containsExactly(
                "access_code.ix_access_code_created_at(created_at)",
                "access_session.ix_access_session_expires_at(expires_at)");

        Map<String, Object> config = jdbc.sql("SELECT admin_name FROM config").query().singleRow();
        assertThat(config.get("admin_name")).isNull();
    }

    @Test
    void insertsTheDefaultConfigurationAndNoConsentForm() {
        Map<String, Object> config = jdbc.sql("SELECT * FROM config").query().singleRow();
        assertThat(config)
                .containsEntry("id", 1)
                .containsEntry("admin_email", "diegoquirino@gmail.com")
                .containsEntry("reservation_minutes", 20)
                .containsEntry("block_size", 10)
                .containsEntry("daily_email_limit", 450)
                .containsEntry("study_phase", "pilot");
        assertThat(config.get("admin_preferred_language")).isNull();
        assertThat(config.get("study_phase_changed_at")).isNull();
        assertThat(count("SELECT COUNT(*) FROM config")).isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM consent_form")).isZero();
        assertThat(count("SELECT COUNT(*) FROM consent_form_text")).isZero();
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void migratingAgainChangesNothing() {
        String fileName = "repeat-" + System.nanoTime() + ".csv";
        long batchId = insertBatch(fileName);
        int history = count("SELECT COUNT(*) FROM flyway_schema_history");

        int executed = flyway.migrate().migrationsExecuted;

        assertThat(executed).isZero();
        assertThat(count("SELECT COUNT(*) FROM flyway_schema_history")).isEqualTo(history);
        assertThat(jdbc.sql("SELECT file_name FROM import_batch WHERE id = ?").param(batchId)
                .query(String.class).single()).isEqualTo(fileName);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void aFreshDatabaseGetsTheSameSchema() {
        try (MySQLContainer other = MySqlContainerConfig.newContainer()) {
            other.start();
            DataSource otherSource = new DriverManagerDataSource(
                    other.getJdbcUrl(), other.getUsername(), other.getPassword());
            Flyway.configure().dataSource(otherSource).locations("classpath:db/migration").load().migrate();

            assertThat(SchemaSnapshot.of(JdbcClient.create(otherSource)))
                    .isEqualTo(SchemaSnapshot.of(JdbcClient.create(dataSource)));
        }
    }

    @Test
    void storesLanguagesOfParticipantsResponsesAndConsentTexts() {
        List<String> columns = jdbc.sql("""
                        SELECT CONCAT(table_name, '.', column_name) FROM information_schema.columns
                        WHERE table_schema = DATABASE()
                          AND ((table_name = 'participant' AND column_name = 'preferred_language')
                            OR (table_name = 'response' AND column_name = 'language'))""")
                .query(String.class).list();
        assertThat(columns).containsExactlyInAnyOrder("participant.preferred_language", "response.language");

        long formId = insertConsentForm("v-test");
        for (String language : List.of("pt-BR", "en-US", "es-ES")) {
            insertConsentText(formId, language);
        }
        assertThat(count("SELECT COUNT(*) FROM consent_form_text WHERE consent_form_id = " + formId)).isEqualTo(3);
        assertRefused(() -> insertConsentText(formId, "pt-BR"), "uq_consent_form_text_language");
    }

    @Nested
    class RefusedValues {

        @Test
        void localeOutsideTheThreeSupported() {
            long formId = insertConsentForm("v-fr");
            assertRefused(() -> insertConsentText(formId, "fr-FR"), "ck_consent_form_text_language");
            assertRefused(() -> jdbc.sql("UPDATE config SET admin_preferred_language = 'fr-FR'").update(),
                    "ck_config_admin_preferred_language");
            assertRefused(() -> insertParticipant("fr@example.org", "fr-FR"), "ck_participant_preferred_language");

            Fixture fixture = new Fixture();
            assertRefused(() -> insertResponse(fixture, "LOW", "KEEP", "fr-FR"), "ck_response_language");
        }

        @Test
        void unknownStudyPhase() {
            assertRefused(() -> jdbc.sql("UPDATE config SET study_phase = 'other'").update(), "ck_config_study_phase");
        }

        @Test
        void secondConfigRow() {
            assertRefused(() -> jdbc.sql("""
                            INSERT INTO config (id, admin_email, reservation_minutes, block_size, daily_email_limit,
                                                study_phase, updated_at)
                            VALUES (2, 'x@example.org', 20, 10, 450, 'pilot', UTC_TIMESTAMP(6))""").update(),
                    "ck_config_single_row");
        }

        @Test
        void duplicateSystemAndEditKey() {
            long batchId = insertBatch("dup.csv");
            long blockId = insertBlock(batchId, 9001);
            insertItem(batchId, blockId, 1, "minitest", "UC01#field#1");
            assertRefused(() -> insertItem(batchId, blockId, 2, "minitest", "UC01#field#1"), "uq_item_system_edit_key");
        }

        @Test
        void sameParticipantInBothTasksOfABlock() {
            long batchId = insertBatch("both.csv");
            long blockId = insertBlock(batchId, 9002);
            long participantId = insertParticipant("both@example.org", "pt-BR");
            insertReservedTask(blockId, 1, 90021, participantId);
            assertRefused(() -> insertReservedTask(blockId, 2, 90022, participantId), "uq_task_block_participant");
        }

        @Test
        void freeTaskWithAParticipant() {
            long batchId = insertBatch("free.csv");
            long blockId = insertBlock(batchId, 9003);
            long participantId = insertParticipant("free@example.org", "pt-BR");
            assertRefused(() -> jdbc.sql("""
                            INSERT INTO task (block_id, role, queue_position, status, participant_id)
                            VALUES (?, 1, 90031, 'free', ?)""").params(blockId, participantId).update(),
                    "ck_task_state");
        }

        @Test
        void itemWithoutOriginAndTarget() {
            long batchId = insertBatch("empty.csv");
            long blockId = insertBlock(batchId, 9004);
            assertRefused(() -> jdbc.sql("""
                            INSERT INTO item (`system`, edit_key, uc, kind, field, origin, target, origin_version,
                                              target_version, origin_usecase, target_usecase, batch_id, block_id,
                                              position)
                            VALUES ('minitest', 'k-empty', 'UC01', 'MOD', 'field', NULL, NULL, 'v1', 'v2', 'a', 'b',
                                    ?, ?, 1)""").params(batchId, blockId).update(),
                    "ck_item_origin_or_target");
        }

        @Test
        void anonymizedParticipantKeepingTheEmail() {
            assertRefused(() -> jdbc.sql("""
                            INSERT INTO participant (pseudonym, name, email, registered_at, anonymized_at)
                            VALUES ('P-ANON', NULL, 'kept@example.org', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6))""")
                            .update(),
                    "ck_participant_anonymization");
        }

        @Test
        void unknownChangeImpactAndMaintenanceOperation() {
            Fixture fixture = new Fixture();
            assertRefused(() -> insertResponse(fixture, "MID", "KEEP", "pt-BR"), "ck_response_change_impact");
            assertRefused(() -> insertResponse(fixture, "LOW", "DELETE", "pt-BR"), "ck_response_maintenance_operation");
        }
    }

    /** One block with an item, a participant and a reserved task, for response inserts. */
    private final class Fixture {
        final long taskId;
        final long itemId;
        final long participantId;

        Fixture() {
            long batchId = insertBatch("fixture.csv");
            long blockId = insertBlock(batchId, 9100);
            itemId = insertItem(batchId, blockId, 1, "minitest", "fixture-key");
            participantId = insertParticipant("fixture@example.org", "pt-BR");
            taskId = insertReservedTask(blockId, 1, 91001, participantId);
        }
    }

    private void assertRefused(Runnable insert, String constraint) {
        assertThatThrownBy(insert::run).isInstanceOf(DataAccessException.class).hasMessageContaining(constraint);
    }

    private int count(String sql) {
        return jdbc.sql(sql).query(Integer.class).single();
    }

    private long insertBatch(String fileName) {
        return insert(jdbc.sql("""
                INSERT INTO import_batch (file_name, imported_at, included_count, ignored_count, error_count)
                VALUES (?, UTC_TIMESTAMP(6), 0, 0, 0)""").param(fileName));
    }

    private long insertBlock(long batchId, int number) {
        return insert(jdbc.sql("INSERT INTO block (batch_id, number, size) VALUES (?, ?, 10)").params(batchId, number));
    }

    private long insertItem(long batchId, long blockId, int position, String system, String editKey) {
        return insert(jdbc.sql("""
                INSERT INTO item (`system`, edit_key, uc, kind, field, origin, target, origin_version, target_version,
                                  origin_usecase, target_usecase, batch_id, block_id, position)
                VALUES (?, ?, 'UC01', 'MOD', 'field', 'old', 'new', 'v1', 'v2', 'a', 'b', ?, ?, ?)""")
                .params(system, editKey, batchId, blockId, position));
    }

    private long insertParticipant(String email, String language) {
        return insert(jdbc.sql("""
                INSERT INTO participant (pseudonym, name, email, registered_at, preferred_language)
                VALUES (?, 'Test person', ?, UTC_TIMESTAMP(6), ?)""")
                .params("P" + Math.abs(email.hashCode() % 1_000_000), email, language));
    }

    private long insertReservedTask(long blockId, int role, int queuePosition, long participantId) {
        Timestamp now = Timestamp.from(Instant.now());
        Timestamp expires = Timestamp.from(Instant.now().plusSeconds(1200));
        return insert(jdbc.sql("""
                INSERT INTO task (block_id, role, queue_position, status, participant_id, reserved_at, expires_at)
                VALUES (?, ?, ?, 'reserved', ?, ?, ?)""")
                .params(blockId, role, queuePosition, participantId, now, expires));
    }

    private long insertResponse(Fixture fixture, String changeImpact, String operation, String language) {
        return insert(jdbc.sql("""
                INSERT INTO response (task_id, item_id, participant_id, state, change_impact, maintenance_operation,
                                      language, saved_at)
                VALUES (?, ?, ?, 'draft', ?, ?, ?, UTC_TIMESTAMP(6))""")
                .params(fixture.taskId, fixture.itemId, fixture.participantId, changeImpact, operation, language));
    }

    private long insertConsentForm(String version) {
        return insert(jdbc.sql("""
                INSERT INTO consent_form (version, reference_language, created_at)
                VALUES (?, 'pt-BR', UTC_TIMESTAMP(6))""").param(version));
    }

    private long insertConsentText(long formId, String language) {
        return insert(jdbc.sql("INSERT INTO consent_form_text (consent_form_id, language, text) VALUES (?, ?, 'text')")
                .params(formId, language));
    }

    private static long insert(JdbcClient.StatementSpec statement) {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        statement.update(keys);
        return keys.getKey().longValue();
    }
}
