package io.github.diegoquirino.veredito.onboarding;

import io.github.diegoquirino.veredito.api.ProblemException;
import io.github.diegoquirino.veredito.api.ProblemType;
import io.github.diegoquirino.veredito.domain.access.PersonName;
import io.github.diegoquirino.veredito.domain.onboarding.OnboardingState;
import io.github.diegoquirino.veredito.domain.onboarding.OnboardingStep;
import io.github.diegoquirino.veredito.domain.onboarding.SkillChoice;
import io.github.diegoquirino.veredito.onboarding.OnboardingService.Onboarding;
import io.github.diegoquirino.veredito.persistence.ParticipantRepository;
import io.github.diegoquirino.veredito.persistence.ParticipantRepository.ParticipantProfile;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Profile step (spec 013): name and professional skill before consent. Also supports later updates
 * of name/skill when the profile step is already complete (spec 014).
 */
@Service
public class SkillService {

    private final ParticipantRepository participants;
    private final OnboardingService onboarding;

    public SkillService(ParticipantRepository participants, OnboardingService onboarding) {
        this.participants = participants;
        this.onboarding = onboarding;
    }

    /** Stored profile fields and whether the profile step is still pending. */
    public record SkillView(String name, String skill, String skillOther, boolean pending) {}

    @Transactional(readOnly = true)
    public SkillView view(long participantId) {
        return view(onboarding.of(participantId), nameOf(participantId));
    }

    private SkillView view(Onboarding onboarding, String name) {
        OnboardingState state = onboarding.state();
        return new SkillView(
                name,
                state.professionalSkill(),
                state.professionalSkillOther(),
                onboarding.step() == OnboardingStep.PROFILE);
    }

    /**
     * Completes or updates name and skill. While the pending step is {@code profile}, both are
     * required. Afterwards, either field may be updated (identical payload is {@code 200}).
     */
    @Transactional
    public SkillView record(long participantId, String rawName, String skill, String other) {
        Map<String, String> errors = new LinkedHashMap<>();
        Optional<PersonName> parsedName = PersonName.parse(rawName);
        if (parsedName.isEmpty()) {
            errors.put("name", rawName == null || rawName.isBlank() ? "required" : "size");
        }
        SkillChoice.Validation validation = SkillChoice.validate(skill, other);
        if (!validation.valid()) {
            errors.putAll(validation.errors());
        }
        if (!errors.isEmpty()) {
            throw ProblemException.invalidFields(errors);
        }
        PersonName name = parsedName.get();
        SkillChoice choice = validation.choice();

        OnboardingState state = participants
                .lockOnboardingState(participantId)
                .orElseThrow(() -> new ProblemException(ProblemType.NOT_FOUND));
        String currentName = nameOf(participantId);
        Onboarding before = onboarding.of(state, onboarding.currentForm().orElse(null), currentName);

        boolean same = name.value().equals(currentName)
                && choice.skill().code().equals(state.professionalSkill())
                && Objects.equals(choice.other(), state.professionalSkillOther());
        if (same) {
            return view(before, currentName);
        }

        if (before.step() == OnboardingStep.PROFILE || state.professionalSkill() != null) {
            participants.updateProfile(participantId, name.value(), choice.skill().code(), choice.other());
            OnboardingState updated = state.withSkill(choice.skill().code(), choice.other());
            return view(onboarding.of(updated, before.currentForm(), name.value()), name.value());
        }
        throw notPending(before.step());
    }

    private String nameOf(long participantId) {
        return participants.findById(participantId).map(ParticipantProfile::name).orElse(null);
    }

    private static ProblemException notPending(OnboardingStep step) {
        return new ProblemException(ProblemType.STEP_NOT_PENDING).with("step", step.slug());
    }
}
