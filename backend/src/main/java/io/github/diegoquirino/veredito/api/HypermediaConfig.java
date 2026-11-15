package io.github.diegoquirino.veredito.api;

import io.github.diegoquirino.veredito.api.v1.ConsentDecisionRequest;
import io.github.diegoquirino.veredito.api.v1.PreferredLanguageRequest;
import io.github.diegoquirino.veredito.api.v1.ScreeningRequest;
import io.github.diegoquirino.veredito.api.v1.SkillRequest;
import io.github.diegoquirino.veredito.domain.onboarding.ConsentDecision;
import io.github.diegoquirino.veredito.domain.onboarding.ProfessionalSkill;
import io.github.diegoquirino.veredito.domain.onboarding.ScreeningAnswer;
import java.util.Arrays;
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

    /**
     * Inline options of the HAL-FORMS templates: the three locales of {@code preferredLanguage}, and the codes
     * of the consent decision, the professional skill and the screening answer (labels never travel).
     */
    @Bean
    public HalFormsConfiguration halFormsConfiguration() {
        return new HalFormsConfiguration()
                .withOptions(
                        PreferredLanguageRequest.class,
                        "preferredLanguage",
                        property -> HalFormsOptions.inline(LocaleConfig.SUPPORTED_TAGS)
                                .withMinItems(1L)
                                .withMaxItems(1L))
                .withOptions(
                        ConsentDecisionRequest.class,
                        "decision",
                        property -> HalFormsOptions.inline(
                                        Arrays.stream(ConsentDecision.values()).map(ConsentDecision::code).toList())
                                .withMinItems(1L)
                                .withMaxItems(1L))
                .withOptions(
                        SkillRequest.class,
                        "skill",
                        property -> HalFormsOptions.inline(ProfessionalSkill.codes())
                                .withMinItems(1L)
                                .withMaxItems(1L))
                .withOptions(
                        ScreeningRequest.class,
                        "answer",
                        property -> HalFormsOptions.inline(ScreeningAnswer.codes())
                                .withMinItems(1L)
                                .withMaxItems(1L));
    }
}
