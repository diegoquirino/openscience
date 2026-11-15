package io.github.diegoquirino.veredito.onboarding;

import io.github.diegoquirino.veredito.queue.EligibilityGate;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Database-backed {@link EligibilityGate} of spec 006 (research section 2): eligible only with the current
 * consent form accepted, a skill set and a {@code yes_*} screening answer. Nothing is cached, so a form
 * published in the database applies on the next request.
 */
@Component
public class OnboardingEligibilityGate implements EligibilityGate {

    private final OnboardingService onboarding;

    public OnboardingEligibilityGate(OnboardingService onboarding) {
        this.onboarding = onboarding;
    }

    @Override
    public boolean isEligible(long participantId) {
        return onboarding.of(participantId).eligible();
    }

    @Override
    public Optional<String> pendingStep(long participantId) {
        return Optional.of(onboarding.of(participantId).step().slug());
    }
}
