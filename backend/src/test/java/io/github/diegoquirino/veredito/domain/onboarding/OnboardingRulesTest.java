package io.github.diegoquirino.veredito.domain.onboarding;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** The truth table of the pending step and of the eligibility gate (spec 006, SC-002), without Spring. */
class OnboardingRulesTest {

    private static final Instant NOW = Instant.parse("2026-11-15T12:00:00Z");
    private static final long CURRENT = 7L;
    private static final long OLD = 3L;

    private static OnboardingState consent(ConsentDecision decision, long formId) {
        return OnboardingState.empty().withConsent(formId, decision, "pt-BR", NOW);
    }

    @Test
    void newParticipantStartsAtConsent() {
        assertThat(OnboardingRules.stepOf(OnboardingState.empty(), CURRENT)).isEqualTo(OnboardingStep.CONSENT);
    }

    @Test
    void noCurrentFormMeansNotOpen() {
        assertThat(OnboardingRules.stepOf(OnboardingState.empty(), null)).isEqualTo(OnboardingStep.NOT_OPEN);
        assertThat(OnboardingRules.stepOf(consent(ConsentDecision.ACCEPTED, CURRENT), null))
                .isEqualTo(OnboardingStep.NOT_OPEN);
    }

    @Test
    void refusedOrOlderAcceptanceSendsBackToConsent() {
        assertThat(OnboardingRules.stepOf(consent(ConsentDecision.REFUSED, CURRENT), CURRENT))
                .isEqualTo(OnboardingStep.CONSENT);
        assertThat(OnboardingRules.stepOf(consent(ConsentDecision.ACCEPTED, OLD), CURRENT))
                .isEqualTo(OnboardingStep.CONSENT);
        assertThat(OnboardingRules.stepOf(consent(ConsentDecision.REFUSED, OLD), CURRENT))
                .isEqualTo(OnboardingStep.CONSENT);
    }

    @Test
    void acceptedCurrentFormThenSkillThenScreeningThenParticipation() {
        OnboardingState accepted = consent(ConsentDecision.ACCEPTED, CURRENT);
        assertThat(OnboardingRules.stepOf(accepted, CURRENT)).isEqualTo(OnboardingStep.SKILL);

        OnboardingState withSkill = accepted.withSkill("developer", null);
        assertThat(OnboardingRules.stepOf(withSkill, CURRENT)).isEqualTo(OnboardingStep.SCREENING);

        OnboardingState screened = withSkill.withScreening(ScreeningAnswer.YES_ACADEMIC);
        assertThat(OnboardingRules.stepOf(screened, CURRENT)).isEqualTo(OnboardingStep.PARTICIPATION);
    }

    @Test
    void noIsPermanentAndWinsOverEveryOtherStep() {
        OnboardingState no = consent(ConsentDecision.ACCEPTED, CURRENT)
                .withSkill("other", "Writer")
                .withScreening(ScreeningAnswer.NO);
        assertThat(OnboardingRules.stepOf(no, CURRENT)).isEqualTo(OnboardingStep.INELIGIBLE);
        assertThat(OnboardingRules.stepOf(no, null)).isEqualTo(OnboardingStep.INELIGIBLE);
        assertThat(OnboardingRules.stepOf(no, 99L)).isEqualTo(OnboardingStep.INELIGIBLE);
        assertThat(OnboardingRules.isEligible(no, CURRENT)).isFalse();
    }

    @Test
    void newVersionAfterAcceptanceAsksOnlyConsentAgain() {
        OnboardingState done = consent(ConsentDecision.ACCEPTED, OLD)
                .withSkill("test_analyst", null)
                .withScreening(ScreeningAnswer.YES_PROFESSIONAL);
        assertThat(OnboardingRules.stepOf(done, OLD)).isEqualTo(OnboardingStep.PARTICIPATION);
        assertThat(OnboardingRules.stepOf(done, CURRENT)).isEqualTo(OnboardingStep.CONSENT);
        assertThat(OnboardingRules.isEligible(done, CURRENT)).isFalse();

        OnboardingState reaccepted = done.withConsent(CURRENT, ConsentDecision.ACCEPTED, "en-US", NOW);
        assertThat(OnboardingRules.stepOf(reaccepted, CURRENT)).isEqualTo(OnboardingStep.PARTICIPATION);
        assertThat(OnboardingRules.isEligible(reaccepted, CURRENT)).isTrue();
    }

    @Test
    void refuseThenReturnAndAccept() {
        OnboardingState refused = consent(ConsentDecision.REFUSED, CURRENT);
        assertThat(OnboardingRules.stepOf(refused, CURRENT)).isEqualTo(OnboardingStep.CONSENT);
        OnboardingState changedMind = refused.withConsent(CURRENT, ConsentDecision.ACCEPTED, "en-US", NOW);
        assertThat(OnboardingRules.stepOf(changedMind, CURRENT)).isEqualTo(OnboardingStep.SKILL);
    }

    /** Every combination of form, consent, skill and screening: eligible only in the single good one. */
    @Test
    void eligibleOnlyWithCurrentAcceptanceSkillAndYesScreening() {
        List<Long> forms = new ArrayList<>();
        forms.add(null);
        forms.add(CURRENT);
        List<OnboardingState> consents = List.of(
                OnboardingState.empty(),
                consent(ConsentDecision.REFUSED, CURRENT),
                consent(ConsentDecision.ACCEPTED, OLD),
                consent(ConsentDecision.ACCEPTED, CURRENT));
        List<String> skills = new ArrayList<>();
        skills.add(null);
        skills.add("developer");
        List<ScreeningAnswer> answers = new ArrayList<>();
        answers.add(null);
        answers.addAll(List.of(ScreeningAnswer.values()));

        int eligible = 0;
        int combinations = 0;
        for (Long form : forms) {
            for (OnboardingState consent : consents) {
                for (String skill : skills) {
                    for (ScreeningAnswer answer : answers) {
                        OnboardingState state = consent.withSkill(skill, null).withScreening(answer);
                        boolean expected = form != null
                                && state.consentDecision() == ConsentDecision.ACCEPTED
                                && CURRENT == (state.consentFormId() == null ? -1 : state.consentFormId())
                                && skill != null
                                && answer != null
                                && answer.eligible();
                        assertThat(OnboardingRules.isEligible(state, form))
                                .as("form=%s consent=%s skill=%s answer=%s", form, consent, skill, answer)
                                .isEqualTo(expected);
                        combinations++;
                        if (expected) {
                            eligible++;
                        }
                    }
                }
            }
        }
        assertThat(combinations).isEqualTo(2 * 4 * 2 * 4);
        assertThat(eligible).isEqualTo(2);
    }
}
