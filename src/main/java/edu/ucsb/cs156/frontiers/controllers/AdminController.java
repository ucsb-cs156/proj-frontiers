package edu.ucsb.cs156.frontiers.controllers;

import java.util.List;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
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
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

@Tag(name = "Admins")
@RequestMapping("/api/admins")
@RestController
@Slf4j
public class AdminController extends ApiController{
    @Autowired
    AdminRepository adminRepository;

    // @Value("${ADMIN_EMAILS:}")
    // private String adminEmailsRaw;

    // private List<String> adminEmails;

    // @PostConstruct
    // public void init() {

    //     log.info("Loaded ADMIN_EMAILS: '{}'", adminEmailsRaw);

    //     if (adminEmailsRaw != null && !adminEmailsRaw.isBlank()) {
    //         adminEmails = List.of(adminEmailsRaw.split("\\s*,\\s*"));
    //     } else {
    //         adminEmails = List.of(); // default to empty list
    //     }

    //     log.info("Parsed adminEmails list: {}", adminEmails);


    // }
    @Value("#{'${app.admin-emails}'.split(',')}")
    private List<String> adminEmails;

    @Operation(summary= "List all Admins")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @GetMapping("/all")
    public Iterable<Admin> allAdmin() {
        Iterable<Admin> admin = adminRepository.findAll();
        return admin;
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
        
        if (adminEmails.contains(email)) {
            throw new UnsupportedOperationException("Cannot delete admin email: " + email);
        }

        Admin  admin = adminRepository.findById(email)
                .orElseThrow(() -> new EntityNotFoundException(Admin.class, email));

        adminRepository.delete(admin);
        return genericMessage("Admin with email is deleted".formatted(email));
    }



}
