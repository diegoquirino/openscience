package io.github.diegoquirino.veredito.domain.access;

import java.util.Locale;

/** Who holds a session; the code is the value of {@code access_session.subject} and of the API. */
public enum Subject {
    PARTICIPANT,
    ADMINISTRATOR;

    public String code() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static Subject fromCode(String code) {
        return valueOf(code.toUpperCase(Locale.ROOT));
    }
}
