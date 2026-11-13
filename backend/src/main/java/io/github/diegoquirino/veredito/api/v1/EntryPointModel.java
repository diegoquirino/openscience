package io.github.diegoquirino.veredito.api.v1;

import io.github.diegoquirino.veredito.config.VereditoProperties.Environment;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.hateoas.RepresentationModel;

@Schema(name = "EntryPoint", description = "State of the system and the links offered to the caller.")
public class EntryPointModel extends RepresentationModel<EntryPointModel> {

    private final String systemVersion;
    private final Environment environment;
    private final boolean available;

    public EntryPointModel(String systemVersion, Environment environment, boolean available) {
        this.systemVersion = systemVersion;
        this.environment = environment;
        this.available = available;
    }

    @Schema(description = "Project version and short commit, as in the page footer.", example = "0.3.0+1a2b3c4")
    public String getSystemVersion() {
        return systemVersion;
    }

    @Schema(description = "production, staging or development.", example = "staging")
    public Environment getEnvironment() {
        return environment;
    }

    @Schema(description = "False when the database does not answer within 2 seconds.", example = "true")
    public boolean isAvailable() {
        return available;
    }
}
