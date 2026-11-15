package io.github.diegoquirino.veredito.onboarding;

import io.github.diegoquirino.veredito.domain.onboarding.ScreeningExample;
import java.io.IOException;
import java.io.InputStream;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * Loads {@code screening/screening-example.json} once at startup and validates it, so a malformed example
 * stops the application instead of reaching a participant (research section 7).
 */
@Component
public class ScreeningExampleLoader {

    static final String RESOURCE = "screening/screening-example.json";

    private final ScreeningExample example;

    public ScreeningExampleLoader(JsonMapper json) {
        this.example = load(json, RESOURCE);
    }

    public ScreeningExample example() {
        return example;
    }

    static ScreeningExample load(JsonMapper json, String resource) {
        try (InputStream in = new ClassPathResource(resource).getInputStream()) {
            return json.readValue(in, ScreeningExample.class).requireValid();
        } catch (IOException | RuntimeException ex) {
            throw new IllegalStateException("Cannot load the screening example " + resource + ": " + ex.getMessage(), ex);
        }
    }
}
