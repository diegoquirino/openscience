package io.github.diegoquirino.veredito.domain.access;

import java.util.Locale;
import java.util.Optional;

/** A normalized e-mail: trimmed, lowercased, at most 254 characters, {@code local@domain.tld}. */
public record EmailAddress(String value) {

    public static final int MAX_LENGTH = 254;

    public EmailAddress {
        if (value == null || !value.equals(normalize(value)) || !isWellFormed(value)) {
            throw new IllegalArgumentException("Not a normalized e-mail address");
        }
    }

    /** The normalized address, or empty when the text is not a valid e-mail. */
    public static Optional<EmailAddress> parse(String raw) {
        if (raw == null) {
            return Optional.empty();
        }
        String normalized = normalize(raw);
        return isWellFormed(normalized) ? Optional.of(new EmailAddress(normalized)) : Optional.empty();
    }

    private static String normalize(String raw) {
        return raw.strip().toLowerCase(Locale.ROOT);
    }

    private static boolean isWellFormed(String address) {
        if (address.isEmpty() || address.length() > MAX_LENGTH || address.chars().anyMatch(Character::isWhitespace)) {
            return false;
        }
        int at = address.indexOf('@');
        if (at <= 0 || at != address.lastIndexOf('@')) {
            return false;
        }
        String domain = address.substring(at + 1);
        int dot = domain.indexOf('.');
        return dot > 0 && !domain.endsWith(".") && !domain.contains("..");
    }

    @Override
    public String toString() {
        return "EmailAddress[hidden]";
    }
}
