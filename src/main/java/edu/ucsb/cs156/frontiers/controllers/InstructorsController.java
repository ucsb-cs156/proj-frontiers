package edu.ucsb.cs156.frontiers.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import edu.ucsb.cs156.frontiers.errors.EntityNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;

import edu.ucsb.cs156.frontiers.entities.Instructor;
import edu.ucsb.cs156.frontiers.repositories.InstructorRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.extern.slf4j.Slf4j;

@Tag(name = "Instructors")
@RequestMapping("/api/instructors")
@RestController
@Slf4j

public class InstructorsController extends ApiController{

    @Autowired
    InstructorRepository instructorRepository;

    /**
     * This method creates a new instructor. Accessible only to users with the role "ROLE_ADMIN".
     * @param email email of the the instructor
     * @return the new instructor
     */
    @Operation(summary= "Add a new Instructor")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PostMapping("/post")
    public Instructor postInstructor(
        @Parameter(name="email") @RequestParam String email
        )
        {

        Instructor instructor = new Instructor(email);

        Instructor savedInstructor = instructorRepository.save(instructor);

        return savedInstructor;
    }

    /**
     * THis method returns a list of all instructors.
     * @return a list of all instructors
     */
    @Operation(summary= "List all Instructors")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @GetMapping("/all")
    public Iterable<Instructor> allInstructors() {
        Iterable<Instructor> instructors = instructorRepository.findAll();
        return instructors;
    }

    /**
     * This method deletes an instructor. Accessible only to users with the role "ROLE_ADMIN".
     * @param email email of the instructor
     * @return a message indicating the instructor was deleted
     * */

    @Operation(summary= "Delete an Instructor")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @DeleteMapping("")
    public Object deleteInstructor(
            @Parameter(name="email") @RequestParam String email) {
        Instructor instructor = instructorRepository.findById(email)
                .orElseThrow(() -> new EntityNotFoundException(Instructor.class, email));
        instructorRepository.delete(instructor);
        return genericMessage("Instructor with id %s deleted".formatted(email));
    }

}