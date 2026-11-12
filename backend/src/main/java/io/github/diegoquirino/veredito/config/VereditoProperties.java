package io.github.diegoquirino.veredito.config;

import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Locale;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties("veredito")
public record VereditoProperties(@DefaultValue("development") Environment environment) {

    public enum Environment {
        PRODUCTION,
        STAGING,
        DEVELOPMENT;

        @JsonValue
        public String code() {
            return name().toLowerCase(Locale.ROOT);
        }
    }
}
