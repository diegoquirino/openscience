package io.github.diegoquirino.veredito.api.v1;

import io.github.diegoquirino.veredito.importing.StudyPhaseService.StudyPhaseView;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import org.springframework.hateoas.RepresentationModel;

@Schema(name = "StudyPhase")
public class StudyPhaseModel extends RepresentationModel<StudyPhaseModel> {

    private final String phase;
    private final Instant changedAt;
    private final boolean switchAllowed;
    private final boolean minitestOnly;

    public StudyPhaseModel(StudyPhaseView view) {
        this.phase = view.phase();
        this.changedAt = view.changedAt();
        this.switchAllowed = view.switchAllowed();
        this.minitestOnly = view.minitestOnly();
    }

    public String getPhase() {
        return phase;
    }

    public Instant getChangedAt() {
        return changedAt;
    }

    public boolean isSwitchAllowed() {
        return switchAllowed;
    }

    public boolean isMinitestOnly() {
        return minitestOnly;
    }
}
