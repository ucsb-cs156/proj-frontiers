package edu.ucsb.cs156.frontiers.controllers;

import edu.ucsb.cs156.frontiers.errors.NoLinkedOrganizationException;

import org.apache.coyote.BadRequestException;
import org.springframework.beans.factory.annotation.Autowired;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

import edu.ucsb.cs156.frontiers.errors.EntityNotFoundException;
import edu.ucsb.cs156.frontiers.models.CurrentUser;
import edu.ucsb.cs156.frontiers.services.CurrentUserService;

import java.util.Map;

/**
 * This is an abstract class that provides common functionality for all API controllers.
 */

@Slf4j
public abstract class ApiController {
  @Autowired
  private CurrentUserService currentUserService;

  /**
   * This method returns the current user.
   * @return the current user
   */
  protected CurrentUser getCurrentUser() {
    return currentUserService.getCurrentUser();
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

  @ExceptionHandler(UnsupportedOperationException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Map<String, String> handleUnsupportedOperation(UnsupportedOperationException ex) {
        return Map.of("message", ex.getMessage());
    }

      /**
     * This method handles BadRequestException.
     * @param e the exception
     * @return a map with the type and message of the exception
     */
    @ExceptionHandler({ BadRequestException.class })
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Object handleBadRequestException(Throwable e) {
      return Map.of(
        "type",    e.getClass().getSimpleName(),
        "message", e.getMessage()
      );
    }
}
