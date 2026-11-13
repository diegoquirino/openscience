package io.github.diegoquirino.veredito.api.v1;

import static io.github.diegoquirino.veredito.api.ApiDocs.HAL_FORMS;
import static io.github.diegoquirino.veredito.api.ApiDocs.NOT_ACCEPTABLE_EXAMPLE;
import static io.github.diegoquirino.veredito.api.ApiDocs.NOT_FOUND_EXAMPLE;
import static io.github.diegoquirino.veredito.api.ApiDocs.PROBLEM_JSON;

import io.github.diegoquirino.veredito.api.ProblemDetailsSchema;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.hateoas.Link;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** Target of the {@code vd} CURIE: {@code vd:request-code} is documented at {@code /api/v1/rels/request-code}. */
@RestController
@RequestMapping("/api/v1/rels")
@Tag(name = "Entry point")
public class RelationController {

    @GetMapping(path = "/{rel}", version = "v1")
    @Operation(
            summary = "Document a link relation",
            description = "Describes a vd: relation: what the link does, the HTTP method to use on it and where"
                    + " its operation is documented. The rel is given without the prefix (request-code for"
                    + " vd:request-code). Relations known in this release: v1 (index), request-code,"
                    + " sign-in, session, sign-out and me.")
    @ApiResponse(
            responseCode = "200",
            description = "The relation",
            content = @Content(
                    mediaType = HAL_FORMS,
                    schema = @Schema(implementation = RelationModel.class),
                    examples = @ExampleObject(value = """
                            {
                              "rel": "vd:request-code",
                              "description": "Request a sign-in code by e-mail.",
                              "method": "POST",
                              "documentation": "/swagger-ui.html#/Access",
                              "_links": { "self": { "href": "/api/v1/rels/request-code" } }
                            }""")))
    @ApiResponse(
            responseCode = "404",
            description = "No relation with this name",
            content = @Content(
                    mediaType = PROBLEM_JSON,
                    schema = @Schema(implementation = ProblemDetailsSchema.class),
                    examples = @ExampleObject(value = NOT_FOUND_EXAMPLE)))
    @ApiResponse(
            responseCode = "406",
            description = "The Accept header asks only for formats the relation cannot be written in",
            content = @Content(
                    mediaType = PROBLEM_JSON,
                    schema = @Schema(implementation = ProblemDetailsSchema.class),
                    examples = @ExampleObject(value = NOT_ACCEPTABLE_EXAMPLE)))
    public RelationModel relation(
            @Parameter(description = "Relation name without the vd: prefix", example = "request-code")
                    @PathVariable String rel) {
        RelationModel model = LinkRelations.find(rel)
                .map(RelationModel::new)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        model.add(Link.of("/api/v1/rels/" + rel));
        return model;
    }
}
