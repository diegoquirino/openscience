package io.github.diegoquirino.veredito.api.v1;

import static io.github.diegoquirino.veredito.api.ApiDocs.HAL_FORMS;
import static io.github.diegoquirino.veredito.api.ApiDocs.PROBLEM_JSON;

import io.github.diegoquirino.veredito.access.VereditoPrincipal;
import io.github.diegoquirino.veredito.api.ProblemDetailsSchema;
import io.github.diegoquirino.veredito.api.ProblemException;
import io.github.diegoquirino.veredito.api.ProblemType;
import io.github.diegoquirino.veredito.queue.SubmissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tasks/{id}/submission")
@Tag(name = "Tasks")
public class TaskSubmissionController {

    private final SubmissionService submissions;
    private final TaskAssembler assembler;

    public TaskSubmissionController(SubmissionService submissions, TaskAssembler assembler) {
        this.submissions = submissions;
        this.assembler = assembler;
    }

    @PostMapping(version = "v1")
    @Operation(
            summary = "Submit the block",
            description = """
                    Turns the drafts of every item of the block into final answers and completes the task. \
                    Every item must have an answer, otherwise block-incomplete is returned with the \
                    firstPendingPosition. Accepted from the holder, or from the owner of an expired reservation \
                    nobody took. Submitting an already completed task by the same participant returns the \
                    completed task unchanged. The outcome tells what to show next: more-available, \
                    database-complete or none-available-for-you.""")
    @ApiResponse(
            responseCode = "200",
            description = "Completed task (also when it was already completed)",
            content = @Content(
                    mediaType = HAL_FORMS,
                    schema = @Schema(implementation = TaskModel.class),
                    examples = @ExampleObject(value = """
                            {
                              "id": 41,
                              "blockNumber": 1,
                              "role": 1,
                              "status": "completed",
                              "size": 10,
                              "answeredCount": 10,
                              "outcome": "more-available",
                              "serverNow": "2026-11-15T18:12:00Z",
                              "_links": {
                                "self": { "href": "/api/v1/tasks/41" },
                                "vd:next-task": { "href": "/api/v1/tasks/reservations" }
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
            description = "Unknown task, or task of another participant",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetailsSchema.class)))
    @ApiResponse(
            responseCode = "409",
            description = "block-incomplete (with firstPendingPosition) or reservation-expired",
            content = @Content(
                    mediaType = PROBLEM_JSON,
                    schema = @Schema(implementation = ProblemDetailsSchema.class),
                    examples = @ExampleObject(value = """
                            {
                              "type": "urn:veredito:problem:block-incomplete",
                              "title": "Block incomplete",
                              "status": 409,
                              "detail": "Every item must have an answer before you can submit the block.",
                              "instance": "/api/v1/tasks/41/submission",
                              "firstPendingPosition": 3
                            }""")))
    public TaskModel submit(@AuthenticationPrincipal VereditoPrincipal caller, @PathVariable long id) {
        if (caller == null || caller.participantId() == null) {
            throw new ProblemException(ProblemType.AUTHENTICATION_REQUIRED);
        }
        return assembler.toModel(submissions.submit(caller.participantId(), id));
    }
}
