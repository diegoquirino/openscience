package io.github.diegoquirino.veredito.api.v1;

import static io.github.diegoquirino.veredito.api.ApiDocs.HAL_FORMS;
import static io.github.diegoquirino.veredito.api.ApiDocs.PROBLEM_JSON;

import io.github.diegoquirino.veredito.api.ProblemDetailsSchema;
import io.github.diegoquirino.veredito.dashboard.DashboardService;
import io.github.diegoquirino.veredito.dashboard.DashboardService.Snapshot;
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

/** Progress dashboard of the data collection (spec 010); administrator only, read-only. */
@RestController
@RequestMapping("/api/v1/dashboard")
@Tag(name = "Administration")
public class DashboardController {

    static final String SELF = "/api/v1/dashboard";

    private final DashboardService dashboard;

    public DashboardController(DashboardService dashboard) {
        this.dashboard = dashboard;
    }

    @GetMapping(version = "v1")
    @Operation(
            summary = "Read the data collection progress",
            description = """
                    One snapshot, computed on each call and never stored: tasks free, in progress and completed \
                    per role (overall first, then per system and batch; an expired reservation counts as free), \
                    the reserved tasks with the pseudonym that holds them, participants by status and skill, \
                    completed tasks per pseudonym, final responses per language and the rolling 24-hour e-mail \
                    quota. Carries no name or e-mail. When no item was imported, every number is zero and \
                    vd:batches links the import. Refresh by calling the same link again.""")
    @ApiResponse(
            responseCode = "200",
            description = "The progress snapshot",
            content = @Content(
                    mediaType = HAL_FORMS,
                    schema = @Schema(implementation = DashboardModel.class),
                    examples = @ExampleObject(value = """
                            {
                              "generatedAt": "2026-11-16T12:00:00Z",
                              "firstFreeQueuePosition": 3,
                              "progress": [
                                {
                                  "system": null,
                                  "batchId": null,
                                  "batchFileName": null,
                                  "items": 20,
                                  "blocks": 2,
                                  "role1": { "free": 1, "inProgress": 1, "completed": 0, "completedPercent": 0.0 },
                                  "role2": { "free": 2, "inProgress": 0, "completed": 0, "completedPercent": 0.0 }
                                }
                              ],
                              "reservations": [
                                {
                                  "pseudonym": "R-0001",
                                  "system": "minitest",
                                  "block": 1,
                                  "role": 1,
                                  "draftCount": 3,
                                  "expiresAt": "2026-11-16T12:30:00Z",
                                  "expired": false
                                }
                              ],
                              "participants": {
                                "eligible": 2,
                                "ineligible": 1,
                                "declinedConsent": 0,
                                "incomplete": 3,
                                "bySkill": { "software_engineer": 2, "professor_researcher": 1 },
                                "completedByPseudonym": [
                                  { "pseudonym": "R-0001", "completedTasks": 2, "shareOfCompleted": 1.0 }
                                ]
                              },
                              "languages": [
                                { "language": "pt-BR", "participantsPreferred": 4, "finalResponses": 10 },
                                { "language": "en-US", "participantsPreferred": 1, "finalResponses": 0 },
                                { "language": "es-ES", "participantsPreferred": 1, "finalResponses": 0 }
                              ],
                              "emailQuota": {
                                "emailsSentLast24h": 12,
                                "dailyEmailLimit": 450,
                                "emailsRemaining": 438,
                                "lowRemaining": false
                              },
                              "_links": {
                                "self": { "href": "/api/v1/dashboard" },
                                "curies": [ { "name": "vd", "href": "/api/v1/rels/{rel}", "templated": true } ]
                              }
                            }""")))
    @ApiResponse(
            responseCode = "401",
            description = "Visitor",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetailsSchema.class)))
    @ApiResponse(
            responseCode = "403",
            description = "Participant",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetailsSchema.class)))
    public DashboardModel dashboard() {
        Snapshot snapshot = dashboard.snapshot();
        DashboardModel model = new DashboardModel(snapshot);
        model.add(Link.of(SELF));
        if (snapshot.empty()) {
            model.add(Link.of(BatchController.COLLECTION, LinkRelations.BATCHES.curied()));
        }
        return model;
    }
}