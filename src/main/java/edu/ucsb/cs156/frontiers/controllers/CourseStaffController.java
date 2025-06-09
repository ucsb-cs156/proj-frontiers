package edu.ucsb.cs156.frontiers.controllers;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import edu.ucsb.cs156.frontiers.entities.Job;
import edu.ucsb.cs156.frontiers.entities.User;
import edu.ucsb.cs156.frontiers.errors.NoLinkedOrganizationException;
import edu.ucsb.cs156.frontiers.jobs.UpdateOrgMembershipJob;
import edu.ucsb.cs156.frontiers.repositories.UserRepository;
import edu.ucsb.cs156.frontiers.services.*;
import edu.ucsb.cs156.frontiers.services.jobs.JobService;
import org.apache.coyote.BadRequestException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.JsonProcessingException;

import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.entities.CourseStaff;
import edu.ucsb.cs156.frontiers.enums.OrgStatus;
import edu.ucsb.cs156.frontiers.enums.RosterStatus;
import edu.ucsb.cs156.frontiers.errors.EntityNotFoundException;
import edu.ucsb.cs156.frontiers.repositories.CourseRepository;
import edu.ucsb.cs156.frontiers.repositories.CourseStaffRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;

@Tag(name = "CourseStaff")
@RequestMapping("/api/coursestaff")
@RestController
@Slf4j
public class CourseStaffController extends ApiController {

    @Autowired
    private JobService jobService;
    @Autowired
    private OrganizationMemberService organizationMemberService;

    @Autowired
    private CourseStaffRepository courseStaffRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private UpdateUserService updateUserService;

    @Autowired
    private CurrentUserService currentUserService;

    /**
     * This method creates a new CourseStaff.
     * 
     * 
     * @return the created CourseStaff
     */

    @Operation(summary = "Create a new course staff")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PostMapping("/post")
    public CourseStaff postCourseStaff(
            @Parameter(name = "firstName") @RequestParam String firstName,
            @Parameter(name = "lastName") @RequestParam String lastName,
            @Parameter(name = "email") @RequestParam String email,
            @Parameter(name = "courseId") @RequestParam Long courseId) throws EntityNotFoundException {

        // Get Course or else throw an error

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new EntityNotFoundException(Course.class, courseId));

        CourseStaff courseStaff = CourseStaff.builder()
                .firstName(firstName)
                .lastName(lastName)
                .email(email)
                .course(course)
                .build();
        CourseStaff savedCourseStaff = courseStaffRepository.save(courseStaff);

        return savedCourseStaff;
    }

    /**
     * This method returns a list of course staff for a given course.
     * 
     * @return a list of all course staff.
     */
    @Operation(summary = "List all course staff for a course")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @GetMapping("/course")
    public Iterable<CourseStaff> courseStaffForCourse(
            @Parameter(name = "courseId") @RequestParam Long courseId) throws EntityNotFoundException {
        courseRepository.findById(courseId).orElseThrow(() -> new EntityNotFoundException(Course.class, courseId));
        Iterable<CourseStaff> courseStaffs = courseStaffRepository.findByCourseId(courseId);
        return courseStaffs;
    }
}
