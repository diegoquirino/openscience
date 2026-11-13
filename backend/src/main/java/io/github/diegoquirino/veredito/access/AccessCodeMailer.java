package io.github.diegoquirino.veredito.access;

import io.github.diegoquirino.veredito.api.ProblemException;
import io.github.diegoquirino.veredito.api.ProblemType;
import io.github.diegoquirino.veredito.config.AccessProperties;
import io.github.diegoquirino.veredito.domain.access.EmailAddress;
import io.github.diegoquirino.veredito.domain.access.PersonName;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/** The plain-text e-mail with the access code, in the language of the request (research sections 6 and 7). */
@Component
public class AccessCodeMailer {

    private static final Logger log = LoggerFactory.getLogger(AccessCodeMailer.class);

    private final JavaMailSender mail;
    private final MessageSource messages;
    private final AccessProperties properties;

    public AccessCodeMailer(JavaMailSender mail, MessageSource messages, AccessProperties properties) {
        this.mail = mail;
        this.messages = messages;
        this.properties = properties;
    }

    /** Sends the code; an SMTP failure or timeout becomes {@code code-not-sent}. */
    public void send(PersonName name, EmailAddress to, String code, Locale locale) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("Veredito <" + properties.mailFrom() + ">");
        message.setTo(to.value());
        message.setSubject(messages.getMessage("mail.access-code.subject", null, locale));
        message.setText(messages.getMessage(
                "mail.access-code.body",
                new Object[] {name.value(), code, properties.codeValidity().toMinutes()},
                locale));
        try {
            mail.send(message);
        } catch (MailException e) {
            // The exception message can carry the recipient address.
            log.warn("Access code e-mail not sent: {}", e.getClass().getSimpleName());
            throw new ProblemException(ProblemType.CODE_NOT_SENT);
        }
    }
}
