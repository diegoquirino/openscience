package io.github.diegoquirino.veredito.domain.onboarding;

import java.time.Instant;

/**
 * What is stored about a participant's onboarding (the columns written by spec 006). Every field is
 * {@code null} until the participant answers; the four consent fields are always set together.
 */
public record OnboardingState(
        Long consentFormId,
        ConsentDecision consentDecision,
        String consentLanguage,
        Instant consentDecidedAt,
        String professionalSkill,
        String professionalSkillOther,
        ScreeningAnswer screeningAnswer) {

    /** A participant who has not done anything yet. */
    public static OnboardingState empty() {
        return new OnboardingState(null, null, null, null, null, null, null);
    }

    public OnboardingState withConsent(
            long formId, ConsentDecision decision, String language, Instant decidedAt) {
        return new OnboardingState(
                formId, decision, language, decidedAt, professionalSkill, professionalSkillOther, screeningAnswer);
    }

    public OnboardingState withSkill(String skill, String other) {
        return new OnboardingState(
                consentFormId, consentDecision, consentLanguage, consentDecidedAt, skill, other, screeningAnswer);
    }

    public OnboardingState withScreening(ScreeningAnswer answer) {
        return new OnboardingState(
                consentFormId, consentDecision, consentLanguage, consentDecidedAt,
                professionalSkill, professionalSkillOther, answer);
    }

    /** True when the last decision is an acceptance of exactly this form. */
    public boolean acceptedForm(Long currentFormId) {
        return currentFormId != null
                && consentDecision == ConsentDecision.ACCEPTED
                && currentFormId.equals(consentFormId);
    }
}
