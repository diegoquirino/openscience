package io.github.diegoquirino.veredito.domain.access;

import java.util.Locale;

/** The participant's pseudonym: {@code R-} and the id with at least four digits (FR-006). */
public final class Pseudonym {

    private Pseudonym() {}

    public static String of(long participantId) {
        if (participantId < 1) {
            throw new IllegalArgumentException("Participant ids start at 1");
        }
        return String.format(Locale.ROOT, "R-%04d", participantId);
    }
}
