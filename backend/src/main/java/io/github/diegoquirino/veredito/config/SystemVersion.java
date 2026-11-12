package io.github.diegoquirino.veredito.config;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;
import org.springframework.stereotype.Component;

@Component
public class SystemVersion {

    static final String FALLBACK = "0.3.0+dev";

    private final String value;

    public SystemVersion(ObjectProvider<BuildProperties> buildProperties) {
        BuildProperties build = buildProperties.getIfAvailable();
        String version = build == null ? null : build.get("veredito.version");
        this.value = version == null || version.isBlank() ? FALLBACK : version;
    }

    public String value() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }
}
