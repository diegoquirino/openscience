package io.github.diegoquirino.veredito.api.v1;

import static io.github.diegoquirino.veredito.api.ApiDocs.HAL_FORMS;
import static io.github.diegoquirino.veredito.api.ApiDocs.PROBLEM_JSON;

import io.github.diegoquirino.veredito.access.ProfileService;
import io.github.diegoquirino.veredito.access.VereditoPrincipal;
import io.github.diegoquirino.veredito.api.ProblemDetailsSchema;
import io.github.diegoquirino.veredito.config.OpenApiConfig;
import io.github.diegoquirino.veredito.persistence.ParticipantRepository.ParticipantProfile;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.mediatype.Affordances;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(EntryPointController.PARTICIPANT_ME)
@Tag(name = "Participation")
public class ParticipantMeController {

    private static final String PROFILE_EXAMPLE = """
            {
              "pseudonym": "R-0001",
              "name": "Ana Souza",
              "preferredLanguage": "es-ES",
              "registeredAt": "2026-11-13T17:03:00Z",
              "_links": { "self": { "href": "/api/v1/participants/me" } },
              "_templates": {
                "default": {
                  "method": "PUT",
                  "properties": [
                    { "name": "preferredLanguage", "required": true,
                      "options": { "inline": ["pt-BR", "en-US", "es-ES"], "minItems": 1, "maxItems": 1 } }
                  ]
                }
              }
            }""";

    private static final String UNAUTHORIZED_EXAMPLE = """
            {
              "type": "urn:veredito:problem:authentication-required",
              "title": "Authentication required",
              "status": 401,
              "detail": "Sign in to access this resource.",
              "instance": "/api/v1/participants/me"
            }""";

    private static final String FORBIDDEN_EXAMPLE = """
            {
              "type": "urn:veredito:problem:forbidden",
              "title": "Access denied",
              "status": 403,
              "detail": "Your session does not give access to this resource.",
              "instance": "/api/v1/participants/me"
            }""";

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
                    sign-in. The default template changes the profile language. Participant sessions only: the \
                    administrator has no participant profile and follows the vd:me of the administrator \
                    instead.""")
    @ApiResponse(
            responseCode = "200",
            description = "The profile",
            content = @Content(
                    mediaType = HAL_FORMS,
                    schema = @Schema(implementation = ParticipantProfileModel.class),
                    examples = @ExampleObject(value = PROFILE_EXAMPLE)))
    @ApiResponse(
            responseCode = "401",
            description = "No valid session: no cookie, or an unknown or expired one",
            content = @Content(
                    mediaType = PROBLEM_JSON,
                    schema = @Schema(implementation = ProblemDetailsSchema.class),
                    examples = @ExampleObject(value = UNAUTHORIZED_EXAMPLE)))
    @ApiResponse(
            responseCode = "403",
            description = "The administrator's session",
            content = @Content(
                    mediaType = PROBLEM_JSON,
                    schema = @Schema(implementation = ProblemDetailsSchema.class),
                    examples = @ExampleObject(value = FORBIDDEN_EXAMPLE)))
    public ParticipantProfileModel me(@AuthenticationPrincipal VereditoPrincipal caller) {
        return model(profiles.participant(caller));
    }

    @PutMapping(version = "v1", consumes = MediaType.APPLICATION_JSON_VALUE)
    @SecurityRequirement(name = OpenApiConfig.SESSION_SCHEME)
    @Operation(
            summary = "Change the participant's profile language",
            description = """
                    Stores pt-BR, en-US or es-ES as the profile language, which wins at the next sign-in on \
                    any browser. The header language switch calls it before reloading the screen under the \
                    new prefix. Idempotent.""")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = PreferredLanguageRequest.class),
                    examples = @ExampleObject(value = """
                            { "preferredLanguage": "en-US" }""")))
    @ApiResponse(
            responseCode = "200",
            description = "The profile with the new language",
            content = @Content(
                    mediaType = HAL_FORMS,
                    schema = @Schema(implementation = ParticipantProfileModel.class),
                    examples = @ExampleObject(value = PROFILE_EXAMPLE)))
    @ApiResponse(
            responseCode = "400",
            description = "preferredLanguage missing (required) or not one of the three locales (locale)",
            content = @Content(
                    mediaType = PROBLEM_JSON,
                    schema = @Schema(implementation = ProblemDetailsSchema.class),
                    examples = @ExampleObject(value = """
                            {
                              "type": "urn:veredito:problem:invalid-request",
                              "title": "Invalid request",
                              "status": 400,
                              "detail": "The request has missing or invalid data; see the list of errors.",
                              "instance": "/api/v1/participants/me",
                              "errors": [ { "field": "preferredLanguage", "code": "locale" } ]
                            }""")))
    @ApiResponse(
            responseCode = "401",
            description = "No valid session: no cookie, or an unknown or expired one",
            content = @Content(
                    mediaType = PROBLEM_JSON,
                    schema = @Schema(implementation = ProblemDetailsSchema.class),
                    examples = @ExampleObject(value = UNAUTHORIZED_EXAMPLE)))
    @ApiResponse(
            responseCode = "403",
            description = "The administrator's session",
            content = @Content(
                    mediaType = PROBLEM_JSON,
                    schema = @Schema(implementation = ProblemDetailsSchema.class),
                    examples = @ExampleObject(value = FORBIDDEN_EXAMPLE)))
    public ParticipantProfileModel changeLanguage(
            @AuthenticationPrincipal VereditoPrincipal caller, @RequestBody PreferredLanguageRequest body) {
        return model(profiles.changeParticipantLanguage(caller, body.preferredLanguage()));
    }

    private static ParticipantProfileModel model(ParticipantProfile profile) {
        ParticipantProfileModel model = new ParticipantProfileModel(profile);
        model.add(Affordances.of(Link.of(EntryPointController.PARTICIPANT_ME))
                .afford(HttpMethod.PUT)
                .withName("default")
                .withInput(PreferredLanguageRequest.class)
                .withInputMediaType(MediaType.APPLICATION_JSON)
                .toLink());
        return model;
    }
}
