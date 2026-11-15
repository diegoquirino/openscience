package io.github.diegoquirino.veredito.onboarding;

import io.github.diegoquirino.veredito.api.ProblemException;
import io.github.diegoquirino.veredito.api.ProblemType;
import io.github.diegoquirino.veredito.domain.onboarding.OnboardingState;
import io.github.diegoquirino.veredito.domain.onboarding.OnboardingStep;
import io.github.diegoquirino.veredito.domain.onboarding.SkillChoice;
import io.github.diegoquirino.veredito.onboarding.OnboardingService.Onboarding;
import io.github.diegoquirino.veredito.persistence.ParticipantRepository;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Reads and records the professional skill, asked once (spec 006 US2). */
@Service
public class SkillService {

    private final ParticipantRepository participants;
    private final OnboardingService onboarding;

    public SkillService(ParticipantRepository participants, OnboardingService onboarding) {
        this.participants = participants;
        this.onboarding = onboarding;
    }

    /** The stored answer (null until answered) and whether the step is the pending one (shows the template). */
    public record SkillView(String skill, String skillOther, boolean pending) {}

    @Transactional(readOnly = true)
    public SkillView view(long participantId) {
        return view(onboarding.of(participantId));
    }

    private static SkillView view(Onboarding onboarding) {
        OnboardingState state = onboarding.state();
        return new SkillView(
                state.professionalSkill(), state.professionalSkillOther(), onboarding.step() == OnboardingStep.SKILL);
    }

    /**
     * Stores the answer when the pending step is {@code skill}. An identical repeat is {@code 200}; any other
     * answer once the step is done, or before it is due, is {@code 409 step-not-pending}.
     */
    @Transactional
    public SkillView record(long participantId, String skill, String other) {
        SkillChoice.Validation validation = SkillChoice.validate(skill, other);
        if (!validation.valid()) {
            throw ProblemException.invalidFields(validation.errors());
        }
        SkillChoice choice = validation.choice();
        OnboardingState state = participants
                .lockOnboardingState(participantId)
                .orElseThrow(() -> new ProblemException(ProblemType.NOT_FOUND));
        Onboarding before = onboarding.of(state, onboarding.currentForm().orElse(null));

        if (state.professionalSkill() != null) {
            if (state.professionalSkill().equals(choice.skill().code())
                    && Objects.equals(state.professionalSkillOther(), choice.other())) {
                return view(before);
            }
            throw notPending(before.step());
        }
        if (before.step() != OnboardingStep.SKILL) {
            throw notPending(before.step());
        }
        if (participants.recordSkill(participantId, choice.skill().code(), choice.other()) == 0) {
            throw notPending(before.step());
        }
        return view(onboarding.of(state.withSkill(choice.skill().code(), choice.other()), before.currentForm()));
    }

    private static ProblemException notPending(OnboardingStep step) {
        return new ProblemException(ProblemType.STEP_NOT_PENDING).with("step", step.slug());
    }
}
