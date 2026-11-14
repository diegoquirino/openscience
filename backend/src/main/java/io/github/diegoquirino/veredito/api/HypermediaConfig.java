package io.github.diegoquirino.veredito.api;

import io.github.diegoquirino.veredito.api.v1.PreferredLanguageRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.hateoas.UriTemplate;
import org.springframework.hateoas.config.EnableHypermediaSupport;
import org.springframework.hateoas.config.EnableHypermediaSupport.HypermediaType;
import org.springframework.hateoas.mediatype.hal.CurieProvider;
import org.springframework.hateoas.mediatype.hal.DefaultCurieProvider;
import org.springframework.hateoas.mediatype.hal.forms.HalFormsConfiguration;
import org.springframework.hateoas.mediatype.hal.forms.HalFormsOptions;

/**
 * HAL-FORMS first, so it is the representation chosen for {@code Accept: *}{@code /*}; the CURIE
 * href stays relative like every other link of the API.
 */
@Configuration(proxyBeanMethods = false)
@EnableHypermediaSupport(type = {HypermediaType.HAL_FORMS, HypermediaType.HAL})
public class HypermediaConfig {

    public static final String CURIE = "vd";
    public static final String RELS_TEMPLATE = "/api/v1/rels/{rel}";

    @Bean
    public CurieProvider curieProvider() {
        return new DefaultCurieProvider(CURIE, UriTemplate.of(RELS_TEMPLATE)) {
            @Override
            protected String getCurieHref(String name, UriTemplate template) {
                return template.toString();
            }
        };
    }

    /** The three locales as the inline options of every {@code preferredLanguage} property. */
    @Bean
    public HalFormsConfiguration halFormsConfiguration() {
        return new HalFormsConfiguration().withOptions(
                PreferredLanguageRequest.class,
                "preferredLanguage",
                property -> HalFormsOptions.inline(LocaleConfig.SUPPORTED_TAGS).withMinItems(1L).withMaxItems(1L));
    }
}
