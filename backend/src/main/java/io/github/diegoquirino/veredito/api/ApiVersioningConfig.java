package io.github.diegoquirino.veredito.api;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.RequestPath;
import org.springframework.web.accept.ApiVersionParser;
import org.springframework.web.accept.SemanticApiVersionParser;
import org.springframework.web.accept.StandardApiVersionDeprecationHandler;
import org.springframework.web.servlet.config.annotation.ApiVersionConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** API version in path segment 1 ({@code /api/v1/...}); paths outside {@code /api/v{n}} carry no version. */
@Configuration(proxyBeanMethods = false)
public class ApiVersioningConfig implements WebMvcConfigurer {

    private static final Pattern VERSIONED_PATH = Pattern.compile("^/api/[vV][^/]*(/.*)?$");
    private static final Pattern VERSION_SEGMENT = Pattern.compile("^/api/([vV][^/]*)(/.*)?$");
    private static final SemanticApiVersionParser.Version SUPPORTED = new PrefixedVersionParser().parseVersion("1");

    @Override
    public void configureApiVersioning(ApiVersionConfigurer configurer) {
        configurer
                .usePathSegment(1, ApiVersioningConfig::isVersioned)
                .setVersionParser(new PrefixedVersionParser())
                .addSupportedVersions("1")
                .detectSupportedVersions(false)
                .setVersionRequired(false)
                .setDeprecationHandler(new StandardApiVersionDeprecationHandler(new PrefixedVersionParser()));
    }

    static boolean isVersioned(RequestPath path) {
        return VERSIONED_PATH.matcher(path.pathWithinApplication().value()).matches();
    }

    /** The version segment as requested ({@code v9}, {@code vx}), for paths under {@code /api/v...}. */
    public static Optional<String> versionSegmentOf(String path) {
        Matcher matcher = VERSION_SEGMENT.matcher(path);
        return matcher.matches() ? Optional.of(matcher.group(1)) : Optional.empty();
    }

    /**
     * The version segment of a path under {@code /api/v{n}} that is not supported, if any. Spring only
     * validates the version when some mapping matches the path, so {@code /api/v9/tasks} would
     * otherwise end as a plain 404.
     */
    public static Optional<String> unsupportedVersionOf(String path) {
        Optional<String> raw = versionSegmentOf(path);
        if (raw.isEmpty()) {
            return Optional.empty();
        }
        String segment = raw.get();
        try {
            SemanticApiVersionParser.Version version = new PrefixedVersionParser().parseVersion(segment);
            return version.compareTo(SUPPORTED) == 0 ? Optional.empty() : Optional.of(segment);
        } catch (RuntimeException unparsable) {
            return Optional.of(segment);
        }
    }

    /** Accepts {@code v1} as well as {@code 1}. */
    static final class PrefixedVersionParser implements ApiVersionParser<SemanticApiVersionParser.Version> {

        private final SemanticApiVersionParser delegate = new SemanticApiVersionParser();

        @Override
        public SemanticApiVersionParser.Version parseVersion(String version) {
            String plain = version.startsWith("v") || version.startsWith("V") ? version.substring(1) : version;
            return delegate.parseVersion(plain);
        }
    }
}
