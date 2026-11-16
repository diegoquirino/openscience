package io.github.diegoquirino.veredito.domain.withdrawal;

import java.util.Locale;
import java.util.Optional;

/** The two administrator choices when handling an LGPD withdrawal (spec 011). */
public enum WithdrawalOption {
    KEEP_RESPONSES("keep_responses"),
    DISCARD_RESPONSES("discard_responses");

    private final String code;

    WithdrawalOption(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    public boolean discardsResponses() {
        return this == DISCARD_RESPONSES;
    }

    public static Optional<WithdrawalOption> parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        for (WithdrawalOption option : values()) {
            if (option.code.equals(normalized)) {
                return Optional.of(option);
            }
        }
        return Optional.empty();
    }
}
