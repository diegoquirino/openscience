package io.github.diegoquirino.veredito.api;

import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.startsWith;

import io.github.diegoquirino.veredito.support.ApiTestBase;
import io.github.diegoquirino.veredito.support.Bundles;
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
    private static final List<String> HEADERS = List.of("pt-BR", "en-US", "es-ES", NO_HEADER);
    private static final List<String> ACCESS_PROBLEMS = List.of(
            "code-resend-too-soon", "code-hourly-limit", "daily-email-limit", "code-not-sent", "invalid-code",
            "code-expired");

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

    @ParameterizedTest(name = "{0} with Accept-Language {1}")
    @MethodSource("accessProblems")
    void accessProblemsAreLocalized(String slug, String acceptLanguage) {
        String expectedLocale = NO_HEADER.equals(acceptLanguage) ? "pt-BR" : acceptLanguage;
        Properties texts = Bundles.load(Bundles.fileOf(expectedLocale));
        Response response = provoke(slug, acceptLanguage);
        Object[] detailArgs = switch (slug) {
            case "code-resend-too-soon" -> new Object[] {response.<Integer>path("retryAfterSeconds")};
            case "code-hourly-limit" -> new Object[] {(Long.parseLong(response.header("Retry-After")) + 59) / 60};
            case "invalid-code" -> new Object[] {response.<Integer>path("attemptsLeft")};
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

    private Response provoke(String slug, String acceptLanguage) {
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
