package io.github.diegoquirino.veredito.dashboard;

import io.github.diegoquirino.veredito.dashboard.DashboardQuery.ReservationRow;
import io.github.diegoquirino.veredito.domain.dashboard.DashboardProgress;
import io.github.diegoquirino.veredito.domain.dashboard.EmailQuota;
import io.github.diegoquirino.veredito.domain.dashboard.LanguageTally;
import io.github.diegoquirino.veredito.domain.dashboard.ParticipantTotals;
import io.github.diegoquirino.veredito.withdrawal.EmailQuotaService;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Builds the progress snapshot of spec 010; read-only, regenerated on each call from one instant. */
@Service
public class DashboardService {

    /** A reserved task; {@code expired} when its expiry is not after the snapshot instant. */
    public record Reservation(
            String pseudonym, String system, int block, int role, long draftCount, Instant expiresAt, boolean expired) {}

    /** Participant totals and the completed-task shares. */
    public record Participants(ParticipantTotals totals, List<ParticipantTotals.Contributor> completedByPseudonym) {}

    /** The whole snapshot. */
    public record Snapshot(
            Instant generatedAt,
            Integer firstFreeQueuePosition,
            List<DashboardProgress.Row> progress,
            List<Reservation> reservations,
            Participants participants,
            List<LanguageTally.Row> languages,
            EmailQuota emailQuota) {

        /** Whether any item was imported; otherwise every number is zero and the UI offers the import. */
        public boolean empty() {
            return progress.get(0).items() == 0;
        }
    }

    private final DashboardQuery query;
    private final EmailQuotaService emailQuota;
    private final Clock clock;

    public DashboardService(DashboardQuery query, EmailQuotaService emailQuota, Clock clock) {
        this.query = query;
        this.emailQuota = emailQuota;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public Snapshot snapshot() {
        Instant now = clock.instant();
        DashboardProgress.Result progress = DashboardProgress.build(query.blocks(), query.tasks(), now);
        List<Reservation> reservations = query.reservations().stream()
                .map(row -> reservation(row, now))
                .toList();
        Participants participants = new Participants(
                ParticipantTotals.of(query.participants()),
                ParticipantTotals.contributors(query.completedByPseudonym()));
        List<LanguageTally.Row> languages =
                LanguageTally.rows(query.preferredLanguages(), query.finalResponseLanguages());
        return new Snapshot(now, progress.firstFreeQueuePosition(), progress.rows(), reservations, participants,
                languages, emailQuota.current());
    }

    private static Reservation reservation(ReservationRow row, Instant now) {
        return new Reservation(row.pseudonym(), row.system(), row.block(), row.role(), row.draftCount(),
                row.expiresAt(), !row.expiresAt().isAfter(now));
    }
}
