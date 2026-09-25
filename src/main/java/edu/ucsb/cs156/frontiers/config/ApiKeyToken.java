package edu.ucsb.cs156.frontiers.config;

import edu.ucsb.cs156.frontiers.entities.CourseApiKey;
import edu.ucsb.cs156.frontiers.entities.User;
import java.util.Collection;
import lombok.Getter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

public class ApiKeyToken extends AbstractAuthenticationToken {

  @Getter private final Long courseId;
  private final User user;

  /**
   * Creates a token with the supplied array of authorities.
   *
   * @param authorities the collection of <tt>GrantedAuthority</tt>s for the principal represented
   *     by this authentication object.
   */
  public ApiKeyToken(
      CourseApiKey key, User user, Collection<? extends GrantedAuthority> authorities) {
    super(authorities);
    this.courseId = key.getCourse().getId();
    this.user = user;
    assert (authorities.stream().anyMatch(a -> a.getAuthority().equals("ROLE_API_KEY")))
        : "Collection must contain ROLE_API_KEY";
  }

  @Override
  public boolean isAuthenticated() {
    return true;
  }

  @Override
  public Object getCredentials() {
    return null;
  }

  @Override
  public User getPrincipal() {
    return this.user;
  }

  @Override
  public String getName() {
    return this.user.getEmail();
  }
}
