package io.github.diegoquirino.veredito.onboarding;

import io.github.diegoquirino.veredito.api.ProblemException;
import io.github.diegoquirino.veredito.api.ProblemType;
import io.github.diegoquirino.veredito.domain.onboarding.OnboardingState;
import io.github.diegoquirino.veredito.domain.onboarding.OnboardingStep;
import io.github.diegoquirino.veredito.domain.onboarding.ScreeningAnswer;
import io.github.diegoquirino.veredito.onboarding.OnboardingService.Onboarding;
import io.github.diegoquirino.veredito.persistence.ParticipantRepository;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Reads and records the screening answer, asked once; {@code no} is permanent (spec 006 US3). */
@Service
public class ScreeningService {

    private final ParticipantRepository participants;
    private final OnboardingService onboarding;

    public ScreeningService(ParticipantRepository participants, OnboardingService onboarding) {
        this.participants = participants;
        this.onboarding = onboarding;
    }

    /** The stored answer and eligibility (null until answered) and whether the step is the pending one. */
    public record ScreeningView(ScreeningAnswer answer, Boolean eligible, boolean pending) {}

    @Transactional(readOnly = true)
    public ScreeningView view(long participantId) {
        return view(onboarding.of(participantId));
    }

    private static ScreeningView view(Onboarding onboarding) {
        OnboardingState state = onboarding.state();
        ScreeningAnswer answer = state.screeningAnswer();
        return new ScreeningView(
                answer, answer == null ? null : answer.eligible(), onboarding.step() == OnboardingStep.SCREENING);
    }

    @Transactional
    public ScreeningView record(long participantId, String answerCode) {
        if (answerCode == null || answerCode.isEmpty()) {
            throw ProblemException.invalidFields(Map.of("answer", "required"));
        }
        ScreeningAnswer answer = ScreeningAnswer.fromCode(answerCode)
                .orElseThrow(() -> ProblemException.invalidFields(Map.of("answer", "option")));
        OnboardingState state = participants
                .lockOnboardingState(participantId)
                .orElseThrow(() -> new ProblemException(ProblemType.NOT_FOUND));
        Onboarding before = onboarding.of(state, onboarding.currentForm().orElse(null));

        if (state.screeningAnswer() != null) {
            if (state.screeningAnswer() == answer) {
                return view(before);
            }
            throw notPending(before.step());
        }
        if (before.step() != OnboardingStep.SCREENING) {
            throw notPending(before.step());
        }
        if (participants.recordScreening(participantId, answer) == 0) {
            throw notPending(before.step());
        }
        return view(onboarding.of(state.withScreening(answer), before.currentForm()));
    }

    private static ProblemException notPending(OnboardingStep step) {
        return new ProblemException(ProblemType.STEP_NOT_PENDING).with("step", step.slug());
    }
}
