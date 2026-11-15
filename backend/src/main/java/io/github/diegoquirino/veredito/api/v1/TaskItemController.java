package io.github.diegoquirino.veredito.api.v1;

import static io.github.diegoquirino.veredito.api.ApiDocs.HAL_FORMS;
import static io.github.diegoquirino.veredito.api.ApiDocs.PROBLEM_JSON;

import io.github.diegoquirino.veredito.access.VereditoPrincipal;
import io.github.diegoquirino.veredito.api.ProblemDetailsSchema;
import io.github.diegoquirino.veredito.api.ProblemException;
import io.github.diegoquirino.veredito.api.ProblemType;
import io.github.diegoquirino.veredito.queue.DraftService;
import io.github.diegoquirino.veredito.queue.DraftService.ItemView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.mediatype.Affordances;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tasks/{id}/items/{position}")
@Tag(name = "Tasks")
public class TaskItemController {

    private final DraftService drafts;

    public TaskItemController(DraftService drafts) {
        this.drafts = drafts;
    }

    @GetMapping(version = "v1")
    @Operation(
            summary = "Read one item of a task and the caller's own draft",
            description = """
                    position is 1 to the block size. Includes the caller's own response when there is one \
                    (draft is null otherwise) and never the other rater's response or identity. Tasks the \
                    caller does not own are 404.""")
    @ApiResponse(
            responseCode = "200",
            description = "Item with optional draft",
            content = @Content(
                    mediaType = HAL_FORMS,
                    schema = @Schema(implementation = TaskItemModel.class),
                    examples = @ExampleObject(value = """
                            {
                              "position": 1,
                              "itemId": 9001,
                              "system": "minitest",
                              "uc": "UC01",
                              "kind": "MOD",
                              "field": "step",
                              "originVersion": "1.1",
                              "targetVersion": "1.2",
                              "originUsecase": "Original use case text",
                              "targetUsecase": "Changed use case text",
                              "origin": "Old step",
                              "target": "New step",
                              "draft": {
                                "changeImpact": "LOW",
                                "maintenanceOperation": "KEEP",
                                "comment": null,
                                "language": "pt-BR"
                              },
                              "_links": {
                                "self": { "href": "/api/v1/tasks/41/items/1" },
                                "vd:save-response": { "href": "/api/v1/tasks/41/responses/9001" }
                              }
                            }""")))
    @ApiResponse(
            responseCode = "401",
            description = "Visitor",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetailsSchema.class)))
    @ApiResponse(
            responseCode = "403",
            description = "Administrator or not eligible",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetailsSchema.class)))
    @ApiResponse(
            responseCode = "404",
            description = "Unknown or foreign task, or position outside the block",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetailsSchema.class)))
    public TaskItemModel get(
            @AuthenticationPrincipal VereditoPrincipal caller, @PathVariable long id, @PathVariable int position) {
        if (caller == null || caller.participantId() == null) {
            throw new ProblemException(ProblemType.AUTHENTICATION_REQUIRED);
        }
        ItemView view = drafts.item(caller.participantId(), id, position);
        TaskItemModel model = new TaskItemModel(view);
        model.add(Link.of("/api/v1/tasks/" + id + "/items/" + position));
        if ("reserved".equals(view.task().status())) {
            model.add(Affordances.of(Link.of(
                            "/api/v1/tasks/" + id + "/responses/" + view.item().id(),
                            LinkRelations.SAVE_RESPONSE.curied()))
                    .afford(HttpMethod.PUT)
                    .withName("default")
                    .withInput(SaveResponseRequest.class)
                    .withInputMediaType(MediaType.APPLICATION_JSON)
                    .toLink());
        }
        return model;
    }
}
