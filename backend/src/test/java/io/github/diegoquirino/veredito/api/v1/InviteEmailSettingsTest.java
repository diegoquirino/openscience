package io.github.diegoquirino.veredito.api.v1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

import io.github.diegoquirino.veredito.support.ApiTestBase;
import io.github.diegoquirino.veredito.support.MailpitClient.Message;
import io.restassured.response.Response;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Spec 031: admin date-time settings round-trip and inicial invite after profile. */
class InviteEmailSettingsTest extends ApiTestBase {

    @BeforeEach
    void clearMailbox() {
        mailpit.clear();
    }

    @Test
    void administratorCanRoundTripDateAndTimeSettings() {
        String admin = administratorSession();
        String href = href(as(admin).get("/api/v1"), "email-settings");

        as(admin).get(href)
                .then().statusCode(200)
                .body("platformUrl", equalTo("https://diegoquirino.cloud"))
                .body("collectionStartAt", equalTo("2026-12-21T03:00:00Z"))
                .body("collectionEndAt", equalTo("2027-01-09T02:59:00Z"))
                .body("reminderAt", equalTo("2026-12-28T15:00:00Z"));

        as(admin).contentType("application/json")
                .body(Map.of(
                        "caae", "CAAE 99999999.9.0001.5188",
                        "platformUrl", "https://example.test/veredito",
                        "collectionStartAt", "2026-12-22T12:30:00Z",
                        "collectionEndAt", "2027-01-10T23:45:00Z",
                        "reminderAt", "2026-12-29T18:15:00Z"))
                .put(href)
                .then().statusCode(200)
                .body("caae", equalTo("CAAE 99999999.9.0001.5188"))
                .body("platformUrl", equalTo("https://example.test/veredito"))
                .body("collectionStartAt", equalTo("2026-12-22T12:30:00Z"))
                .body("collectionEndAt", equalTo("2027-01-10T23:45:00Z"))
                .body("reminderAt", equalTo("2026-12-29T18:15:00Z"));

        as(admin).contentType("application/json")
                .body(Map.of(
                        "caae", "x",
                        "platformUrl", "https://ok.example",
                        "collectionStartAt", "2027-01-10T00:00:00Z",
                        "collectionEndAt", "2027-01-09T00:00:00Z",
                        "reminderAt", "2027-01-05T00:00:00Z"))
                .put(href)
                .then().statusCode(400);
    }

    @Test
    void profileCompletionSendsInicialOnce() {
        String email = "invite-" + System.nanoTime() + "@example.com";
        String token = signIn("Maria Silva", email, "pt-BR");
        mailpit.clear();

        Response onboarding = as(token).get(href(as(token).get("/api/v1"), "onboarding"));
        as(token).contentType("application/json")
                .body(Map.of(
                        "name", "Maria Silva",
                        "skill", "developer",
                        "organization", "ufcg"))
                .put(href(onboarding, "skill"))
                .then().statusCode(200);

        Message first = mailpit.latestTo(email);
        assertThat(first.subject()).contains("pesquisa");
        assertThat(first.text()).contains("Maria Silva");
        assertThat(first.text()).contains("https://diegoquirino.cloud");

        InstantSentAt sentAt = sentAt(email);
        assertThat(sentAt.inicial()).isNotNull();
        assertThat(mailpit.countTo(email)).isEqualTo(1);

        as(token).contentType("application/json")
                .body(Map.of(
                        "name", "Maria Silva",
                        "skill", "test_engineer",
                        "organization", "ufcg"))
                .put(href(onboarding, "skill"))
                .then().statusCode(200);

        assertThat(mailpit.countTo(email)).isEqualTo(1);
        assertThat(sentAt(email).inicial()).isEqualTo(sentAt.inicial());
    }

    @Test
    void smtpFailureLeavesInicialUnsetForRetry() {
        String email = "invite-fail-" + System.nanoTime() + "@example.com";
        String token = signIn("Joao Lima", email, "pt-BR");
        mailpit.clear();
        mailSender.failing(true);

        Response onboarding = as(token).get(href(as(token).get("/api/v1"), "onboarding"));
        as(token).contentType("application/json")
                .body(Map.of(
                        "name", "Joao Lima",
                        "skill", "developer",
                        "organization", "ufcg"))
                .put(href(onboarding, "skill"))
                .then().statusCode(200);

        assertThat(sentAt(email).inicial()).isNull();
        assertThat(mailpit.countTo(email)).isEqualTo(0);

        mailSender.failing(false);
        as(token).contentType("application/json")
                .body(Map.of(
                        "name", "Joao Lima",
                        "skill", "test_engineer",
                        "organization", "ufcg"))
                .put(href(onboarding, "skill"))
                .then().statusCode(200);

        Message retry = mailpit.latestTo(email);
        assertThat(retry.text()).contains("Joao Lima");
        assertThat(sentAt(email).inicial()).isNotNull();
    }

    @Test
    void participantCannotReadEmailSettings() {
        as(participantSession()).get("/api/v1/administrator/email-settings").then().statusCode(403);
        entryPoint().then().body("_links.'vd:email-settings'.href", nullValue());
    }

    private InstantSentAt sentAt(String email) {
        Map<String, Object> row = jdbc.sql("""
                        SELECT invite_inicial_sent_at FROM participant WHERE email = ?""")
                .param(normalized(email))
                .query()
                .singleRow();
        Object value = row.get("invite_inicial_sent_at");
        return new InstantSentAt(value == null ? null : value.toString());
    }

    private record InstantSentAt(String inicial) {}
}
