package io.github.diegoquirino.veredito.api.v1;

import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;

import io.github.diegoquirino.veredito.support.ApiTestBase;
import io.restassured.response.Response;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntFunction;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

/**
 * Spec 009: the administrator follows {@code vd:exports} and downloads the wide, long, completo and rater
 * files. Fixtures are the Minitest import with roles completed straight in the tables.
 */
class ExportTest extends ApiTestBase {

    private static final String PROBLEM = "urn:veredito:problem:";
    private static final String PROBLEM_SCHEMA = "schemas/problem-details.json";
    private static final DateTimeFormatter OFFSET =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssxxx").withZone(ZoneOffset.UTC);

    private static final String WIDE_HEADER = "system,edit_key,uc,kind,field,origin,target,"
            + "rater_1_name,rater_1_email,rater_1_accepted_research_terms_lgpd,rater_1_professional_skill,"
            + "rater_1_organization,rater_1_organization_other,"
            + "rater_1_change_impact_choice,rater_1_maintenance_operation_choice,rater_1_comment,"
            + "rater_1_response_language,"
            + "rater_2_name,rater_2_email,rater_2_accepted_research_terms_lgpd,rater_2_professional_skill,"
            + "rater_2_organization,rater_2_organization_other,"
            + "rater_2_change_impact_choice,rater_2_maintenance_operation_choice,rater_2_comment,"
            + "rater_2_response_language";
    private static final String LONG_HEADER = "system,edit_key,block,role,pseudonym,professional_skill,"
            + "organization,organization_other,"
            + "participant_preferred_language,consent_language,change_impact,maintenance_operation,comment,"
            + "response_language,reserved_at,submitted_at";
    private static final String COMPLETO_HEADER = "system,edit_key,uc,kind,field,origin,target,origin_version,"
            + "target_version,origin_usecase,target_usecase,origin_tests,target_tests,rater_1_pseudonym,rater_1_change_impact,"
            + "rater_1_maintenance_operation,rater_1_comment,rater_1_response_language,rater_2_pseudonym,"
            + "rater_2_change_impact,rater_2_maintenance_operation,rater_2_comment,rater_2_response_language";
    private static final String RATER_HEADER =
            "edit_key,uc,kind,field,origin,target,change_impact,maintenance_operation,comment";

    private String admin;

    @BeforeEach
    void minitestImported() {
        clearStudyDatabase();
        importMinitestBatch();
        admin = administratorSession();
    }

    // index and access

    @Test
    void indexListsTheSystemsAndEveryDownloadLink() {
        Response entry = as(admin).get("/api/v1");
        entry.then().statusCode(200).body("_links.'vd:exports'.href", equalTo("/api/v1/exports"));

        Response index = follow(as(admin), entry, "exports");
        index.then().statusCode(200)
                .body("systems", contains("minitest"))
                .body("_links.self.href", equalTo("/api/v1/exports"))
                .body("_links.curies[0].name", equalTo("vd"))
                .body("_links.'vd:export-wide'.href", equalTo("/api/v1/exports/wide"))
                .body("_links.'vd:export-long'.href", equalTo("/api/v1/exports/long"))
                .body("_links.'vd:export-completo'.href", equalTo("/api/v1/exports/completo?system=minitest"))
                .body("_links.'vd:export-rater1'.href", equalTo("/api/v1/exports/rater1?system=minitest"))
                .body("_links.'vd:export-rater2'.href", equalTo("/api/v1/exports/rater2?system=minitest"));
    }

    @Test
    void indexRepeatsThePerSystemRelationOncePerSystemInQueueOrder() {
        insertOtherSystemItem("alpha-system");

        Response index = index();
        index.then().statusCode(200).body("systems", contains("minitest", "alpha-system"));
        List<Map<String, String>> completo = index.path("_links.'vd:export-completo'");
        assertThat(completo).extracting(link -> link.get("href")).containsExactly(
                "/api/v1/exports/completo?system=minitest", "/api/v1/exports/completo?system=alpha-system");
        assertThat(this.<List<Map<String, String>>>pathOf(index, "export-rater1")).hasSize(2);
        assertThat(this.<List<Map<String, String>>>pathOf(index, "export-rater2")).hasSize(2);
    }

    @Test
    void emptyDatabaseListsNoSystemAndOnlyWideAndLong() {
        clearStudyDatabase();

        index().then().statusCode(200)
                .body("systems", empty())
                .body("_links.keySet()", containsInAnyOrder("self", "curies", "vd:export-wide", "vd:export-long"));
        CsvFile wide = get("/api/v1/exports/wide");
        assertThat(wide.records()).isEmpty();
        assertThat(wide.header()).isEqualTo(WIDE_HEADER);
    }

    @Test
    void visitorsAndParticipantsAreRefusedEverywhere() {
        String participant = participantSession();
        for (String path : List.of(
                "/api/v1/exports", "/api/v1/exports/wide", "/api/v1/exports/long",
                "/api/v1/exports/completo?system=minitest", "/api/v1/exports/rater1?system=minitest",
                "/api/v1/exports/rater2?system=minitest")) {
            request().get(path).then().statusCode(401).body("type", equalTo(PROBLEM + "authentication-required"));
            as(participant).get(path).then().statusCode(403).body("type", equalTo(PROBLEM + "forbidden"));
        }
    }

    @Test
    void entryPointOffersExportsOnlyToTheAdministrator() {
        entryPoint().then().body("_links", not(hasKey("vd:exports")));
        as(participantSession()).get("/api/v1").then().body("_links", not(hasKey("vd:exports")));
    }

    // US1: pipeline files

    @Test
    void raterFilesHaveTheNinePipelineColumnsOneRowPerItemInQueueOrder() {
        long ana = newRater("Ana Souza", "ana-export@example.com");
        long bruno = newRater("Bruno Lima", "bruno-export@example.com");
        completeBlock(1, 1, ana, "pt-BR", p -> responseOf(p, "HIGH", "UPDATE", "r1 " + p));
        completeBlock(1, 2, bruno, "en-US", p -> responseOf(p, "LOW", "KEEP", "r2 " + p));

        Response index = index();
        CsvFile rater1 = download(index, "export-rater1", "minitest");
        CsvFile rater2 = download(index, "export-rater2", "minitest");

        assertThat(rater1.header()).isEqualTo(RATER_HEADER);
        assertThat(rater2.header()).isEqualTo(RATER_HEADER);
        assertThat(rater1.filename()).isEqualTo("t1_minitest_rater1.csv");
        assertThat(rater2.filename()).isEqualTo("t1_minitest_rater2.csv");
        assertThat(rater1.records()).hasSize(itemCount());
        assertThat(rater1.column("edit_key")).isEqualTo(queueEditKeys());
        assertThat(rater2.column("edit_key")).isEqualTo(queueEditKeys());

        for (int i = 0; i < 10; i++) {
            assertThat(rater1.records().get(i).get("change_impact")).isEqualTo("HIGH");
            assertThat(rater1.records().get(i).get("maintenance_operation")).isEqualTo("UPDATE");
            assertThat(rater1.records().get(i).get("comment")).isEqualTo("r1 " + (i + 1));
            assertThat(rater2.records().get(i).get("change_impact")).isEqualTo("LOW");
            assertThat(rater2.records().get(i).get("comment")).isEqualTo("r2 " + (i + 1));
        }
        // Items of blocks not completed keep blank label cells.
        for (int i = 10; i < rater1.records().size(); i++) {
            assertThat(rater1.records().get(i).get("change_impact")).isEmpty();
            assertThat(rater1.records().get(i).get("maintenance_operation")).isEmpty();
            assertThat(rater1.records().get(i).get("comment")).isEmpty();
        }
        // The pipeline files carry exactly the stored item cells and no language column.
        assertThat(rater1.text()).doesNotContain("pt-BR").doesNotContain("en-US").doesNotContain("R-0");
    }

    @Test
    void incompleteRoleAndDraftsAreBlankInTheRaterFile() {
        long ana = newRater("Ana Souza", "ana2-export@example.com");
        completeBlock(1, 1, ana, "pt-BR", p -> responseOf(p, "HIGH", "UPDATE", "done"));
        // Role 2 of block 1 is only reserved, with one draft.
        long bruno = newRater("Bruno Lima", "bruno2-export@example.com");
        reserveWithDraft(1, 2, bruno);

        CsvFile rater1 = get("/api/v1/exports/rater1?system=minitest");
        CsvFile rater2 = get("/api/v1/exports/rater2?system=minitest");

        assertThat(rater1.records().get(0).get("change_impact")).isEqualTo("HIGH");
        assertThat(rater2.records()).hasSize(itemCount());
        assertThat(rater2.records()).allSatisfy(record -> {
            assertThat(record.get("change_impact")).isEmpty();
            assertThat(record.get("maintenance_operation")).isEmpty();
            assertThat(record.get("comment")).isEmpty();
        });
        assertThat(rater2.text()).doesNotContain("secret draft");
    }

    @Test
    void raterFilesKeepTextsExactlyAndOtherSystemsOut() {
        long ana = newRater("Ana Souza", "ana3-export@example.com");
        completeBlock(1, 1, ana, "es-ES", p -> responseOf(p, "LOW", "FLAG", p == 1 ? "=SUM(A1), \"ok\"\nsegunda l�nea" : null));
        insertOtherSystemItem("other");

        CsvFile rater1 = get("/api/v1/exports/rater1?system=minitest");

        assertThat(rater1.records().get(0).get("comment")).isEqualTo("=SUM(A1), \"ok\"\nsegunda l�nea");
        assertThat(rater1.records().get(1).get("comment")).isEmpty();
        assertThat(rater1.column("edit_key")).doesNotContain("other-edit-key");
        assertThat(get("/api/v1/exports/rater1?system=other").records()).hasSize(1);
    }

    @Test
    void missingSystemIsInvalidRequestAndUnknownSystemIsNotFound() {
        for (String rel : List.of("completo", "rater1", "rater2")) {
            as(admin).get("/api/v1/exports/" + rel)
                    .then().statusCode(400)
                    .contentType(startsWith("application/problem+json"))
                    .body(matchesJsonSchemaInClasspath(PROBLEM_SCHEMA))
                    .body("type", equalTo(PROBLEM + "invalid-request"))
                    .body("errors.field", contains("system"));
            as(admin).get("/api/v1/exports/" + rel + "?system=")
                    .then().statusCode(400)
                    .body("type", equalTo(PROBLEM + "invalid-request"));
            as(admin).get("/api/v1/exports/" + rel + "?system=nope")
                    .then().statusCode(404)
                    .contentType(startsWith("application/problem+json"))
                    .body(matchesJsonSchemaInClasspath(PROBLEM_SCHEMA))
                    .body("type", equalTo(PROBLEM + "system-not-found"))
                    .body("system", equalTo("nope"));
        }
    }

    @Test
    void problemsStayProblemJsonWhenTheClientAsksForCsv() {
        as(admin).accept("text/csv, application/problem+json").get("/api/v1/exports/rater1?system=nope")
                .then().statusCode(404)
                .contentType(startsWith("application/problem+json"))
                .body("type", equalTo(PROBLEM + "system-not-found"));
        as(admin).accept("text/csv").get("/api/v1/exports/completo")
                .then().statusCode(400)
                .contentType(startsWith("application/problem+json"));
    }

    @Test
    void aSystemMatchesExactlyAndSpecialCharactersAreSafeInTheFilename() {
        insertOtherSystemItem("My Sys/tem&1");

        Response index = index();
        String href = hrefFor(index, "export-completo", "My Sys/tem&1");
        assertThat(href).isEqualTo("/api/v1/exports/completo?system=My+Sys%2Ftem%261");
        CsvFile file = get(href);
        assertThat(file.filename()).isEqualTo("veredito_My_Sys_tem_1_completo.csv");
        assertThat(file.records()).hasSize(1);
        as(admin).get("/api/v1/exports/completo?system=MINITEST").then().statusCode(404);
    }

    // US2: wide table

    @Test
    void wideHasAppendixCHeaderBothRatersAndOneRowPerItemOfAllSystems() {
        long ana = newRater("Ana Souza", "ana4-export@example.com");
        long bruno = newRater("Bruno Lima", "bruno4-export@example.com");
        completeBlock(1, 1, ana, "es-ES", p -> responseOf(p, "HIGH", "UPDATE", "Comentario " + p));
        completeBlock(1, 2, bruno, "en-US", p -> responseOf(p, "LOW", "KEEP", null));
        insertOtherSystemItem("other");

        CsvFile wide = get(href(index(), "export-wide"));

        assertThat(wide.header()).isEqualTo(WIDE_HEADER);
        assertThat(wide.filename()).isEqualTo("veredito_wide.csv");
        assertThat(wide.records()).hasSize(itemCount() + 1);
        CSVRecord first = wide.records().get(0);
        assertThat(first.get("system")).isEqualTo("minitest");
        assertThat(first.get("rater_1_name")).isEqualTo("Ana Souza");
        assertThat(first.get("rater_1_email")).isEqualTo("ana4-export@example.com");
        assertThat(first.get("rater_1_professional_skill")).isEqualTo("test_analyst");
        assertThat(first.get("rater_1_change_impact_choice")).isEqualTo("HIGH");
        assertThat(first.get("rater_1_maintenance_operation_choice")).isEqualTo("UPDATE");
        assertThat(first.get("rater_1_comment")).isEqualTo("Comentario 1");
        assertThat(first.get("rater_1_response_language")).isEqualTo("es-ES");
        assertThat(first.get("rater_2_name")).isEqualTo("Bruno Lima");
        assertThat(first.get("rater_2_change_impact_choice")).isEqualTo("LOW");
        assertThat(first.get("rater_2_comment")).isEmpty();
        assertThat(first.get("rater_2_response_language")).isEqualTo("en-US");
        // Items of blocks not completed: both raters blank.
        CSVRecord pending = wide.records().get(10);
        assertThat(wide.header().split(",")).hasSize(27);
        for (String column : List.of(
                "rater_1_name", "rater_1_email", "rater_1_accepted_research_terms_lgpd", "rater_1_professional_skill",
                "rater_1_organization", "rater_1_organization_other",
                "rater_1_change_impact_choice", "rater_1_response_language", "rater_2_name", "rater_2_response_language")) {
            assertThat(pending.get(column)).as(column).isEmpty();
        }
        assertThat(wide.records().get(itemCount()).get("system")).isEqualTo("other");
    }

    @Test
    void wideConsentCellHasVersionLanguageAndTimestamp() {
        long ana = newRater("Ana Souza", "ana5-export@example.com");
        completeBlock(1, 1, ana, "pt-BR", p -> responseOf(p, "LOW", "KEEP", null));
        Instant decidedAt = jdbc.sql("SELECT consent_decided_at FROM participant WHERE id = ?")
                .param(ana).query(Timestamp.class).single().toInstant();

        CSVRecord first = get("/api/v1/exports/wide").records().get(0);

        assertThat(first.get("rater_1_accepted_research_terms_lgpd"))
                .isEqualTo("fixture-1 pt-BR " + OFFSET.format(decidedAt.truncatedTo(ChronoUnit.SECONDS)));
    }

    @Test
    void wideNeutralizesFormulasAndAnonymizedParticipantsExportTheMarker() {
        long ana = newRater("Ana Souza", "ana6-export@example.com");
        completeBlock(1, 1, ana, "pt-BR", p -> responseOf(p, "LOW", "KEEP", p == 1 ? "=HYPERLINK(\"x\")" : p == 2 ? "-1+1" : p == 3 ? "@cmd" : "+55"));
        jdbc.sql("UPDATE item SET origin = '=2+2' WHERE edit_key = ? AND `system` = 'minitest'")
                .param(queueEditKeys().get(0)).update();
        jdbc.sql("UPDATE participant SET name = NULL, email = NULL, anonymized_at = ? WHERE id = ?")
                .params(Timestamp.from(clock.instant()), ana).update();

        CsvFile wide = get("/api/v1/exports/wide");

        assertThat(wide.records().get(0).get("origin")).isEqualTo("'=2+2");
        assertThat(wide.records().get(0).get("rater_1_comment")).isEqualTo("'=HYPERLINK(\"x\")");
        assertThat(wide.records().get(1).get("rater_1_comment")).isEqualTo("'-1+1");
        assertThat(wide.records().get(2).get("rater_1_comment")).isEqualTo("'@cmd");
        assertThat(wide.records().get(3).get("rater_1_comment")).isEqualTo("'+55");
        assertThat(wide.records().get(0).get("rater_1_name")).isEqualTo("anonymized");
        assertThat(wide.records().get(0).get("rater_1_email")).isEqualTo("anonymized");
        // The pipeline file never neutralizes.
        assertThat(get("/api/v1/exports/rater1?system=minitest").records().get(0).get("origin")).isEqualTo("=2+2");
    }

    @Test
    void theAdministratorLanguageChangesNeitherHeadersNorCodes() {
        long ana = newRater("Ana Souza", "ana7-export@example.com");
        completeBlock(1, 1, ana, "es-ES", p -> responseOf(p, "HIGH", "SPLIT", "Texto sin traducir"));

        byte[] pt = as(admin).header("Accept-Language", "pt-BR").get("/api/v1/exports/wide").asByteArray();
        byte[] en = as(admin).header("Accept-Language", "en-US").get("/api/v1/exports/wide").asByteArray();
        byte[] es = as(admin).header("Accept-Language", "es-ES").get("/api/v1/exports/wide").asByteArray();

        assertThat(en).isEqualTo(pt).isEqualTo(es);
        assertThat(new String(en, StandardCharsets.UTF_8))
                .contains("HIGH,SPLIT,Texto sin traducir,es-ES")
                .startsWith(WIDE_HEADER + "\n");
    }

    // US3: completo

    @Test
    void completoMatchesAppendixDOfTheImportAndHasTheFiveRoleColumns() throws IOException {
        long ana = newRater("Ana Souza", "ana8-export@example.com");
        long bruno = newRater("Bruno Lima", "bruno8-export@example.com");
        completeAll(1, ana, "pt-BR", "Primeiro");
        completeAll(2, bruno, "en-US", "Second");

        CsvFile completo = download(index(), "export-completo", "minitest");

        assertThat(completo.header()).isEqualTo(COMPLETO_HEADER);
        assertThat(completo.filename()).isEqualTo("veredito_minitest_completo.csv");
        List<Map<String, String>> imported = importedFixture();
        assertThat(completo.records()).hasSize(imported.size());
        // SC-001b: every Appendix D cell equals the imported one, character by character.
        for (Map<String, String> source : imported) {
            CSVRecord row = completo.records().stream()
                    .filter(record -> record.get("edit_key").equals(source.get("edit_key")))
                    .findFirst().orElseThrow();
            for (String column : List.of(
                    "system", "uc", "kind", "field", "origin", "target", "origin_version", "target_version",
                    "origin_usecase", "target_usecase", "origin_tests", "target_tests")) {
                assertThat(row.get(column)).as("%s of %s", column, source.get("edit_key")).isEqualTo(source.get(column));
            }
            assertThat(row.get("rater_1_pseudonym")).matches("R-\\d{4}");
            assertThat(row.get("rater_1_change_impact")).isIn("LOW", "HIGH");
            assertThat(row.get("rater_1_maintenance_operation")).isEqualTo("KEEP");
            assertThat(row.get("rater_1_comment")).startsWith("Primeiro");
            assertThat(row.get("rater_1_response_language")).isEqualTo("pt-BR");
            assertThat(row.get("rater_2_pseudonym")).matches("R-\\d{4}").isNotEqualTo(row.get("rater_1_pseudonym"));
            assertThat(row.get("rater_2_comment")).startsWith("Second");
            assertThat(row.get("rater_2_response_language")).isEqualTo("en-US");
        }
        assertThat(completo.text()).doesNotContain("Ana Souza").doesNotContain("@example.com");
        assertThat(completo.column("edit_key")).isEqualTo(queueEditKeys());
    }

    @Test
    void completoLeavesIncompleteRolesBlankAndFiltersBySystem() {
        long ana = newRater("Ana Souza", "ana9-export@example.com");
        completeBlock(1, 1, ana, "pt-BR", p -> responseOf(p, "LOW", "KEEP", "x"));
        insertOtherSystemItem("other");

        CsvFile minitest = get("/api/v1/exports/completo?system=minitest");
        CsvFile other = get("/api/v1/exports/completo?system=other");

        assertThat(minitest.records()).hasSize(itemCount());
        assertThat(minitest.column("system")).containsOnly("minitest");
        CSVRecord pending = minitest.records().get(itemCount() - 1);
        for (String column : List.of(
                "rater_1_pseudonym", "rater_1_change_impact", "rater_1_maintenance_operation", "rater_1_comment",
                "rater_1_response_language", "rater_2_pseudonym", "rater_2_change_impact",
                "rater_2_maintenance_operation", "rater_2_comment", "rater_2_response_language")) {
            assertThat(pending.get(column)).as(column).isEmpty();
        }
        assertThat(minitest.records().get(0).get("rater_2_pseudonym")).isEmpty();
        assertThat(other.records()).hasSize(1);
        assertThat(other.records().get(0).get("system")).isEqualTo("other");
    }

    // US4: long table

    @Test
    void longHasOneRowPerFinalResponseWithoutNameOrEmail() {
        long ana = newRater("Ana Souza", "ana10-export@example.com");
        long bruno = newRater("Bruno Lima", "bruno10-export@example.com");
        jdbc.sql("UPDATE participant SET preferred_language = 'pt-BR' WHERE id = ?").param(ana).update();
        jdbc.sql("UPDATE participant SET preferred_language = NULL WHERE id = ?").param(bruno).update();
        completeBlock(1, 1, ana, "en-US", p -> responseOf(p, "HIGH", "UPDATE", "en comment " + p));
        completeBlock(1, 2, bruno, "es-ES", p -> responseOf(p, "LOW", "KEEP", "=oops"));
        completeBlock(2, 1, ana, "pt-BR", p -> responseOf(p, "LOW", "FLAG", null));
        reserveWithDraft(3, 1, bruno);

        CsvFile file = download(index(), "export-long", null);

        assertThat(file.header()).isEqualTo(LONG_HEADER);
        assertThat(file.filename()).isEqualTo("veredito_long.csv");
        long completedRoles = jdbc.sql("SELECT COUNT(*) FROM task WHERE status = 'completed'").query(Long.class).single();
        long expected = jdbc.sql("""
                SELECT COUNT(*) FROM response r JOIN task t ON t.id = r.task_id
                WHERE r.state = 'final' AND t.status = 'completed'""").query(Long.class).single();
        assertThat(completedRoles).isEqualTo(3);
        assertThat(file.records()).hasSize((int) expected);
        // SC-003: completed roles times the size of each block.
        assertThat(file.records()).hasSize(10 + 10 + 10);
        assertThat(file.text()).doesNotContain("Ana Souza").doesNotContain("Bruno Lima").doesNotContain("@example.com")
                .doesNotContain("secret draft");

        CSVRecord first = file.records().get(0);
        assertThat(first.get("system")).isEqualTo("minitest");
        assertThat(first.get("edit_key")).isEqualTo(queueEditKeys().get(0));
        assertThat(first.get("block")).isEqualTo("1");
        assertThat(first.get("role")).isEqualTo("1");
        assertThat(first.get("pseudonym")).matches("R-\\d{4}");
        assertThat(first.get("professional_skill")).isEqualTo("test_analyst");
        assertThat(first.get("participant_preferred_language")).isEqualTo("pt-BR");
        assertThat(first.get("consent_language")).isEqualTo("pt-BR");
        assertThat(first.get("change_impact")).isEqualTo("HIGH");
        assertThat(first.get("maintenance_operation")).isEqualTo("UPDATE");
        assertThat(first.get("comment")).isEqualTo("en comment 1");
        assertThat(first.get("response_language")).isEqualTo("en-US");
        assertThat(first.get("reserved_at")).matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}[+-]\\d{2}:\\d{2}");
        assertThat(first.get("submitted_at")).matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}[+-]\\d{2}:\\d{2}");
        // Queue order: the second row is the role 2 response of the same item, then the next item.
        CSVRecord second = file.records().get(1);
        assertThat(second.get("edit_key")).isEqualTo(first.get("edit_key"));
        assertThat(second.get("role")).isEqualTo("2");
        assertThat(second.get("response_language")).isEqualTo("es-ES");
        assertThat(second.get("comment")).isEqualTo("'=oops");
        assertThat(second.get("participant_preferred_language")).isEmpty();
        assertThat(file.records()).allSatisfy(record -> assertThat(record.get("response_language"))
                .isIn("pt-BR", "en-US", "es-ES"));
    }

    @Test
    void longIsHeaderOnlyWhileNothingIsFinal() {
        CsvFile file = get("/api/v1/exports/long");
        assertThat(file.records()).isEmpty();
        assertThat(file.text()).isEqualTo(LONG_HEADER + "\n");
    }

    // format

    @Test
    void downloadsAreUtf8WithoutBomRfc4180WithLineFeedsAndAttachmentHeaders() {
        long ana = newRater("Ana Souza", "ana11-export@example.com");
        completeBlock(1, 1, ana, "pt-BR", p -> responseOf(p, "LOW", "KEEP", "a��o, \"quoted\"\nline"));

        for (String path : List.of(
                "/api/v1/exports/wide", "/api/v1/exports/long", "/api/v1/exports/completo?system=minitest",
                "/api/v1/exports/rater1?system=minitest", "/api/v1/exports/rater2?system=minitest")) {
            Response response = as(admin).accept("text/csv").get(path);
            response.then().statusCode(200)
                    .header("Content-Type", equalTo("text/csv;charset=UTF-8"))
                    .header("Content-Disposition", containsString("attachment; filename=\""));
            byte[] bytes = response.asByteArray();
            assertThat(bytes).as(path).isNotEmpty();
            assertThat(bytes[0]).as("no BOM in %s", path).isNotEqualTo((byte) 0xEF);
            String text = new String(bytes, StandardCharsets.UTF_8);
            assertThat(text).doesNotContain("\r\n").endsWith("\n");
        }
        String rater1 = new String(as(admin).get("/api/v1/exports/rater1?system=minitest").asByteArray(), StandardCharsets.UTF_8);
        assertThat(rater1).contains("\"a��o, \"\"quoted\"\"\nline\"");
        as(admin).get("/api/v1/exports/wide")
                .then().header("Content-Disposition", equalTo("attachment; filename=\"veredito_wide.csv\""));
    }

    @Test
    void downloadsAreFinalOnlyAcrossAllFormats() {
        long ana = newRater("Ana Souza", "ana12-export@example.com");
        reserveWithDraft(1, 1, ana);

        for (String path : List.of(
                "/api/v1/exports/wide", "/api/v1/exports/long", "/api/v1/exports/completo?system=minitest",
                "/api/v1/exports/rater1?system=minitest")) {
            assertThat(get(path).text()).as(path).doesNotContain("secret draft");
        }
    }

    // helpers

    private record ResponseSpec(String impact, String operation, String comment) {}

    private static ResponseSpec responseOf(int position, String impact, String operation, String comment) {
        return new ResponseSpec(impact, operation, comment);
    }

    private record CsvFile(String text, String filename, List<CSVRecord> records) {

        String header() {
            return text.substring(0, text.indexOf('\n'));
        }

        List<String> column(String name) {
            return records.stream().map(record -> record.get(name)).toList();
        }
    }

    private Response index() {
        Response index = follow(as(admin), as(admin).get("/api/v1"), "exports");
        index.then().statusCode(200);
        return index;
    }

    private Response follow(io.restassured.specification.RequestSpecification caller, Response from, String rel) {
        return caller.get(href(from, rel));
    }

    @SuppressWarnings("unchecked")
    private <T> T pathOf(Response index, String rel) {
        return (T) index.path("_links.'vd:" + rel + "'");
    }

    private String hrefFor(Response index, String rel, String system) {
        Object link = index.path("_links.'vd:" + rel + "'");
        List<Map<String, String>> links = link instanceof List<?> list
                ? (List<Map<String, String>>) list
                : List.of((Map<String, String>) link);
        String suffix = "system=" + java.net.URLEncoder.encode(system, StandardCharsets.UTF_8);
        return links.stream().map(entry -> entry.get("href")).filter(href -> href.endsWith(suffix)).findFirst().orElseThrow();
    }

    private CsvFile download(Response index, String rel, String system) {
        return get(system == null ? href(index, rel) : hrefFor(index, rel, system));
    }

    private CsvFile get(String path) {
        // The hrefs of the index are already percent-encoded: send them as they are.
        Response response = as(admin).urlEncodingEnabled(false).accept("text/csv").get(path);
        response.then().statusCode(200).header("Content-Type", equalTo("text/csv;charset=UTF-8"));
        String text = new String(response.asByteArray(), StandardCharsets.UTF_8);
        String disposition = response.header("Content-Disposition");
        String filename = disposition.substring(disposition.indexOf("filename=\"") + 10, disposition.length() - 1);
        return new CsvFile(text, filename, parse(text));
    }

    private static List<CSVRecord> parse(String text) {
        try (CSVParser parser = CSVParser.parse(
                new StringReader(text), CSVFormat.RFC4180.builder().setHeader().setSkipHeaderRecord(true).get())) {
            return parser.getRecords();
        } catch (IOException ex) {
            throw new java.io.UncheckedIOException(ex);
        }
    }

    private List<Map<String, String>> importedFixture() throws IOException {
        try (InputStream in = new ClassPathResource("minitest/veredito_minitest_vazio.csv").getInputStream()) {
            String text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            List<Map<String, String>> rows = new ArrayList<>();
            parse(text).forEach(record -> rows.add(new LinkedHashMap<>(record.toMap())));
            return rows;
        }
    }

    private int itemCount() {
        return jdbc.sql("SELECT COUNT(*) FROM item WHERE `system` = 'minitest'").query(Integer.class).single();
    }

    private List<String> queueEditKeys() {
        return jdbc.sql("""
                SELECT i.edit_key FROM item i JOIN block b ON b.id = i.block_id
                WHERE i.`system` = 'minitest' ORDER BY b.number, i.position""").query(String.class).list();
    }

    private long newRater(String name, String email) {
        String token = eligibleParticipantSession();
        long id = participantIdOf(token);
        jdbc.sql("UPDATE participant SET name = ?, email = ? WHERE id = ?").params(name, email, id).update();
        return id;
    }

    private long blockId(int number) {
        return jdbc.sql("SELECT id FROM block WHERE number = ?").param(number).query(Long.class).single();
    }

    /** A completed task of the block with a final response for each of its items. */
    private void completeBlock(int blockNumber, int role, long participantId, String language, IntFunction<ResponseSpec> answer) {
        long block = blockId(blockNumber);
        Instant reservedAt = clock.instant().minus(30, ChronoUnit.MINUTES);
        Instant submittedAt = clock.instant();
        jdbc.sql("""
                        UPDATE task SET status = 'completed', participant_id = ?, reserved_at = ?, expires_at = ?,
                            submitted_at = ?
                        WHERE block_id = ? AND role = ?""")
                .params(participantId, Timestamp.from(reservedAt), Timestamp.from(reservedAt.plus(1, ChronoUnit.HOURS)),
                        Timestamp.from(submittedAt), block, role)
                .update();
        long task = jdbc.sql("SELECT id FROM task WHERE block_id = ? AND role = ?").params(block, role)
                .query(Long.class).single();
        for (Map<String, Object> item : jdbc.sql("SELECT id, position FROM item WHERE block_id = ? ORDER BY position")
                .param(block).query().listOfRows()) {
            ResponseSpec spec = answer.apply(((Number) item.get("position")).intValue());
            insertResponse(task, ((Number) item.get("id")).longValue(), participantId, "final", spec, language);
        }
    }

    private void completeAll(int role, long participantId, String language, String commentPrefix) {
        // One rater per role of every block would need distinct participants per block; the role keeps one.
        for (int block = 1; block <= 4; block++) {
            long rater = block == 1 ? participantId : newRater("Extra " + role + "-" + block, "extra-" + role + "-" + block
                    + "-" + System.nanoTime() + "@example.com");
            completeBlock(block, role, rater, language, p -> responseOf(p, p % 2 == 0 ? "HIGH" : "LOW", "KEEP", commentPrefix + " " + p));
        }
    }

    private void reserveWithDraft(int blockNumber, int role, long participantId) {
        long block = blockId(blockNumber);
        Instant now = clock.instant();
        jdbc.sql("""
                        UPDATE task SET status = 'reserved', participant_id = ?, reserved_at = ?, expires_at = ?
                        WHERE block_id = ? AND role = ?""")
                .params(participantId, Timestamp.from(now), Timestamp.from(now.plus(1, ChronoUnit.HOURS)), block, role)
                .update();
        long task = jdbc.sql("SELECT id FROM task WHERE block_id = ? AND role = ?").params(block, role)
                .query(Long.class).single();
        long item = jdbc.sql("SELECT id FROM item WHERE block_id = ? ORDER BY position LIMIT 1").param(block)
                .query(Long.class).single();
        insertResponse(task, item, participantId, "draft", new ResponseSpec("HIGH", "FLAG", "secret draft"), "pt-BR");
    }

    private void insertResponse(long task, long item, long participantId, String state, ResponseSpec spec, String language) {
        jdbc.sql("""
                        INSERT INTO response (task_id, item_id, participant_id, state, change_impact,
                            maintenance_operation, comment, language, saved_at)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)""")
                .params(task, item, participantId, state, spec.impact(), spec.operation(), spec.comment(), language,
                        Timestamp.from(clock.instant()))
                .update();
    }

    /** An item of another system in a block of its own, queued after the Minitest blocks. */
    private void insertOtherSystemItem(String system) {
        long batch = jdbc.sql("SELECT id FROM import_batch ORDER BY id LIMIT 1").query(Long.class).single();
        int number = jdbc.sql("SELECT MAX(number) + 1 FROM block").query(Integer.class).single();
        jdbc.sql("INSERT INTO block (batch_id, number, size) VALUES (?, ?, 1)").params(batch, number).update();
        long block = blockId(number);
        jdbc.sql("""
                        INSERT INTO item (`system`, edit_key, uc, kind, field, origin, target, origin_version,
                            target_version, origin_usecase, target_usecase, origin_tests, target_tests,
                            batch_id, block_id, position)
                        VALUES (?, 'other-edit-key', 'UC', 'ADD', 'step', NULL, 'new step', 'v1', 'v2', 'a', 'b',
                            'ot', 'tt', ?, ?, 1)""")
                .params(system, batch, block)
                .update();
    }
}
