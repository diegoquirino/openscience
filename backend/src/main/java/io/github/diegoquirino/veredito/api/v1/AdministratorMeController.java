package io.github.diegoquirino.veredito.api.v1;

import static io.github.diegoquirino.veredito.api.ApiDocs.HAL_FORMS;
import static io.github.diegoquirino.veredito.api.ApiDocs.PROBLEM_JSON;

import io.github.diegoquirino.veredito.access.ProfileService;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(EntryPointController.ADMINISTRATOR_ME)
@Tag(name = "Administration")
public class AdministratorMeController {

    private final ProfileService profiles;

    public AdministratorMeController(ProfileService profiles) {
        this.profiles = profiles;
    }

    @GetMapping(version = "v1")
    @SecurityRequirement(name = OpenApiConfig.SESSION_SCHEME)
    @Operation(
            summary = "The administrator's own profile",
            description = """
                    The latest name typed at sign-in, the administrator's e-mail and the profile language. \
                    Administrator session only; a participant gets 403, and the refusal is logged with the \
                    pseudonym, the method and the path.""")
    @ApiResponse(
            responseCode = "200",
            description = "The profile",
            content = @Content(
                    mediaType = HAL_FORMS,
                    schema = @Schema(implementation = AdministratorProfileModel.class),
                    examples = @ExampleObject(value = """
                            {
                              "name": "Diego Quirino",
                              "email": "diegoquirino@gmail.com",
                              "preferredLanguage": "pt-BR",
                              "_links": { "self": { "href": "/api/v1/administrator/me" } }
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
                              "instance": "/api/v1/administrator/me"
                            }""")))
    @ApiResponse(
            responseCode = "403",
            description = "A participant's session",
            content = @Content(
                    mediaType = PROBLEM_JSON,
                    schema = @Schema(implementation = ProblemDetailsSchema.class),
                    examples = @ExampleObject(value = """
                            {
                              "type": "urn:veredito:problem:forbidden",
                              "title": "Access denied",
                              "status": 403,
                              "detail": "Your session does not give access to this resource.",
                              "instance": "/api/v1/administrator/me"
                            }""")))
    public AdministratorProfileModel me() {
        AdministratorProfileModel model = new AdministratorProfileModel(profiles.administrator());
        model.add(Link.of(EntryPointController.ADMINISTRATOR_ME));
        return model;
    }
}
