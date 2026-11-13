package io.github.diegoquirino.veredito.domain.access;

import java.util.Optional;

/** The name typed at sign-in: trimmed, 2 to 120 characters. */
public record PersonName(String value) {

    public static final int MIN_LENGTH = 2;
    public static final int MAX_LENGTH = 120;

    public PersonName {
        if (value == null || !value.equals(value.strip()) || value.length() < MIN_LENGTH
                || value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("Not a trimmed name of 2 to 120 characters");
        }
    }

    /** The trimmed name, or empty when it is missing, too short or too long. */
    public static Optional<PersonName> parse(String raw) {
        if (raw == null) {
            return Optional.empty();
        }
        String trimmed = raw.strip();
        return trimmed.length() >= MIN_LENGTH && trimmed.length() <= MAX_LENGTH
                ? Optional.of(new PersonName(trimmed))
                : Optional.empty();
    }
}
