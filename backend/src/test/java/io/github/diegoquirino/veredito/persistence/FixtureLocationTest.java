package io.github.diegoquirino.veredito.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

/** Spec 006 T004: the fixture consent form can only reach staging and tests, never production. */
class FixtureLocationTest {

    private static final String STAGING_LOCATIONS = "classpath:db/migration,classpath:db/staging";

    @Test
    void theFixtureIsOnlyInTheStagingLocation() throws IOException {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        List<String> migration = names(resolver.getResources("classpath*:db/migration/*.sql"));
        List<String> staging = names(resolver.getResources("classpath*:db/staging/*.sql"));

        assertThat(migration).containsExactly(
                "V1__create_schema.sql", "V2__insert_default_config.sql", "V3__access.sql",
                "V4__import_preview.sql", "V5__task_queue_lookup.sql");
        assertThat(staging).containsExactly("V6__consent_form_fixture.sql");
        for (Resource resource : resolver.getResources("classpath*:db/migration/*.sql")) {
            assertThat(new String(resource.getContentAsByteArray(), java.nio.charset.StandardCharsets.UTF_8))
                    .as(resource.getFilename())
                    .doesNotContain("fixture-1")
                    .doesNotContain("INSERT INTO consent_form");
        }
    }

    @Test
    void onlyTheStagingProfileListsTheStagingLocation() {
        assertThat(flywayLocations("application.yaml")).isNull();
        assertThat(flywayLocations("application-prod.yaml")).isNull();
        assertThat(flywayLocations("application-staging.yaml")).isEqualTo(STAGING_LOCATIONS);
    }

    private static String flywayLocations(String file) {
        YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(new ClassPathResource(file));
        Properties properties = yaml.getObject();
        return properties == null ? null : properties.getProperty("spring.flyway.locations");
    }

    private static List<String> names(Resource[] resources) {
        return Arrays.stream(resources).map(Resource::getFilename).sorted().toList();
    }
}
