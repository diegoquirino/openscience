package io.github.diegoquirino.veredito.api.v1;

import io.github.diegoquirino.veredito.withdrawal.WithdrawalService.ParticipantCard;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;

@Relation(itemRelation = "participants")
@Schema(name = "ParticipantSearch")
public class ParticipantSearchModel extends RepresentationModel<ParticipantSearchModel> {

    private final List<ParticipantCardModel> participants;

    private ParticipantSearchModel(List<ParticipantCardModel> participants, String selfHref) {
        this.participants = List.copyOf(participants);
        add(Link.of(selfHref).withSelfRel());
    }

    static ParticipantSearchModel of(List<ParticipantCard> cards, String selfHref) {
        return new ParticipantSearchModel(cards.stream().map(ParticipantCardModel::of).toList(), selfHref);
    }

    @Schema(description = "Zero or one participant card matching the exact search.")
    public List<ParticipantCardModel> getParticipants() {
        return participants;
    }
}
