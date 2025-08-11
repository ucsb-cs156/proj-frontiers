package edu.ucsb.cs156.frontiers.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import edu.ucsb.cs156.frontiers.models.UserDataDTO;
import edu.ucsb.cs156.frontiers.services.UserDataDTOService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * This is a REST controller for getting information about the users.
 *
 * <p>These endpoints are only accessible to users with the role "ROLE_ADMIN".
 */
@Tag(name = "User information (admin only)")
@RequestMapping("/api/admin/users")
@RestController
public class UsersController extends ApiController {
  @Autowired private UserDataDTOService userDataDTOService;

  /**
   * This method returns a list of all users. Accessible only to users with the role "ROLE_ADMIN".
   *
   * @return a list of all users
   * @throws JsonProcessingException if there is an error processing the JSON
   */
  @Operation(summary = "Get a list of all users")
  @PreAuthorize("hasRole('ROLE_ADMIN')")
  @GetMapping("")
  public Page<UserDataDTO> users(Pageable pageable) throws JsonProcessingException {
    return userDataDTOService.getUserDataDTOs(pageable);
  }
}
