package edu.ucsb.cs156.frontiers.controllers;

import static edu.ucsb.cs156.frontiers.controllers.RosterStudentsController.upsertStudent;

import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.entities.Job;
import edu.ucsb.cs156.frontiers.entities.RosterStudent;
import edu.ucsb.cs156.frontiers.enums.InsertStatus;
import edu.ucsb.cs156.frontiers.enums.RosterStatus;
import edu.ucsb.cs156.frontiers.errors.EntityNotFoundException;
import edu.ucsb.cs156.frontiers.jobs.PullTeamsFromCanvasJob;
import edu.ucsb.cs156.frontiers.jobs.RemoveStudentsJob;
import edu.ucsb.cs156.frontiers.models.CanvasGroupSet;
import edu.ucsb.cs156.frontiers.models.LoadResult;
import edu.ucsb.cs156.frontiers.models.UpsertResponse;
import edu.ucsb.cs156.frontiers.repositories.CourseRepository;
import edu.ucsb.cs156.frontiers.repositories.RosterStudentRepository;
import edu.ucsb.cs156.frontiers.repositories.TeamMemberRepository;
import edu.ucsb.cs156.frontiers.repositories.TeamRepository;
import edu.ucsb.cs156.frontiers.services.CanvasService;
import edu.ucsb.cs156.frontiers.services.OrganizationMemberService;
import edu.ucsb.cs156.frontiers.services.UpdateUserService;
import edu.ucsb.cs156.frontiers.services.jobs.JobService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/courses/canvas")
@Tag(name = "Canvas")
@Slf4j
public class CanvasController extends ApiController {

  private final CourseRepository courseRepository;
  private final CanvasService canvasService;
  private final TeamRepository teamRepository;
  private final JobService jobService;
  private final RosterStudentRepository rosterStudentRepository;
  private final UpdateUserService updateUserService;
  private final OrganizationMemberService organizationMemberService;
  private final TeamMemberRepository teamMemberRepository;

  public CanvasController(
      CourseRepository courseRepository,
      CanvasService canvasService,
      TeamRepository teamRepository,
      JobService jobService,
      RosterStudentRepository rosterStudentRepository,
      UpdateUserService updateUserService,
      OrganizationMemberService organizationMemberService,
      TeamMemberRepository teamMemberRepository) {
    this.courseRepository = courseRepository;
    this.canvasService = canvasService;
    this.teamRepository = teamRepository;
    this.jobService = jobService;
    this.rosterStudentRepository = rosterStudentRepository;
    this.updateUserService = updateUserService;
    this.organizationMemberService = organizationMemberService;
    this.teamMemberRepository = teamMemberRepository;
  }

  /**
   * Upload Roster students for Course from Canvas. It is important to keep the code in this method
   * consistent with the code in uploadRosterStudentsCSV.
   *
   * @param courseId the internal course ID in Frontiers
   * @return LoadResult with counts of inserted, updated, dropped students and any rejected students
   */
  @Operation(summary = "Upload Roster students for Course from Canvas")
  @PreAuthorize("@CourseSecurity.hasInstructorPermissions(#root, #courseId)")
  @PostMapping("/sync/students")
  public ResponseEntity<LoadResult> uploadRosterFromCanvas(
      @Parameter(name = "courseId") @RequestParam Long courseId) {

    Course course =
        courseRepository
            .findById(courseId)
            .orElseThrow(() -> new EntityNotFoundException(Course.class, courseId.toString()));

    course.getRosterStudents().stream()
        .filter(filteredStudent -> filteredStudent.getRosterStatus() == RosterStatus.ROSTER)
        .forEach(student -> student.setRosterStatus(RosterStatus.DROPPED));

    int counts[] = {0, 0};
    List<RosterStudent> rejectedStudents = new ArrayList<>();

    List<RosterStudent> canvasStudents = canvasService.getCanvasRoster(course);
    for (RosterStudent rosterStudent : canvasStudents) {
      UpsertResponse upsertResponse = upsertStudent(rosterStudent, course, RosterStatus.ROSTER);
      if (upsertResponse.getInsertStatus() == InsertStatus.REJECTED) {
        rejectedStudents.add(upsertResponse.rosterStudent());
      } else {
        InsertStatus s = upsertResponse.getInsertStatus();
        if (s == InsertStatus.INSERTED) {
          course.getRosterStudents().add(upsertResponse.rosterStudent());
        }
        counts[s.ordinal()]++;
      }
    }

    if (rejectedStudents.isEmpty()) {
      List<RosterStudent> droppedStudents =
          course.getRosterStudents().stream()
              .filter(student -> student.getRosterStatus() == RosterStatus.DROPPED)
              .toList();
      LoadResult successfulResult =
          new LoadResult(
              counts[InsertStatus.INSERTED.ordinal()],
              counts[InsertStatus.UPDATED.ordinal()],
              droppedStudents.size(),
              List.of());
      rosterStudentRepository.saveAll(course.getRosterStudents());
      updateUserService.attachUsersToRosterStudents(course.getRosterStudents());
      RemoveStudentsJob job =
          RemoveStudentsJob.builder()
              .students(droppedStudents)
              .organizationMemberService(organizationMemberService)
              .rosterStudentRepository(rosterStudentRepository)
              .build();
      jobService.runAsJob(job);
      return ResponseEntity.ok(successfulResult);
    } else {
      LoadResult conflictResult = new LoadResult(0, 0, 0, rejectedStudents);
      return ResponseEntity.status(HttpStatus.CONFLICT).body(conflictResult);
    }
  }

  @Operation(summary = "See available Canvas GroupSets")
  @GetMapping("/groupsets")
  @PreAuthorize("@CourseSecurity.hasInstructorPermissions(#root, #courseId)")
  public List<CanvasGroupSet> getCanvasGroupSets(
      @Parameter(name = "courseId") @RequestParam Long courseId) {
    Course course =
        courseRepository
            .findById(courseId)
            .orElseThrow(() -> new EntityNotFoundException(Course.class, courseId.toString()));
    return canvasService.getCanvasGroupSets(course);
  }

  @Operation(summary = "Load Groups from Canvas")
  @PostMapping("/sync/teams")
  @PreAuthorize("@CourseSecurity.hasInstructorPermissions(#root, #courseId)")
  public Job loadCanvasTeams(
      @Parameter(name = "courseId") @RequestParam Long courseId,
      @Parameter(name = "groupSetId") @RequestParam String groupSetId) {
    Course course =
        courseRepository
            .findById(courseId)
            .orElseThrow(() -> new EntityNotFoundException(Course.class, courseId.toString()));
    PullTeamsFromCanvasJob job =
        PullTeamsFromCanvasJob.builder()
            .course(course)
            .canvasService(canvasService)
            .teamRepository(teamRepository)
            .teamMemberRepository(teamMemberRepository)
            .groupsetId(groupSetId)
            .build();
    return jobService.runAsJob(job);
  }
}
