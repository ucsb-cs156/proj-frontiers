package edu.ucsb.cs156.frontiers.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import edu.ucsb.cs156.frontiers.entities.Admin;
import edu.ucsb.cs156.frontiers.repositories.AdminRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.extern.slf4j.Slf4j;

@Tag(name = "Admins")
@RequestMapping("/api/admins")
@RestController
@Slf4j

public class AdminsController extends ApiController{

    @Autowired
    AdminRepository adminRepository;

    /**
     * This method creates a new admin. Accessible only to users with the role "ROLE_ADMIN".
     * @param email email of the the admin
     * @return the new admin
     */
    @Operation(summary= "Add a new Admin")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PostMapping("/post")
    public Admin postAdmin(
        @Parameter(name="email") @RequestParam String email
        )
        {

        Admin admin = new Admin(email);

        Admin savedAdmin = adminRepository.save(admin);

        return savedAdmin;
    }

    /**
     * THis method returns a list of all admins.
     * @return a list of all admins
     */
    @Operation(summary= "List all Admins")
    @PreAuthorize("hasRole('ROLE_USER')")
    @GetMapping("/all")
    public Iterable<Admin> allAdmins() {
        Iterable<Admin> admins = adminRepository.findAll();
        return admins;
    }
}