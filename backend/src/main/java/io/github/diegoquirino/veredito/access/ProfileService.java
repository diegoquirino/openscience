package io.github.diegoquirino.veredito.access;

import io.github.diegoquirino.veredito.api.ProblemException;
import io.github.diegoquirino.veredito.api.ProblemType;
import io.github.diegoquirino.veredito.persistence.ConfigRepository;
import io.github.diegoquirino.veredito.persistence.ConfigRepository.AdminProfile;
import io.github.diegoquirino.veredito.persistence.ParticipantRepository;
import io.github.diegoquirino.veredito.persistence.ParticipantRepository.ParticipantProfile;
import org.springframework.stereotype.Service;

/** {@code vd:me}: the own profile of the participant or of the administrator. */
@Service
public class ProfileService {

    private final ParticipantRepository participants;
    private final ConfigRepository config;

    public ProfileService(ParticipantRepository participants, ConfigRepository config) {
        this.participants = participants;
        this.config = config;
    }

    /** The authorization rules let only participant sessions reach this call. */
    public ParticipantProfile participant(VereditoPrincipal principal) {
        return participants.findById(principal.participantId())
                .orElseThrow(() -> new ProblemException(ProblemType.AUTHENTICATION_REQUIRED));
    }

    public AdminProfile administrator() {
        return config.adminProfile();
    }
}
