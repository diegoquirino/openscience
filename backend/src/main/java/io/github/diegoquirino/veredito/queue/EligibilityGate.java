package io.github.diegoquirino.veredito.queue;

/**
 * Single point for FR-012. Until spec 006 exists, every participant with a session is eligible
 * ({@link AlwaysEligibleGate}).
 */
public interface EligibilityGate {

    boolean isEligible(long participantId);
}
