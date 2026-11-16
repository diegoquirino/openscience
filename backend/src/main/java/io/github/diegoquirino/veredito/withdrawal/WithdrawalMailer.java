package io.github.diegoquirino.veredito.withdrawal;

import io.github.diegoquirino.veredito.api.ProblemException;
import io.github.diegoquirino.veredito.api.ProblemType;
import io.github.diegoquirino.veredito.config.AccessProperties;
import io.github.diegoquirino.veredito.domain.withdrawal.WithdrawalOption;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/** Confirmation e-mail for an LGPD withdrawal, in the participant's preferred language. */
@Component
public class WithdrawalMailer {

    private static final Logger log = LoggerFactory.getLogger(WithdrawalMailer.class);

    private final JavaMailSender mail;
    private final MessageSource messages;
    private final AccessProperties properties;

    public WithdrawalMailer(JavaMailSender mail, MessageSource messages, AccessProperties properties) {
        this.mail = mail;
        this.messages = messages;
        this.properties = properties;
    }

    public void send(String to, String name, WithdrawalOption option, Locale locale) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("Veredito <" + properties.mailFrom() + ">");
        message.setTo(to);
        message.setSubject(messages.getMessage("mail.withdrawal.subject", null, locale));
        String optionLabel = messages.getMessage("mail.withdrawal.option." + option.code(), null, locale);
        message.setText(messages.getMessage(
                "mail.withdrawal.body",
                new Object[] {name == null || name.isBlank() ? to : name, optionLabel},
                locale));
        try {
            mail.send(message);
        } catch (MailException e) {
            log.warn("Withdrawal confirmation e-mail not sent: {}", e.getClass().getSimpleName());
            throw new ProblemException(ProblemType.CODE_NOT_SENT);
        }
    }
}
