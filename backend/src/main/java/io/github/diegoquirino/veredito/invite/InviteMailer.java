package io.github.diegoquirino.veredito.invite;

import io.github.diegoquirino.veredito.config.AccessProperties;
import io.github.diegoquirino.veredito.domain.access.EmailAddress;
import io.github.diegoquirino.veredito.persistence.EmailTemplateRepository;
import io.github.diegoquirino.veredito.persistence.InviteSettingsRepository;
import io.github.diegoquirino.veredito.persistence.InviteSettingsRepository.InviteSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/** Sends research invite e-mails from editable templates (spec 031). */
@Component
public class InviteMailer {

    private static final Logger log = LoggerFactory.getLogger(InviteMailer.class);

    private final JavaMailSender mail;
    private final AccessProperties properties;
    private final EmailTemplateRepository templates;
    private final InviteSettingsRepository settings;

    public InviteMailer(
            JavaMailSender mail,
            AccessProperties properties,
            EmailTemplateRepository templates,
            InviteSettingsRepository settings) {
        this.mail = mail;
        this.properties = properties;
        this.templates = templates;
        this.settings = settings;
    }

    /**
     * Renders and sends one invite. Returns {@code true} when SMTP accepted the message;
     * failures are logged and return {@code false} (caller keeps {@code sent_at} null for retry).
     */
    public boolean send(InviteTemplateKind kind, EmailAddress to, String nome) {
        var template = templates.require(kind);
        InviteSettings inviteSettings = settings.load();
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("Veredito <" + properties.mailFrom() + ">");
        message.setTo(to.value());
        message.setSubject(InviteTemplateRenderer.render(template.subject(), nome, inviteSettings));
        message.setText(InviteTemplateRenderer.render(template.body(), nome, inviteSettings));
        try {
            mail.send(message);
            return true;
        } catch (MailException e) {
            log.warn(
                    "Invite e-mail {} not sent to {}: {}",
                    kind.code(),
                    to.value(),
                    e.getClass().getSimpleName());
            return false;
        }
    }
}
