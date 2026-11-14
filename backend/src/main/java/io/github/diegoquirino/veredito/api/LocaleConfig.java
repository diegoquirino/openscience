package io.github.diegoquirino.veredito.api;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Locale.LanguageRange;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

@Configuration(proxyBeanMethods = false)
public class LocaleConfig {

    public static final Locale PT_BR = Locale.of("pt", "BR");
    public static final Locale EN_US = Locale.of("en", "US");
    public static final Locale ES_ES = Locale.of("es", "ES");
    public static final List<Locale> SUPPORTED = List.of(PT_BR, EN_US, ES_ES);
    /** The values of {@code preferred_language} and of the cookie {@code veredito-lang}. */
    public static final List<String> SUPPORTED_TAGS = SUPPORTED.stream().map(Locale::toLanguageTag).toList();

    @Bean(DispatcherServlet.LOCALE_RESOLVER_BEAN_NAME)
    public SupportedLocaleResolver localeResolver() {
        return new SupportedLocaleResolver();
    }

    @Bean
    public FilterRegistrationBean<ContentLanguageFilter> contentLanguageFilter(SupportedLocaleResolver resolver) {
        FilterRegistrationBean<ContentLanguageFilter> registration =
                new FilterRegistrationBean<>(new ContentLanguageFilter(resolver));
        registration.addUrlPatterns("/api", "/api/*", "/v3/api-docs/*", "/swagger-ui.html", "/swagger-ui/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }

    /**
     * Picks the first supported locale by weight, matching the full tag first and then the language
     * alone; the servlet container's own locale is never used, so a missing, wildcard or unparsable
     * header gives pt-BR on every machine.
     */
    public static final class SupportedLocaleResolver extends AcceptHeaderLocaleResolver {

        SupportedLocaleResolver() {
            setSupportedLocales(SUPPORTED);
            setDefaultLocale(PT_BR);
        }

        @Override
        public Locale resolveLocale(HttpServletRequest request) {
            String header = request.getHeader(HttpHeaders.ACCEPT_LANGUAGE);
            if (header == null || header.isBlank()) {
                return PT_BR;
            }
            List<LanguageRange> ranges;
            try {
                ranges = LanguageRange.parse(header);
            } catch (IllegalArgumentException unparsable) {
                return PT_BR;
            }
            for (LanguageRange range : ranges) {
                if (range.getWeight() == 0 || "*".equals(range.getRange())) {
                    continue;
                }
                Locale requested = Locale.forLanguageTag(range.getRange());
                for (Locale supported : SUPPORTED) {
                    if (supported.equals(requested)) {
                        return supported;
                    }
                }
                for (Locale supported : SUPPORTED) {
                    if (supported.getLanguage().equals(requested.getLanguage())) {
                        return supported;
                    }
                }
            }
            return PT_BR;
        }
    }

    /** States the resolved locale in {@code Content-Language}. */
    static final class ContentLanguageFilter extends OncePerRequestFilter {

        private final SupportedLocaleResolver resolver;

        ContentLanguageFilter(SupportedLocaleResolver resolver) {
            this.resolver = resolver;
        }

        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
                throws ServletException, IOException {
            response.setHeader(HttpHeaders.CONTENT_LANGUAGE, resolver.resolveLocale(request).toLanguageTag());
            chain.doFilter(request, response);
        }
    }
}
