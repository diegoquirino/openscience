package io.github.diegoquirino.veredito.api.v1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;

import io.github.diegoquirino.veredito.support.ApiTestBase;
import io.restassured.response.Response;
import java.util.Map;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Spec 006 SC-001, SC-004 and SC-005: a new participant goes through the three steps by following links, in
 * each language, and the stored codes are the same.
 */
class OnboardingFlowTest extends ApiTestBase {

    @ParameterizedTest
    @ValueSource(strings = {"pt-BR", "en-US", "es-ES"})
    void aNewParticipantReachesParticipationFollowingLinksOnly(String locale) {
        String email = "flow-" + System.nanoTime() + "@example.com";
        String token = signIn("Ana Souza", email, locale);

        Response onboarding = as(token).get(href(as(token).get("/api/v1"), "onboarding"));
        onboarding.then().body("step", equalTo("consent")).body("eligible", equalTo(false));

        Response form = as(token).header("Accept-Language", locale).get(href(onboarding, "consent-form"));
        form.then().statusCode(200).body("language", equalTo(locale)).body("version", equalTo("fixture-1"));
        String version = form.path("version");
        as(token).header("Accept-Language", locale).contentType("application/json")
                .body(Map.of("decision", "accepted", "consentFormVersion", version))
                .put(href(form, "consent"))
                .then().statusCode(200).body("language", equalTo(locale));

        onboarding = as(token).get(href(onboarding, "self"));
        onboarding.then().body("step", equalTo("skill"));
        as(token).header("Accept-Language", locale).contentType("application/json")
                .body(Map.of("skill", "other", "skillOther", "Redator"))
                .put(href(onboarding, "skill"))
                .then().statusCode(200);

        onboarding = as(token).get(href(onboarding, "self"));
        onboarding.then().body("step", equalTo("screening"));
        Response example = as(token).get(href(onboarding, "screening-example"));
        example.then().statusCode(200).header("Content-Language", equalTo("en-US"));
        as(token).header("Accept-Language", locale).contentType("application/json")
                .body(Map.of("answer", "yes_professional"))
                .put(href(example, "screening"))
                .then().statusCode(200).body("eligible", equalTo(true));

        as(token).get(href(onboarding, "self"))
                .then().body("step", equalTo("participation")).body("eligible", equalTo(true));
        Response entry = as(token).get("/api/v1");
        as(token).post(href(entry, "next-task")).then().statusCode(org.hamcrest.Matchers.anyOf(equalTo(200), equalTo(201)));

        Map<String, Object> row = jdbc.sql("""
                        SELECT consent_decision, consent_language, professional_skill, professional_skill_other,
                               screening_answer, eligible
                        FROM participant WHERE email = ?""")
                .param(normalized(email)).query().singleRow();
        assertThat(row)
                .containsEntry("consent_decision", "accepted")
                .containsEntry("consent_language", locale)
                .containsEntry("professional_skill", "other")
                .containsEntry("professional_skill_other", "Redator")
                .containsEntry("screening_answer", "yes_professional")
                .containsEntry("eligible", true);
    }

    @ParameterizedTest
    @ValueSource(strings = {"pt-BR", "en-US", "es-ES"})
    void theParticipantResumesAtTheFirstPendingStep(String locale) {
        String token = signIn("Ana Souza", "resume-" + System.nanoTime() + "@example.com", locale);
        long id = participantIdOf(token);
        as(token).get("/api/v1").then().body("_links", not(hasKey("vd:next-task")));

        acceptCurrentConsent(id, locale);
        as(token).get(EntryPointController.ONBOARDING).then().body("step", equalTo("skill"));
        as(token).get("/api/v1").then().body("_links", not(hasKey("vd:next-task")));

        seedSkill(id, "developer", null);
        as(token).get(EntryPointController.ONBOARDING).then().body("step", equalTo("screening"));

        seedScreening(id, io.github.diegoquirino.veredito.domain.onboarding.ScreeningAnswer.YES_ACADEMIC);
        as(token).get(EntryPointController.ONBOARDING).then().body("step", equalTo("participation"));
        as(token).get("/api/v1").then().body("_links", hasKey("vd:next-task"));
    }
}