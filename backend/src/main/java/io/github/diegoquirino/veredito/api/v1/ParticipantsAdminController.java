package io.github.diegoquirino.veredito.api.v1;

import static io.github.diegoquirino.veredito.api.ApiDocs.HAL_FORMS;
import static io.github.diegoquirino.veredito.api.ApiDocs.PROBLEM_JSON;

import io.github.diegoquirino.veredito.api.ProblemDetailsSchema;
import io.github.diegoquirino.veredito.api.ProblemException;
import io.github.diegoquirino.veredito.api.ProblemType;
import io.github.diegoquirino.veredito.domain.withdrawal.WithdrawalOption;
import io.github.diegoquirino.veredito.withdrawal.WithdrawalService;
import io.github.diegoquirino.veredito.withdrawal.WithdrawalService.AnonymizationResult;
import io.github.diegoquirino.veredito.withdrawal.WithdrawalService.ParticipantCard;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.Link;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Administrator search and LGPD anonymization (spec 011). */
@RestController
@RequestMapping("/api/v1/participants")
@Tag(name = "Administration")
public class ParticipantsAdminController {

    static final String COLLECTION = "/api/v1/participants";

    private final WithdrawalService withdrawals;

    public ParticipantsAdminController(WithdrawalService withdrawals) {
        this.withdrawals = withdrawals;
    }

    @GetMapping(version = "v1")
    @Operation(
            summary = "Search a participant by e-mail or pseudonym",
            description = """
                    Exactly one of the query parameters email or pseudonym. Returns zero or one card \
                    (never a fuzzy list). Already anonymized people are found only by pseudonym and have \
                    no vd:anonymize link.""")
    @ApiResponse(
            responseCode = "200",
            description = "Search result (possibly empty)",
            content = @Content(
                    mediaType = HAL_FORMS,
                    schema = @Schema(implementation = ParticipantSearchModel.class),
                    examples = @ExampleObject(value = """
                            {
                              "participants": [
                                {
                                  "id": 12,
                                  "pseudonym": "R-0007",
                                  "email": "ana@example.com",
                                  "preferredLanguage": "es-ES",
                                  "anonymizedAt": null,
                                  "completedTasks": 2,
                                  "reserved": null,
                                  "finalResponseCount": 20,
                                  "draftResponseCount": 0,
                                  "tasksReturnableOnDiscard": 2,
                                  "emailCanBeSent": true,
                                  "_links": {
                                    "self": { "href": "/api/v1/participants/12" },
                                    "vd:anonymize": { "href": "/api/v1/participants/12/anonymizations" }
                                  }
                                }
                              ],
                              "_links": { "self": { "href": "/api/v1/participants?email=ana%40example.com" } }
                            }""")))
    @ApiResponse(
            responseCode = "400",
            description = "Neither or both of email and pseudonym",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetailsSchema.class)))
    @ApiResponse(
            responseCode = "401",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetailsSchema.class)))
    @ApiResponse(
            responseCode = "403",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetailsSchema.class)))
    public ParticipantSearchModel search(
            @RequestParam(required = false) String email, @RequestParam(required = false) String pseudonym) {
        List<ParticipantCard> cards = withdrawals.search(Optional.ofNullable(email), Optional.ofNullable(pseudonym));
        String self = COLLECTION
                + (email != null && !email.isBlank()
                        ? "?email=" + encode(email)
                        : "?pseudonym=" + encode(pseudonym));
        return ParticipantSearchModel.of(cards, self);
    }

    @GetMapping(path = "/{id}", version = "v1")
    @Operation(
            summary = "Read one participant card for withdrawal",
            description = """
                    The same card shape as an item of the search collection: impact counts, preferred \
                    language, e-mail quota flag and vd:anonymize while the person is not yet anonymized.""")
    @ApiResponse(
            responseCode = "200",
            description = "Participant card",
            content = @Content(
                    mediaType = HAL_FORMS,
                    schema = @Schema(implementation = ParticipantCardModel.class),
                    examples = @ExampleObject(value = """
                            {
                              "id": 12,
                              "pseudonym": "R-0007",
                              "email": "ana@example.com",
                              "preferredLanguage": "es-ES",
                              "anonymizedAt": null,
                              "completedTasks": 2,
                              "reserved": null,
                              "finalResponseCount": 20,
                              "draftResponseCount": 0,
                              "tasksReturnableOnDiscard": 2,
                              "emailCanBeSent": true,
                              "_links": {
                                "self": { "href": "/api/v1/participants/12" },
                                "vd:anonymize": { "href": "/api/v1/participants/12/anonymizations" }
                              }
                            }""")))
    @ApiResponse(
            responseCode = "404",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetailsSchema.class)))
    public ParticipantCardModel one(@PathVariable long id) {
        return ParticipantCardModel.of(withdrawals.requireCard(id));
    }

    @PostMapping(path = "/{id}/anonymizations", version = "v1")
    @Operation(
            summary = "Confirm anonymization (keep or discard responses)",
            description = """
                    Sends the confirmation e-mail in the participant preferred language when the rolling \
                    quota allows, then anonymizes atomically. When the quota is exhausted the withdrawal \
                    still proceeds with emailSent false.""")
    @ApiResponse(
            responseCode = "201",
            description = "Withdrawal recorded",
            content = @Content(
                    mediaType = HAL_FORMS,
                    schema = @Schema(implementation = AnonymizationModel.class),
                    examples = @ExampleObject(value = """
                            {
                              "pseudonym": "R-0007",
                              "option": "keep_responses",
                              "emailSent": true,
                              "finalResponsesDiscarded": 0,
                              "tasksReturned": 0,
                              "occurredAt": "2026-11-16T14:30:00Z"
                            }""")))
    @ApiResponse(
            responseCode = "409",
            description = "Already anonymized",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetailsSchema.class)))
    public ResponseEntity<AnonymizationModel> anonymize(
            @PathVariable long id, @RequestBody Map<String, String> body) {
        WithdrawalOption option = WithdrawalOption.parse(body == null ? null : body.get("option"))
                .orElseThrow(() -> new ProblemException(ProblemType.INVALID_REQUEST)
                        .with("reason", "option must be keep_responses or discard_responses"));
        AnonymizationResult result = withdrawals.anonymize(id, option);
        AnonymizationModel model = AnonymizationModel.of(result);
        return ResponseEntity.created(URI.create("/api/v1/participants/" + id)).body(model);
    }

    private static String encode(String value) {
        return java.net.URLEncoder.encode(value == null ? "" : value, java.nio.charset.StandardCharsets.UTF_8);
    }
}
