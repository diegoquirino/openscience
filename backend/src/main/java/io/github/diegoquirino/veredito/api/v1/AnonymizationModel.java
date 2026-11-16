package io.github.diegoquirino.veredito.api.v1;

import io.github.diegoquirino.veredito.withdrawal.WithdrawalService.AnonymizationResult;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;

@Relation(itemRelation = "anonymization")
@Schema(name = "AnonymizationResult")
public class AnonymizationModel extends RepresentationModel<AnonymizationModel> {

    private final String pseudonym;
    private final String option;
    private final boolean emailSent;
    private final int finalResponsesDiscarded;
    private final int tasksReturned;
    private final Instant occurredAt;

    private AnonymizationModel(AnonymizationResult result) {
        this.pseudonym = result.pseudonym();
        this.option = result.option().code();
        this.emailSent = result.emailSent();
        this.finalResponsesDiscarded = result.finalResponsesDiscarded();
        this.tasksReturned = result.tasksReturned();
        this.occurredAt = result.occurredAt();
        add(Link.of("/api/v1/participants/" + result.participantId()).withSelfRel());
        add(Link.of("/api/v1/participants", LinkRelations.PARTICIPANTS.curied()));
    }

    static AnonymizationModel of(AnonymizationResult result) {
        return new AnonymizationModel(result);
    }

    public String getPseudonym() {
        return pseudonym;
    }

    public String getOption() {
        return option;
    }

    public boolean isEmailSent() {
        return emailSent;
    }

    public int getFinalResponsesDiscarded() {
        return finalResponsesDiscarded;
    }

    public int getTasksReturned() {
        return tasksReturned;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }
}
