package io.github.diegoquirino.veredito.api.v1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import io.github.diegoquirino.veredito.domain.onboarding.ScreeningAnswer;
import io.github.diegoquirino.veredito.support.ApiTestBase;
import io.restassured.response.Response;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Spec 006 US3: the screening decides eligibility; "no" is permanent (FR-004, FR-005, FR-007, FR-013). */
class ScreeningTest extends ApiTestBase {

    private static final String PROBLEM = "urn:veredito:problem:";

    /** A participant who accepted the consent and chose a skill: at the screening step. */
    private String atScreeningStep() {
        String token = participantSession();
        long id = participantIdOf(token);
        seedSkill(id, "developer", null);
        acceptCurrentConsent(id, "pt-BR");
        return token;
    }

    private Response putAnswer(String token, String locale, String answer) {
        return as(token).header("Accept-Language", locale).contentType("application/json")
                .body(Map.of("answer", answer)).put(EntryPointController.SCREENING);
    }

    private String step(String token) {
        return as(token).get(href(as(token).get("/api/v1"), "onboarding")).path("step");
    }

    @Test
    void theEnglishExampleIsTheSameInEveryLanguage() {
        String token = atScreeningStep();
        Response onboarding = as(token).get(href(as(token).get("/api/v1"), "onboarding"));
        String exampleHref = href(onboarding, "screening-example");

        Response english = null;
        for (String locale : new String[] {"pt-BR", "en-US", "es-ES"}) {
            Response response = as(token).header("Accept-Language", locale).get(exampleHref);
            response.then().statusCode(200)
                    .header("Content-Language", equalTo("en-US"))
                    .body("version", equalTo("1"))
                    .body("useCase.id", equalTo("UC-S1"))
                    .body("useCase.title", equalTo("Reset password"))
                    .body("useCase.basicFlow", hasSize(6))
                    .body("useCase.exceptionFlow.id", equalTo("E1"))
                    .body("useCase.exceptionFlow.condition", notNullValue())
                    .body("useCase.exceptionFlow.steps", hasSize(2))
                    .body("testCase.id", equalTo("TC-S1"))
                    .body("testCase.derivedFrom", contains("UC-S1 / E1"))
                    .body("testCase.steps", hasSize(2))
                    .body("testCase.expected", equalTo("The confirmation is shown and no message is sent."))
                    .body("_links.'vd:screening'.href", equalTo("/api/v1/participants/me/screening"));
            if (english == null) {
                english = response;
            } else {
                assertThat(response.asString()).isEqualTo(english.asString());
            }
        }
    }

    @Test
    void theExampleLinkAppearsOnlyAtTheScreeningStep() {
        String token = participantSession();
        as(token).get(EntryPointController.ONBOARDING).then().body("_links", not(hasKey("vd:screening-example")));
        acceptCurrentConsent(participantIdOf(token), "pt-BR");
        seedSkill(participantIdOf(token), "developer", null);
        as(token).get(EntryPointController.ONBOARDING).then().body("_links", hasKey("vd:screening-example"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"yes_professional", "yes_academic"})
    void aYesAnswerMakesTheParticipantEligibleForParticipation(String answer) {
        String token = atScreeningStep();
        as(token).post(EntryPointController.TASK_RESERVATIONS)
                .then().statusCode(403)
                .body("type", equalTo(PROBLEM + "not-eligible"))
                .body("step", equalTo("screening"));

        putAnswer(token, "en-US", answer)
                .then().statusCode(200)
                .header("Content-Location", equalTo("/api/v1/participants/me/onboarding"))
                .body("answer", equalTo(answer))
                .body("eligible", equalTo(true));

        as(token).get(EntryPointController.ONBOARDING)
                .then().body("step", equalTo("participation"))
                .body("eligible", equalTo(true))
                .body("_links.'vd:next-task'.href", equalTo("/api/v1/tasks/reservations"));
        as(token).post(EntryPointController.TASK_RESERVATIONS).then()
                .statusCode(org.hamcrest.Matchers.anyOf(equalTo(200), equalTo(201)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"pt-BR", "en-US", "es-ES"})
    void theSameCodesAreStoredInEveryLanguage(String locale) {
        String token = atScreeningStep();
        putAnswer(token, locale, "yes_academic").then().statusCode(200);
        assertThat(jdbc.sql("SELECT screening_answer FROM participant WHERE id = ?").param(participantIdOf(token))
                .query(String.class).single()).isEqualTo("yes_academic");
    }

    @Test
    void noIsPermanentAndTheParticipantCannotReserve() {
        String token = atScreeningStep();
        long id = participantIdOf(token);

        putAnswer(token, "pt-BR", "no")
                .then().statusCode(200)
                .body("answer", equalTo("no"))
                .body("eligible", equalTo(false));

        assertThat(jdbc.sql("SELECT eligible FROM participant WHERE id = ?").param(id)
                .query(Boolean.class).single()).isFalse();
        assertThat(step(token)).isEqualTo("ineligible");
        as(token).get(EntryPointController.ONBOARDING)
                .then().body("eligible", equalTo(false)).body("_links", not(hasKey("vd:next-task")));
        as(token).post(EntryPointController.TASK_RESERVATIONS)
                .then().statusCode(403)
                .body("type", equalTo(PROBLEM + "not-eligible"))
                .body("step", equalTo("ineligible"));

        // Returning: nothing changes, not even with a new consent form version.
        insertCompleteConsentForm("v2", clock.instant().minusSeconds(60));
        assertThat(step(token)).isEqualTo("ineligible");
        putAnswer(token, "en-US", "yes_professional")
                .then().statusCode(409)
                .body("type", equalTo(PROBLEM + "step-not-pending"))
                .body("step", equalTo("ineligible"));
        putAnswer(token, "en-US", "no").then().statusCode(200).body("answer", equalTo("no"));
    }

    @Test
    void theScreeningIsNotPendingBeforeConsentAndSkill() {
        String token = participantSession();
        putAnswer(token, "en-US", "yes_professional")
                .then().statusCode(409)
                .body("type", equalTo(PROBLEM + "step-not-pending"))
                .body("step", equalTo("profile"));

        acceptCurrentConsent(participantIdOf(token), "pt-BR");
        putAnswer(token, "en-US", "yes_professional")
                .then().statusCode(409).body("step", equalTo("profile"));
        as(token).get(EntryPointController.SCREENING)
                .then().statusCode(200)
                .body("answer", nullValue())
                .body("eligible", nullValue())
                .body("$", not(hasKey("_templates")));
    }

    @Test
    void anAnsweredScreeningCannotBeChangedButAnIdenticalRepeatIsOk() {
        String token = atScreeningStep();
        putAnswer(token, "pt-BR", "yes_professional").then().statusCode(200);

        putAnswer(token, "pt-BR", "yes_professional").then().statusCode(200).body("answer", equalTo("yes_professional"));
        putAnswer(token, "pt-BR", "yes_academic")
                .then().statusCode(409)
                .body("type", equalTo(PROBLEM + "step-not-pending"))
                .body("step", equalTo("participation"));
        putAnswer(token, "pt-BR", "no").then().statusCode(409);
        as(token).get(EntryPointController.SCREENING).then().body("answer", equalTo("yes_professional"));
    }

    @Test
    void invalidAnswersAreFieldErrors() {
        String token = atScreeningStep();
        as(token).contentType("application/json").body("{}").put(EntryPointController.SCREENING)
                .then().statusCode(400)
                .body("errors.field", contains("answer")).body("errors.code", contains("required"));
        putAnswer(token, "pt-BR", "maybe")
                .then().statusCode(400)
                .body("errors.field", contains("answer")).body("errors.code", contains("option"));
    }

    @Test
    void theTemplateOffersTheThreeCodesOnlyWhileTheStepIsPending() {
        String token = atScreeningStep();
        as(token).get(EntryPointController.SCREENING)
                .then().statusCode(200)
                .body("_templates.default.properties.find { it.name == 'answer' }.options.inline",
                        equalTo(ScreeningAnswer.codes()));
        putAnswer(token, "pt-BR", "yes_academic").then().statusCode(200);
        as(token).get(EntryPointController.SCREENING).then().body("$", not(hasKey("_templates")));
    }
}
