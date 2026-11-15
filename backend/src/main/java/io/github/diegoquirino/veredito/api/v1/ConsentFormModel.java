package io.github.diegoquirino.veredito.api.v1;

import io.github.diegoquirino.veredito.onboarding.ConsentService.ConsentText;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import org.springframework.hateoas.RepresentationModel;

@Schema(name = "ConsentForm", description = "One language of one published consent form version.")
public class ConsentFormModel extends RepresentationModel<ConsentFormModel> {

    private final String version;
    private final String language;
    private final String referenceLanguage;
    private final boolean translation;
    private final String text;
    private final Instant publishedAt;

    public ConsentFormModel(ConsentText consent) {
        this.version = consent.version();
        this.language = consent.language();
        this.referenceLanguage = consent.referenceLanguage();
        this.translation = consent.translation();
        this.text = consent.text();
        this.publishedAt = consent.publishedAt();
    }

    @Schema(description = "Version of the consent form.", example = "v1")
    public String getVersion() {
        return version;
    }

    @Schema(description = "Language of the text.", example = "en-US")
    public String getLanguage() {
        return language;
    }

    @Schema(description = "Language of the version approved by the research ethics committee.", example = "pt-BR")
    public String getReferenceLanguage() {
        return referenceLanguage;
    }

    @Schema(description = "True when language differs from referenceLanguage.", example = "true")
    public boolean isTranslation() {
        return translation;
    }

    @Schema(description = "Plain text; paragraphs separated by blank lines; stored as published.")
    public String getText() {
        return text;
    }

    @Schema(description = "When the version was published (ISO 8601, UTC).", example = "2026-11-20T12:00:00Z")
    public Instant getPublishedAt() {
        return publishedAt;
    }
}