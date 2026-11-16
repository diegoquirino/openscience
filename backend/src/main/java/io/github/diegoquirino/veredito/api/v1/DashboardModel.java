package io.github.diegoquirino.veredito.api.v1;

import io.github.diegoquirino.veredito.dashboard.DashboardService.Participants;
import io.github.diegoquirino.veredito.dashboard.DashboardService.Reservation;
import io.github.diegoquirino.veredito.dashboard.DashboardService.Snapshot;
import io.github.diegoquirino.veredito.domain.dashboard.DashboardProgress;
import io.github.diegoquirino.veredito.domain.dashboard.EmailQuota;
import io.github.diegoquirino.veredito.domain.dashboard.LanguageTally;
import io.github.diegoquirino.veredito.domain.dashboard.ParticipantTotals.Contributor;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.hateoas.RepresentationModel;

@Schema(
        name = "Dashboard",
        description = "Read-only snapshot of the data collection: queue progress, reservations, participants, "
                + "languages and the e-mail quota. Never carries a name or an e-mail.")
public class DashboardModel extends RepresentationModel<DashboardModel> {

    private final Snapshot snapshot;

    public DashboardModel(Snapshot snapshot) {
        this.snapshot = snapshot;
    }

    @Schema(description = "The instant every number was computed against.")
    public Instant getGeneratedAt() {
        return snapshot.generatedAt();
    }

    @Schema(description = "Lowest queue position that is free or reclaimable; null when none is left.")
    public Integer getFirstFreeQueuePosition() {
        return snapshot.firstFreeQueuePosition();
    }

    @Schema(description = "The overall row first (system and batchId null), then one row per system and batch. "
            + "Free includes expired reservations.")
    public List<DashboardProgress.Row> getProgress() {
        return snapshot.progress();
    }

    @Schema(description = "Every reserved task, oldest expiry first; expired ones are flagged.")
    public List<Reservation> getReservations() {
        return snapshot.reservations();
    }

    @Schema(description = "Participants by status and skill, and completed tasks per pseudonym.")
    public ParticipantsModel getParticipants() {
        Participants participants = snapshot.participants();
        return new ParticipantsModel(participants);
    }

    @Schema(description = "Always pt-BR, en-US and es-ES, in that order, zeros included.")
    public List<LanguageTally.Row> getLanguages() {
        return snapshot.languages();
    }

    @Schema(description = "Rolling 24-hour e-mail budget; lowRemaining when fewer than 10 are left.")
    public EmailQuota getEmailQuota() {
        return snapshot.emailQuota();
    }

    /** The participants section: flat status counts, the skill map and the contributors. */
    public record ParticipantsModel(
            long eligible,
            long ineligible,
            long declinedConsent,
            long incomplete,
            Map<String, Long> bySkill,
            List<Contributor> completedByPseudonym) {

        ParticipantsModel(Participants participants) {
            this(
                    participants.totals().eligible(),
                    participants.totals().ineligible(),
                    participants.totals().declinedConsent(),
                    participants.totals().incomplete(),
                    participants.totals().bySkill(),
                    participants.completedByPseudonym());
        }
    }
}