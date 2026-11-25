package io.github.diegoquirino.veredito.invite;

import java.util.Locale;
import java.util.Optional;

/** Kinds of research invite e-mails (spec 031). */
public enum InviteTemplateKind {
    INICIAL("inicial"),
    LEMBRETE("lembrete"),
    AGRADECIMENTO("agradecimento");

    private final String code;

    InviteTemplateKind(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    public static Optional<InviteTemplateKind> fromCode(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        for (InviteTemplateKind kind : values()) {
            if (kind.code.equals(normalized)) {
                return Optional.of(kind);
            }
        }
        return Optional.empty();
    }
}
