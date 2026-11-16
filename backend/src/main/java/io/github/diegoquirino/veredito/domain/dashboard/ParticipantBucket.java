package io.github.diegoquirino.veredito.domain.dashboard;

/** Mutually exclusive participant status (research section 5); anonymized participants are never counted. */
public enum ParticipantBucket {
    DECLINED_CONSENT,
    INELIGIBLE,
    ELIGIBLE,
    INCOMPLETE;

    /**
     * Classifies from the onboarding columns: {@code consent_decision}, {@code screening_answer} and
     * {@code eligible} (each may be {@code null}).
     */
    public static ParticipantBucket of(String consentDecision, String screeningAnswer, Boolean eligible) {
        if ("refused".equals(consentDecision)) {
            return DECLINED_CONSENT;
        }
        if ("no".equals(screeningAnswer) || Boolean.FALSE.equals(eligible)) {
            return INELIGIBLE;
        }
        return Boolean.TRUE.equals(eligible) ? ELIGIBLE : INCOMPLETE;
    }
}