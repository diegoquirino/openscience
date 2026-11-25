package io.github.diegoquirino.veredito.invite;

import io.github.diegoquirino.veredito.api.ProblemException;
import io.github.diegoquirino.veredito.persistence.EmailTemplateRepository;
import io.github.diegoquirino.veredito.persistence.EmailTemplateRepository.Template;
import io.github.diegoquirino.veredito.persistence.InviteSettingsRepository;
import io.github.diegoquirino.veredito.persistence.InviteSettingsRepository.InviteSettings;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Administrator invite send settings and templates (spec 031). */
@Service
public class InviteSettingsService {

    private final InviteSettingsRepository settings;
    private final EmailTemplateRepository templates;
    private final Clock clock;

    public InviteSettingsService(
            InviteSettingsRepository settings, EmailTemplateRepository templates, Clock clock) {
        this.settings = settings;
        this.templates = templates;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public InviteSettings viewSettings() {
        return settings.load();
    }

    @Transactional
    public InviteSettings saveSettings(
            String caae,
            String platformUrl,
            Instant collectionStartAt,
            Instant collectionEndAt,
            Instant reminderAt) {
        Map<String, String> errors = new LinkedHashMap<>();
        String url = platformUrl == null ? "" : platformUrl.trim();
        if (url.isEmpty()) {
            errors.put("platformUrl", "required");
        } else if (url.length() > 500) {
            errors.put("platformUrl", "size");
        }
        String caaeValue = caae == null ? null : caae.trim();
        if (caaeValue != null && caaeValue.isEmpty()) {
            caaeValue = null;
        }
        if (caaeValue != null && caaeValue.length() > 64) {
            errors.put("caae", "size");
        }
        if (collectionStartAt != null
                && collectionEndAt != null
                && !collectionEndAt.isAfter(collectionStartAt)) {
            errors.put("collectionEndAt", "range");
        }
        if (!errors.isEmpty()) {
            throw ProblemException.invalidFields(errors);
        }
        InviteSettings next =
                new InviteSettings(caaeValue, url, collectionStartAt, collectionEndAt, reminderAt);
        settings.save(next, clock.instant());
        return settings.load();
    }

    @Transactional(readOnly = true)
    public Template viewTemplate(InviteTemplateKind kind) {
        return templates.require(kind);
    }

    @Transactional
    public Template saveTemplate(InviteTemplateKind kind, String subject, String body) {
        Map<String, String> errors = new LinkedHashMap<>();
        String subjectValue = subject == null ? "" : subject.trim();
        String bodyValue = body == null ? "" : body;
        if (subjectValue.isEmpty()) {
            errors.put("subject", "required");
        } else if (subjectValue.length() > 200) {
            errors.put("subject", "size");
        }
        if (bodyValue.isBlank()) {
            errors.put("body", "required");
        }
        if (!errors.isEmpty()) {
            throw ProblemException.invalidFields(errors);
        }
        templates.update(kind, subjectValue, bodyValue, clock.instant());
        return templates.require(kind);
    }
}
