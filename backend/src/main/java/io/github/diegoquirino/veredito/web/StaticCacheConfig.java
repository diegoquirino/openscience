package io.github.diegoquirino.veredito.web;

import java.time.Duration;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Hashed bundles of each locale build are immutable; everything else, including every
 * {@code index.html} and the root page assets, is revalidated ({@code no-cache}, set by
 * {@code spring.web.resources.cache.cachecontrol.no-cache} on the default handler).
 */
@Configuration(proxyBeanMethods = false)
public class StaticCacheConfig implements WebMvcConfigurer {

    private static final CacheControl IMMUTABLE =
            CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable();

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        for (String prefix : LocaleRoutingFilter.PREFIXES) {
            registry.addResourceHandler("/" + prefix + "/*.js", "/" + prefix + "/*.css")
                    .addResourceLocations("classpath:/static/" + prefix + "/")
                    .setCacheControl(IMMUTABLE);
            registry.addResourceHandler("/" + prefix + "/media/**")
                    .addResourceLocations("classpath:/static/" + prefix + "/media/")
                    .setCacheControl(IMMUTABLE);
        }
    }
}
