package io.github.diegoquirino.veredito.domain.importing;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Whether a file may contain systems other than {@code minitest} (FR-011). */
public final class StudyPhaseGate {

    public static final String MINITEST = "minitest";
    public static final String PHASE_PILOT = "pilot";
    public static final String PHASE_COLLECTION = "collection";

    private StudyPhaseGate() {}

    /** {@code true} when only Minitest rows are accepted. */
    public static boolean minitestOnly(boolean stagingProfile, String studyPhase) {
        if (stagingProfile) {
            return true;
        }
        return !PHASE_COLLECTION.equals(studyPhase);
    }

    /**
     * Distinct non-minitest {@code system} values in the data rows, in first-seen order.
     * Empty when the file is allowed.
     */
    public static List<String> foreignSystems(List<Map<String, String>> dataRows) {
        Objects.requireNonNull(dataRows, "dataRows");
        Set<String> foreign = new LinkedHashSet<>();
        for (Map<String, String> row : dataRows) {
            String system = row.get("system");
            if (system != null && !system.isEmpty() && !MINITEST.equals(system)) {
                foreign.add(system);
            }
        }
        return List.copyOf(foreign);
    }
}
