package edu.ucsb.cs156.frontiers.services;

import edu.ucsb.cs156.frontiers.entities.User;
import edu.ucsb.cs156.frontiers.models.CurrentUser;
import java.util.Collection;
import org.springframework.security.core.GrantedAuthority;

/**
 * This is a service that provides information about the current user.
 *
 * <p>It is an abstract class because we have different implementations for testing and production.
 */
public abstract class CurrentUserService {

  /**
   * This method returns the current user as a User object.
   *
   * @return the current user
   */
  public abstract User getUser();

  /**
   * This method returns the current user as a CurrentUser object
   *
   * @return the current user
   */
  public abstract CurrentUser getCurrentUser();

  /**
   * This method returns the roles of the current user.
   *
   * @return a collection of roles
   */
  public abstract Collection<? extends GrantedAuthority> getRoles();

  /**
   * This method returns whether the current user is logged in.
   *
   * @return whether the current user is logged in
   */
  public final boolean isLoggedIn() {
    return getUser() != null;
  }
}
