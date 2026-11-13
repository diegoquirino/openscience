package io.github.diegoquirino.veredito.support;

import jakarta.mail.internet.MimeMessage;
import java.io.InputStream;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

/** The application's mail sender, which a test can turn into an SMTP outage within the shared context. */
public class SwitchableMailSender implements JavaMailSender {

    private final JavaMailSender delegate;
    private volatile boolean failing;

    SwitchableMailSender(JavaMailSender delegate) {
        this.delegate = delegate;
    }

    /** While on, every send fails as a refused SMTP connection would. */
    public void failing(boolean failing) {
        this.failing = failing;
    }

    @Override
    public MimeMessage createMimeMessage() {
        return delegate.createMimeMessage();
    }

    @Override
    public MimeMessage createMimeMessage(InputStream contentStream) {
        return delegate.createMimeMessage(contentStream);
    }

    @Override
    public void send(MimeMessage... mimeMessages) {
        refuseWhenFailing();
        delegate.send(mimeMessages);
    }

    @Override
    public void send(SimpleMailMessage... simpleMessages) {
        refuseWhenFailing();
        delegate.send(simpleMessages);
    }

    private void refuseWhenFailing() {
        if (failing) {
            throw new MailSendException("Simulated SMTP outage");
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    public static class Config {

        @Bean
        static BeanPostProcessor switchableMailSender() {
            return new BeanPostProcessor() {
                @Override
                public Object postProcessAfterInitialization(Object bean, String beanName) {
                    return bean instanceof JavaMailSender sender && !(bean instanceof SwitchableMailSender)
                            ? new SwitchableMailSender(sender)
                            : bean;
                }
            };
        }
    }
}
