package io.github.diegoquirino.veredito.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;
import java.util.List;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** OpenAPI groups v1 and index, at /v3/api-docs/{group} and in Swagger UI. */
@Configuration(proxyBeanMethods = false)
public class OpenApiConfig {

    public static final String SESSION_SCHEME = "session";
    public static final String SESSION_COOKIE = "veredito-session";

    private static final String STUDY = "Veredito is the data labeling platform of a doctoral study on software"
            + " testing: volunteers label how edits to use cases affect their tests. Responses are HAL-FORMS"
            + " (application/prs.hal-forms+json); errors are RFC 9457 Problem Details whose title and detail"
            + " follow Accept-Language (pt-BR, en-US, es-ES).";

    @Bean
    public OpenAPI vereditoOpenApi(SystemVersion systemVersion) {
        return new OpenAPI()
                .info(new Info()
                        .title("Veredito API")
                        .version("v1")
                        .description(STUDY + " System version: " + systemVersion.value() + ".")
                        .contact(new Contact()
                                .name("Carlos Diego Quirino Lima")
                                .email("diegoquirino@gmail.com")
                                .url("https://github.com/diegoquirino/openscience"))
                        .license(new License().name("MIT").url("https://opensource.org/license/mit")))
                .tags(List.of(
                        tag("API index", "Versions of the API, outside any version"),
                        tag("Entry point", "Where every client starts, and the documentation of link relations"),
                        tag("Access", "Sign-in with a code sent by e-mail (spec 004)"),
                        tag("Participation", "Consent and profile of the participant (specs 004 and 006)"),
                        tag("Tasks", "Labeling tasks (spec 007)"),
                        tag("Administration", "Import, export, dashboard and participants (specs 005 and 009 to 011)")))
                .components(new Components().addSecuritySchemes(SESSION_SCHEME, new SecurityScheme()
                        .type(SecurityScheme.Type.APIKEY)
                        .in(SecurityScheme.In.COOKIE)
                        .name(SESSION_COOKIE)
                        .description("HttpOnly session cookie set after sign-in (from spec 004).")));
    }

    @Bean
    public GroupedOpenApi v1Group() {
        return GroupedOpenApi.builder().group("v1").pathsToMatch("/api/v1", "/api/v1/**").build();
    }

    @Bean
    public GroupedOpenApi indexGroup() {
        return GroupedOpenApi.builder().group("index").pathsToMatch("/api").build();
    }

    private static Tag tag(String name, String description) {
        return new Tag().name(name).description(description);
    }
}
