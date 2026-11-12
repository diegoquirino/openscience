package io.github.diegoquirino.veredito.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Rules 1 to 10 of {@code specs/003-deployment-foundation/contracts/web-routing.md}, in order. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class LocaleRoutingFilter extends OncePerRequestFilter {

    public static final List<String> PREFIXES = List.of("pt-BR", "en-US", "es-ES");

    private final ResourceLoader resources;

    public LocaleRoutingFilter(ResourceLoader resources) {
        this.resources = resources;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        if (isApi(path) || isDocumentation(path)) {
            chain.doFilter(request, response);
            return;
        }
        if (!"GET".equals(request.getMethod()) && !"HEAD".equals(request.getMethod())) {
            response.setHeader(HttpHeaders.ALLOW, "GET, HEAD");
            response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }
        if (isUnsafe(path)) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        if (path.isEmpty() || "/".equals(path)) {
            forward("/index.html", request, response);
            return;
        }

        int end = path.indexOf('/', 1);
        String first = end < 0 ? path.substring(1) : path.substring(1, end);
        String rest = end < 0 ? "" : path.substring(end);

        if (end < 0 && exists(path)) {
            chain.doFilter(request, response);
            return;
        }
        String canonical = canonicalPrefix(first);
        if (canonical == null) {
            redirect(response, HttpServletResponse.SC_FOUND, "/");
            return;
        }
        if (!canonical.equals(first)) {
            redirect(response, HttpServletResponse.SC_MOVED_PERMANENTLY, "/" + canonical + rest + query(request));
            return;
        }
        if (rest.isEmpty()) {
            redirect(response, HttpServletResponse.SC_MOVED_PERMANENTLY, "/" + canonical + "/" + query(request));
            return;
        }
        if (!"/".equals(rest) && exists(path)) {
            chain.doFilter(request, response);
            return;
        }
        if (hasExtension(rest)) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        forward("/" + canonical + "/index.html", request, response);
    }

    static boolean isApi(String path) {
        return "/api".equals(path) || path.startsWith("/api/");
    }

    static boolean isDocumentation(String path) {
        return "/swagger-ui.html".equals(path)
                || path.startsWith("/swagger-ui/")
                || "/v3/api-docs".equals(path)
                || path.startsWith("/v3/api-docs/")
                || path.startsWith("/v3/api-docs.yaml");
    }

    private static boolean isUnsafe(String path) {
        String lower = path.toLowerCase(Locale.ROOT);
        return lower.contains("..") || lower.contains("%2e") || lower.contains("%2f") || lower.contains("\\")
                || lower.contains("//");
    }

    private static String canonicalPrefix(String segment) {
        for (String prefix : PREFIXES) {
            if (prefix.equalsIgnoreCase(segment)) {
                return prefix;
            }
        }
        return null;
    }

    private static boolean hasExtension(String rest) {
        String last = rest.substring(rest.lastIndexOf('/') + 1);
        return last.lastIndexOf('.') > 0;
    }

    private boolean exists(String path) {
        Resource resource = resources.getResource("classpath:/static" + path);
        return resource.exists() && resource.isReadable();
    }

    private static String query(HttpServletRequest request) {
        String query = request.getQueryString();
        return query == null ? "" : "?" + query;
    }

    private static void redirect(HttpServletResponse response, int status, String location) {
        response.setStatus(status);
        response.setHeader(HttpHeaders.LOCATION, location);
    }

    private static void forward(String target, HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.getRequestDispatcher(target).forward(request, response);
    }
}
