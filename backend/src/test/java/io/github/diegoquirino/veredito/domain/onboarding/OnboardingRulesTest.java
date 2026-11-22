package io.github.diegoquirino.veredito.domain.onboarding;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** The truth table of the pending step and of the eligibility gate (specs 006 and 013), without Spring. */
class OnboardingRulesTest {

    private static final Instant NOW = Instant.parse("2026-11-15T12:00:00Z");
    private static final long CURRENT = 7L;
    private static final long OLD = 3L;
    private static final String NAME = "Ana Souza";

    private static OnboardingState consent(ConsentDecision decision, long formId) {
        return OnboardingState.empty().withConsent(formId, decision, "pt-BR", NOW);
    }

    @Test
    void newParticipantWithoutNameStartsAtProfile() {
        assertThat(OnboardingRules.stepOf(OnboardingState.empty(), CURRENT, null))
                .isEqualTo(OnboardingStep.PROFILE);
        assertThat(OnboardingRules.stepOf(OnboardingState.empty(), CURRENT, NAME))
                .isEqualTo(OnboardingStep.PROFILE);
    }

    @Test
    void noCurrentFormMeansNotOpen() {
        assertThat(OnboardingRules.stepOf(OnboardingState.empty(), null, NAME)).isEqualTo(OnboardingStep.NOT_OPEN);
        assertThat(OnboardingRules.stepOf(consent(ConsentDecision.ACCEPTED, CURRENT), null, NAME))
                .isEqualTo(OnboardingStep.NOT_OPEN);
    }

    @Test
    void profileBeforeConsentEvenWhenConsentWouldOtherwiseBeDue() {
        assertThat(OnboardingRules.stepOf(consent(ConsentDecision.REFUSED, CURRENT), CURRENT, null))
                .isEqualTo(OnboardingStep.PROFILE);
        OnboardingState named = consent(ConsentDecision.REFUSED, CURRENT)
                .withSkill("developer", null)
                .withOrganization("ufcg", null);
        assertThat(OnboardingRules.stepOf(named, CURRENT, NAME)).isEqualTo(OnboardingStep.CONSENT);
        assertThat(OnboardingRules.stepOf(
                        consent(ConsentDecision.REFUSED, CURRENT).withSkill("developer", null), CURRENT, NAME))
                .isEqualTo(OnboardingStep.PROFILE);
    }

    @Test
    void namedWithSkillAndOrganizationThenConsentThenScreeningThenParticipation() {
        OnboardingState withProfile = OnboardingState.empty()
                .withSkill("developer", null)
                .withOrganization("tjpb", null);
        assertThat(OnboardingRules.stepOf(withProfile, CURRENT, NAME)).isEqualTo(OnboardingStep.CONSENT);

        OnboardingState accepted = withProfile.withConsent(CURRENT, ConsentDecision.ACCEPTED, "pt-BR", NOW);
        assertThat(OnboardingRules.stepOf(accepted, CURRENT, NAME)).isEqualTo(OnboardingStep.SCREENING);

        OnboardingState screened = accepted.withScreening(ScreeningAnswer.YES_ACADEMIC);
        assertThat(OnboardingRules.stepOf(screened, CURRENT, NAME)).isEqualTo(OnboardingStep.PARTICIPATION);
    }

    @Test
    void noIsPermanentAndWinsOverEveryOtherStep() {
        OnboardingState no = consent(ConsentDecision.ACCEPTED, CURRENT)
                .withSkill("other", "Writer")
                .withOrganization("other", "Acme")
                .withScreening(ScreeningAnswer.NO);
        assertThat(OnboardingRules.stepOf(no, CURRENT, NAME)).isEqualTo(OnboardingStep.INELIGIBLE);
        assertThat(OnboardingRules.stepOf(no, null, NAME)).isEqualTo(OnboardingStep.INELIGIBLE);
        assertThat(OnboardingRules.isEligible(no, CURRENT, NAME)).isFalse();
    }

    @Test
    void newVersionAfterAcceptanceAsksOnlyConsentAgain() {
        OnboardingState done = consent(ConsentDecision.ACCEPTED, OLD)
                .withSkill("test_analyst", null)
                .withOrganization("ufcg", null)
                .withScreening(ScreeningAnswer.YES_PROFESSIONAL);
        assertThat(OnboardingRules.stepOf(done, OLD, NAME)).isEqualTo(OnboardingStep.PARTICIPATION);
        assertThat(OnboardingRules.stepOf(done, CURRENT, NAME)).isEqualTo(OnboardingStep.CONSENT);
        assertThat(OnboardingRules.isEligible(done, CURRENT, NAME)).isFalse();

        OnboardingState reaccepted = done.withConsent(CURRENT, ConsentDecision.ACCEPTED, "en-US", NOW);
        assertThat(OnboardingRules.stepOf(reaccepted, CURRENT, NAME)).isEqualTo(OnboardingStep.PARTICIPATION);
        assertThat(OnboardingRules.isEligible(reaccepted, CURRENT, NAME)).isTrue();
    }

    @Test
    void eligibleOnlyWithNameSkillOrganizationCurrentAcceptanceAndYesScreening() {
        List<Long> forms = new ArrayList<>();
        forms.add(null);
        forms.add(CURRENT);
        List<String> names = new ArrayList<>();
        names.add(null);
        names.add(NAME);
        List<OnboardingState> consents = List.of(
                OnboardingState.empty(),
                consent(ConsentDecision.REFUSED, CURRENT),
                consent(ConsentDecision.ACCEPTED, OLD),
                consent(ConsentDecision.ACCEPTED, CURRENT));
        List<String> skills = new ArrayList<>();
        skills.add(null);
        skills.add("developer");
        List<String> organizations = new ArrayList<>();
        organizations.add(null);
        organizations.add("ufcg");
        List<ScreeningAnswer> answers = new ArrayList<>();
        answers.add(null);
        answers.addAll(List.of(ScreeningAnswer.values()));

        int eligible = 0;
        int combinations = 0;
        for (Long form : forms) {
            for (String name : names) {
                for (OnboardingState consent : consents) {
                    for (String skill : skills) {
                        for (String organization : organizations) {
                            for (ScreeningAnswer answer : answers) {
                                OnboardingState state = consent
                                        .withSkill(skill, null)
                                        .withOrganization(organization, null)
                                        .withScreening(answer);
                                boolean expected = form != null
                                        && name != null
                                        && state.consentDecision() == ConsentDecision.ACCEPTED
                                        && CURRENT == (state.consentFormId() == null ? -1 : state.consentFormId())
                                        && skill != null
                                        && organization != null
                                        && answer != null
                                        && answer.eligible();
                                assertThat(OnboardingRules.isEligible(state, form, name))
                                        .as(
                                                "form=%s name=%s consent=%s skill=%s org=%s answer=%s",
                                                form,
                                                name,
                                                consent,
                                                skill,
                                                organization,
                                                answer)
                                        .isEqualTo(expected);
                                combinations++;
                                if (expected) {
                                    eligible++;
                                }
                            }
                        }
                    }
                }
            }
        }
        assertThat(combinations).isEqualTo(2 * 2 * 4 * 2 * 2 * 4);
        assertThat(eligible).isEqualTo(2);
    }
}
