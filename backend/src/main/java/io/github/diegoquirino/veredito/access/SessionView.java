package io.github.diegoquirino.veredito.access;

import io.github.diegoquirino.veredito.domain.access.Subject;
import io.github.diegoquirino.veredito.persistence.ConfigRepository.AdminProfile;
import io.github.diegoquirino.veredito.persistence.ParticipantRepository.ParticipantProfile;
import java.time.Instant;

/** Who is signed in, as {@code vd:session} shows it; {@code pseudonym} is {@code null} for the administrator. */
public record SessionView(Subject subject, String name, String pseudonym, String preferredLanguage, Instant expiresAt) {

    static SessionView participant(ParticipantProfile profile, Instant expiresAt) {
        return new SessionView(
                Subject.PARTICIPANT, profile.name(), profile.pseudonym(), profile.preferredLanguage(), expiresAt);
    }

    static SessionView administrator(AdminProfile profile, Instant expiresAt) {
        return new SessionView(Subject.ADMINISTRATOR, profile.name(), null, profile.preferredLanguage(), expiresAt);
    }
}
