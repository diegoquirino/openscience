package io.github.diegoquirino.veredito.api.v1;

import static io.github.diegoquirino.veredito.api.ApiDocs.HAL_FORMS;
import static io.github.diegoquirino.veredito.api.ApiDocs.PROBLEM_JSON;

import io.github.diegoquirino.veredito.access.ProfileService;
import io.github.diegoquirino.veredito.api.ProblemDetailsSchema;
import io.github.diegoquirino.veredito.config.OpenApiConfig;
import io.github.diegoquirino.veredito.persistence.ConfigRepository.AdminProfile;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(EntryPointController.ADMINISTRATOR_ME)
@Tag(name = "Administration")
public class AdministratorMeController {

    private static final String PROFILE_EXAMPLE = """
            {
              "name": "Diego Quirino",
              "email": "diegoquirino@gmail.com",
              "preferredLanguage": "pt-BR",
              "_links": { "self": { "href": "/api/v1/administrator/me" } },
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
              "instance": "/api/v1/administrator/me"
            }""";

    private static final String FORBIDDEN_EXAMPLE = """
            {
              "type": "urn:veredito:problem:forbidden",
              "title": "Access denied",
              "status": 403,
              "detail": "Your session does not give access to this resource.",
              "instance": "/api/v1/administrator/me"
            }""";

    private final ProfileService profiles;

    public AdministratorMeController(ProfileService profiles) {
        this.profiles = profiles;
    }

    @GetMapping(version = "v1")
    @SecurityRequirement(name = OpenApiConfig.SESSION_SCHEME)
    @Operation(
            summary = "The administrator's own profile",
            description = """
                    The latest name typed at sign-in, the administrator's e-mail and the profile language; the \
                    default template changes the language. Administrator session only; a participant gets 403, \
                    and the refusal is logged with the pseudonym, the method and the path.""")
    @ApiResponse(
            responseCode = "200",
            description = "The profile",
            content = @Content(
                    mediaType = HAL_FORMS,
                    schema = @Schema(implementation = AdministratorProfileModel.class),
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
            description = "A participant's session",
            content = @Content(
                    mediaType = PROBLEM_JSON,
                    schema = @Schema(implementation = ProblemDetailsSchema.class),
                    examples = @ExampleObject(value = FORBIDDEN_EXAMPLE)))
    public AdministratorProfileModel me() {
        return model(profiles.administrator());
    }

    @PutMapping(version = "v1", consumes = MediaType.APPLICATION_JSON_VALUE)
    @SecurityRequirement(name = OpenApiConfig.SESSION_SCHEME)
    @Operation(
            summary = "Change the administrator's profile language",
            description = """
                    Stores pt-BR, en-US or es-ES in config.admin_preferred_language, which wins at the next \
                    sign-in on any browser. The header language switch calls it before reloading the screen \
                    under the new prefix. Idempotent.""")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = PreferredLanguageRequest.class),
                    examples = @ExampleObject(value = """
                            { "preferredLanguage": "es-ES" }""")))
    @ApiResponse(
            responseCode = "200",
            description = "The profile with the new language",
            content = @Content(
                    mediaType = HAL_FORMS,
                    schema = @Schema(implementation = AdministratorProfileModel.class),
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
                              "instance": "/api/v1/administrator/me",
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
            description = "A participant's session",
            content = @Content(
                    mediaType = PROBLEM_JSON,
                    schema = @Schema(implementation = ProblemDetailsSchema.class),
                    examples = @ExampleObject(value = FORBIDDEN_EXAMPLE)))
    public AdministratorProfileModel changeLanguage(@RequestBody PreferredLanguageRequest body) {
        return model(profiles.changeAdministratorLanguage(body.preferredLanguage()));
    }

    private static AdministratorProfileModel model(AdminProfile profile) {
        AdministratorProfileModel model = new AdministratorProfileModel(profile);
        model.add(Affordances.of(Link.of(EntryPointController.ADMINISTRATOR_ME))
                .afford(HttpMethod.PUT)
                .withName("default")
                .withInput(PreferredLanguageRequest.class)
                .withInputMediaType(MediaType.APPLICATION_JSON)
                .toLink());
        return model;
    }
}
