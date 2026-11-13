package io.github.diegoquirino.veredito.web;

import io.github.diegoquirino.veredito.api.ProblemResponses;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 * Stateless Spring Security with the session cookie of spec 004 and the authorization rules of
 * {@code specs/004-email-code-access/contracts/access.md}. Paths outside the API stay open, so the
 * routing and documentation filters of spec 003 keep their behavior.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class SecurityConfig {

    public static final String PARTICIPANT = "PARTICIPANT";
    public static final String ADMINISTRATOR = "ADMINISTRATOR";

    /** Administrative resources; later specs add the paths of their "Administration" operations here. */
    public static final RequestMatcher ADMINISTRATION =
            PathPatternRequestMatcher.withDefaults().matcher("/api/v1/administrator/**");

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            CurrentSession currentSession,
            ProblemResponses problems,
            ProblemAuthenticationEntryPoint authenticationEntryPoint,
            ProblemAccessDeniedHandler accessDeniedHandler) throws Exception {
        http
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // Research section 10: SameSite=Lax cookie, JSON-only bodies and no CORS replace the token.
                .csrf(csrf -> csrf.disable())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .logout(logout -> logout.disable())
                .requestCache(cache -> cache.disable())
                .addFilterBefore(new SessionCookieFilter(currentSession, problems), AnonymousAuthenticationFilter.class)
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers("/api/v1/access-sessions/current").hasAnyRole(PARTICIPANT, ADMINISTRATOR)
                        .requestMatchers("/api/v1/participants/**").hasRole(PARTICIPANT)
                        .requestMatchers(ADMINISTRATION).hasRole(ADMINISTRATOR)
                        .anyRequest().permitAll());
        return http.build();
    }
}
