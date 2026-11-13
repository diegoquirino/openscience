package io.github.diegoquirino.veredito.config;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/** Checks the key of the code hash at startup (research section 4). */
@Configuration(proxyBeanMethods = false)
public class AccessConfig {

    public static final int MIN_KEY_LENGTH = 32;

    private static final Logger log = LoggerFactory.getLogger(AccessConfig.class);

    @Bean
    public CodeHashKey codeHashKey(AccessProperties properties, Environment environment) {
        String key = properties.codeHashKey();
        if (key != null && !key.isBlank()) {
            if (key.length() < MIN_KEY_LENGTH) {
                throw new IllegalStateException(
                        "CODE_HASH_KEY must have at least " + MIN_KEY_LENGTH + " characters");
            }
            return new CodeHashKey(key.getBytes(StandardCharsets.UTF_8));
        }
        if (environment.matchesProfiles("prod")) {
            throw new IllegalStateException(
                    "CODE_HASH_KEY is missing; production reads it from the secret veredito-code-key");
        }
        log.warn("CODE_HASH_KEY is not set; using a random key, so codes stop working after a restart");
        byte[] random = new byte[MIN_KEY_LENGTH];
        new SecureRandom().nextBytes(random);
        return new CodeHashKey(random);
    }

    /** The key bytes; {@link #toString()} never shows them. */
    public record CodeHashKey(byte[] bytes) {

        public CodeHashKey {
            bytes = bytes.clone();
        }

        @Override
        public byte[] bytes() {
            return bytes.clone();
        }

        @Override
        public String toString() {
            return "CodeHashKey[hidden]";
        }
    }
}
