package edu.ucsb.cs156.frontiers.controllers;

import edu.ucsb.cs156.frontiers.entities.User;
import edu.ucsb.cs156.frontiers.repositories.UserRepository;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/github")
public class GithubController extends ApiController {

  private final UserRepository userRepository;

  public GithubController(UserRepository userRepository) {
    super();
    this.userRepository = userRepository;
  }

  @PreAuthorize("hasRole('ROLE_GITHUB')")
  @DeleteMapping("/disconnect")
  public Object disconnect(SecurityContext context) {
    User currentUser = getCurrentUser().getUser();
    currentUser.setGithubId(null);
    currentUser.setGithubLogin(null);
    userRepository.save(currentUser);
    Authentication auth = context.getAuthentication();
    List<? extends GrantedAuthority> removedAuthority =
        auth.getAuthorities().stream()
            .filter(r -> !"ROLE_GITHUB".equals(r.getAuthority()))
            .toList();
    OAuth2AuthenticationToken token = (OAuth2AuthenticationToken) context.getAuthentication();
    context.setAuthentication(
        new OAuth2AuthenticationToken(
            (OidcUser) auth.getPrincipal(),
            removedAuthority,
            token.getAuthorizedClientRegistrationId()));
    SecurityContextHolder.setContext(context);
    return genericMessage("Disconnected from GitHub. You may now log in with a different account.");
  }
}
