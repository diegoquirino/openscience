package io.github.diegoquirino.veredito.api.v1;

import static io.github.diegoquirino.veredito.api.ApiDocs.HAL_FORMS;
import static io.github.diegoquirino.veredito.api.ApiDocs.PROBLEM_JSON;

import io.github.diegoquirino.veredito.access.VereditoPrincipal;
import io.github.diegoquirino.veredito.api.ProblemDetailsSchema;
import io.github.diegoquirino.veredito.api.ProblemException;
import io.github.diegoquirino.veredito.api.ProblemType;
import io.github.diegoquirino.veredito.config.OpenApiConfig;
import io.github.diegoquirino.veredito.domain.onboarding.OnboardingStep;
import io.github.diegoquirino.veredito.onboarding.OnboardingService;
import io.github.diegoquirino.veredito.onboarding.OnboardingService.Onboarding;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.hateoas.Link;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(EntryPointController.ONBOARDING)
@Tag(name = "Participation")
public class OnboardingController {

    private final OnboardingService onboarding;

    public OnboardingController(OnboardingService onboarding) {
        this.onboarding = onboarding;
    }

    @GetMapping(version = "v1")
    @SecurityRequirement(name = OpenApiConfig.SESSION_SCHEME)
    @Operation(
            summary = "The step the participant is at before labeling",
            description = """
                    Names the first pending step: consent, skill, screening or participation, or a closing \
                    state (ineligible after a screening answer of no, not-open while no consent form is \
                    current). ineligible wins over not-open and consent. eligible is the answer of the single \
                    eligibility gate and is true only at participation. The links lead to the resources of the \
                    step; vd:next-task appears only at participation. Participant sessions only.""")
    @ApiResponse(
            responseCode = "200",
            description = "The pending step and the links of the step",
            content = @Content(
                    mediaType = HAL_FORMS,
                    schema = @Schema(implementation = OnboardingModel.class),
                    examples = @ExampleObject(value = """
                            {
                              "step": "skill",
                              "eligible": false,
                              "currentConsentFormVersion": "v1",
                              "_links": {
                                "self": { "href": "/api/v1/participants/me/onboarding" },
                                "curies": [ { "name": "vd", "href": "/api/v1/rels/{rel}", "templated": true } ],
                                "vd:consent-form": { "href": "/api/v1/consent-forms/current" },
                                "vd:consent": { "href": "/api/v1/participants/me/consent" },
                                "vd:skill": { "href": "/api/v1/participants/me/skill" },
                                "vd:screening": { "href": "/api/v1/participants/me/screening" }
                              }
                            }""")))
    @ApiResponse(
            responseCode = "401",
            description = "No valid session",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetailsSchema.class)))
    @ApiResponse(
            responseCode = "403",
            description = "The administrator's session",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetailsSchema.class)))
    public OnboardingModel get(@AuthenticationPrincipal VereditoPrincipal caller) {
        if (caller == null || caller.participantId() == null) {
            throw new ProblemException(ProblemType.AUTHENTICATION_REQUIRED);
        }
        return model(onboarding.of(caller.participantId()));
    }

    static OnboardingModel model(Onboarding view) {
        OnboardingModel model = new OnboardingModel(view);
        model.add(Link.of(EntryPointController.ONBOARDING));
        if (view.currentForm() != null) {
            model.add(Link.of(EntryPointController.CONSENT_FORM_CURRENT, LinkRelations.CONSENT_FORM.curied()));
        }
        model.add(Link.of(EntryPointController.CONSENT, LinkRelations.CONSENT.curied()));
        model.add(Link.of(EntryPointController.SKILL, LinkRelations.SKILL.curied()));
        model.add(Link.of(EntryPointController.SCREENING, LinkRelations.SCREENING.curied()));
        if (view.step() == OnboardingStep.SCREENING) {
            model.add(Link.of(EntryPointController.SCREENING_EXAMPLE, LinkRelations.SCREENING_EXAMPLE.curied()));
        }
        if (view.step() == OnboardingStep.PARTICIPATION) {
            model.add(Link.of(EntryPointController.TASK_RESERVATIONS, LinkRelations.NEXT_TASK.curied()));
        }
        return model;
    }
}
