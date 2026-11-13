package io.github.diegoquirino.veredito.api.v1;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.diegoquirino.veredito.access.SessionView;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import org.springframework.hateoas.RepresentationModel;

@Schema(name = "Session", description = "Who is signed in, the profile language and when the session ends.")
public class SessionModel extends RepresentationModel<SessionModel> {

    private final String subject;
    private final String name;
    private final String pseudonym;
    private final String preferredLanguage;
    private final Instant expiresAt;

    public SessionModel(SessionView view) {
        this.subject = view.subject().code();
        this.name = view.name();
        this.pseudonym = view.pseudonym();
        this.preferredLanguage = view.preferredLanguage();
        this.expiresAt = view.expiresAt();
    }

    @Schema(description = "participant or administrator.", example = "participant")
    public String getSubject() {
        return subject;
    }

    @Schema(description = "The latest name typed at sign-in.", example = "Ana Souza")
    public String getName() {
        return name;
    }

    @Schema(description = "The participant's pseudonym; absent for the administrator.", example = "R-0001")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getPseudonym() {
        return pseudonym;
    }

    @Schema(description = "pt-BR, en-US or es-ES; the client opens the screens in this language.", example = "es-ES")
    public String getPreferredLanguage() {
        return preferredLanguage;
    }

    @Schema(description = "End of the session, 24 hours after sign-in (ISO 8601, UTC).", example = "2026-11-14T17:03:00Z")
    public Instant getExpiresAt() {
        return expiresAt;
    }
}
