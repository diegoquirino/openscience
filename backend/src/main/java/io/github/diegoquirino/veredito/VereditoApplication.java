package io.github.diegoquirino.veredito;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;

/** Sign-in is by e-mail code only, so the default in-memory user of Spring Boot is left out. */
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@ConfigurationPropertiesScan
public class VereditoApplication {

    public static void main(String[] args) {
        SpringApplication.run(VereditoApplication.class, args);
    }
}
