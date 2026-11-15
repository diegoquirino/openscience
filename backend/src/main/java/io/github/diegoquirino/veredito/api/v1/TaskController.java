package io.github.diegoquirino.veredito.api.v1;

import static io.github.diegoquirino.veredito.api.ApiDocs.HAL_FORMS;
import static io.github.diegoquirino.veredito.api.ApiDocs.PROBLEM_JSON;

import io.github.diegoquirino.veredito.access.VereditoPrincipal;
import io.github.diegoquirino.veredito.api.ProblemDetailsSchema;
import io.github.diegoquirino.veredito.api.ProblemException;
import io.github.diegoquirino.veredito.api.ProblemType;
import io.github.diegoquirino.veredito.queue.TaskReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tasks/{id}")
@Tag(name = "Tasks")
public class TaskController {

    private final TaskReservationService reservations;
    private final TaskAssembler assembler;

    public TaskController(TaskReservationService reservations, TaskAssembler assembler) {
        this.reservations = reservations;
        this.assembler = assembler;
    }

    @GetMapping(version = "v1")
    @Operation(
            summary = "Read a reserved or completed task owned by the caller",
            description = "Returns 404 for tasks the caller does not own, so the other rater is never revealed.")
    @ApiResponse(
            responseCode = "200",
            description = "Task",
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
                              "reservationExpiresAt": "2026-11-15T18:20:00Z",
                              "reservationMinutes": 20,
                              "serverNow": "2026-11-15T18:00:00Z",
                              "_links": {
                                "self": { "href": "/api/v1/tasks/41" },
                                "vd:items": { "href": "/api/v1/tasks/41/items/{position}", "templated": true },
                                "vd:save-response": { "href": "/api/v1/tasks/41/responses/{itemId}", "templated": true }
                              }
                            }""")))
    @ApiResponse(
            responseCode = "401",
            description = "Visitor",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetailsSchema.class)))
    @ApiResponse(
            responseCode = "403",
            description = "Administrator",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetailsSchema.class)))
    @ApiResponse(
            responseCode = "404",
            description = "Unknown or foreign task",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetailsSchema.class)))
    public TaskModel get(@AuthenticationPrincipal VereditoPrincipal caller, @PathVariable long id) {
        if (caller == null || caller.participantId() == null) {
            throw new ProblemException(ProblemType.AUTHENTICATION_REQUIRED);
        }
        return assembler.toModel(reservations.requireOwnedTask(caller.participantId(), id));
    }
}
