package io.github.diegoquirino.veredito.domain.onboarding;

import java.util.Arrays;
import java.util.Optional;

/** The step a participant is at; the slug is the value of {@code step} in the API. */
public enum OnboardingStep {
    INELIGIBLE("ineligible"),
    NOT_OPEN("not-open"),
    CONSENT("consent"),
    SKILL("skill"),
    SCREENING("screening"),
    PARTICIPATION("participation");

    private final String slug;

    OnboardingStep(String slug) {
        this.slug = slug;
    }

    public String slug() {
        return slug;
    }

    public static Optional<OnboardingStep> fromSlug(String slug) {
        return Arrays.stream(values()).filter(step -> step.slug.equals(slug)).findFirst();
    }
}
