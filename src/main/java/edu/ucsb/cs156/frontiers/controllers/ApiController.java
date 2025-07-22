package edu.ucsb.cs156.frontiers.controllers;

import edu.ucsb.cs156.frontiers.errors.NoLinkedOrganizationException;

import org.checkerframework.checker.units.qual.Current;
import org.springframework.beans.factory.annotation.Autowired;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

import edu.ucsb.cs156.frontiers.errors.CourseNotAuthorized;
import edu.ucsb.cs156.frontiers.errors.EntityNotFoundException;
import edu.ucsb.cs156.frontiers.models.CurrentUser;
import edu.ucsb.cs156.frontiers.services.CurrentUserService;

import java.util.Collection;
import java.util.Map;



/**
 * This is an abstract class that provides common functionality for all API controllers.
 */

@Slf4j
public abstract class ApiController {
  @Autowired
  private CurrentUserService currentUserService;

  @Autowired RoleHierarchy roleHierarchy;

  /**
   * This method returns the current user.
   * @return the current user
   */
  protected CurrentUser getCurrentUser() {
    return currentUserService.getCurrentUser();
  }

/**
   * This method checks if the current user has the given role
   * @return true if the current user has the role, false otherwise
   * @param role the role to check
   */
  protected boolean doesCurrentUserHaveRole(
      String roleToCheck
    ) {  
    CurrentUser currentUser = getCurrentUser();    
    Collection<? extends GrantedAuthority> authorities = currentUser.getRoles();

    Collection<? extends GrantedAuthority> extendedAuthorities = roleHierarchy.getReachableGrantedAuthorities(authorities);

    return extendedAuthorities.stream()
         .anyMatch(role -> role.getAuthority().equals(roleToCheck));
  }

  /**
   * This method checks if the current user is an admin.
   * @return true if the current user is an admin, false otherwise
   */
  protected boolean isCurrentUserAdmin() {  
    return doesCurrentUserHaveRole("ROLE_ADMIN");
  }


  /**
   * This method returns a generic message.
   * @param message the message
   * @return a map with the message
   */
  protected Object genericMessage(String message) {
    return Map.of("message", message);
  }

  /**
   * This method handles the EntityNotFoundException.
   * @param e the exception
   * @return a map with the type and message of the exception
   */
  @ExceptionHandler({ EntityNotFoundException.class })
  @ResponseStatus(HttpStatus.NOT_FOUND)
  public Object handleEntityNotFoundException(Throwable e) {
    return Map.of(
      "type", e.getClass().getSimpleName(),
      "message", e.getMessage()
    );
  }

  /**
   * This method handles the NoLinkedOrganizationException.
   * @param e the exception
   * @return a map with the type and message of the exception
   */
  @ExceptionHandler({ NoLinkedOrganizationException.class })
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public Object handleNoLinkedOrgException(Throwable e) {
    return Map.of(
            "type", e.getClass().getSimpleName(),
            "message", e.getMessage()
    );
  }

  /**
   * This method handles the UnsupportedOperationException.
   * @param e the exception
   * @return a map with the type and message of the exception
   */
    @ExceptionHandler(UnsupportedOperationException.class)
    public ResponseEntity<Map<String, String>> handleUnsupportedOperation(UnsupportedOperationException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("message", ex.getMessage()));
    }

  /**
   * This method handles the NoLinkedOrganizationException.
   * @param e the exception
   * @return a map with the type and message of the exception
   */
  @ExceptionHandler({ IllegalArgumentException.class })
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public Object handleIllegalArgument(Throwable e) {
    return Map.of(
            "type", e.getClass().getSimpleName(),
            "message", e.getMessage()
    );
  }

  /**
   * This method handles the NoLinkedOrganizationException.
   * @param e the exception
   * @return a map with the type and message of the exception
   */
  @ExceptionHandler({ CourseNotAuthorized.class })
  @ResponseStatus(HttpStatus.FORBIDDEN)
  public Object handleCourseNotAuthorized(Throwable e) {
    return Map.of(
            "type", e.getClass().getSimpleName(),
            "message", e.getMessage()
    );
  }
}
