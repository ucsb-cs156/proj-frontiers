package edu.ucsb.cs156.frontiers.controllers;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.fasterxml.jackson.core.JsonProcessingException;

import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.entities.CourseStaff;
import edu.ucsb.cs156.frontiers.entities.RosterStudent;
import edu.ucsb.cs156.frontiers.entities.User;
import edu.ucsb.cs156.frontiers.enums.OrgStatus;
import edu.ucsb.cs156.frontiers.errors.EntityNotFoundException;
import edu.ucsb.cs156.frontiers.errors.InvalidInstallationTypeException;
import edu.ucsb.cs156.frontiers.models.CurrentUser;
import edu.ucsb.cs156.frontiers.models.RosterStudentDTO;
import edu.ucsb.cs156.frontiers.repositories.CourseRepository;
import edu.ucsb.cs156.frontiers.repositories.CourseStaffRepository;
import edu.ucsb.cs156.frontiers.repositories.RosterStudentRepository;
import edu.ucsb.cs156.frontiers.repositories.UserRepository;
import edu.ucsb.cs156.frontiers.repositories.InstructorRepository;
import edu.ucsb.cs156.frontiers.repositories.AdminRepository;
import edu.ucsb.cs156.frontiers.services.OrganizationLinkerService;
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

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RosterStudentRepository rosterStudentRepository;

    @Autowired
    private CourseStaffRepository courseStaffRepository;

    @Autowired
    private InstructorRepository instructorRepository;

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private OrganizationLinkerService linkerService;

    /**
     * This method creates a new Course.
     * 
     * @param courseName the name of the course
     * @param term       the term of the course
     * @param school     the school of the course
     * @return the created course
     */

    @Operation(summary = "Create a new course")
    @PreAuthorize("hasRole('ROLE_ADMIN') || hasRole('ROLE_INSTRUCTOR')")
    @PostMapping("/post")
    public InstructorCourseView postCourse(
            @Parameter(name = "courseName") @RequestParam String courseName,
            @Parameter(name = "term") @RequestParam String term,
            @Parameter(name = "school") @RequestParam String school) {
        // get current date right now and set status to pending
        CurrentUser currentUser = getCurrentUser();
        Course course = Course.builder()
                .courseName(courseName)
                .term(term)
                .school(school)
                .instructorEmail(currentUser.getUser().getEmail())
                .build();
        Course savedCourse = courseRepository.save(course);

        return new InstructorCourseView(savedCourse);
    }

    /**
     * Projection of Course entity with fields that are relevant for instructors
     * and admins
     */
    public static record InstructorCourseView(
            Long id,
            String installationId,
            String orgName,
            String courseName,
            String term,
            String school,
            String instructorEmail) {

        // Creates view from Course entity
        public InstructorCourseView(Course c) {
            this(
                    c.getId(),
                    c.getInstallationId(),
                    c.getOrgName(),
                    c.getCourseName(),
                    c.getTerm(),
                    c.getSchool(),
                    c.getInstructorEmail());
        }
    }

    /**
     * This method returns a list of courses.
     * 
     * @return a list of all courses for an instructor.
     */
    @Operation(summary = "List all courses for an instructor")
    @PreAuthorize("hasRole('ROLE_INSTRUCTOR')")
    @GetMapping("/allForInstructors")
    public Iterable<InstructorCourseView> allForInstructors() {
        CurrentUser currentUser = getCurrentUser();
        String instructorEmail = currentUser.getUser().getEmail();
        List<Course> courses = courseRepository.findByInstructorEmail(instructorEmail);

        List<InstructorCourseView> courseViews = courses.stream()
                .map(InstructorCourseView::new)
                .collect(Collectors.toList());
        return courseViews;
    }

    /**
     * This method returns a list of courses.
     * 
     * @return a list of all courses for an admin.
     */
    @Operation(summary = "List all courses for an admin")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @GetMapping("/allForAdmins")
    public Iterable<InstructorCourseView> allForAdmins() {
        List<Course> courses = courseRepository.findAll();

        List<InstructorCourseView> courseViews = courses.stream()
                .map(InstructorCourseView::new)
                .collect(Collectors.toList());
        return courseViews;
    }
    
    @Operation(summary = "List all courses")
    @PreAuthorize("hasRole('ROLE_ADMIN') || hasRole('ROLE_INSTRUCTOR')")
    @GetMapping("/all")
    public Iterable<InstructorCourseView> allCourses() {
        List<Course> courses = null;
        if (!isCurrentUserAdmin()) {
            // if the user is not an admin, return only the courses they created
            CurrentUser currentUser = getCurrentUser();
            String userEmail = currentUser.getUser().getEmail();
            courses = courseRepository.findByInstructorEmail(userEmail);
            // Convert to InstructorCourseView
            List<InstructorCourseView> courseViews = courses.stream()
                    .map(InstructorCourseView::new)
                    .collect(Collectors.toList());
            // Return as Iterable
            return courseViews;
        } else {
            courses = courseRepository.findAll();
        }
        List<InstructorCourseView> courseViews = courses.stream()
                .map(InstructorCourseView::new)
                .collect(Collectors.toList());
        return courseViews;
    }

    /**
     * This method returns single course by its id
     * 
     * @return a course
     */
    @Operation(summary = "Get course by id")
    @PreAuthorize("hasRole('ROLE_ADMIN') || hasRole('ROLE_INSTRUCTOR')")
    @GetMapping("/{id}")
    public InstructorCourseView getCourseById(@Parameter(name = "id") @PathVariable Long id) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(Course.class, id));
        if (!isCurrentUserAdmin() && !course.getInstructorEmail().equals(getCurrentUser().getUser().getEmail())) {
            throw new EntityNotFoundException(Course.class, id);
        }
        // Convert to InstructorCourseView
        InstructorCourseView courseView = new InstructorCourseView(course);
        return courseView;
    }

    /**
     * <p>
     * This is the outgoing method, redirecting from Frontiers to GitHub to allow a
     * Course to be linked to a GitHub Organization.
     * It redirects from Frontiers to the GitHub app installation process, and will
     * return with the {@link #addInstallation(Optional, String, String, Long)
     * addInstallation()} endpoint
     * </p>
     * 
     * @param courseId id of the course to be linked to
     * @return dynamically loaded url to install Frontiers to a Github Organization,
     *         with the courseId marked as the state parameter, which GitHub will
     *         return.
     *
     */
    @Operation(summary = "Authorize Frontiers to a Github Course")
    @PreAuthorize("hasRole('ROLE_ADMIN') || hasRole('ROLE_INSTRUCTOR')")
    @GetMapping("/redirect")
    public ResponseEntity<Void> linkCourse(@Parameter Long courseId)
            throws JsonProcessingException, NoSuchAlgorithmException, InvalidKeySpecException {
        String newUrl = linkerService.getRedirectUrl();
        newUrl += "/installations/new?state=" + courseId;
        // found this convenient solution here:
        // https://stackoverflow.com/questions/29085295/spring-mvc-restcontroller-and-redirect
        return ResponseEntity.status(HttpStatus.MOVED_PERMANENTLY).header(HttpHeaders.LOCATION, newUrl).build();
    }

    /**
     *
     * @param installation_id id of the incoming GitHub Organization installation
     * @param setup_action    whether the permissions are installed or updated.
     *                        Required RequestParam but not used by the method.
     * @param code            token to be exchanged with GitHub to ensure the
     *                        request is legitimate and not spoofed.
     * @param state           id of the Course to be linked with the GitHub
     *                        installation.
     * @return ResponseEntity, returning /success if the course was successfully
     *         linked or /noperms if the user does not have the permission to
     *         install the application on GitHub. Alternately returns 403 Forbidden
     *         if the user is not the creator.
     */

    @Operation(summary = "Link a Course to a Github Organization by installing Github App")
    @PreAuthorize("hasRole('ROLE_ADMIN') || hasRole('ROLE_INSTRUCTOR')")
    @GetMapping("link")
    public ResponseEntity<Void> addInstallation(
            @Parameter(name = "installationId") @RequestParam Optional<String> installation_id,
            @Parameter(name = "setupAction") @RequestParam String setup_action,
            @Parameter(name = "code") @RequestParam String code,
            @Parameter(name = "state") @RequestParam Long state)
            throws NoSuchAlgorithmException, InvalidKeySpecException, JsonProcessingException {
        if (installation_id.isEmpty()) {
            return ResponseEntity.status(HttpStatus.MOVED_PERMANENTLY)
                    .header(HttpHeaders.LOCATION, "/courses/nopermissions").build();
        } else {
            Course course = courseRepository.findById(state)
                    .orElseThrow(() -> new EntityNotFoundException(Course.class, state));
            if (!isCurrentUserAdmin() && !course.getInstructorEmail().equals(getCurrentUser().getUser().getEmail())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            } else {
                String orgName = linkerService.getOrgName(installation_id.get());
                course.setInstallationId(installation_id.get());
                course.setOrgName(orgName);
                course.getRosterStudents().forEach(rs -> {
                    rs.setOrgStatus(OrgStatus.JOINCOURSE);
                });
                course.getCourseStaff().forEach(cs -> {
                    cs.setOrgStatus(OrgStatus.JOINCOURSE);
                });
                courseRepository.save(course);
                return ResponseEntity.status(HttpStatus.MOVED_PERMANENTLY)
                        .header(HttpHeaders.LOCATION, "/instructor/courses?success=True&course=" + state).build();
            }
        }
    }

    /**
     * This method handles the InvalidInstallationTypeException.
     * 
     * @param e the exception
     * @return a map with the type and message of the exception
     */
    @ExceptionHandler({ InvalidInstallationTypeException.class })
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Object handleInvalidInstallationType(Throwable e) {
        return Map.of(
                "type", e.getClass().getSimpleName(),
                "message", e.getMessage());
    }

    public record RosterStudentCoursesDTO(
            Long id,
            String installationId,
            String orgName,
            String courseName,
            String term,
            String school,
            OrgStatus studentStatus,
            Long rosterStudentId) {
    }

    /**
     * This method returns a list of courses that the current user is enrolled.
     * 
     * @return a list of courses in the DTO form along with the student status in
     *         the organization.
     */
    @Operation(summary = "List all courses for the current student, including their org status")
    @PreAuthorize("hasRole('ROLE_USER')")
    @GetMapping("/list")
    public List<RosterStudentCoursesDTO> listCoursesForCurrentUser() {
        String email = getCurrentUser().getUser().getEmail();
        Iterable<RosterStudent> rosterStudentsIterable = rosterStudentRepository.findAllByEmail(email);
        List<RosterStudent> rosterStudents = new ArrayList<>();
        rosterStudentsIterable.forEach(rosterStudents::add);
        return rosterStudents.stream()
                .map(rs -> {
                    Course course = rs.getCourse();
                    RosterStudentCoursesDTO rsDto = new RosterStudentCoursesDTO(
                            course.getId(),
                            course.getInstallationId(),
                            course.getOrgName(),
                            course.getCourseName(),
                            course.getTerm(),
                            course.getSchool(),
                            rs.getOrgStatus(),
                            rs.getId());
                    return rsDto;
                })
                .collect(Collectors.toList());
    }

    public record StaffCoursesDTO(
            Long id,
            String installationId,
            String orgName,
            String courseName,
            String term,
            String school,
            OrgStatus studentStatus,
            Long staffId) {
    }

    /**
     * student see what courses they appear as staff in
     * 
     * @param studentId the id of the student making request
     * @return a list of all courses student is staff in
     */
    @Operation(summary = "Student see what courses they appear as staff in")
    @PreAuthorize("hasRole('ROLE_USER')")
    @GetMapping("/staffCourses")
    public List<StaffCoursesDTO> staffCourses() {
        CurrentUser currentUser = getCurrentUser();
        User user = currentUser.getUser();

        String email = user.getEmail();

        List<CourseStaff> staffMembers = courseStaffRepository.findAllByEmail(email);
        return staffMembers.stream()
                .map(s -> {
                    Course course = s.getCourse();
                    StaffCoursesDTO sDto = new StaffCoursesDTO(
                            course.getId(),
                            course.getInstallationId(),
                            course.getOrgName(),
                            course.getCourseName(),
                            course.getTerm(),
                            course.getSchool(),
                            s.getOrgStatus(),
                            s.getId());
                    return sDto;
                })
                .collect(Collectors.toList());
    }

    @Operation(summary = "Update instructor email for a course (admin only)")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PutMapping("/updateInstructor")
    public InstructorCourseView updateInstructorEmail(
            @Parameter(name = "courseId") @RequestParam Long courseId,
            @Parameter(name = "instructorEmail") @RequestParam String instructorEmail) {
        
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new EntityNotFoundException(Course.class, courseId));
        
        // Validate that the email exists in either instructor or admin table
        boolean isInstructor = instructorRepository.existsByEmail(instructorEmail);
        boolean isAdmin = adminRepository.existsByEmail(instructorEmail);
        
        if (!isInstructor && !isAdmin) {
            throw new IllegalArgumentException("Email must belong to either an instructor or admin");
        }
        
        course.setInstructorEmail(instructorEmail);
        Course savedCourse = courseRepository.save(course);
        
        return new InstructorCourseView(savedCourse);
    }

    @Operation(summary = "Delete a course")
    @PreAuthorize("@CourseSecurity.hasManagePermissions(#root, #courseId)")
    @DeleteMapping("")
    public Object deleteCourse(@RequestParam Long courseId) throws NoSuchAlgorithmException, InvalidKeySpecException {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new EntityNotFoundException(Course.class, courseId));
        linkerService.unenrollOrganization(course);
        courseRepository.delete(course);
        return genericMessage("Course with id %s deleted".formatted(course.getId()));
    }

}
