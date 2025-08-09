package edu.ucsb.cs156.frontiers.services;

import edu.ucsb.cs156.frontiers.entities.User;
import edu.ucsb.cs156.frontiers.models.CurrentUser;
import edu.ucsb.cs156.frontiers.repositories.UserRepository;
import java.util.Collection;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

/**
 * This is a service that provides information about the current user.
 *
 * <p>This is the version of the service used in production.
 */
@Slf4j
@Service
public class CurrentUserServiceImpl extends CurrentUserService {
  @Autowired private UserRepository userRepository;

  @Autowired GrantedAuthoritiesService grantedAuthoritiesService;
  @Autowired private RoleHierarchy roleHierarchy;

  /**
   * This method returns the current user as a User object.
   *
   * @return the current user
   */
  public CurrentUser getCurrentUser() {
    CurrentUser cu = CurrentUser.builder().user(this.getUser()).roles(this.getRoles()).build();
    log.info("getCurrentUser returns {}", cu);
    return cu;
  }

  /**
   * This method obtains the current user that is logged in with OAuth2, if any. The parameters are
   * automatically injected by Spring.
   *
   * <p>This method also has a side effect of storing the user in the database if they are not
   * already there.
   *
   * @param securityContext the security context (provided by Spring)
   * @param authentication the authentication token (provided by Spring)
   * @return the User object representing the current user
   */
  public User getOAuth2AuthenticatedUser(
      SecurityContext securityContext, Authentication authentication) {
    OidcUser oAuthUser = (OidcUser) authentication.getPrincipal();
    User currentUser = userRepository.findByEmail(oAuthUser.getEmail()).orElse(null);
    return currentUser;
  }

  /**
   * This method returns the current user as a User object.
   *
   * @return the current user
   */
  public User getUser() {
    SecurityContext securityContext = SecurityContextHolder.getContext();
    Authentication authentication = securityContext.getAuthentication();
    if (authentication instanceof OAuth2AuthenticationToken) {
      return getOAuth2AuthenticatedUser(securityContext, authentication);
    }
    return null;
  }

  /**
   * This method returns the roles of the current user.
   *
   * @return a collection of roles
   */
  public Collection<? extends GrantedAuthority> getRoles() {
    return roleHierarchy.getReachableGrantedAuthorities(
        grantedAuthoritiesService.getGrantedAuthorities());
  }
}
