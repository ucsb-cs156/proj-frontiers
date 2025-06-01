package edu.ucsb.cs156.frontiers.controllers;


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

import edu.ucsb.cs156.frontiers.entities.Instructor;
import edu.ucsb.cs156.frontiers.errors.EntityNotFoundException;
import edu.ucsb.cs156.frontiers.repositories.InstructorRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;

@Tag(name = "Instructors")
@RequestMapping("/api/admin/instructors")
@RestController
@Slf4j
public class InstructorController extends ApiController{
    @Autowired
    InstructorRepository instructorRepository;

    @Operation(summary= "List all instructors")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @GetMapping("/all")
    public Iterable<Instructor> allInstructors() {
        return instructorRepository.findAll(Sort.by("email").ascending());
    }

    @Operation(summary= "Create a new instructor")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PostMapping("/post")
    public Instructor postInstructor(
    @Parameter(name="email") @RequestParam String email)
    throws JsonProcessingException {

    Instructor instructor = new Instructor();
    instructor.setEmail(email);

    Instructor savedInstructor = instructorRepository.save(instructor);

    return savedInstructor;
    }

    @Operation(summary= "Delete an Instructor")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @DeleteMapping("")
    public Object deleteInstructor(@Parameter(name="email") @RequestParam String email) {
        
        Instructor  instructor = instructorRepository.findById(email)
                .orElseThrow(() -> new EntityNotFoundException(Instructor.class, email));

        instructorRepository.delete(instructor);
        return genericMessage("Instructor with valid email is deleted".formatted(email));
    }
}
