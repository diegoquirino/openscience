package io.github.diegoquirino.veredito.api.v1;

import io.github.diegoquirino.veredito.persistence.ParticipantRepository.ParticipantProfile;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import org.springframework.hateoas.RepresentationModel;

@Schema(name = "ParticipantProfile", description = "The participant's own profile.")
public class ParticipantProfileModel extends RepresentationModel<ParticipantProfileModel> {

    private final String pseudonym;
    private final String name;
    private final String preferredLanguage;
    private final Instant registeredAt;

    public ParticipantProfileModel(ParticipantProfile profile) {
        this.pseudonym = profile.pseudonym();
        this.name = profile.name();
        this.preferredLanguage = profile.preferredLanguage();
        this.registeredAt = profile.registeredAt();
    }

    @Schema(description = "R- followed by at least four digits, given at the first sign-in.", example = "R-0001")
    public String getPseudonym() {
        return pseudonym;
    }

    @Schema(description = "The latest name typed at sign-in.", example = "Ana Souza")
    public String getName() {
        return name;
    }

    @Schema(description = "pt-BR, en-US or es-ES; the client opens the screens in this language.", example = "es-ES")
    public String getPreferredLanguage() {
        return preferredLanguage;
    }

    @Schema(description = "First sign-in (ISO 8601, UTC).", example = "2026-11-13T17:03:00Z")
    public Instant getRegisteredAt() {
        return registeredAt;
    }
}
