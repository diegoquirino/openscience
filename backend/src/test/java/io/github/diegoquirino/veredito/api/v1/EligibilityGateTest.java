package io.github.diegoquirino.veredito.api.v1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

import io.github.diegoquirino.veredito.domain.onboarding.ConsentDecision;
import io.github.diegoquirino.veredito.domain.onboarding.ScreeningAnswer;
import io.github.diegoquirino.veredito.queue.EligibilityGate;
import io.github.diegoquirino.veredito.support.ApiTestBase;
import io.restassured.response.Response;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Spec 006 SC-002 and FR-007: the single gate refuses every non-eligible combination on a direct call to
 * the Tasks operations, with the pending step in the answer.
 */
class EligibilityGateTest extends ApiTestBase {

    private static final String PROBLEM = "urn:veredito:problem:";

    @Autowired
    EligibilityGate gate;

    private void assertRefusedEverywhere(String token, String expectedStep) {
        as(token).post(EntryPointController.TASK_RESERVATIONS)
                .then().statusCode(403)
                .body("type", equalTo(PROBLEM + "not-eligible"))
                .body("step", equalTo(expectedStep));
        as(token).contentType("application/json")
                .body(Map.of("changeImpact", "LOW", "maintenanceOperation", "KEEP"))
                .put("/api/v1/tasks/1/responses/1")
                .then().statusCode(403).body("type", equalTo(PROBLEM + "not-eligible"));
        as(token).post("/api/v1/tasks/1/submission")
                .then().statusCode(403).body("type", equalTo(PROBLEM + "not-eligible"));
    }

    @Test
    void aNewParticipantIsRefused() {
        String token = participantSession();
        assertThat(gate.isEligible(participantIdOf(token))).isFalse();
        assertRefusedEverywhere(token, "consent");
    }

    @Test
    void aRefusalOfTheConsentFormIsRefused() {
        String token = participantSession();
        recordConsent(participantIdOf(token), ConsentDecision.REFUSED, "pt-BR");
        assertRefusedEverywhere(token, "consent");
    }

    @Test
    void consentWithoutSkillOrScreeningIsRefused() {
        String token = participantSession();
        long id = participantIdOf(token);
        acceptCurrentConsent(id, "en-US");
        assertRefusedEverywhere(token, "skill");
        seedSkill(id, "tech_lead", null);
        assertRefusedEverywhere(token, "screening");
        assertThat(gate.isEligible(id)).isFalse();
    }

    @Test
    void aNoAnswerIsRefusedForever() {
        String token = participantSession();
        long id = participantIdOf(token);
        acceptCurrentConsent(id, "pt-BR");
        seedSkill(id, "developer", null);
        seedScreening(id, ScreeningAnswer.NO);
        assertRefusedEverywhere(token, "ineligible");
        assertThat(gate.isEligible(id)).isFalse();
    }

    @Test
    void anEligibleParticipantPassesTheGate() {
        String token = eligibleParticipantSession();
        assertThat(gate.isEligible(participantIdOf(token))).isTrue();
        Response reserve = as(token).post(EntryPointController.TASK_RESERVATIONS);
        reserve.then().statusCode(org.hamcrest.Matchers.anyOf(equalTo(200), equalTo(201)));
    }

    @Test
    void aNewConsentVersionTakesEffectOnTheNextRequestWithoutARestart() {
        String token = eligibleParticipantSession();
        long id = participantIdOf(token);
        assertThat(gate.isEligible(id)).isTrue();

        insertCompleteConsentForm("v2", clock.instant().minus(Duration.ofMinutes(1)));

        assertThat(gate.isEligible(id)).isFalse();
        assertRefusedEverywhere(token, "consent");
    }

    @Test
    void whenTheStudyClosesNoOneIsEligible() {
        String token = eligibleParticipantSession();
        jdbc.sql("UPDATE consent_form SET published_at = NULL").update();
        assertRefusedEverywhere(token, "not-open");
    }
}
