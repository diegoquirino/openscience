package io.github.diegoquirino.veredito.support;

import java.net.URI;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

/** Mailpit v1.33.1, the mailbox of local staging; one container for the whole test run. */
@TestConfiguration(proxyBeanMethods = false)
public class MailpitContainerConfig {

    public static final String IMAGE = "axllent/mailpit:v1.33.1";
    public static final int SMTP_PORT = 1025;
    public static final int HTTP_PORT = 8025;

    private static final GenericContainer<?> MAILPIT = new GenericContainer<>(DockerImageName.parse(IMAGE))
            .withExposedPorts(SMTP_PORT, HTTP_PORT)
            .waitingFor(Wait.forListeningPort())
            .withStartupTimeout(MySqlContainerConfig.STARTUP_TIMEOUT);

    static synchronized GenericContainer<?> started() {
        if (!MAILPIT.isRunning()) {
            MAILPIT.start();
        }
        return MAILPIT;
    }

    @Bean
    public DynamicPropertyRegistrar mailpitProperties() {
        return registry -> {
            GenericContainer<?> mailpit = started();
            registry.add("spring.mail.host", mailpit::getHost);
            registry.add("spring.mail.port", () -> mailpit.getMappedPort(SMTP_PORT));
        };
    }

    @Bean
    public MailpitClient mailpitClient() {
        GenericContainer<?> mailpit = started();
        return new MailpitClient(URI.create("http://" + mailpit.getHost() + ":" + mailpit.getMappedPort(HTTP_PORT)));
    }
}
