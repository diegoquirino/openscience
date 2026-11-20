package io.github.diegoquirino.veredito.access;

import io.github.diegoquirino.veredito.api.LocaleConfig;
import io.github.diegoquirino.veredito.api.ProblemException;
import io.github.diegoquirino.veredito.api.ProblemType;
import io.github.diegoquirino.veredito.domain.access.PersonName;
import io.github.diegoquirino.veredito.persistence.ConfigRepository;
import io.github.diegoquirino.veredito.persistence.ConfigRepository.AdminProfile;
import io.github.diegoquirino.veredito.persistence.ParticipantRepository;
import io.github.diegoquirino.veredito.persistence.ParticipantRepository.ParticipantProfile;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;

/** {@code vd:me}: the own profile of the participant or of the administrator, and its language. */
@Service
public class ProfileService {

    private static final String LANGUAGE_FIELD = "preferredLanguage";

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

    public ParticipantProfile changeParticipantLanguage(VereditoPrincipal principal, String rawLanguage) {
        participants.updatePreferredLanguage(principal.participantId(), language(rawLanguage));
        return participant(principal);
    }

    public AdminProfile administrator() {
        return config.adminProfile();
    }

    public AdminProfile changeAdministratorLanguage(String rawLanguage) {
        config.updateAdminPreferredLanguage(language(rawLanguage));
        return administrator();
    }

    /**
     * Updates administrator name and/or language. Language-only keeps the header switch working;
     * name-only supports the profile menu (spec 014).
     */
    public AdminProfile updateAdministrator(String rawName, String rawLanguage) {
        Map<String, String> errors = new LinkedHashMap<>();
        Optional<PersonName> name = Optional.empty();
        if (rawName != null) {
            name = PersonName.parse(rawName);
            if (name.isEmpty()) {
                errors.put("name", rawName.isBlank() ? "required" : "size");
            }
        }
        String lang = null;
        if (rawLanguage != null) {
            if (rawLanguage.isBlank()) {
                errors.put(LANGUAGE_FIELD, "required");
            } else if (!LocaleConfig.SUPPORTED_TAGS.contains(rawLanguage)) {
                errors.put(LANGUAGE_FIELD, "locale");
            } else {
                lang = rawLanguage;
            }
        }
        if (rawName == null && rawLanguage == null) {
            errors.put(LANGUAGE_FIELD, "required");
        }
        if (!errors.isEmpty()) {
            throw ProblemException.invalidFields(errors);
        }
        name.ifPresent(config::updateAdminName);
        if (lang != null) {
            config.updateAdminPreferredLanguage(lang);
        }
        return administrator();
    }

    private static String language(String raw) {
        if (raw == null || raw.isBlank()) {
            throw ProblemException.invalidFields(Map.of(LANGUAGE_FIELD, "required"));
        }
        if (!LocaleConfig.SUPPORTED_TAGS.contains(raw)) {
            throw ProblemException.invalidFields(Map.of(LANGUAGE_FIELD, "locale"));
        }
        return raw;
    }
}
