package edu.ucsb.cs156.frontiers.services;

import java.util.Collection;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * The is a service that retrieves and logs the granted authorities for the current user's
 * authentication.
 */
@Slf4j
@Service("grantedAuthorities")
public class GrantedAuthoritiesService {

  /**
   * The function retrieves and logs the granted authorities from the current security context in a
   * Java application.
   *
   * @return collection of authorities granted to the currently authenticated user.
   */
  public Collection<? extends GrantedAuthority> getGrantedAuthorities() {
    SecurityContext securityContext = SecurityContextHolder.getContext();
    Authentication authentication = securityContext.getAuthentication();
    Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
    log.info("authorities={}", authorities);
    return authorities;
  }
}
