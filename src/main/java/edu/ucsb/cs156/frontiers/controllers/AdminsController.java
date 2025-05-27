package edu.ucsb.cs156.frontiers.controllers;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Map;
import java.util.Optional;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;

import edu.ucsb.cs156.frontiers.entities.Admin;
import edu.ucsb.cs156.frontiers.errors.EntityNotFoundException;
import edu.ucsb.cs156.frontiers.errors.InvalidInstallationTypeException;
import edu.ucsb.cs156.frontiers.models.CurrentUser;
import edu.ucsb.cs156.frontiers.repositories.AdminRepository;
import edu.ucsb.cs156.frontiers.services.OrganizationLinkerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.DeleteMapping;

@Tag(name = "Admins")
@RequestMapping("/api/admins")
@RestController
@Slf4j
public class AdminsController extends ApiController {
    
    @Autowired
    private AdminRepository adminRepository;

    @Value("${admin.emails:}")
    private String adminEmails;

     /**
     * This method creates a new Admin.
     * 
    * @param email the email of the admin
    * @return the created admin
     */

    @Operation(summary = "Create a new admin")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PostMapping("/post")
    public Admin postAdmin(@Parameter(name = "email") @RequestParam String email) {
        Admin admin = Admin.builder()
                .email(email)
                .build();
        Admin savedAdmin = adminRepository.save(admin);
        return savedAdmin;
    }

    /**
     * This method returns a list of admins.
     * @return a list of all admins.
     */
    @Operation(summary = "List all admins")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @GetMapping("/all")
    public Iterable<Admin> allAdmins(
    ) {
        Iterable<Admin> admins = adminRepository.findAll();
        return admins;
    }

    /**
     * This method deletes an admin.
     * @param email the email of the admin
     * @return the deleted admin
     */
    @Operation(summary = "Delete admin by email (unless in ADMIN_EMAILS)")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @DeleteMapping("")
    public String deleteAdmin(
        @Parameter(name = "email") @RequestParam String email) {

    List<String> permanentAdmins = List.of(adminEmails.split(","));

    if (permanentAdmins.contains(email)) {
        throw new UnsupportedOperationException("Cannot delete permanent admin: " + email);
    }

    Admin admin = adminRepository.findById(email)
            .orElseThrow(() -> new EntityNotFoundException(Admin.class, email));

    adminRepository.delete(admin);
    return String.format("Admin with email %s deleted", email);
    }

    @ExceptionHandler(UnsupportedOperationException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Object handleUnsupportedOperation(Throwable e) {
        return Map.of(
            "type", e.getClass().getSimpleName(),
            "message", e.getMessage()
        );
    }

}
