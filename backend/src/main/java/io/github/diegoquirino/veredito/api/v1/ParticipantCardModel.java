package io.github.diegoquirino.veredito.api.v1;

import io.github.diegoquirino.veredito.domain.withdrawal.WithdrawalImpact;
import io.github.diegoquirino.veredito.withdrawal.WithdrawalQuery.ReservedSummary;
import io.github.diegoquirino.veredito.withdrawal.WithdrawalService.ParticipantCard;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;

@Relation(collectionRelation = "vd:participant", itemRelation = "vd:participant")
@Schema(name = "ParticipantCard")
public class ParticipantCardModel extends RepresentationModel<ParticipantCardModel> {

    private final long id;
    private final String pseudonym;
    private final String email;
    private final String preferredLanguage;
    private final Instant anonymizedAt;
    private final long completedTasks;
    private final ReservedModel reserved;
    private final long finalResponseCount;
    private final long draftResponseCount;
    private final long tasksReturnableOnDiscard;
    private final boolean emailCanBeSent;

    private ParticipantCardModel(ParticipantCard card) {
        this.id = card.id();
        this.pseudonym = card.pseudonym();
        this.email = card.email();
        this.preferredLanguage = card.preferredLanguage();
        this.anonymizedAt = card.anonymizedAt();
        this.completedTasks = card.completedTasks();
        this.reserved = card.reserved() == null ? null : ReservedModel.of(card.reserved());
        WithdrawalImpact impact = card.impact();
        this.finalResponseCount = impact.finalResponseCount();
        this.draftResponseCount = impact.draftResponseCount();
        this.tasksReturnableOnDiscard = impact.tasksReturnableOnDiscard();
        this.emailCanBeSent = card.emailCanBeSent();
        add(org.springframework.hateoas.Link.of("/api/v1/participants/" + id).withSelfRel());
        if (anonymizedAt == null) {
            add(org.springframework.hateoas.Link.of(
                    "/api/v1/participants/" + id + "/anonymizations", LinkRelations.ANONYMIZE.curied()));
        }
    }

    static ParticipantCardModel of(ParticipantCard card) {
        return new ParticipantCardModel(card);
    }

    public long getId() {
        return id;
    }

    public String getPseudonym() {
        return pseudonym;
    }

    public String getEmail() {
        return email;
    }

    public String getPreferredLanguage() {
        return preferredLanguage;
    }

    public Instant getAnonymizedAt() {
        return anonymizedAt;
    }

    public long getCompletedTasks() {
        return completedTasks;
    }

    public ReservedModel getReserved() {
        return reserved;
    }

    public long getFinalResponseCount() {
        return finalResponseCount;
    }

    public long getDraftResponseCount() {
        return draftResponseCount;
    }

    public long getTasksReturnableOnDiscard() {
        return tasksReturnableOnDiscard;
    }

    public boolean isEmailCanBeSent() {
        return emailCanBeSent;
    }

    @Schema(name = "ReservedSummary")
    public record ReservedModel(int block, int role, Instant expiresAt) {
        static ReservedModel of(ReservedSummary reserved) {
            return new ReservedModel(reserved.block(), reserved.role(), reserved.expiresAt());
        }
    }
}
