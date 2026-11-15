package io.github.diegoquirino.veredito.domain.onboarding;

/** Which step is pending and who is eligible (research section 2); the only place these rules live. */
public final class OnboardingRules {

    private OnboardingRules() {}

    /**
     * The first step that holds: {@code ineligible}, {@code not-open}, {@code consent}, {@code skill},
     * {@code screening}, then {@code participation}. {@code ineligible} is checked first on purpose: a
     * participant who answered "no" always sees the same closing message.
     *
     * @param currentFormId id of the current consent form, or {@code null} when there is none
     */
    public static OnboardingStep stepOf(OnboardingState state, Long currentFormId) {
        if (state.screeningAnswer() == ScreeningAnswer.NO) {
            return OnboardingStep.INELIGIBLE;
        }
        if (currentFormId == null) {
            return OnboardingStep.NOT_OPEN;
        }
        if (!state.acceptedForm(currentFormId)) {
            return OnboardingStep.CONSENT;
        }
        if (state.professionalSkill() == null) {
            return OnboardingStep.SKILL;
        }
        if (state.screeningAnswer() == null) {
            return OnboardingStep.SCREENING;
        }
        return OnboardingStep.PARTICIPATION;
    }

    /**
     * Eligible only with the current form accepted, a skill set and a {@code yes_*} screening answer; the
     * truth table of the gate (FR-007, SC-002).
     */
    public static boolean isEligible(OnboardingState state, Long currentFormId) {
        return stepOf(state, currentFormId) == OnboardingStep.PARTICIPATION
                && state.screeningAnswer() != null
                && state.screeningAnswer().eligible();
    }
}
