package io.github.diegoquirino.veredito.onboarding;

import io.github.diegoquirino.veredito.api.ProblemException;
import io.github.diegoquirino.veredito.api.ProblemType;
import io.github.diegoquirino.veredito.domain.access.PersonName;
import io.github.diegoquirino.veredito.domain.onboarding.OnboardingState;
import io.github.diegoquirino.veredito.domain.onboarding.OnboardingStep;
import io.github.diegoquirino.veredito.domain.onboarding.OrganizationChoice;
import io.github.diegoquirino.veredito.domain.onboarding.SkillChoice;
import io.github.diegoquirino.veredito.invite.InviteInicialDispatcher;
import io.github.diegoquirino.veredito.onboarding.OnboardingService.Onboarding;
import io.github.diegoquirino.veredito.persistence.ParticipantRepository;
import io.github.diegoquirino.veredito.persistence.ParticipantRepository.ParticipantProfile;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Profile step (specs 013–025): name, professional skill and organization before consent. Also
 * supports later updates when the profile step is already complete (spec 014).
 */
@Service
public class SkillService {

    private final ParticipantRepository participants;
    private final OnboardingService onboarding;
    private final InviteInicialDispatcher inviteInicial;

    public SkillService(
            ParticipantRepository participants,
            OnboardingService onboarding,
            InviteInicialDispatcher inviteInicial) {
        this.participants = participants;
        this.onboarding = onboarding;
        this.inviteInicial = inviteInicial;
    }

    /** Stored profile fields and whether the profile step is still pending. */
    public record SkillView(
            String name,
            String skill,
            String skillOther,
            String organization,
            String organizationOther,
            boolean pending) {}

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
                state.organization(),
                state.organizationOther(),
                onboarding.step() == OnboardingStep.PROFILE);
    }

    @Transactional(readOnly = true)
    public List<String> organizationSuggestions(long participantId) {
        return participants.organizationSuggestions(participantId);
    }

    /**
     * Completes or updates name, skill and organization. While the pending step is {@code profile},
     * all are required. Afterwards, the same PUT updates the profile (identical payload is {@code 200}).
     */
    @Transactional
    public SkillView record(
            long participantId,
            String rawName,
            String skill,
            String skillOther,
            String organization,
            String organizationOther) {
        Map<String, String> errors = new LinkedHashMap<>();
        Optional<PersonName> parsedName = PersonName.parse(rawName);
        if (parsedName.isEmpty()) {
            errors.put("name", rawName == null || rawName.isBlank() ? "required" : "size");
        }
        SkillChoice.Validation skillValidation = SkillChoice.validate(skill, skillOther);
        if (!skillValidation.valid()) {
            errors.putAll(skillValidation.errors());
        }
        OrganizationChoice.Validation orgValidation = OrganizationChoice.validate(organization, organizationOther);
        if (!orgValidation.valid()) {
            errors.putAll(orgValidation.errors());
        }
        if (!errors.isEmpty()) {
            throw ProblemException.invalidFields(errors);
        }
        PersonName name = parsedName.get();
        SkillChoice skillChoice = skillValidation.choice();
        OrganizationChoice orgChoice = orgValidation.choice();

        OnboardingState state = participants
                .lockOnboardingState(participantId)
                .orElseThrow(() -> new ProblemException(ProblemType.NOT_FOUND));
        String currentName = nameOf(participantId);
        Onboarding before = onboarding.of(state, onboarding.currentForm().orElse(null), currentName);

        boolean same = name.value().equals(currentName)
                && skillChoice.skill().code().equals(state.professionalSkill())
                && Objects.equals(skillChoice.other(), state.professionalSkillOther())
                && orgChoice.organization().code().equals(state.organization())
                && Objects.equals(orgChoice.other(), state.organizationOther());
        if (same) {
            return view(before, currentName);
        }

        boolean profilePending = before.step() == OnboardingStep.PROFILE;
        boolean alreadyHadSkill = state.professionalSkill() != null;
        if (profilePending || alreadyHadSkill || state.organization() != null) {
            participants.updateProfile(
                    participantId,
                    name.value(),
                    skillChoice.skill().code(),
                    skillChoice.other(),
                    orgChoice.organization().code(),
                    orgChoice.other());
            // Spec 031: inicial invite after a complete profile; idempotent via sent_at / retry when null.
            inviteInicial.scheduleAfterCommit(participantId);
            OnboardingState updated = state.withProfile(
                    skillChoice.skill().code(),
                    skillChoice.other(),
                    orgChoice.organization().code(),
                    orgChoice.other());
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
