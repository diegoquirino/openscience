package io.github.diegoquirino.veredito.api.v1;

import static io.github.diegoquirino.veredito.api.ApiDocs.HAL_FORMS;
import static io.github.diegoquirino.veredito.api.ApiDocs.PROBLEM_JSON;

import io.github.diegoquirino.veredito.api.ProblemDetailsSchema;
import io.github.diegoquirino.veredito.config.OpenApiConfig;
import io.github.diegoquirino.veredito.onboarding.ScreeningExampleLoader;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.hateoas.Link;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(EntryPointController.SCREENING_EXAMPLE)
@Tag(name = "Participation")
public class ScreeningExampleController {

    private final ScreeningExampleLoader loader;

    public ScreeningExampleController(ScreeningExampleLoader loader) {
        this.loader = loader;
    }

    @GetMapping(version = "v1")
    @SecurityRequirement(name = OpenApiConfig.SESSION_SCHEME)
    @Operation(
            summary = "The English example shown before the screening question",
            description = """
                    A fixed use case (basic flow and exception flow) and a test case derived from it, shown \
                    before the screening question (Appendix B.3). The text is study data, like the items: \
                    Content-Language is en-US whatever Accept-Language says. Participant sessions only.""")
    @ApiResponse(
            responseCode = "200",
            description = "The example",
            content = @Content(
                    mediaType = HAL_FORMS,
                    schema = @Schema(implementation = ScreeningExampleModel.class),
                    examples = @ExampleObject(value = """
                            {
                              "version": "1",
                              "useCase": {
                                "id": "UC-S1",
                                "title": "Reset password",
                                "basicFlow": [ "The user selects \\"Forgot my password\\" on the sign-in page.", "..." ],
                                "exceptionFlow": {
                                  "id": "E1",
                                  "condition": "The e-mail address is not registered (step 3).",
                                  "steps": [ "The system shows the same confirmation as in the basic flow.", "..." ]
                                }
                              },
                              "testCase": {
                                "id": "TC-S1",
                                "title": "Reset request for an unregistered e-mail address",
                                "derivedFrom": [ "UC-S1 / E1" ],
                                "steps": [ "Open the sign-in page and select \\"Forgot my password\\".", "..." ],
                                "expected": "The confirmation is shown and no message is sent."
                              },
                              "_links": {
                                "self": { "href": "/api/v1/screening-example" },
                                "curies": [ { "name": "vd", "href": "/api/v1/rels/{rel}", "templated": true } ],
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
    public ResponseEntity<ScreeningExampleModel> get() {
        ScreeningExampleModel model = new ScreeningExampleModel(loader.example());
        model.add(Link.of(EntryPointController.SCREENING_EXAMPLE));
        model.add(Link.of(EntryPointController.SCREENING, LinkRelations.SCREENING.curied()));
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_LANGUAGE, "en-US").body(model);
    }
}
