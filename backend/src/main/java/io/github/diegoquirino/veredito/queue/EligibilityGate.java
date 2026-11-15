package io.github.diegoquirino.veredito.queue;

import io.github.diegoquirino.veredito.api.ProblemException;
import io.github.diegoquirino.veredito.api.ProblemType;
import java.util.Optional;

/**
 * Single point for FR-007 of spec 006 (and FR-012 of spec 007): the answer to "may this participant
 * request, save or submit blocks?". The implementation is the database-backed
 * {@code OnboardingEligibilityGate}.
 */
public interface EligibilityGate {

    boolean isEligible(long participantId);

    /** The slug of the pending onboarding step, for the {@code step} extension of {@code not-eligible}. */
    default Optional<String> pendingStep(long participantId) {
        return Optional.empty();
    }

    /** The {@code 403 not-eligible} to throw when {@link #isEligible} is false. */
    default ProblemException refusal(long participantId) {
        ProblemException refusal = new ProblemException(ProblemType.NOT_ELIGIBLE);
        pendingStep(participantId).ifPresent(step -> refusal.with("step", step));
        return refusal;
    }
}
