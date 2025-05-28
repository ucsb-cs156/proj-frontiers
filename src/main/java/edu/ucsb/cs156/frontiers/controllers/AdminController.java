package edu.ucsb.cs156.frontiers.controllers;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;

import edu.ucsb.cs156.frontiers.entities.Admin;
import edu.ucsb.cs156.frontiers.errors.EntityNotFoundException;
import edu.ucsb.cs156.frontiers.repositories.AdminRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;

@Tag(name = "Admins")
@RequestMapping("/api/admin/admins")
@RestController
@Slf4j
public class AdminController extends ApiController{
    @Autowired
    AdminRepository adminRepository;

    @Value("${app.admin.emails}")
    private String[] protectedAdminEmails;

    @Operation(summary= "List all Admins")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @GetMapping("/all")
    public Iterable<Admin> allAdmin() {
        return adminRepository.findAll(Sort.by("email").ascending());
    }

    @Operation(summary= "Create a new admin")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PostMapping("/post")
    public Admin postAdmin(
    @Parameter(name="email") @RequestParam String email)
    throws JsonProcessingException {

    Admin admin = new Admin();
    admin.setEmail(email);

    Admin savedAdmin = adminRepository.save(admin);

    return savedAdmin;
    }

    @Operation(summary= "Delete an Admin")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @DeleteMapping("")
    public Object deleteAdmin(@Parameter(name="email") @RequestParam String email) {

        if (Arrays.asList(protectedAdminEmails).contains(email)) {
            throw new UnsupportedOperationException("Cannot delete protected admin: " + email);
        }
        
        Admin  admin = adminRepository.findById(email)
                .orElseThrow(() -> new EntityNotFoundException(Admin.class, email));

        adminRepository.delete(admin);
        return genericMessage("Admin with email is deleted".formatted(email));
    }
}
