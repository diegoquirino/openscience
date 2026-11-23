package io.github.diegoquirino.veredito.api.v1;

import io.github.diegoquirino.veredito.onboarding.ConsentService.ConsentView;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import org.springframework.hateoas.RepresentationModel;

@Schema(name = "ConsentDecision", description = "The last decision of the participant on a consent form.")
public class ConsentDecisionModel extends RepresentationModel<ConsentDecisionModel> {

    private final String decision;
    private final String consentFormVersion;
    private final String language;
    private final Instant decidedAt;
    private final boolean current;
    private final boolean acknowledgmentAnonymous;

    public ConsentDecisionModel(ConsentView view) {
        this.decision = view.decision() == null ? null : view.decision().code();
        this.consentFormVersion = view.consentFormVersion();
        this.language = view.language();
        this.decidedAt = view.decidedAt();
        this.current = view.current();
        this.acknowledgmentAnonymous = view.acknowledgmentAnonymous();
    }

    @Schema(description = "accepted or refused; null when nothing was recorded.", example = "accepted")
    public String getDecision() {
        return decision;
    }

    @Schema(description = "Version the decision refers to.", example = "v1")
    public String getConsentFormVersion() {
        return consentFormVersion;
    }

    @Schema(description = "Language of the text shown when the participant decided.", example = "en-US")
    public String getLanguage() {
        return language;
    }

    @Schema(description = "When the participant decided (ISO 8601, UTC).", example = "2026-11-20T17:03:00Z")
    public Instant getDecidedAt() {
        return decidedAt;
    }

    @Schema(description = "True when the recorded version is the current one.", example = "true")
    public boolean isCurrent() {
        return current;
    }

    @Schema(
            description =
                    "True when the participant asked to stay anonymous in paper acknowledgments "
                            + "(collective \"Rotuladores Anonimizados por pedido [N]\").",
            example = "false")
    public boolean isAcknowledgmentAnonymous() {
        return acknowledgmentAnonymous;
    }
}