package edu.ucsb.cs156.frontiers.interceptors;

import edu.ucsb.cs156.frontiers.repositories.AdminRepository;
import edu.ucsb.cs156.frontiers.repositories.InstructorRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * RoleUpdateInterceptor reloads a user's security context on each request to the backend. This is
 * necessary to ensure that the user has the correct roles and does not have to log in or out to
 * have access to restricted endpoints.
 *
 * <p>To prevent interference with @WebMvcTest test slices, ControllerTestCase contains a
 * passthrough RoleUpdateInterceptor MockitoBean so that every ControllerTestCase is not required to
 * add an AdminRepository and InstructorRepository MockitoBean.
 */
@Component
public class RoleUpdateInterceptor implements HandlerInterceptor {

  private final AdminRepository adminRepository;

  private final InstructorRepository instructorRepository;

  public RoleUpdateInterceptor(
      AdminRepository adminRepository, InstructorRepository instructorRepository) {
    this.adminRepository = adminRepository;
    this.instructorRepository = instructorRepository;
  }

  @Override
  public boolean preHandle(
      HttpServletRequest request, HttpServletResponse response, Object handler) {
    // Update user's security context on server each time the user makes HTTP request to the backend
    // If user has admin or instructor status in database, we will update their roles in security
    // context
    SecurityContext securityContext = SecurityContextHolder.getContext();
    Authentication authentication = securityContext.getAuthentication();

    if (authentication instanceof OAuth2AuthenticationToken) {
      OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
      if (oauthToken.getPrincipal() instanceof OidcUser) {
        OidcUser oidcUser = (OidcUser) oauthToken.getPrincipal();
        String email = oidcUser.getEmail();
        Set<GrantedAuthority> newAuthorities = new HashSet<>();
        Collection<? extends GrantedAuthority> currentAuthorities = authentication.getAuthorities();

        // Copy all existing authorities except ROLE_ADMIN and ROLE_INSTRUCTOR
        currentAuthorities.stream()
            .filter(
                authority ->
                    !authority.getAuthority().equals("ROLE_ADMIN")
                        && !authority.getAuthority().equals("ROLE_INSTRUCTOR"))
            .forEach(newAuthorities::add);

        // Check if user is admin or instructor and add appropriate role
        if (adminRepository.existsByEmail(email)) {
          newAuthorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        } else if (instructorRepository.existsByEmail(email)) {
          newAuthorities.add(new SimpleGrantedAuthority("ROLE_INSTRUCTOR"));
        }

        // Create new authentication with updated authorities
        Authentication newAuth =
            new OAuth2AuthenticationToken(
                oidcUser, newAuthorities, oauthToken.getAuthorizedClientRegistrationId());

        SecurityContextHolder.getContext().setAuthentication(newAuth);
      }
    }

    return true;
  }
}
