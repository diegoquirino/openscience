package io.github.diegoquirino.veredito.importing;

import io.github.diegoquirino.veredito.api.ProblemException;
import io.github.diegoquirino.veredito.api.ProblemType;
import io.github.diegoquirino.veredito.config.VereditoProperties;
import io.github.diegoquirino.veredito.config.VereditoProperties.Environment;
import io.github.diegoquirino.veredito.domain.importing.StudyPhaseGate;
import io.github.diegoquirino.veredito.persistence.ConfigRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Reads and switches the study phase (spec 005 US4). */
@Service
public class StudyPhaseService {

    private final ConfigRepository config;
    private final VereditoProperties properties;
    private final JdbcClient jdbc;
    private final Clock clock;

    public StudyPhaseService(
            ConfigRepository config, VereditoProperties properties, JdbcClient jdbc, Clock clock) {
        this.config = config;
        this.properties = properties;
        this.jdbc = jdbc;
        this.clock = clock;
    }

    public record StudyPhaseView(
            String phase, Instant changedAt, boolean switchAllowed, boolean minitestOnly) {}

    @Transactional(readOnly = true)
    public StudyPhaseView current() {
        refuseIfStaging();
        String phase = config.studyPhase();
        boolean staging = properties.environment() == Environment.STAGING;
        boolean empty = studyEmpty();
        boolean switchAllowed = !staging && StudyPhaseGate.PHASE_PILOT.equals(phase) && empty;
        return new StudyPhaseView(
                phase,
                config.studyPhaseChangedAt(),
                switchAllowed,
                StudyPhaseGate.minitestOnly(staging, phase));
    }

    @Transactional
    public StudyPhaseView switchToCollection() {
        refuseIfStaging();
        Instant now = clock.instant().truncatedTo(ChronoUnit.MICROS);
        config.lockForUpdate();
        String phase = config.studyPhase();
        if (!StudyPhaseGate.PHASE_PILOT.equals(phase) || !studyEmpty()) {
            throw new ProblemException(ProblemType.PHASE_SWITCH_NOT_ALLOWED);
        }
        config.updateStudyPhase(StudyPhaseGate.PHASE_COLLECTION, now);
        return new StudyPhaseView(
                StudyPhaseGate.PHASE_COLLECTION,
                now,
                false,
                StudyPhaseGate.minitestOnly(false, StudyPhaseGate.PHASE_COLLECTION));
    }

    private void refuseIfStaging() {
        if (properties.environment() == Environment.STAGING) {
            throw new ProblemException(ProblemType.NOT_FOUND);
        }
    }

    private boolean studyEmpty() {
        long items = jdbc.sql("SELECT COUNT(*) FROM item").query(Long.class).single();
        long participants = jdbc.sql("SELECT COUNT(*) FROM participant").query(Long.class).single();
        long responses = jdbc.sql("SELECT COUNT(*) FROM response").query(Long.class).single();
        return items == 0 && participants == 0 && responses == 0;
    }

    public static void requireCollectionPhase(String phase) {
        if (phase == null || phase.isBlank()) {
            throw ProblemException.invalidFields(Map.of("phase", "required"));
        }
        if (!StudyPhaseGate.PHASE_COLLECTION.equals(phase)) {
            throw ProblemException.invalidFields(Map.of("phase", "collection"));
        }
    }
}
