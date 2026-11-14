package io.github.diegoquirino.veredito.api.v1;

import static io.github.diegoquirino.veredito.api.ApiDocs.HAL_FORMS;
import static io.github.diegoquirino.veredito.api.ApiDocs.PROBLEM_JSON;

import io.github.diegoquirino.veredito.access.ProfileService;
import io.github.diegoquirino.veredito.access.VereditoPrincipal;
import io.github.diegoquirino.veredito.api.ProblemDetailsSchema;
import io.github.diegoquirino.veredito.config.OpenApiConfig;
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
@RequestMapping(EntryPointController.PARTICIPANT_ME)
@Tag(name = "Participation")
public class ParticipantMeController {

    private final ProfileService profiles;

    public ParticipantMeController(ProfileService profiles) {
        this.profiles = profiles;
    }

    @GetMapping(version = "v1")
    @SecurityRequirement(name = OpenApiConfig.SESSION_SCHEME)
    @Operation(
            summary = "The participant's own profile",
            description = """
                    The pseudonym, the latest name typed at sign-in, the profile language and the first \
                    sign-in. Participant sessions only: the administrator has no participant profile and \
                    follows the vd:me of the administrator instead.""")
    @ApiResponse(
            responseCode = "200",
            description = "The profile",
            content = @Content(
                    mediaType = HAL_FORMS,
                    schema = @Schema(implementation = ParticipantProfileModel.class),
                    examples = @ExampleObject(value = """
                            {
                              "pseudonym": "R-0001",
                              "name": "Ana Souza",
                              "preferredLanguage": "es-ES",
                              "registeredAt": "2026-11-13T17:03:00Z",
                              "_links": { "self": { "href": "/api/v1/participants/me" } }
                            }""")))
    @ApiResponse(
            responseCode = "401",
            description = "No valid session: no cookie, or an unknown or expired one",
            content = @Content(
                    mediaType = PROBLEM_JSON,
                    schema = @Schema(implementation = ProblemDetailsSchema.class),
                    examples = @ExampleObject(value = """
                            {
                              "type": "urn:veredito:problem:authentication-required",
                              "title": "Authentication required",
                              "status": 401,
                              "detail": "Sign in to access this resource.",
                              "instance": "/api/v1/participants/me"
                            }""")))
    @ApiResponse(
            responseCode = "403",
            description = "The administrator's session",
            content = @Content(
                    mediaType = PROBLEM_JSON,
                    schema = @Schema(implementation = ProblemDetailsSchema.class),
                    examples = @ExampleObject(value = """
                            {
                              "type": "urn:veredito:problem:forbidden",
                              "title": "Access denied",
                              "status": 403,
                              "detail": "Your session does not give access to this resource.",
                              "instance": "/api/v1/participants/me"
                            }""")))
    public ParticipantProfileModel me(@AuthenticationPrincipal VereditoPrincipal caller) {
        ParticipantProfileModel model = new ParticipantProfileModel(profiles.participant(caller));
        model.add(Link.of(EntryPointController.PARTICIPANT_ME));
        return model;
    }
}
