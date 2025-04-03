package edu.ucsb.cs156.frontiers.controllers;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.time.LocalDateTime;
import java.util.Optional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.ucsb.cs156.frontiers.errors.EntityNotFoundException;
import edu.ucsb.cs156.frontiers.services.JwtService;
import edu.ucsb.cs156.frontiers.services.OrganizationLinkerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
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
import org.springframework.web.client.RestTemplate;

@Tag(name = "Course")
@RequestMapping("/api/courses")
@RestController
@Slf4j
public class CoursesController extends ApiController {
    
    @Autowired
    private CourseRepository courseRepository;

    @Autowired private JwtService jwtService;

    @Autowired private RestTemplateBuilder restTemplateBuilder;

    @Autowired private ObjectMapper objectMapper;

    @Autowired private OrganizationLinkerService linkerService;

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

    @Operation(summary = "Authorize a Course to a Github Course")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @GetMapping("/redirect")
    public ResponseEntity<Void> linkCourse(@Parameter Long courseId) throws JsonProcessingException, NoSuchAlgorithmException, InvalidKeySpecException {
        String newUrl = linkerService.getRedirectUrl();
        newUrl += "/installations/new?state="+courseId;
        //found this convenient solution here: https://stackoverflow.com/questions/29085295/spring-mvc-restcontroller-and-redirect
        return ResponseEntity.status(HttpStatus.MOVED_PERMANENTLY).header(HttpHeaders.LOCATION, newUrl).build();
    }


    @Operation(summary = "Link a Course to a Github Course")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @GetMapping("link")
    public ResponseEntity<Void> addInstallation(@Parameter(name = "installationId") @RequestParam Optional<String> installation_id,
                                                @Parameter(name = "setupAction") @RequestParam String setup_action,
                                                @Parameter(name = "code") @RequestParam String code,
                                                @Parameter(name = "state") @RequestParam Long state) throws NoSuchAlgorithmException, InvalidKeySpecException, JsonProcessingException {
        if(installation_id.isEmpty()) {
            return ResponseEntity.status(HttpStatus.MOVED_PERMANENTLY).header(HttpHeaders.LOCATION, "/courses/nopermissions").build();
        }else {
            String orgName = linkerService.getOrgName(installation_id.get());
            Course course = courseRepository.findById(state).orElseThrow(() -> new EntityNotFoundException(Course.class, state));
            if(!course.getCreator().equals(getCurrentUser().getUser())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }else{
                course.setInstallationId(installation_id.get());
                course.setOrgName(orgName);
                courseRepository.save(course);
                return ResponseEntity.status(HttpStatus.MOVED_PERMANENTLY).header(HttpHeaders.LOCATION, "/courses/success").build();
            }
        }
    }


}
