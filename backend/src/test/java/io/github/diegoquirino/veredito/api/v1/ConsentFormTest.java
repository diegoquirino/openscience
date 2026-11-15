package io.github.diegoquirino.veredito.api.v1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import io.github.diegoquirino.veredito.onboarding.ConsentFormAudit;
import io.github.diegoquirino.veredito.support.ApiTestBase;
import io.restassured.response.Response;
import java.sql.Timestamp;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

/** Spec 006 US1 and US4: the consent form, the decision and its language (FR-001, FR-002, FR-008 to FR-012). */
@ExtendWith(OutputCaptureExtension.class)
class ConsentFormTest extends ApiTestBase {

    private static final String PROBLEM = "urn:veredito:problem:";

    @Autowired
    ConsentFormAudit audit;

    private Response consentForm(String token, String locale) {
        Response entry = as(token).get("/api/v1");
        return as(token).header("Accept-Language", locale).get(href(entry, "consent-form"));
    }

    private Response decide(String token, String locale, String decision, String version) {
        Response onboarding = as(token).get(href(as(token).get("/api/v1"), "onboarding"));
        return as(token)
                .header("Accept-Language", locale)
                .contentType("application/json")
                .body(Map.of("decision", decision, "consentFormVersion", version))
                .put(href(onboarding, "consent"));
    }

    private String step(String token) {
        return as(token).get(href(as(token).get("/api/v1"), "onboarding")).path("step");
    }

    @ParameterizedTest
    @ValueSource(strings = {"pt-BR", "en-US", "es-ES"})
    void theCurrentFormComesInTheLanguageOfTheRequest(String locale) {
        String token = participantSession();
        boolean translation = !locale.equals("pt-BR");

        consentForm(token, locale)
                .then().statusCode(200)
                .header("Content-Language", locale)
                .body("version", equalTo("fixture-1"))
                .body("language", equalTo(locale))
                .body("referenceLanguage", equalTo("pt-BR"))
                .body("translation", equalTo(translation))
                .body("text", containsString("FIXTURE"))
                .body("publishedAt", notNullValue())
                .body("_links.'vd:consent-text'.href", equalTo("/api/v1/consent-forms/fixture-1/texts/pt-BR"))
                .body("_links.'vd:consent'.href", equalTo("/api/v1/participants/me/consent"));
    }

    @Test
    void theReferenceTextOfThePinnedVersionIsPortugueseWhateverTheRequestLocale() {
        String token = participantSession();
        Response english = consentForm(token, "en-US");

        as(token).header("Accept-Language", "en-US").get(href(english, "consent-text"))
                .then().statusCode(200)
                .header("Content-Language", "pt-BR")
                .body("language", equalTo("pt-BR"))
                .body("translation", equalTo(false))
                .body("version", equalTo("fixture-1"))
                .body("text", containsString("TERMO DE CONSENTIMENTO"));
    }

    @Test
    void aTextThatDoesNotExistIsNotFound() {
        String token = participantSession();
        as(token).get("/api/v1/consent-forms/nope/texts/pt-BR").then().statusCode(404)
                .body("type", equalTo(PROBLEM + "not-found"));
        as(token).get("/api/v1/consent-forms/fixture-1/texts/fr-FR").then().statusCode(404)
                .body("type", equalTo(PROBLEM + "not-found"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"pt-BR", "en-US", "es-ES"})
    void acceptingRecordsVersionLanguageAndTime(String locale) {
        String token = participantSession();
        long id = participantIdOf(token);

        decide(token, locale, "accepted", "fixture-1")
                .then().statusCode(200)
                .header("Content-Location", equalTo("/api/v1/participants/me/onboarding"))
                .body("decision", equalTo("accepted"))
                .body("consentFormVersion", equalTo("fixture-1"))
                .body("language", equalTo(locale))
                .body("decidedAt", notNullValue())
                .body("current", equalTo(true));

        Map<String, Object> row = jdbc.sql("""
                        SELECT consent_decision, consent_language, consent_decided_at, cf.version AS version
                        FROM participant p JOIN consent_form cf ON cf.id = p.consent_form_id WHERE p.id = ?""")
                .param(id).query().singleRow();
        assertThat(row).containsEntry("consent_decision", "accepted")
                .containsEntry("consent_language", locale)
                .containsEntry("version", "fixture-1");
        assertThat(row.get("consent_decided_at")).isNotNull();
        assertThat(step(token)).isEqualTo("skill");
    }

    @Test
    void switchingTheLanguageOnTheScreenRecordsNothingUntilTheParticipantChooses() {
        String token = participantSession();
        long id = participantIdOf(token);

        consentForm(token, "en-US").then().statusCode(200);
        consentForm(token, "es-ES").then().statusCode(200);

        assertThat(jdbc.sql("SELECT COUNT(*) FROM participant WHERE id = ? AND consent_decision IS NULL")
                .param(id).query(Long.class).single()).isEqualTo(1L);
        as(token).get(EntryPointController.CONSENT)
                .then().statusCode(200)
                .body("decision", nullValue())
                .body("language", nullValue())
                .body("current", equalTo(false));
    }

    @Test
    void refusingRecordsTheRefusalAndTheNextStepIsStillConsent() {
        String token = participantSession();
        long id = participantIdOf(token);

        decide(token, "es-ES", "refused", "fixture-1")
                .then().statusCode(200)
                .body("decision", equalTo("refused"))
                .body("language", equalTo("es-ES"));

        assertThat(jdbc.sql("SELECT consent_decision FROM participant WHERE id = ?").param(id)
                .query(String.class).single()).isEqualTo("refused");
        assertThat(step(token)).isEqualTo("consent");
        as(token).get("/api/v1").then().body("_links", not(hasKey("vd:next-task")));
    }

    @Test
    void aParticipantWhoRefusedCanAcceptOnReturn() {
        String token = participantSession();
        decide(token, "pt-BR", "refused", "fixture-1").then().statusCode(200);
        assertThat(step(token)).isEqualTo("consent");

        decide(token, "en-US", "accepted", "fixture-1")
                .then().statusCode(200)
                .body("decision", equalTo("accepted"))
                .body("language", equalTo("en-US"));
        assertThat(step(token)).isEqualTo("skill");
    }

    @Test
    void repeatingTheSameDecisionChangesNothing() {
        String token = participantSession();
        Response first = decide(token, "pt-BR", "accepted", "fixture-1");
        first.then().statusCode(200);

        decide(token, "en-US", "accepted", "fixture-1")
                .then().statusCode(200)
                .body("language", equalTo("pt-BR"))
                .body("decidedAt", equalTo(first.path("decidedAt")));

        String other = participantSession();
        decide(other, "pt-BR", "refused", "fixture-1").then().statusCode(200);
        decide(other, "es-ES", "refused", "fixture-1").then().statusCode(200).body("language", equalTo("pt-BR"));
    }

    @Test
    void anAcceptanceOfTheCurrentVersionCannotBeFlippedToARefusal() {
        String token = participantSession();
        decide(token, "pt-BR", "accepted", "fixture-1").then().statusCode(200);

        decide(token, "pt-BR", "refused", "fixture-1")
                .then().statusCode(409)
                .body("type", equalTo(PROBLEM + "step-not-pending"))
                .body("step", equalTo("skill"));
    }

    @Test
    void aNewVersionAsksOnlyForConsentAgainAndKeepsSkillAndScreening() {
        String token = eligibleParticipantSession();
        long id = participantIdOf(token);
        as(token).get("/api/v1").then().body("_links", hasKey("vd:next-task"));

        insertCompleteConsentForm("v2", clock.instant().minus(Duration.ofDays(1)));

        Response onboarding = as(token).get(href(as(token).get("/api/v1"), "onboarding"));
        onboarding.then().statusCode(200)
                .body("step", equalTo("consent"))
                .body("eligible", equalTo(false))
                .body("currentConsentFormVersion", equalTo("v2"));
        as(token).get("/api/v1").then().body("_links", not(hasKey("vd:next-task")));
        as(token).get(EntryPointController.CONSENT)
                .then().body("current", equalTo(false)).body("consentFormVersion", equalTo("fixture-1"));
        assertThat(jdbc.sql("SELECT professional_skill FROM participant WHERE id = ?").param(id)
                .query(String.class).single()).isEqualTo("test_analyst");

        decide(token, "en-US", "accepted", "v2").then().statusCode(200).body("current", equalTo(true));
        assertThat(step(token)).isEqualTo("participation");
        as(token).get("/api/v1").then().body("_links", hasKey("vd:next-task"));
    }

    @Test
    void anOutdatedVersionIsRefusedWithTheCurrentOne() {
        String token = participantSession();
        insertCompleteConsentForm("v2", clock.instant().minus(Duration.ofDays(1)));

        decide(token, "pt-BR", "accepted", "fixture-1")
                .then().statusCode(409)
                .body("type", equalTo(PROBLEM + "consent-form-outdated"))
                .body("currentVersion", equalTo("v2"));
        assertThat(step(token)).isEqualTo("consent");
    }

    @Test
    void anIncompleteVersionNeverBecomesCurrentAndIsLogged(CapturedOutput output) {
        insertConsentForm("v3", clock.instant().minus(Duration.ofDays(1)), "pt-BR", "en-US");

        audit.audit();

        assertThat(output.getOut()).contains("Consent form v3 is published but lacks texts in: es-ES");
        assertThat(output.getOut()).contains("Current consent form: fixture-1");
        String token = participantSession();
        consentForm(token, "pt-BR").then().statusCode(200).body("version", equalTo("fixture-1"));
        decide(token, "pt-BR", "accepted", "v3").then().statusCode(409)
                .body("type", equalTo(PROBLEM + "consent-form-outdated"))
                .body("currentVersion", equalTo("fixture-1"));
    }

    @Test
    void withoutACurrentFormTheStudyIsNotOpen(CapturedOutput output) {
        jdbc.sql("UPDATE consent_form SET published_at = NULL").update();
        String token = participantSession();

        as(token).get(href(as(token).get("/api/v1"), "onboarding"))
                .then().statusCode(200)
                .body("step", equalTo("not-open"))
                .body("currentConsentFormVersion", nullValue())
                .body("eligible", equalTo(false))
                .body("_links", not(hasKey("vd:consent-form")));
        as(token).get(EntryPointController.CONSENT_FORM_CURRENT)
                .then().statusCode(404).body("type", equalTo(PROBLEM + "not-found"));
        decide(token, "pt-BR", "accepted", "fixture-1")
                .then().statusCode(409).body("type", equalTo(PROBLEM + "study-not-open"));

        audit.audit();
        assertThat(output.getOut()).contains("The study is not open yet");
    }

    @Test
    void anIneligibleParticipantCannotRecordAnything() {
        String token = participantSession();
        long id = participantIdOf(token);
        acceptCurrentConsent(id, "pt-BR");
        seedSkill(id, "developer", null);
        seedScreening(id, io.github.diegoquirino.veredito.domain.onboarding.ScreeningAnswer.NO);

        decide(token, "pt-BR", "accepted", "fixture-1")
                .then().statusCode(409)
                .body("type", equalTo(PROBLEM + "step-not-pending"))
                .body("step", equalTo("ineligible"));
        assertThat(step(token)).isEqualTo("ineligible");
    }

    @Test
    void invalidBodiesAreFieldErrors() {
        String token = participantSession();
        Response onboarding = as(token).get(href(as(token).get("/api/v1"), "onboarding"));
        String consent = href(onboarding, "consent");

        as(token).contentType("application/json").body("{}").put(consent)
                .then().statusCode(400)
                .body("type", equalTo(PROBLEM + "invalid-request"))
                .body("errors.field", equalTo(List.of("decision", "consentFormVersion")))
                .body("errors.code", equalTo(List.of("required", "required")));
        as(token).contentType("application/json")
                .body(Map.of("decision", "maybe", "consentFormVersion", "fixture-1")).put(consent)
                .then().statusCode(400)
                .body("errors.field", equalTo(List.of("decision")))
                .body("errors.code", equalTo(List.of("option")));
    }

    @Test
    void theConsentTemplateIsOfferedOnlyWhileConsentIsPending() {
        String token = participantSession();
        as(token).get(EntryPointController.CONSENT)
                .then().statusCode(200)
                .body("_templates.default.method", equalTo("PUT"))
                .body("_templates.default.properties.find { it.name == 'decision' }.options.inline",
                        equalTo(List.of("accepted", "refused")));

        decide(token, "pt-BR", "accepted", "fixture-1").then().statusCode(200);
        as(token).get(EntryPointController.CONSENT)
                .then().statusCode(200)
                .body("$", not(hasKey("_templates")));
    }

    @Test
    void decisionsKeepTheTimestampInUtc() {
        String token = participantSession();
        long id = participantIdOf(token);
        decide(token, "pt-BR", "accepted", "fixture-1").then().statusCode(200);
        Timestamp decided = jdbc.sql("SELECT consent_decided_at FROM participant WHERE id = ?").param(id)
                .query(Timestamp.class).single();
        assertThat(decided.toInstant()).isBetween(
                clock.instant().minus(Duration.ofMinutes(1)), clock.instant().plus(Duration.ofMinutes(1)));
    }
}
