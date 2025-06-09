package edu.ucsb.cs156.frontiers.controllers;

import edu.ucsb.cs156.frontiers.entities.Instructor;
import edu.ucsb.cs156.frontiers.errors.EntityNotFoundException;
import edu.ucsb.cs156.frontiers.repositories.InstructorRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Instructors")
@RequestMapping("/api/instructors")
@RestController
@Slf4j
public class InstructorsController extends ApiController {

    @Autowired
    private InstructorRepository instructorRepository;

    // POST
    @Operation(summary = "Create a new instructor")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PostMapping("/post")
    public Instructor postInstructor(
            @Parameter(name = "email") @RequestParam String email) {

        Instructor instructor = Instructor.builder().email(email).build();
        return instructorRepository.save(instructor);
    }

    // GET
    @Operation(summary = "Get all instructors")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @GetMapping("/all")
    public Iterable<Instructor> allInstructors() {
        return instructorRepository.findAll();
    }

    // DELETE
    @Operation(summary = "Delete instructor by email")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @DeleteMapping("")
    public String deleteInstructor(
            @Parameter(name = "email") @RequestParam String email) {

        Instructor instructor = instructorRepository.findById(email)
                .orElseThrow(() -> new EntityNotFoundException(Instructor.class, email));
        instructorRepository.delete(instructor);
        return String.format("Instructor with email %s deleted", email);
    }
}
