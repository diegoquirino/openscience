package io.github.diegoquirino.veredito;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class VereditoApplication {

    public static void main(String[] args) {
        SpringApplication.run(VereditoApplication.class, args);
    }
}
