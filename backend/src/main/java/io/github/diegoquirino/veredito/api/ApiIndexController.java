package io.github.diegoquirino.veredito.api;

import static io.github.diegoquirino.veredito.api.ApiDocs.HAL_FORMS;
import static io.github.diegoquirino.veredito.api.ApiDocs.NOT_ACCEPTABLE_EXAMPLE;
import static io.github.diegoquirino.veredito.api.ApiDocs.PROBLEM_JSON;

import io.github.diegoquirino.veredito.api.ApiIndexModel.ApiVersion;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.hateoas.Link;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Version index, outside any version and not part of the contract of v1. */
@RestController
@Tag(name = "API index")
public class ApiIndexController {

    static final String SELF = "/api";

    @GetMapping(SELF)
    @Operation(
            summary = "List the versions of the API",
            description = "Lists every version with its deprecation state and links each one as vd:<version>"
                    + " (vd:v1 in this release). A client starts here once and then follows the link of the"
                    + " version it supports.")
    @ApiResponse(
            responseCode = "200",
            description = "The supported versions",
            content = @Content(
                    mediaType = HAL_FORMS,
                    schema = @Schema(implementation = ApiIndexModel.class),
                    examples = @ExampleObject(value = """
                            {
                              "versions": [ { "version": "v1", "deprecated": false } ],
                              "_links": {
                                "self": { "href": "/api" },
                                "curies": [ { "name": "vd", "href": "/api/v1/rels/{rel}", "templated": true } ],
                                "vd:v1": { "href": "/api/v1" }
                              }
                            }""")))
    @ApiResponse(
            responseCode = "406",
            description = "The Accept header asks only for formats the index cannot produce",
            content = @Content(
                    mediaType = PROBLEM_JSON,
                    schema = @Schema(implementation = ProblemDetailsSchema.class),
                    examples = @ExampleObject(value = NOT_ACCEPTABLE_EXAMPLE)))
    public ApiIndexModel index() {
        ApiIndexModel model = new ApiIndexModel(
                ProblemDetailsAdvice.SUPPORTED_VERSIONS.stream().map(version -> new ApiVersion(version, false)).toList());
        model.add(Link.of(SELF));
        ProblemDetailsAdvice.SUPPORTED_VERSIONS.forEach(
                version -> model.add(Link.of("/api/" + version, HypermediaConfig.CURIE + ":" + version)));
        return model;
    }
}
