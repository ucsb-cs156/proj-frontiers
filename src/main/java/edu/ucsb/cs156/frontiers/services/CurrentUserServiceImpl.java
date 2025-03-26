package edu.ucsb.cs156.frontiers.services;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import edu.ucsb.cs156.frontiers.entities.User;
import edu.ucsb.cs156.frontiers.models.CurrentUser;
import edu.ucsb.cs156.frontiers.repositories.UserRepository;

/**
 * This is a service that provides information about the current user.
 * 
 * This is the version of the service used in production.
 */

@Slf4j
@Service("currentUser")
@Primary
public class CurrentUserServiceImpl extends CurrentUserService {
  @Autowired
  private UserRepository userRepository;

  @Autowired
  GrantedAuthoritiesService grantedAuthoritiesService;

  @Value("${app.admin.emails}")
  final private List<String> adminEmails = new ArrayList<String>();

  /**
   * This method returns the current user as a User object.
   * @return the current user
   */
  public CurrentUser getCurrentUser() {
    CurrentUser cu = CurrentUser.builder()
      .user(this.getUser())
      .roles(this.getRoles())
      .build();
    log.info("getCurrentUser returns {}",cu);
    return cu;
  }

  /**
   * This method obtains the current user that is logged in with OAuth2, if any.
   * The parameters are automatically injected by Spring.
   * 
   * This method also has a side effect of storing the user in the database if they are not already there.
   * 
   * @param securityContext the security context (provided by Spring)
   * @param authentication the authentication token (provided by Spring)
   * @return the User object representing the current user
   */
  
  public User getOAuth2AuthenticatedUser(SecurityContext securityContext, Authentication authentication) {
    OAuth2User oAuthUser = ((OAuth2AuthenticationToken) authentication).getPrincipal();
    String email = oAuthUser.getAttribute("email");
    String googleSub = oAuthUser.getAttribute("sub");
    String pictureUrl = oAuthUser.getAttribute("picture");
    String fullName = oAuthUser.getAttribute("name");
    String givenName = oAuthUser.getAttribute("given_name");
    String familyName = oAuthUser.getAttribute("family_name");
    boolean emailVerified = oAuthUser.getAttribute("email_verified");
    String locale = oAuthUser.getAttribute("locale");
    String hostedDomain = oAuthUser.getAttribute("hd");

    java.util.Map<java.lang.String,java.lang.Object> attrs = oAuthUser.getAttributes();
    log.info("attrs={}",attrs);

    Optional<User> ou = userRepository.findByEmail(email);
    if (ou.isPresent()) {
      User u = ou.get();
      if (adminEmails.contains(email) && !u.getAdmin()) {
        u.setAdmin(true);
        userRepository.save(u);
      }
      return u;
    }

    User u = User.builder()
        .googleSub(googleSub)
        .email(email)
        .pictureUrl(pictureUrl)
        .fullName(fullName)
        .givenName(givenName)
        .familyName(familyName)
        .emailVerified(emailVerified)
        .locale(locale)
        .hostedDomain(hostedDomain)
        .admin(adminEmails.contains(email))
        .build();
    userRepository.save(u);
    return u;
  }

  /**
   * This method returns the current user as a User object.
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
   * @return a collection of roles
   */
  public Collection<? extends GrantedAuthority> getRoles() {
   return grantedAuthoritiesService.getGrantedAuthorities();
  }
}
