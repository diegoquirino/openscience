package io.github.diegoquirino.veredito.api.v1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.matchesPattern;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import io.github.diegoquirino.veredito.support.ApiTestBase;
import io.github.diegoquirino.veredito.support.Bundles;
import io.github.diegoquirino.veredito.support.MailpitClient.Message;
import io.github.diegoquirino.veredito.web.CurrentSession;
import io.restassured.http.Cookie;
import io.restassured.response.Response;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/** User story 1 of spec 004: request a code by e-mail and sign in with it (contracts/access.md). */
class AccessTest extends ApiTestBase {

    private static final String PROBLEM = "urn:veredito:problem:";
    private static final String CURRENT = "/api/v1/access-sessions/current";
    private static final Duration RESEND_INTERVAL = Duration.ofSeconds(60);

    @Test
    void aValidRequestSendsExactlyOneCode() {
        String email = unique("request");

        requestCode("Ana Souza", email, "pt-BR")
                .then().statusCode(200)
                .body("email", equalTo(email))
                .body("validityMinutes", equalTo(10))
                .body("resendAfterSeconds", equalTo(60))
                .body("_links.self.href", equalTo("/api/v1/access-codes"))
                .body("_links.'vd:sign-in'.href", equalTo(SIGN_IN))
                .body("_links.'vd:request-code'.href", equalTo("/api/v1/access-codes"));

        assertThat(mailpit.latestCodeTo(email)).matches("[0-9]{6}");
        assertThat(mailpit.countTo(email)).isEqualTo(1);
    }

    @ParameterizedTest(name = "e-mail in {0}")
    @ValueSource(strings = {"pt-BR", "en-US", "es-ES"})
    void theEmailIsWrittenInTheLanguageOfTheRequest(String locale) {
        String email = unique("language");
        requestCode("Ana Souza", email, locale).then().statusCode(200);

        Properties texts = Bundles.load(Bundles.fileOf(locale));
        Message message = mailpit.latestTo(email);
        String code = mailpit.latestCodeTo(email);
        assertThat(message.subject()).isEqualTo(texts.getProperty("mail.access-code.subject"));
        assertThat(message.text().replace("\r\n", "\n").strip())
                .isEqualTo(Bundles.format(texts, "mail.access-code.body", locale, "Ana Souza", code, 10L).strip());
    }

    @ParameterizedTest(name = "name \"{0}\", e-mail \"{1}\" gives {2}/{3}")
    @CsvSource({
        "A,         valid,      name,  size",
        "Ana Souza, ana@,       email, email",
        "'  ',      valid,      name,  required"
    })
    void invalidFieldsAreRefusedAndNothingIsSent(String name, String typedEmail, String field, String code) {
        String email = "valid".equals(typedEmail) ? unique("invalid") : typedEmail;
        long codesBefore = countCodes();

        requestCode(name, email, "en-US")
                .then().statusCode(400)
                .body("type", equalTo(PROBLEM + "invalid-request"))
                .body("errors.field", contains(field))
                .body("errors.code", contains(code));

        assertThat(countCodes()).isEqualTo(codesBefore);
        if (email.endsWith("@example.com")) {
            assertThat(mailpit.countTo(email)).isZero();
        }
    }

    @Test
    void theRightCodeTypedWithSpacesAndHyphensSignsIn() {
        String email = unique("right");
        requestCode("Ana Souza", email, "es-ES").then().statusCode(200);
        String code = mailpit.latestCodeTo(email);
        Instant before = clock.instant();

        Response signedIn = sendCode(email, code.substring(0, 3) + " " + code.charAt(3) + "-" + code.substring(4), "es-ES");

        signedIn.then().statusCode(201)
                .header("Location", equalTo(CURRENT))
                .body("subject", equalTo("participant"))
                .body("name", equalTo("Ana Souza"))
                .body("pseudonym", matchesPattern("R-[0-9]{4,}"))
                .body("preferredLanguage", equalTo("es-ES"))
                .body("_links.self.href", equalTo(CURRENT))
                .body("_links.'vd:me'.href", equalTo("/api/v1/participants/me"))
                .body("_links.'vd:sign-out'.href", equalTo(CURRENT));
        assertThat(Instant.parse(signedIn.path("expiresAt")))
                .isBetween(before.plus(Duration.ofHours(24)).minusSeconds(1), clock.instant().plus(Duration.ofHours(24)));

        Cookie cookie = signedIn.getDetailedCookie(CurrentSession.COOKIE);
        assertThat(cookie.getValue()).matches("[A-Za-z0-9_-]{43}");
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.isSecured()).isTrue();
        assertThat(cookie.getSameSite()).isEqualTo("Lax");
        assertThat(cookie.getPath()).isEqualTo("/");
        assertThat(cookie.getMaxAge()).isEqualTo(86400);

        as(cookie.getValue()).get(CURRENT)
                .then().statusCode(200)
                .body("subject", equalTo("participant"))
                .body("pseudonym", equalTo(signedIn.path("pseudonym")))
                .body("_links.'vd:me'.href", equalTo("/api/v1/participants/me"));
    }

    @Test
    void fourWrongCodesCountDownAndTheFifthKillsTheCode() {
        String email = unique("wrong");
        requestCode("Ana Souza", email, "en-US").then().statusCode(200);
        String code = mailpit.latestCodeTo(email);
        String wrong = wrongCode(code);

        for (int attemptsLeft = 4; attemptsLeft >= 1; attemptsLeft--) {
            sendCode(email, wrong, "en-US")
                    .then().statusCode(422)
                    .body("type", equalTo(PROBLEM + "invalid-code"))
                    .body("attemptsLeft", equalTo(attemptsLeft));
        }
        sendCode(email, wrong, "en-US").then().statusCode(422).body("type", equalTo(PROBLEM + "code-expired"));
        sendCode(email, code, "en-US").then().statusCode(422).body("type", equalTo(PROBLEM + "code-expired"));
    }

    @Test
    void aCodeOfTheWrongShapeCountsAsAWrongAttempt() {
        String email = unique("shape");
        requestCode("Ana Souza", email, "en-US").then().statusCode(200);

        sendCode(email, "12345", "en-US")
                .then().statusCode(422)
                .body("type", equalTo(PROBLEM + "invalid-code"))
                .body("attemptsLeft", equalTo(4));
    }

    @Test
    void aCodeDiesTenMinutesAfterItWasSent() {
        String email = unique("expired");
        requestCode("Ana Souza", email, "en-US").then().statusCode(200);
        String code = mailpit.latestCodeTo(email);

        clock.advance(Duration.ofMinutes(10));

        sendCode(email, code, "en-US").then().statusCode(422).body("type", equalTo(PROBLEM + "code-expired"));
    }

    @Test
    void aUsedCodeCannotSignInAgain() {
        String email = unique("used");
        requestCode("Ana Souza", email, "en-US").then().statusCode(200);
        String code = mailpit.latestCodeTo(email);

        sendCode(email, code, "en-US").then().statusCode(201);
        sendCode(email, code, "en-US").then().statusCode(422).body("type", equalTo(PROBLEM + "code-expired"));
    }

    @Test
    void aNewCodeWithinSixtySecondsIsTooSoon() {
        String email = unique("soon");
        requestCode("Ana Souza", email, "en-US").then().statusCode(200);
        clock.advance(Duration.ofSeconds(10));

        Response refused = requestCode("Ana Souza", email, "en-US");
        refused.then().statusCode(429)
                .body("type", equalTo(PROBLEM + "code-resend-too-soon"))
                .body("retryAfterSeconds", allOf(greaterThan(0), lessThanOrEqualTo(50)));
        assertThat(refused.header("Retry-After")).isEqualTo(String.valueOf(refused.<Integer>path("retryAfterSeconds")));

        clock.advance(Duration.ofSeconds(50));
        requestCode("Ana Souza", email, "en-US").then().statusCode(200);
        assertThat(mailpit.countTo(email)).isEqualTo(2);
    }

    @Test
    void theSixthCodeWithinAnHourHitsTheHourlyLimit() {
        String email = unique("hourly");
        for (int sent = 0; sent < 5; sent++) {
            requestCode("Ana Souza", email, "en-US").then().statusCode(200);
            clock.advance(RESEND_INTERVAL);
        }

        Instant askedAt = clock.instant();
        Response refused = requestCode("Ana Souza", email, "en-US");
        Instant answeredAt = clock.instant();

        refused.then().statusCode(429).body("type", equalTo(PROBLEM + "code-hourly-limit"));
        Instant firstSend = jdbc.sql("SELECT MIN(created_at) FROM access_code WHERE email = ?")
                .param(email)
                .query(Timestamp.class)
                .single()
                .toInstant();
        Instant oneHourLater = firstSend.plus(Duration.ofHours(1));
        Instant retryAt = Instant.parse(refused.path("retryAt"));
        assertThat(retryAt).isBetween(oneHourLater, oneHourLater.plusSeconds(1));
        assertThat(Long.parseLong(refused.header("Retry-After"))).isBetween(
                Duration.between(answeredAt, retryAt).toSeconds(),
                Duration.between(askedAt, retryAt).toSeconds() + 1);
        assertThat(mailpit.countTo(email)).isEqualTo(5);
    }

    @Test
    void anEmailIsOneParticipantWhateverItsCaseAndSpaces() {
        long id = System.nanoTime();
        String typed = " Same-" + id + "@Example.com ";
        String plain = "same-" + id + "@example.com";

        String first = signIn("Ana Souza", typed, "pt-BR");
        clock.advance(RESEND_INTERVAL);
        String second = signIn("Ana Souza", plain, "pt-BR");

        assertThat(pseudonymOf(second)).isEqualTo(pseudonymOf(first));
        assertThat(jdbc.sql("SELECT COUNT(*) FROM participant WHERE email = ?").param(plain).query(Long.class).single())
                .isEqualTo(1L);
    }

    @Test
    void theDailyLimitStopsEveryEmail() {
        int original = jdbc.sql("SELECT daily_email_limit FROM config WHERE id = 1").query(Integer.class).single();
        jdbc.sql("UPDATE config SET daily_email_limit = ? WHERE id = 1").param(countCodesOfTheLastDay() + 1).update();
        try {
            String second = unique("daily-second");
            requestCode("Ana Souza", unique("daily-first"), "en-US").then().statusCode(200);

            requestCode("Ana Souza", second, "en-US")
                    .then().statusCode(429)
                    .body("type", equalTo(PROBLEM + "daily-email-limit"));
            assertThat(mailpit.countTo(second)).isZero();
        } finally {
            jdbc.sql("UPDATE config SET daily_email_limit = ? WHERE id = 1").param(original).update();
        }
    }

    @Test
    void firstSignInsGetConsecutivePseudonyms() {
        String first = pseudonymOf(signIn("Ana Souza", unique("pseudonym-a"), "pt-BR"));
        String second = pseudonymOf(signIn("Bruno Lima", unique("pseudonym-b"), "pt-BR"));

        assertThat(first).matches("R-[0-9]{4,}");
        assertThat(second).matches("R-[0-9]{4,}");
        assertThat(Long.parseLong(second.substring(2))).isEqualTo(Long.parseLong(first.substring(2)) + 1);
    }

    @Test
    void theLatestNameIsKept() {
        String email = unique("rename");
        signIn("Ana Souza", email, "pt-BR");
        clock.advance(RESEND_INTERVAL);
        String token = signIn("Ana S.", email, "pt-BR");

        as(token).get(CURRENT).then().statusCode(200).body("name", equalTo("Ana S."));
        assertThat(jdbc.sql("SELECT name FROM participant WHERE email = ?").param(email).query(String.class).single())
                .isEqualTo("Ana S.");
    }

    @Test
    void onlyTheLatestOfTwoCodesWorks() {
        String email = unique("devices");
        requestCode("Ana Souza", email, "pt-BR").then().statusCode(200);
        String older = mailpit.latestCodeTo(email);
        clock.advance(RESEND_INTERVAL);
        requestCode("Ana Souza", email, "pt-BR").then().statusCode(200);
        String newer = mailpit.latestCodeTo(email);
        assumeFalse(older.equals(newer), "both draws gave the same code");

        sendCode(email, older, "pt-BR").then().statusCode(422).body("type", equalTo(PROBLEM + "invalid-code"));
        sendCode(email, newer, "pt-BR").then().statusCode(201);
    }

    @Test
    void anSmtpFailureAnswers503AndLeavesNoCode() {
        String email = unique("smtp");
        mailSender.failing(true);

        requestCode("Ana Souza", email, "pt-BR")
                .then().statusCode(503)
                .body("type", equalTo(PROBLEM + "code-not-sent"));

        assertThat(jdbc.sql("SELECT COUNT(*) FROM access_code WHERE email = ?").param(email).query(Long.class).single())
                .isZero();
        mailSender.failing(false);
        requestCode("Ana Souza", email, "pt-BR").then().statusCode(200);
    }

    @Test
    void wrongAndDeadCodeAnswersDoNotTellWhetherTheEmailIsRegistered() {
        String registered = unique("registered");
        String unknown = unique("unknown");
        signIn("Ana Souza", registered, "pt-BR");
        clock.advance(RESEND_INTERVAL);
        requestCode("Ana Souza", registered, "pt-BR").then().statusCode(200);
        requestCode("Ana Souza", unknown, "pt-BR").then().statusCode(200);

        Map<String, Object> wrongForRegistered = problem(sendCode(registered, wrongCode(mailpit.latestCodeTo(registered)), "pt-BR"));
        Map<String, Object> wrongForUnknown = problem(sendCode(unknown, wrongCode(mailpit.latestCodeTo(unknown)), "pt-BR"));
        assertThat(wrongForRegistered).isEqualTo(wrongForUnknown).containsEntry("attemptsLeft", 4);

        clock.advance(Duration.ofMinutes(10));
        Map<String, Object> deadForRegistered = problem(sendCode(registered, "123456", "pt-BR"));
        Map<String, Object> deadForUnknown = problem(sendCode(unknown, "123456", "pt-BR"));
        Map<String, Object> neverRequested = problem(sendCode(unique("never"), "123456", "pt-BR"));
        assertThat(deadForRegistered)
                .isEqualTo(deadForUnknown)
                .isEqualTo(neverRequested)
                .containsEntry("type", PROBLEM + "code-expired");
    }

    @Test
    void missingEmailOrCodeIsAnInvalidRequest() {
        request().contentType("application/json").body(Map.of("email", "", "code", " - "))
                .post(SIGN_IN)
                .then().statusCode(400)
                .body("type", equalTo(PROBLEM + "invalid-request"))
                .body("errors.field", contains("email", "code"))
                .body("errors.code", contains("required", "required"));
    }

    private Map<String, Object> problem(Response response) {
        response.then().statusCode(422);
        return response.jsonPath().getMap("$");
    }

    private String pseudonymOf(String token) {
        return as(token).get(CURRENT).then().statusCode(200).extract().path("pseudonym");
    }

    private long countCodes() {
        return jdbc.sql("SELECT COUNT(*) FROM access_code").query(Long.class).single();
    }

    private long countCodesOfTheLastDay() {
        return jdbc.sql("SELECT COUNT(*) FROM access_code WHERE created_at > ?")
                .param(Timestamp.from(clock.instant().minus(Duration.ofHours(24))))
                .query(Long.class)
                .single();
    }

    private static String wrongCode(String code) {
        return String.format(Locale.ROOT, "%06d", (Integer.parseInt(code) + 1) % 1_000_000);
    }

    private static String unique(String prefix) {
        return prefix + "-" + System.nanoTime() + "@example.com";
    }
}
