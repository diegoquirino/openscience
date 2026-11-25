package io.github.diegoquirino.veredito.invite;

import io.github.diegoquirino.veredito.persistence.InviteSettingsRepository.InviteSettings;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;

/** Substitutes {{nome}}, {{caae}}, {{url}}, {{coleta_inicio}}, {{coleta_fim}} in invite templates. */
public final class InviteTemplateRenderer {

    private static final ZoneId DISPLAY_ZONE = ZoneId.of("America/Sao_Paulo");
    private static final DateTimeFormatter DISPLAY =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.ROOT).withZone(DISPLAY_ZONE);

    private InviteTemplateRenderer() {}

    public static String render(String template, String nome, InviteSettings settings) {
        String text = Objects.requireNonNullElse(template, "");
        text = text.replace("{{nome}}", Objects.requireNonNullElse(nome, ""));
        text = text.replace("{{caae}}", Objects.requireNonNullElse(settings.caae(), ""));
        text = text.replace("{{url}}", Objects.requireNonNullElse(settings.platformUrl(), ""));
        text = text.replace("{{coleta_inicio}}", format(settings.collectionStartAt()));
        text = text.replace("{{coleta_fim}}", format(settings.collectionEndAt()));
        return text;
    }

    private static String format(Instant instant) {
        return instant == null ? "" : DISPLAY.format(instant);
    }
}
