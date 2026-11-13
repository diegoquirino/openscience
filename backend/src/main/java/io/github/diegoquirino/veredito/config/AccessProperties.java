package io.github.diegoquirino.veredito.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Settings of the sign-in ({@code specs/004-email-code-access/research.md}, sections 4 to 6). The key
 * of the code hash is read once by {@link AccessConfig} and never logged.
 */
@ConfigurationProperties("veredito.access")
public record AccessProperties(
        String codeHashKey,
        @DefaultValue("10m") Duration codeValidity,
        @DefaultValue("60s") Duration resendInterval,
        @DefaultValue("5") int hourlyLimit,
        @DefaultValue("5") int maxAttempts,
        @DefaultValue("24h") Duration sessionDuration,
        @DefaultValue("veredito@localhost") String mailFrom) {

    @Override
    public String toString() {
        return "AccessProperties[codeValidity=" + codeValidity + ", resendInterval=" + resendInterval
                + ", hourlyLimit=" + hourlyLimit + ", maxAttempts=" + maxAttempts + ", sessionDuration="
                + sessionDuration + "]";
    }
}
