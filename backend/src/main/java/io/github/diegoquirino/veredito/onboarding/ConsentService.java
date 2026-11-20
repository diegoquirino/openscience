package io.github.diegoquirino.veredito.onboarding;

import io.github.diegoquirino.veredito.api.ProblemException;
import io.github.diegoquirino.veredito.api.ProblemType;
import io.github.diegoquirino.veredito.domain.onboarding.ConsentDecision;
import io.github.diegoquirino.veredito.domain.onboarding.OnboardingState;
import io.github.diegoquirino.veredito.domain.onboarding.OnboardingStep;
import io.github.diegoquirino.veredito.onboarding.OnboardingService.Onboarding;
import io.github.diegoquirino.veredito.persistence.ConsentFormRepository;
import io.github.diegoquirino.veredito.persistence.ConsentFormRepository.ConsentFormRow;
import io.github.diegoquirino.veredito.persistence.ConsentFormRepository.ConsentTextRow;
import io.github.diegoquirino.veredito.persistence.ParticipantRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Reads the consent form texts and records the participant's decision (spec 006 US1 and US4). */
@Service
public class ConsentService {

    private final ConsentFormRepository forms;
    private final ParticipantRepository participants;
    private final OnboardingService onboarding;
    private final Clock clock;

    public ConsentService(
            ConsentFormRepository forms, ParticipantRepository participants, OnboardingService onboarding, Clock clock) {
        this.forms = forms;
        this.participants = participants;
        this.onboarding = onboarding;
        this.clock = clock;
    }

    /** A consent text; {@code translation} is derived: the language differs from the reference language. */
    public record ConsentText(
            String version,
            String language,
            String referenceLanguage,
            boolean translation,
            String text,
            Instant publishedAt) {}

    /** The last recorded decision; every field but {@code current} is null when nothing was recorded. */
    public record ConsentView(
            ConsentDecision decision,
            String consentFormVersion,
            String language,
            Instant decidedAt,
            boolean current,
            OnboardingStep step) {}

    /** The current version in {@code language}; {@code 404 not-found} when the study has no current form. */
    @Transactional(readOnly = true)
    public ConsentText currentText(String language) {
        ConsentFormRow form = onboarding.currentForm().orElseThrow(() -> new ProblemException(ProblemType.NOT_FOUND));
        return text(form.version(), language);
    }

    /** One language of one published and complete version; {@code 404 not-found} otherwise. */
    @Transactional(readOnly = true)
    public ConsentText text(String version, String language) {
        ConsentTextRow row = forms.findText(version, language, clock.instant())
                .orElseThrow(() -> new ProblemException(ProblemType.NOT_FOUND));
        return new ConsentText(
                row.version(),
                row.language(),
                row.referenceLanguage(),
                !row.language().equals(row.referenceLanguage()),
                row.text(),
                row.publishedAt());
    }

    @Transactional(readOnly = true)
    public ConsentView view(long participantId) {
        return view(onboarding.of(participantId));
    }

    private ConsentView view(Onboarding onboarding) {
        OnboardingState state = onboarding.state();
        if (state.consentFormId() == null) {
            return new ConsentView(null, null, null, null, false, onboarding.step());
        }
        String version = forms.findById(state.consentFormId()).map(ConsentFormRow::version).orElse(null);
        boolean current = onboarding.currentForm() != null && onboarding.currentForm().id() == state.consentFormId();
        return new ConsentView(
                state.consentDecision(), version, state.consentLanguage(), state.consentDecidedAt(), current,
                onboarding.step());
    }

    /**
     * Records the decision on the current version, in the language of the text shown (the request locale).
     * Accepting twice or refusing twice is {@code 200} unchanged; a refusal can become an acceptance; an
     * acceptance of the current version is final.
     */
    @Transactional
    public ConsentView record(long participantId, String decisionCode, String version, String language) {
        Map<String, String> errors = new LinkedHashMap<>();
        ConsentDecision decision = null;
        if (decisionCode == null || decisionCode.isEmpty()) {
            errors.put("decision", "required");
        } else {
            decision = ConsentDecision.fromCode(decisionCode).orElse(null);
            if (decision == null) {
                errors.put("decision", "option");
            }
        }
        if (version == null || version.isBlank()) {
            errors.put("consentFormVersion", "required");
        }
        if (!errors.isEmpty()) {
            throw ProblemException.invalidFields(errors);
        }

        OnboardingState state = participants
                .lockOnboardingState(participantId)
                .orElseThrow(() -> new ProblemException(ProblemType.NOT_FOUND));
        ConsentFormRow current = onboarding.currentForm().orElse(null);
        String name = participants.findById(participantId).map(p -> p.name()).orElse(null);
        Onboarding before = onboarding.of(state, current, name);
        if (before.step() == OnboardingStep.INELIGIBLE || before.step() == OnboardingStep.PROFILE) {
            throw notPending(before.step());
        }
        if (current == null) {
            throw new ProblemException(ProblemType.STUDY_NOT_OPEN);
        }
        if (!current.version().equals(version)) {
            throw new ProblemException(ProblemType.CONSENT_FORM_OUTDATED).with("currentVersion", current.version());
        }

        boolean sameForm = state.consentFormId() != null && state.consentFormId() == current.id();
        if (sameForm && state.consentDecision() == decision) {
            return view(before);
        }
        if (sameForm && state.consentDecision() == ConsentDecision.ACCEPTED) {
            // Refusing an accepted version is the withdrawal of spec 011, not a flip of this decision.
            throw notPending(before.step());
        }

        Instant now = clock.instant().truncatedTo(ChronoUnit.MICROS);
        participants.recordConsent(participantId, current.id(), decision, language, now);
        return view(onboarding.of(state.withConsent(current.id(), decision, language, now), current, name));
    }

    private static ProblemException notPending(OnboardingStep step) {
        return new ProblemException(ProblemType.STEP_NOT_PENDING).with("step", step.slug());
    }
}
