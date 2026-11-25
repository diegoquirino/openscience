package io.github.diegoquirino.veredito.api.v1;

import io.github.diegoquirino.veredito.persistence.EmailTemplateRepository.Template;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import org.springframework.hateoas.RepresentationModel;

@Schema(name = "EmailTemplate")
public class EmailTemplateModel extends RepresentationModel<EmailTemplateModel> {

    private final String kind;
    private final String subject;
    private final String body;
    private final Instant updatedAt;

    public EmailTemplateModel(Template template) {
        this.kind = template.kind();
        this.subject = template.subject();
        this.body = template.body();
        this.updatedAt = template.updatedAt();
    }

    public String getKind() {
        return kind;
    }

    public String getSubject() {
        return subject;
    }

    public String getBody() {
        return body;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
