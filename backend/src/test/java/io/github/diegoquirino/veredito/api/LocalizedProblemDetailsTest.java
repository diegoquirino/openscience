package io.github.diegoquirino.veredito.api;

import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.startsWith;

import io.github.diegoquirino.veredito.support.ApiTestBase;
import io.github.diegoquirino.veredito.support.Bundles;
import io.github.diegoquirino.veredito.web.CurrentSession;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import java.sql.Timestamp;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * US6 scenario 5 and FR-016 of spec 003, and the types added by spec 004: the same Problem Details in
 * the language of {@code Accept-Language}.
 */
class LocalizedProblemDetailsTest extends ApiTestBase {

    private static final String NO_HEADER = "<none>";
    private static final String TASK_RESERVATIONS = "/api/v1/tasks/reservations";
    private static final List<String> TASK_PROBLEMS = List.of("reservation-expired", "task-already-submitted");
    private static final List<String> HEADERS = List.of("pt-BR", "en-US", "es-ES", NO_HEADER);
    private static final List<String> ACCESS_PROBLEMS = List.of(
            "code-resend-too-soon", "code-hourly-limit", "daily-email-limit", "code-not-sent", "invalid-code",
            "code-expired");
    private static final List<String> IMPORT_PROBLEMS = List.of(
            "csv-not-utf8", "csv-invalid", "csv-missing-column", "minitest-only", "preview-not-found");

    @ParameterizedTest(name = "Accept-Language {0} gives {1}")
    @CsvSource({
        "pt-BR,    pt-BR",
        "en-US,    en-US",
        "es-ES,    es-ES",
        "es,       es-ES",
        "fr-FR,    pt-BR",
        "<none>,   pt-BR"
    })
    void unsupportedVersionIsLocalized(String acceptLanguage, String expectedLocale) {
        Properties texts = Bundles.load(Bundles.fileOf(expectedLocale));
        localized(acceptLanguage).get("/api/v9/tasks")
                .then().statusCode(400)
                .contentType(startsWith("application/problem+json"))
                .header("Content-Language", expectedLocale)
                .body(matchesJsonSchemaInClasspath("schemas/problem-details.json"))
                .body("type", equalTo("urn:veredito:problem:unsupported-api-version"))
                .body("supportedVersions", contains("v1"))
                .body("title", equalTo(texts.getProperty("problem.unsupported-api-version.title")))
                .body("detail", equalTo(Bundles.format(
                        texts, "problem.unsupported-api-version.detail", expectedLocale, "v9", "v1")));
    }

    static Stream<Arguments> accessProblems() {
        return ACCESS_PROBLEMS.stream().flatMap(slug -> HEADERS.stream().map(header -> Arguments.of(slug, header)));
    }

    static Stream<Arguments> importProblems() {
        return IMPORT_PROBLEMS.stream().flatMap(slug -> HEADERS.stream().map(header -> Arguments.of(slug, header)));
    }

    static Stream<Arguments> taskProblems() {
        return TASK_PROBLEMS.stream().flatMap(slug -> HEADERS.stream().map(header -> Arguments.of(slug, header)));
    }

    @ParameterizedTest(name = "{0} with Accept-Language {1}")
    @MethodSource("taskProblems")
    void taskProblemsAreLocalized(String slug, String acceptLanguage) {
        assertLocalized(slug, acceptLanguage, provokeTask(slug, acceptLanguage));
    }

    @ParameterizedTest(name = "{0} with Accept-Language {1}")
    @MethodSource("accessProblems")
    void accessProblemsAreLocalized(String slug, String acceptLanguage) {
        assertLocalized(slug, acceptLanguage, provokeAccess(slug, acceptLanguage));
    }

    @ParameterizedTest(name = "{0} with Accept-Language {1}")
    @MethodSource("importProblems")
    void importProblemsAreLocalized(String slug, String acceptLanguage) {
        assertLocalized(slug, acceptLanguage, provokeImport(slug, acceptLanguage));
    }

    private void assertLocalized(String slug, String acceptLanguage, Response response) {
        String expectedLocale = NO_HEADER.equals(acceptLanguage) ? "pt-BR" : acceptLanguage;
        Properties texts = Bundles.load(Bundles.fileOf(expectedLocale));
        Object[] detailArgs = switch (slug) {
            case "code-resend-too-soon" -> new Object[] {response.<Integer>path("retryAfterSeconds")};
            case "code-hourly-limit" -> new Object[] {(Long.parseLong(response.header("Retry-After")) + 59) / 60};
            case "invalid-code" -> new Object[] {response.<Integer>path("attemptsLeft")};
            case "csv-missing-column" -> new Object[] {response.<String>path("column")};
            case "minitest-only" -> new Object[] {String.join(", ", response.<List<String>>path("systems"))};
            default -> new Object[0];
        };

        response.then()
                .contentType(startsWith("application/problem+json"))
                .header("Content-Language", expectedLocale)
                .body(matchesJsonSchemaInClasspath("schemas/problem-details.json"))
                .body("type", equalTo("urn:veredito:problem:" + slug))
                .body("title", equalTo(texts.getProperty("problem." + slug + ".title")))
                .body("detail", equalTo(Bundles.format(texts, "problem." + slug + ".detail", expectedLocale, detailArgs)));
    }

    private Response provokeAccess(String slug, String acceptLanguage) {
        String email = "localized-" + System.nanoTime() + "@example.com";
        return switch (slug) {
            case "code-resend-too-soon" -> {
                requestCode("Ana Souza", email, "pt-BR").then().statusCode(200);
                yield postCode(acceptLanguage, email);
            }
            case "code-hourly-limit" -> {
                for (int sent = 0; sent < 5; sent++) {
                    requestCode("Ana Souza", email, "pt-BR").then().statusCode(200);
                    clock.advance(Duration.ofSeconds(60));
                }
                yield postCode(acceptLanguage, email);
            }
            case "daily-email-limit" -> withDailyLimitReached(() -> postCode(acceptLanguage, email));
            case "code-not-sent" -> {
                mailSender.failing(true);
                yield postCode(acceptLanguage, email);
            }
            case "invalid-code" -> {
                requestCode("Ana Souza", email, "pt-BR").then().statusCode(200);
                yield postSignIn(acceptLanguage, email, "abcdef");
            }
            case "code-expired" -> postSignIn(acceptLanguage, email, "123456");
            default -> throw new IllegalArgumentException(slug);
        };
    }

    private Response provokeImport(String slug, String acceptLanguage) {
        String token = administratorSession();
        return switch (slug) {
            case "csv-not-utf8" -> localized(acceptLanguage)
                    .cookie(io.github.diegoquirino.veredito.web.CurrentSession.COOKIE, token)
                    .multiPart("file", "bad.csv", new byte[] {(byte) 0xFF, (byte) 0xFE}, "text/csv")
                    .post("/api/v1/batches/previews");
            case "csv-invalid" -> localized(acceptLanguage)
                    .cookie(io.github.diegoquirino.veredito.web.CurrentSession.COOKIE, token)
                    .multiPart("file", "bad.csv", "\"unclosed".getBytes(java.nio.charset.StandardCharsets.UTF_8), "text/csv")
                    .post("/api/v1/batches/previews");
            case "csv-missing-column" -> localized(acceptLanguage)
                    .cookie(io.github.diegoquirino.veredito.web.CurrentSession.COOKIE, token)
                    .multiPart(
                            "file",
                            "missing.csv",
                            classpath("minitest/minitest_missing_kind_column.csv"),
                            "text/csv")
                    .post("/api/v1/batches/previews");
            case "minitest-only" -> localized(acceptLanguage)
                    .cookie(io.github.diegoquirino.veredito.web.CurrentSession.COOKIE, token)
                    .multiPart(
                            "file",
                            "other.csv",
                            classpath("minitest/other_system_sample.csv"),
                            "text/csv")
                    .post("/api/v1/batches/previews");
            case "preview-not-found" -> localized(acceptLanguage)
                    .cookie(io.github.diegoquirino.veredito.web.CurrentSession.COOKIE, token)
                    .get("/api/v1/batches/previews/999999");
            default -> throw new IllegalArgumentException(slug);
        };
    }

    /**
     * Spec 007. {@code not-eligible} needs the gate to answer false (there is no participant that
     * is ineligible until spec 006), so it is not provoked here.
     */
    private Response provokeTask(String slug, String acceptLanguage) {
        clearStudyDatabase();
        importMinitestBatch();
        String token = participantSession();
        Response reserved = as(token).post(TASK_RESERVATIONS);
        reserved.then().statusCode(201);
        String taskUrl = "/api/v1/tasks/" + reserved.<Integer>path("id");
        long itemId = as(token).get(taskUrl + "/items/1").then().statusCode(200).extract().jsonPath().getLong("itemId");
        Map<String, String> body = Map.of("changeImpact", "LOW", "maintenanceOperation", "KEEP");
        return switch (slug) {
            case "reservation-expired" -> {
                // The reservation lapses and the participant is handed another block meanwhile.
                clock.advance(Duration.ofMinutes(reserved.<Integer>path("reservationMinutes") + 1));
                as(token).post(TASK_RESERVATIONS).then().statusCode(201);
                yield localized(acceptLanguage).cookie(CurrentSession.COOKIE, token)
                        .contentType("application/json")
                        .body(body)
                        .put(taskUrl + "/responses/" + itemId);
            }
            case "task-already-submitted" -> {
                jdbc.sql("UPDATE task SET status = 'completed', submitted_at = ? WHERE id = ?")
                        .params(Timestamp.from(clock.instant()), reserved.<Integer>path("id"))
                        .update();
                yield localized(acceptLanguage).cookie(CurrentSession.COOKIE, token)
                        .contentType("application/json")
                        .body(body)
                        .put(taskUrl + "/responses/" + itemId);
            }
            default -> throw new IllegalArgumentException(slug);
        };
    }

    private static byte[] classpath(String path) {
        try (var in = new org.springframework.core.io.ClassPathResource(path).getInputStream()) {
            return in.readAllBytes();
        } catch (java.io.IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private Response withDailyLimitReached(Supplier<Response> call) {
        int original = jdbc.sql("SELECT daily_email_limit FROM config WHERE id = 1").query(Integer.class).single();
        long sentInTheLastDay = jdbc.sql("SELECT COUNT(*) FROM access_code WHERE created_at > ?")
                .param(Timestamp.from(clock.instant().minus(Duration.ofHours(24))))
                .query(Long.class)
                .single();
        jdbc.sql("UPDATE config SET daily_email_limit = ? WHERE id = 1").param(sentInTheLastDay).update();
        try {
            return call.get();
        } finally {
            jdbc.sql("UPDATE config SET daily_email_limit = ? WHERE id = 1").param(original).update();
        }
    }

    private Response postCode(String acceptLanguage, String email) {
        return localized(acceptLanguage)
                .contentType("application/json")
                .body(Map.of("name", "Ana Souza", "email", email))
                .post("/api/v1/access-codes");
    }

    private Response postSignIn(String acceptLanguage, String email, String code) {
        return localized(acceptLanguage)
                .contentType("application/json")
                .body(Map.of("email", email, "code", code))
                .post(SIGN_IN);
    }

    private RequestSpecification localized(String acceptLanguage) {
        RequestSpecification request = request();
        return NO_HEADER.equals(acceptLanguage) ? request : request.header("Accept-Language", acceptLanguage);
    }
}
