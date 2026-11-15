package io.github.diegoquirino.veredito.onboarding;

import io.github.diegoquirino.veredito.api.ProblemException;
import io.github.diegoquirino.veredito.api.ProblemType;
import io.github.diegoquirino.veredito.domain.onboarding.OnboardingRules;
import io.github.diegoquirino.veredito.domain.onboarding.OnboardingState;
import io.github.diegoquirino.veredito.domain.onboarding.OnboardingStep;
import io.github.diegoquirino.veredito.persistence.ConsentFormRepository;
import io.github.diegoquirino.veredito.persistence.ConsentFormRepository.ConsentFormRow;
import io.github.diegoquirino.veredito.persistence.ParticipantRepository;
import java.time.Clock;
import java.util.Optional;
import org.springframework.stereotype.Service;

/** Reads where a participant is in the steps; the answer to {@code vd:onboarding} and the base of the gate. */
@Service
public class OnboardingService {

    private final ParticipantRepository participants;
    private final ConsentFormRepository forms;
    private final Clock clock;

    public OnboardingService(ParticipantRepository participants, ConsentFormRepository forms, Clock clock) {
        this.participants = participants;
        this.forms = forms;
        this.clock = clock;
    }

    /** The state of a participant read together with the current consent form. */
    public record Onboarding(
            OnboardingState state, ConsentFormRow currentForm, OnboardingStep step, boolean eligible) {

        public String currentConsentFormVersion() {
            return currentForm == null ? null : currentForm.version();
        }
    }

    public Onboarding of(long participantId) {
        OnboardingState state = participants
                .findOnboardingState(participantId)
                .orElseThrow(() -> new ProblemException(ProblemType.NOT_FOUND));
        return of(state, currentForm().orElse(null));
    }

    /** Same, for a state already read (and locked) by the caller in its own transaction. */
    public Onboarding of(OnboardingState state, ConsentFormRow currentForm) {
        Long currentId = currentForm == null ? null : currentForm.id();
        return new Onboarding(
                state,
                currentForm,
                OnboardingRules.stepOf(state, currentId),
                OnboardingRules.isEligible(state, currentId));
    }

    public Optional<ConsentFormRow> currentForm() {
        return forms.findCurrent(clock.instant());
    }
}
