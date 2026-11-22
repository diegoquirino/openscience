package io.github.diegoquirino.veredito.domain.onboarding;

/** Which step is pending and who is eligible (research section 2); the only place these rules live. */
public final class OnboardingRules {

    private OnboardingRules() {}

    /**
     * The first step that holds: {@code ineligible}, {@code not-open}, {@code profile}, {@code consent},
     * {@code screening}, then {@code participation}. {@code ineligible} is checked first on purpose: a
     * participant who answered "no" always sees the same closing message. Name and skill come before
     * the consent form (spec 013).
     *
     * @param currentFormId id of the current consent form, or {@code null} when there is none
     * @param name display name, or {@code null}/blank when the profile step is still pending
     */
    public static OnboardingStep stepOf(OnboardingState state, Long currentFormId, String name) {
        if (state.screeningAnswer() == ScreeningAnswer.NO) {
            return OnboardingStep.INELIGIBLE;
        }
        if (currentFormId == null) {
            return OnboardingStep.NOT_OPEN;
        }
        if (name == null
                || name.isBlank()
                || state.professionalSkill() == null
                || state.organization() == null) {
            return OnboardingStep.PROFILE;
        }
        if (!state.acceptedForm(currentFormId)) {
            return OnboardingStep.CONSENT;
        }
        if (state.screeningAnswer() == null) {
            return OnboardingStep.SCREENING;
        }
        return OnboardingStep.PARTICIPATION;
    }

    /**
     * Eligible only with name, skill, the current form accepted and a {@code yes_*} screening answer.
     */
    public static boolean isEligible(OnboardingState state, Long currentFormId, String name) {
        return stepOf(state, currentFormId, name) == OnboardingStep.PARTICIPATION
                && state.screeningAnswer() != null
                && state.screeningAnswer().eligible();
    }
}
