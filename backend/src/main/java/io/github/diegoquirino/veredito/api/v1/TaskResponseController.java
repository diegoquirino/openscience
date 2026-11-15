package io.github.diegoquirino.veredito.api.v1;

import static io.github.diegoquirino.veredito.api.ApiDocs.HAL_FORMS;
import static io.github.diegoquirino.veredito.api.ApiDocs.PROBLEM_JSON;

import io.github.diegoquirino.veredito.access.VereditoPrincipal;
import io.github.diegoquirino.veredito.api.ProblemDetailsSchema;
import io.github.diegoquirino.veredito.api.ProblemException;
import io.github.diegoquirino.veredito.api.ProblemType;
import io.github.diegoquirino.veredito.queue.DraftService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Locale;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tasks/{id}/responses/{itemId}")
@Tag(name = "Tasks")
public class TaskResponseController {

    private final DraftService drafts;
    private final TaskAssembler assembler;

    public TaskResponseController(DraftService drafts, TaskAssembler assembler) {
        this.drafts = drafts;
        this.assembler = assembler;
    }

    @PutMapping(version = "v1", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Save the draft response for one item",
            description = """
                    Replaces the draft of the item and renews the reservation by reservation_minutes. Accepted \
                    from the holder, or from the owner of an expired reservation nobody took. Repeating the same \
                    body is idempotent.""")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = SaveResponseRequest.class),
                    examples = @ExampleObject(value = """
                            {
                              "changeImpact": "HIGH",
                              "maintenanceOperation": "UPDATE",
                              "comment": "optional, up to 1000 characters"
                            }""")))
    @ApiResponse(
            responseCode = "200",
            description = "Refreshed task with the new reservationExpiresAt",
            content = @Content(
                    mediaType = HAL_FORMS,
                    schema = @Schema(implementation = TaskModel.class),
                    examples = @ExampleObject(value = """
                            {
                              "id": 41,
                              "blockNumber": 1,
                              "role": 1,
                              "status": "reserved",
                              "size": 10,
                              "answeredCount": 3,
                              "reservationExpiresAt": "2026-11-15T18:25:00Z",
                              "reservationMinutes": 20,
                              "serverNow": "2026-11-15T18:05:00Z",
                              "_links": {
                                "self": { "href": "/api/v1/tasks/41" },
                                "vd:items": { "href": "/api/v1/tasks/41/items/{position}", "templated": true },
                                "vd:save-response": { "href": "/api/v1/tasks/41/responses/{itemId}", "templated": true }
                              }
                            }""")))
    @ApiResponse(
            responseCode = "400",
            description = "Invalid or missing codes, or comment too long (invalid-request)",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetailsSchema.class)))
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
            description = "Unknown or foreign task, or item not in this block",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetailsSchema.class)))
    @ApiResponse(
            responseCode = "409",
            description = "reservation-expired or task-already-submitted",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetailsSchema.class)))
    public TaskModel put(
            @AuthenticationPrincipal VereditoPrincipal caller,
            @PathVariable long id,
            @PathVariable long itemId,
            @RequestBody(required = false) SaveResponseRequest body,
            Locale locale) {
        if (caller == null || caller.participantId() == null) {
            throw new ProblemException(ProblemType.AUTHENTICATION_REQUIRED);
        }
        SaveResponseRequest request = body == null ? new SaveResponseRequest(null, null, null) : body;
        return assembler.toModel(drafts.saveDraft(
                caller.participantId(),
                id,
                itemId,
                request.changeImpact(),
                request.maintenanceOperation(),
                request.comment(),
                locale));
    }
}
