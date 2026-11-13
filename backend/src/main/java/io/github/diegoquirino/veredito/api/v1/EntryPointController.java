package io.github.diegoquirino.veredito.api.v1;

import static io.github.diegoquirino.veredito.api.ApiDocs.HAL_FORMS;
import static io.github.diegoquirino.veredito.api.ApiDocs.NOT_ACCEPTABLE_EXAMPLE;
import static io.github.diegoquirino.veredito.api.ApiDocs.PROBLEM_JSON;
import static io.github.diegoquirino.veredito.api.ApiDocs.UNSUPPORTED_VERSION_EXAMPLE;

import io.github.diegoquirino.veredito.api.ProblemDetailsSchema;
import io.github.diegoquirino.veredito.config.SystemVersion;
import io.github.diegoquirino.veredito.config.VereditoProperties;
import io.github.diegoquirino.veredito.persistence.DatabaseStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.hateoas.Link;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Entry point")
public class EntryPointController {

    static final String SELF = "/api/v1";
    static final String REQUEST_CODE = "/api/v1/access-codes";

    private final SystemVersion systemVersion;
    private final VereditoProperties properties;
    private final DatabaseStatus databaseStatus;

    public EntryPointController(
            SystemVersion systemVersion, VereditoProperties properties, DatabaseStatus databaseStatus) {
        this.systemVersion = systemVersion;
        this.properties = properties;
        this.databaseStatus = databaseStatus;
    }

    @GetMapping(version = "v1")
    @Operation(
            summary = "Start here: system state and the links offered to the caller",
            description = """
                    Reports the deployed version, the environment and whether the database answers, and links \
                    what the caller may do next. Clients follow the links and never build addresses.

                    Links by state:
                    - database available, visitor (no session): self, curies, vd:request-code;
                    - database unavailable: self only, and available is false.

                    Later releases add vd:me, vd:consent-form and vd:next-task for a participant, and vd:me, \
                    vd:batches, vd:exports, vd:dashboard and vd:participants for the administrator. No \
                    administrative link is ever offered to a visitor. The operation behind vd:request-code \
                    arrives in spec 004; until then it answers 404.""")
    @ApiResponse(
            responseCode = "200",
            description = "The entry point; also 200 when the database is unavailable",
            content = @Content(
                    mediaType = HAL_FORMS,
                    schema = @Schema(implementation = EntryPointModel.class),
                    examples = {
                        @ExampleObject(name = "available", value = """
                                {
                                  "systemVersion": "0.3.0+1a2b3c4",
                                  "environment": "staging",
                                  "available": true,
                                  "_links": {
                                    "self": { "href": "/api/v1" },
                                    "curies": [ { "name": "vd", "href": "/api/v1/rels/{rel}", "templated": true } ],
                                    "vd:request-code": { "href": "/api/v1/access-codes" }
                                  }
                                }"""),
                        @ExampleObject(name = "unavailable", value = """
                                {
                                  "systemVersion": "0.3.0+1a2b3c4",
                                  "environment": "production",
                                  "available": false,
                                  "_links": { "self": { "href": "/api/v1" } }
                                }""")
                    }))
    @ApiResponse(
            responseCode = "400",
            description = "The version in the path is not supported (/api/v9)",
            content = @Content(
                    mediaType = PROBLEM_JSON,
                    schema = @Schema(implementation = ProblemDetailsSchema.class),
                    examples = @ExampleObject(value = UNSUPPORTED_VERSION_EXAMPLE)))
    @ApiResponse(
            responseCode = "406",
            description = "The Accept header asks only for formats the entry point cannot produce",
            content = @Content(
                    mediaType = PROBLEM_JSON,
                    schema = @Schema(implementation = ProblemDetailsSchema.class),
                    examples = @ExampleObject(value = NOT_ACCEPTABLE_EXAMPLE)))
    public EntryPointModel entryPoint() {
        boolean available = databaseStatus.isAvailable();
        EntryPointModel model = new EntryPointModel(systemVersion.value(), properties.environment(), available);
        model.add(Link.of(SELF));
        if (available) {
            model.add(Link.of(REQUEST_CODE, LinkRelations.REQUEST_CODE.curied()));
        }
        return model;
    }
}
