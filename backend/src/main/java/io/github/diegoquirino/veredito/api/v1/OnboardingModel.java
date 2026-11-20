package io.github.diegoquirino.veredito.api.v1;

import io.github.diegoquirino.veredito.onboarding.OnboardingService.Onboarding;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.hateoas.RepresentationModel;

@Schema(name = "Onboarding", description = "Where the participant is in the steps before labeling.")
public class OnboardingModel extends RepresentationModel<OnboardingModel> {

    private final String step;
    private final boolean eligible;
    private final String currentConsentFormVersion;

    public OnboardingModel(Onboarding onboarding) {
        this.step = onboarding.step().slug();
        this.eligible = onboarding.eligible();
        this.currentConsentFormVersion = onboarding.currentConsentFormVersion();
    }

    @Schema(
            description = "consent, skill, screening, participation, ineligible or not-open.",
            allowableValues = {"consent", "profile", "screening", "participation", "ineligible", "not-open"},
            example = "profile")
    public String getStep() {
        return step;
    }

    @Schema(description = "True only when the step is participation.", example = "false")
    public boolean isEligible() {
        return eligible;
    }

    @Schema(description = "Version of the current consent form, or null when there is none.", example = "v1")
    public String getCurrentConsentFormVersion() {
        return currentConsentFormVersion;
    }
}