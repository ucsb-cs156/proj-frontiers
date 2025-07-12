package edu.ucsb.cs156.frontiers.controllers;


import edu.ucsb.cs156.frontiers.entities.Admin;
import edu.ucsb.cs156.frontiers.errors.EntityNotFoundException;
import edu.ucsb.cs156.frontiers.repositories.AdminRepository;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;


import com.fasterxml.jackson.core.JsonProcessingException;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import jakarta.validation.Valid;


import java.time.LocalDateTime;
import java.util.List;


/**
* This is a REST controller for Admin
*/
@Tag(name = "Admin")
@RequestMapping("/api/admin")
@RestController
@Slf4j

public class AdminsController extends ApiController {
   @Autowired
   AdminRepository adminRepository;

    @Value("#{'${app.admin.emails}'.split(',')}")
    List<String> adminEmails;


  public static record AdminDTO(
          String email,
          boolean isInAdminEmails
  ) {
      public AdminDTO(Admin admin, List<String> adminEmails) {
          this(
                  admin.getEmail(),
                  adminEmails.contains(admin.getEmail())
          );
      }
  }

   /**
   * Create a new admin
   * @param adminEmail       the email in typical email format
   * @return the saved admin
   */
  @Operation(summary= "Create a new admin")
  @PreAuthorize("hasRole('ROLE_ADMIN')")
  @PostMapping("/post")
  public Admin postAdmin(
          @Parameter(name="email") @RequestParam String email)
      {
      
      Admin admin = new Admin(email);
      Admin savedAdmin = adminRepository.save(admin);
      return savedAdmin;
  }

  /**
    * List all admins
    *
    * @return an iterable of Admin
    */
    @Operation(summary= "List all admins")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @GetMapping("/all")
    public Iterable<AdminDTO> allAdmins() {
        Iterable<Admin> admins = adminRepository.findAll();
        Iterable<AdminDTO> adminDTOs = ((List<Admin>) admins).stream()
                .map(admin -> new AdminDTO(admin, adminEmails))
                .toList();

        return adminDTOs;
    }

    /**
    * Delete an Admin
    *
    * @param email the email of the admin to delete
    * @return a message indicating the admin was deleted
    */
   @Operation(summary= "Delete an Admin")
   @PreAuthorize("hasRole('ROLE_ADMIN')")
   @DeleteMapping("/delete")
   public Object deleteAdmin(
           @Parameter(name="email") @RequestParam String email) {
       Admin admin = adminRepository.findByEmail(email)
               .orElseThrow(() -> new EntityNotFoundException(Admin.class, email));
      if (adminEmails.contains(email)) {
        throw new UnsupportedOperationException("Forbidden to delete an admin from ADMIN_EMAILS list");
      }
       adminRepository.delete(admin);
       return genericMessage("Admin with id %s deleted".formatted(email));
   }

}


