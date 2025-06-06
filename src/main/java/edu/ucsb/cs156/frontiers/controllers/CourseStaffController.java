package edu.ucsb.cs156.frontiers.controllers;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.*; 

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

import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.entities.User; 
import edu.ucsb.cs156.frontiers.entities.RosterStudent;
import edu.ucsb.cs156.frontiers.enums.OrgStatus;
import edu.ucsb.cs156.frontiers.enums.RosterStatus;
import edu.ucsb.cs156.frontiers.entities.CourseStaff; 
import edu.ucsb.cs156.frontiers.errors.EntityNotFoundException;
import edu.ucsb.cs156.frontiers.errors.InvalidInstallationTypeException;
import edu.ucsb.cs156.frontiers.models.CurrentUser;
import edu.ucsb.cs156.frontiers.repositories.CourseRepository;
import edu.ucsb.cs156.frontiers.repositories.CourseStaffRepository;
import edu.ucsb.cs156.frontiers.repositories.RosterStudentRepository;
import edu.ucsb.cs156.frontiers.repositories.UserRepository;
import edu.ucsb.cs156.frontiers.services.OrganizationLinkerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;

@Tag(name = "CourseStaff")
@RequestMapping("/api/coursestaff")
@RestController
@Slf4j
public class CourseStaffController extends ApiController {
    
    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private CourseStaffRepository courseStaffRepository; 


     /**
     * This method creates a new Course Staff Roster.
     * 
     * @return the created course staff roster
     */

    @Operation(summary = "Create a new course staff member")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PostMapping("/post")
    public CourseStaff postStaffRoster(
            @Parameter(name = "role") @RequestParam String role,
            @Parameter(name = "studentEmail") @RequestParam String studentEmail, 
            @Parameter(name = "courseId") @RequestParam Long courseId) throws EntityNotFoundException {

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new EntityNotFoundException(Course.class, courseId));
        CourseStaff cs = CourseStaff.builder()
                .email(studentEmail)
                .course(course)
                .role(role) 
                .orgStatus(OrgStatus.NONE)
                .build();
        CourseStaff savedCourseStaff = courseStaffRepository.save(cs);

        return savedCourseStaff;
    }

}
