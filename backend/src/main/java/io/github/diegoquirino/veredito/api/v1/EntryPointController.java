package io.github.diegoquirino.veredito.api.v1;

import io.github.diegoquirino.veredito.config.SystemVersion;
import io.github.diegoquirino.veredito.config.VereditoProperties;
import io.github.diegoquirino.veredito.persistence.DatabaseStatus;
import org.springframework.hateoas.Link;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/{version}")
public class EntryPointController {

    static final String SELF = "/api/v1";
    static final String REQUEST_CODE = "/api/v1/access-codes";

    private final SystemVersion systemVersion;
    private final VereditoProperties properties;
    private final DatabaseStatus databaseStatus;

    public EntryPointController(
            SystemVersion systemVersion, VereditoProperties properties, DatabaseStatus databaseStatus) {
        this.systemVersion = systemVersion;
        this.properties = properties;
        this.databaseStatus = databaseStatus;
    }

    @GetMapping(version = "1")
    public EntryPointModel entryPoint() {
        boolean available = databaseStatus.isAvailable();
        EntryPointModel model = new EntryPointModel(systemVersion.value(), properties.environment(), available);
        model.add(Link.of(SELF));
        if (available) {
            model.add(Link.of(REQUEST_CODE, "vd:request-code"));
        }
        return model;
    }
}
