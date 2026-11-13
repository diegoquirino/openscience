package io.github.diegoquirino.veredito.support;

import java.time.Duration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

/** MySQL 9.7, the version of Cloud SQL; one container per cached application context. */
@TestConfiguration(proxyBeanMethods = false)
public class MySqlContainerConfig {

    public static final String IMAGE = "mysql:9.7";

    /** The first initialization of InnoDB can take minutes on a busy Docker Desktop disk. */
    public static final Duration STARTUP_TIMEOUT = Duration.ofMinutes(5);

    @Bean
    @ServiceConnection
    public MySQLContainer mysql() {
        return newContainer();
    }

    public static MySQLContainer newContainer() {
        return new MySQLContainer(DockerImageName.parse(IMAGE))
                .withStartupTimeoutSeconds((int) STARTUP_TIMEOUT.toSeconds());
    }
}
