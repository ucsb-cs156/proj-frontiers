package edu.ucsb.cs156.frontiers.controllers;

import edu.ucsb.cs156.frontiers.entities.Assignment;
import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.enums.AssignmentType;
import edu.ucsb.cs156.frontiers.enums.Permission;
import edu.ucsb.cs156.frontiers.enums.RepositoryCreationOption;
import edu.ucsb.cs156.frontiers.enums.RepositoryPermissions;
import edu.ucsb.cs156.frontiers.enums.Visibility;
import edu.ucsb.cs156.frontiers.errors.EntityNotFoundException;
import edu.ucsb.cs156.frontiers.errors.NoLinkedOrganizationException;
import edu.ucsb.cs156.frontiers.jobs.CreateStudentOrStaffRepositoriesJob;
import edu.ucsb.cs156.frontiers.jobs.CreateTeamRepositoriesJob;
import edu.ucsb.cs156.frontiers.models.AssignmentWithJob;
import edu.ucsb.cs156.frontiers.repositories.AssignmentRepository;
import edu.ucsb.cs156.frontiers.repositories.CourseRepository;
import edu.ucsb.cs156.frontiers.services.GithubTeamService;
import edu.ucsb.cs156.frontiers.services.RepositoryService;
import edu.ucsb.cs156.jobs.entities.Job;
import edu.ucsb.cs156.jobs.services.JobService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Assignments of a course, and the jobs that create their repositories. Creating or editing an
 * assignment saves it and starts the job that creates its individual or team repositories, and the
 * id of that job is kept as the assignment's last job id.
 */
@Tag(name = "Assignments")
@RequestMapping("/api/assignments")
@RestController
@Slf4j
public class AssignmentsController extends ApiController {

  /** The team regular expression that matches every team. */
  public static final String DEFAULT_TEAM_REGEX = ".*";

  @Autowired private AssignmentRepository assignmentRepository;

  @Autowired private CourseRepository courseRepository;

  @Autowired private RepositoryService repositoryService;

  @Autowired private GithubTeamService githubTeamService;

  @Autowired private JobService jobService;

  /**
   * Lists the assignments of a course, sorted by repository prefix.
   *
   * @param courseId the ID of the course
   * @return the assignments of the course
   */
  @Operation(summary = "List the assignments of a course")
  @PreAuthorize("@CourseSecurity.hasManagePermissions(#root, #courseId)")
  @GetMapping("")
  public List<Assignment> getAssignments(@Parameter(name = "courseId") @RequestParam Long courseId)
      throws EntityNotFoundException {
    ensureCourseExists(courseId);
    return assignmentRepository.findByCourseIdOrderByRepoPrefixAsc(courseId);
  }

  /**
   * Creates an assignment and starts the job that creates its repositories.
   *
   * <p>For an INDIVIDUAL assignment, createReposFor says whom repositories are created for (default
   * STUDENTS_ONLY) and teamRegex must not be given. For a TEAM assignment, teamRegex is the regular
   * expression that team names must match (default {@value #DEFAULT_TEAM_REGEX}, every team) and
   * createReposFor must not be given.
   *
   * @param courseId the ID of the course the assignment is for
   * @param repoPrefix each repository created begins with this prefix
   * @param asnType the assignment type (individual vs team)
   * @param visibility the visibility of the repositories (public vs private)
   * @param permission the permission that students, or teams, have on the repositories
   * @param createReposFor INDIVIDUAL only: whom to create repositories for
   * @param teamRegex TEAM only: only teams whose names match get a repository
   * @return the created assignment, with the id of the job as its last job id, and that job
   */
  @Operation(summary = "Create an assignment and start creating its repositories")
  @PreAuthorize("@CourseSecurity.hasManagePermissions(#root, #courseId)")
  @PostMapping("/post")
  public AssignmentWithJob postAssignment(
      @Parameter(name = "courseId") @RequestParam Long courseId,
      @Parameter(name = "repoPrefix") @RequestParam String repoPrefix,
      @Parameter(name = "asnType") @RequestParam AssignmentType asnType,
      @Parameter(name = "visibility") @RequestParam Visibility visibility,
      @Parameter(name = "permission") @RequestParam Permission permission,
      @Parameter(name = "createReposFor") @RequestParam(required = false)
          RepositoryCreationOption createReposFor,
      @Parameter(name = "teamRegex") @RequestParam(required = false) String teamRegex)
      throws EntityNotFoundException {
    Course course = ensureCourseExists(courseId);
    Assignment assignment = Assignment.builder().course(course).asnType(asnType).build();
    applyFields(assignment, repoPrefix, visibility, permission, createReposFor, teamRegex);
    requireLinkedOrganization(course);

    return launchAndSave(course, assignment);
  }

  /**
   * Updates an assignment and starts the job that creates its repositories. The assignment type
   * cannot be changed: to change an individual assignment to a team one, or the other way round,
   * delete the assignment and create a new one. The same rules as for creating apply to
   * createReposFor and teamRegex, according to the assignment's type.
   *
   * @param courseId the ID of the course the assignment belongs to
   * @param assignmentId the ID of the assignment
   * @param repoPrefix each repository created begins with this prefix
   * @param visibility the visibility of the repositories (public vs private)
   * @param permission the permission that students, or teams, have on the repositories
   * @param createReposFor INDIVIDUAL only: whom to create repositories for
   * @param teamRegex TEAM only: only teams whose names match get a repository
   * @return the updated assignment, with the id of the job as its last job id, and that job
   */
  @Operation(summary = "Update an assignment and start creating its repositories")
  @PreAuthorize("@CourseSecurity.hasManagePermissions(#root, #courseId)")
  @PutMapping("/put")
  public AssignmentWithJob updateAssignment(
      @Parameter(name = "courseId") @RequestParam Long courseId,
      @Parameter(name = "assignmentId") @RequestParam Long assignmentId,
      @Parameter(name = "repoPrefix") @RequestParam String repoPrefix,
      @Parameter(name = "visibility") @RequestParam Visibility visibility,
      @Parameter(name = "permission") @RequestParam Permission permission,
      @Parameter(name = "createReposFor") @RequestParam(required = false)
          RepositoryCreationOption createReposFor,
      @Parameter(name = "teamRegex") @RequestParam(required = false) String teamRegex)
      throws EntityNotFoundException {
    Course course = ensureCourseExists(courseId);
    Assignment assignment = findAssignmentInCourse(courseId, assignmentId);
    applyFields(assignment, repoPrefix, visibility, permission, createReposFor, teamRegex);
    requireLinkedOrganization(course);

    return launchAndSave(course, assignment);
  }

  /**
   * Starts the job that creates the repositories of an existing assignment again, for example to
   * pick up students who have joined since the last run, and records it as the assignment's last
   * job.
   *
   * @param courseId the ID of the course the assignment belongs to
   * @param assignmentId the ID of the assignment
   * @return the assignment, with its new last job id, and the job that was started
   */
  @Operation(summary = "Start creating the repositories of an assignment")
  @PreAuthorize("@CourseSecurity.hasManagePermissions(#root, #courseId)")
  @PostMapping("/launch")
  public AssignmentWithJob launchAssignmentJob(
      @Parameter(name = "courseId") @RequestParam Long courseId,
      @Parameter(name = "assignmentId") @RequestParam Long assignmentId)
      throws EntityNotFoundException {
    Course course = ensureCourseExists(courseId);
    Assignment assignment = findAssignmentInCourse(courseId, assignmentId);
    requireLinkedOrganization(course);

    return launchAndSave(course, assignment);
  }

  @Operation(summary = "Delete an assignment")
  @PreAuthorize("@CourseSecurity.hasManagePermissions(#root, #courseId)")
  @DeleteMapping("/{id}")
  public Object deleteAssignment(
      @Parameter(name = "id") @PathVariable Long id,
      @Parameter(name = "courseId") @RequestParam Long courseId) {
    ensureCourseExists(courseId);
    Assignment assignment = findAssignmentInCourse(courseId, id);

    assignmentRepository.delete(assignment);

    return genericMessage(String.format("Assignment with id %s deleted", id));
  }

  private Course ensureCourseExists(Long courseId) {
    return courseRepository
        .findById(courseId)
        .orElseThrow(() -> new EntityNotFoundException(Course.class, courseId));
  }

  /** Finds an assignment, which must belong to the given course. */
  private Assignment findAssignmentInCourse(Long courseId, Long assignmentId) {
    return assignmentRepository
        .findById(assignmentId)
        .filter(a -> a.getCourse().getId().equals(courseId))
        .orElseThrow(() -> new EntityNotFoundException(Assignment.class, assignmentId));
  }

  /**
   * Starts the job that creates the assignment's repositories and saves the assignment with the id
   * of that job as its last job id.
   */
  private AssignmentWithJob launchAndSave(Course course, Assignment assignment) {
    Job job = launchJob(course, assignment);
    assignment.setLastJobId(job.getId());
    return new AssignmentWithJob(assignmentRepository.save(assignment), job);
  }

  private static void requireLinkedOrganization(Course course) {
    if (course.getOrgName() == null || course.getInstallationId() == null) {
      throw new NoLinkedOrganizationException(course.getCourseName());
    }
  }

  /**
   * Sets the editable fields of an assignment, checking the ones that depend on its type.
   *
   * @throws IllegalArgumentException if a field is blank, does not apply to the type of the
   *     assignment, or is not a valid regular expression
   */
  private static void applyFields(
      Assignment assignment,
      String repoPrefix,
      Visibility visibility,
      Permission permission,
      RepositoryCreationOption createReposFor,
      String teamRegex) {
    String normalizedPrefix = repoPrefix.strip();
    if (normalizedPrefix.isEmpty()) {
      throw new IllegalArgumentException("repoPrefix must not be blank");
    }
    boolean hasTeamRegex = teamRegex != null && !teamRegex.isBlank();

    if (assignment.getAsnType() == AssignmentType.TEAM) {
      if (createReposFor != null) {
        throw new IllegalArgumentException(
            "createReposFor is only allowed for INDIVIDUAL assignments");
      }
      String regex = hasTeamRegex ? teamRegex : DEFAULT_TEAM_REGEX;
      try {
        Pattern.compile(regex);
      } catch (PatternSyntaxException e) {
        throw new IllegalArgumentException("teamRegex is not a valid regular expression", e);
      }
      assignment.setCreateReposFor(null);
      assignment.setTeamRegex(regex);
    } else {
      if (hasTeamRegex) {
        throw new IllegalArgumentException("teamRegex is only allowed for TEAM assignments");
      }
      assignment.setCreateReposFor(
          createReposFor == null ? RepositoryCreationOption.STUDENTS_ONLY : createReposFor);
      assignment.setTeamRegex(null);
    }

    assignment.setRepoPrefix(normalizedPrefix);
    assignment.setVisibility(visibility);
    assignment.setPermission(permission);
  }

  /** Starts the job that creates the individual or team repositories of an assignment. */
  private Job launchJob(Course course, Assignment assignment) {
    boolean isPrivate = assignment.getVisibility() == Visibility.PRIVATE;
    RepositoryPermissions permissions =
        RepositoryPermissions.valueOf(assignment.getPermission().name());
    if (assignment.getAsnType() == AssignmentType.TEAM) {
      return jobService.runAsJob(
          CreateTeamRepositoriesJob.builder()
              .repositoryPrefix(assignment.getRepoPrefix())
              .isPrivate(isPrivate)
              .repositoryService(repositoryService)
              .githubTeamService(githubTeamService)
              .course(course)
              .permissions(permissions)
              .teamRegex(assignment.getTeamRegex())
              .build());
    }
    return jobService.runAsJob(
        CreateStudentOrStaffRepositoriesJob.builder()
            .repositoryPrefix(assignment.getRepoPrefix())
            .isPrivate(isPrivate)
            .repositoryService(repositoryService)
            .course(course)
            .permissions(permissions)
            .creationOption(assignment.getCreateReposFor())
            .build());
  }
}
