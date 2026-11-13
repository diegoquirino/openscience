package io.github.diegoquirino.veredito.access;

import io.github.diegoquirino.veredito.domain.access.Subject;
import java.time.Instant;

/** Who a valid session belongs to; the participant fields are {@code null} for the administrator. */
public record VereditoPrincipal(
        Subject subject, Long participantId, String pseudonym, Instant expiresAt, String tokenHash) {

    public boolean isAdministrator() {
        return subject == Subject.ADMINISTRATOR;
    }

    /** The role checked by the authorization rules: {@code PARTICIPANT} or {@code ADMINISTRATOR}. */
    public String role() {
        return subject.name();
    }

    @Override
    public String toString() {
        return "VereditoPrincipal[" + subject.code() + (pseudonym == null ? "" : " " + pseudonym) + "]";
    }
}
