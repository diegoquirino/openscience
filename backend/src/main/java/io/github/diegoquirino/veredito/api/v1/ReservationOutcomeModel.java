package io.github.diegoquirino.veredito.api.v1;

import io.github.diegoquirino.veredito.domain.queue.QueueOutcome;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;

@Schema(name = "ReservationOutcome", description = "No task was reserved; the outcome drives the end screen.")
@Relation(itemRelation = "vd:reservation-outcome")
public class ReservationOutcomeModel extends RepresentationModel<ReservationOutcomeModel> {

    private final String outcome;

    public ReservationOutcomeModel(QueueOutcome outcome) {
        this.outcome = outcome.slug();
    }

    public String getOutcome() {
        return outcome;
    }
}
