package io.github.diegoquirino.veredito.api.v1;

import io.github.diegoquirino.veredito.config.VereditoProperties.Environment;
import org.springframework.hateoas.RepresentationModel;

public class EntryPointModel extends RepresentationModel<EntryPointModel> {

    private final String systemVersion;
    private final Environment environment;
    private final boolean available;

    public EntryPointModel(String systemVersion, Environment environment, boolean available) {
        this.systemVersion = systemVersion;
        this.environment = environment;
        this.available = available;
    }

    public String getSystemVersion() {
        return systemVersion;
    }

    public Environment getEnvironment() {
        return environment;
    }

    public boolean isAvailable() {
        return available;
    }
}
