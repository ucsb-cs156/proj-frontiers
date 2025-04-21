package edu.ucsb.cs156.frontiers.config;

import edu.ucsb.cs156.frontiers.services.GithubSignInService;
import edu.ucsb.cs156.frontiers.services.GoogleSignInService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.Http403ForbiddenEntryPoint;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.CsrfTokenRequestHandler;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import static org.springframework.security.web.util.matcher.AntPathRequestMatcher.antMatcher;


import java.io.IOException;
import java.util.function.Supplier;

/**
 * The `SecurityConfig` class in Java configures web security with OAuth2 login,
 * CSRF protection, and
 * role-based authorization based on user email addresses.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@Slf4j
public class SecurityConfig {

  @Autowired
  private GoogleSignInService googleSignInService;

  @Autowired
  private GithubSignInService githubSignInService;

  /**
   * The `filterChain` method in this Java code configures various security
   * settings for an HTTP request,
   * including authorization, exception handling, OAuth2 login, CSRF protection,
   * and logout behavior.
   * 
   * @param http injected HttpSecurity object (injected by Spring framework)
   *             //
   */
  // https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html#csrf-integration-javascript-spa
  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        .exceptionHandling(handling -> handling.authenticationEntryPoint(new Http403ForbiddenEntryPoint()))
        .oauth2Login(
            oauth2 -> oauth2.userInfoEndpoint(userInfo ->
                    userInfo.oidcUserService(googleSignInService).userService(githubSignInService)))
        .csrf(csrf -> csrf
            .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
            .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler()).ignoringRequestMatchers("/api/webhooks/github"))
        .addFilterAfter(new CsrfCookieFilter(), BasicAuthenticationFilter.class)
        .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
        .logout(logout -> logout.logoutRequestMatcher(new AntPathRequestMatcher("/logout")).logoutSuccessUrl("/"));
    return http.build();
  }
  /**
   * The `webSecurityCustomizer` method is used to configure web security in Java,
   * specifically ignoring requests
   * to the "/h2-console/**" path.
   */
  @Bean
  public WebSecurityCustomizer webSecurityCustomizer() {
    return web -> web.ignoring().requestMatchers(antMatcher("/h2-console/**"));
  }

  @Bean
  static RoleHierarchy roleHierarchy() {
    return RoleHierarchyImpl.withDefaultRolePrefix()
            .role("ADMIN").implies("PROFESSOR")
            .role("PROFESSOR").implies("USER")
            .build();
  }
}

final class SpaCsrfTokenRequestHandler extends CsrfTokenRequestAttributeHandler {
  private final CsrfTokenRequestHandler delegate = new XorCsrfTokenRequestAttributeHandler();

  @Override
  public void handle(HttpServletRequest request, HttpServletResponse response,
      Supplier<CsrfToken> deferredCsrfToken) {
    /*
     * Always use XorCsrfTokenRequestAttributeHandler to provide BREACH protection
     * of
     * the CsrfToken when it is rendered in the response body.
     */
    this.delegate.handle(request, response, deferredCsrfToken);
  }

  @Override
  public String resolveCsrfTokenValue(HttpServletRequest request, CsrfToken csrfToken) {
    /*
     * If the request contains a request header, use
     * CsrfTokenRequestAttributeHandler
     * to resolve the CsrfToken. This applies when a single-page application
     * includes
     * the header value automatically, which was obtained via a cookie containing
     * the
     * raw CsrfToken.
     */
    if (StringUtils.hasText(request.getHeader(csrfToken.getHeaderName()))) {
      return super.resolveCsrfTokenValue(request, csrfToken);
    }
    /*
     * In all other cases (e.g. if the request contains a request parameter), use
     * XorCsrfTokenRequestAttributeHandler to resolve the CsrfToken. This applies
     * when a server-side rendered form includes the _csrf request parameter as a
     * hidden input.
     */
    return this.delegate.resolveCsrfTokenValue(request, csrfToken);
  }
}

final class CsrfCookieFilter extends OncePerRequestFilter {

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    CsrfToken csrfToken = (CsrfToken) request.getAttribute("_csrf");
    // Render the token value to a cookie by causing the deferred token to be loaded
    csrfToken.getToken();
    filterChain.doFilter(request, response);
  }
}