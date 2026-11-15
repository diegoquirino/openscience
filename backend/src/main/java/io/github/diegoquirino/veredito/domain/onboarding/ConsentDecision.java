package io.github.diegoquirino.veredito.domain.onboarding;

import java.util.Arrays;
import java.util.Optional;

/** The decision on the consent form; the code is the value of {@code participant.consent_decision}. */
public enum ConsentDecision {
    ACCEPTED("accepted"),
    REFUSED("refused");

    private final String code;

    ConsentDecision(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    public static Optional<ConsentDecision> fromCode(String code) {
        return Arrays.stream(values()).filter(decision -> decision.code.equals(code)).findFirst();
    }
}
