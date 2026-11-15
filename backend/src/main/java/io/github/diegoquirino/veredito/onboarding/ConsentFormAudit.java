package io.github.diegoquirino.veredito.onboarding;

import io.github.diegoquirino.veredito.domain.onboarding.ConsentFormRules;
import io.github.diegoquirino.veredito.domain.onboarding.ConsentFormRules.FormSnapshot;
import io.github.diegoquirino.veredito.persistence.ConsentFormRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Operator's early warning (FR-012): logs the current consent form version once at startup, and every
 * published version that lacks a language. The gate and the API evaluate the rule on every request, so a form
 * fixed later becomes current without a restart.
 */
@Component
public class ConsentFormAudit {

    private static final Logger log = LoggerFactory.getLogger(ConsentFormAudit.class);

    private final ConsentFormRepository forms;
    private final Clock clock;

    public ConsentFormAudit(ConsentFormRepository forms, Clock clock) {
        this.forms = forms;
        this.clock = clock;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void logAtStartup() {
        try {
            audit();
        } catch (RuntimeException ex) {
            log.warn("Could not check the consent forms at startup: {}", ex.getMessage());
        }
    }

    /** Visible for tests: the same check, callable at any time. */
    public void audit() {
        Instant now = clock.instant();
        List<FormSnapshot> snapshots = forms.listSnapshots();
        for (FormSnapshot form : ConsentFormRules.incompletePublished(snapshots, now)) {
            log.warn(
                    "Consent form {} is published but lacks texts in: {}; it is not current",
                    form.version(),
                    String.join(", ", ConsentFormRules.missingLanguages(form)));
        }
        ConsentFormRules.current(snapshots, now).ifPresentOrElse(
                form -> log.info("Current consent form: {}", form.version()),
                () -> log.warn("The study is not open yet: no published consent form has the three texts"));
    }
}
