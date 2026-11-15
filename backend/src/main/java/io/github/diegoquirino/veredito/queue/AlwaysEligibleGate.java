package io.github.diegoquirino.veredito.queue;

import org.springframework.stereotype.Component;

/** Stub until consent and screening (spec 006) replace this bean. */
@Component
public class AlwaysEligibleGate implements EligibilityGate {

    @Override
    public boolean isEligible(long participantId) {
        return true;
    }
}
