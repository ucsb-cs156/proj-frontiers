package edu.ucsb.cs156.frontiers.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import edu.ucsb.cs156.frontiers.models.UserDataDTO;
import edu.ucsb.cs156.frontiers.repositories.UserRepository;

import edu.ucsb.cs156.frontiers.services.UserDataDTOService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

/**
 * This is a REST controller for getting information about the users.
 * 
 * These endpoints are only accessible to users with the role "ROLE_ADMIN".
 */

@Tag(name="User information (admin only)")
@RequestMapping("/api/admin/users")
@RestController
public class UsersController extends ApiController {
    @Autowired
    private UserDataDTOService userDataDTOService;


    /**
     * This method returns a list of all users.  Accessible only to users with the role "ROLE_ADMIN".
     * @return a list of all users
     * @throws JsonProcessingException if there is an error processing the JSON
     */
    @Operation(summary= "Get a list of all users")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @GetMapping("")
    public List<UserDataDTO> users()
            throws JsonProcessingException {
        return userDataDTOService.getUserDataDTOs();
    }
}