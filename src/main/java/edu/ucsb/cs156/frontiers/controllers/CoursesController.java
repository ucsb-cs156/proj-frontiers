package edu.ucsb.cs156.frontiers.controllers;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.models.CurrentUser;
import edu.ucsb.cs156.frontiers.repositories.CourseRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;

@Tag(name = "Course")
@RequestMapping("/api/courses")
@RestController
@Slf4j
public class CoursesController extends ApiController {
    
    @Autowired
    private CourseRepository courseRepository;

     /**
     * This method creates a new Course.
     * 
    * @param orgName the name of the organization
    * @param courseName the name of the course
    * @param term the term of the course
    * @param school the school of the course
    * @return the created course
     */

    @Operation(summary = "Create a new course")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PostMapping("/post")
    public Course postCourse(
            @Parameter(name = "orgName") @RequestParam String orgName,
            @Parameter(name = "courseName") @RequestParam String courseName,
            @Parameter(name = "term") @RequestParam String term,
            @Parameter(name = "school") @RequestParam String school
           )
            {
        //get current date right now and set status to pending
        CurrentUser currentUser = getCurrentUser();
        Course course = Course.builder()
                .orgName(orgName)
                .courseName(courseName)
                .term(term)
                .school(school)
                .creator(currentUser.getUser())
                .build();
        Course savedCourse = courseRepository.save(course);

        return savedCourse;
    }

      /**
     * This method returns a list of courses.
     * @return a list of all courses.
     */
    @Operation(summary = "List all courses")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @GetMapping("/all")
    public Iterable<Course> allCourses(
    ) {
        Iterable<Course> courses = courseRepository.findAll();
        return courses;
    }

}
