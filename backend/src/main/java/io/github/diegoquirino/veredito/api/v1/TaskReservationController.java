package io.github.diegoquirino.veredito.api.v1;

import static io.github.diegoquirino.veredito.api.ApiDocs.HAL_FORMS;
import static io.github.diegoquirino.veredito.api.ApiDocs.PROBLEM_JSON;

import io.github.diegoquirino.veredito.access.VereditoPrincipal;
import io.github.diegoquirino.veredito.api.ProblemDetailsSchema;
import io.github.diegoquirino.veredito.api.ProblemException;
import io.github.diegoquirino.veredito.api.ProblemType;
import io.github.diegoquirino.veredito.queue.TaskReservationService;
import io.github.diegoquirino.veredito.queue.TaskReservationService.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.net.URI;
import org.springframework.hateoas.Link;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tasks/reservations")
@Tag(name = "Tasks")
public class TaskReservationController {

    private final TaskReservationService reservations;
    private final TaskAssembler assembler;

    public TaskReservationController(TaskReservationService reservations, TaskAssembler assembler) {
        this.reservations = reservations;
        this.assembler = assembler;
    }

    @PostMapping(version = "v1")
    @Operation(
            summary = "Request the next task from the queue",
            description = """
                    Reserves the first free or expired task the participant can label, or returns the current \
                    reservation. When nothing is available, returns an outcome slug (no-items-yet, \
                    database-complete, none-available-for-you).""")
    @ApiResponse(
            responseCode = "201",
            description = "New reservation",
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
                              "answeredCount": 0,
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
            responseCode = "200",
            description = "Existing reservation or outcome without a task",
            content = @Content(
                    mediaType = HAL_FORMS,
                    schema = @Schema(oneOf = {TaskModel.class, ReservationOutcomeModel.class}),
                    examples = @ExampleObject(value = """
                            {
                              "outcome": "no-items-yet",
                              "_links": { "self": { "href": "/api/v1/tasks/reservations" } }
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
            responseCode = "409",
            description = "Task conflict",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetailsSchema.class)))
    public ResponseEntity<?> reserve(@AuthenticationPrincipal VereditoPrincipal caller) {
        requireParticipant(caller);
        Result result = reservations.reserve(caller.participantId());
        return switch (result) {
            case Result.Reserved reserved when reserved.created() -> {
                TaskModel model = assembler.toModel(reserved.task());
                yield ResponseEntity.created(URI.create("/api/v1/tasks/" + reserved.task().task().id())).body(model);
            }
            case Result.Reserved reserved -> ResponseEntity.ok(assembler.toModel(reserved.task()));
            case Result.NoTask none -> {
                ReservationOutcomeModel model = new ReservationOutcomeModel(none.outcome());
                model.add(Link.of(EntryPointController.TASK_RESERVATIONS));
                yield ResponseEntity.ok(model);
            }
        };
    }

    private static void requireParticipant(VereditoPrincipal caller) {
        if (caller == null || caller.participantId() == null) {
            throw new ProblemException(ProblemType.AUTHENTICATION_REQUIRED);
        }
    }
}
