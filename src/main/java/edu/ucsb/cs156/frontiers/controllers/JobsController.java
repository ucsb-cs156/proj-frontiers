package edu.ucsb.cs156.frontiers.controllers;

import edu.ucsb.cs156.frontiers.errors.EntityNotFoundException;
import edu.ucsb.cs156.frontiers.jobs.AddTeamMemberToGithubJob;
import edu.ucsb.cs156.frontiers.jobs.AddTeamToGithubJob;
import edu.ucsb.cs156.frontiers.jobs.DeleteTeamFromGithubJob;
import edu.ucsb.cs156.frontiers.jobs.DeleteTeamMemberFromGithubJob;
import edu.ucsb.cs156.frontiers.jobs.MembershipAuditJob;
import edu.ucsb.cs156.frontiers.jobs.PullTeamsFromGithubJob;
import edu.ucsb.cs156.frontiers.jobs.PushTeamsToGithubJob;
import edu.ucsb.cs156.frontiers.jobs.UpdateAllJob;
import edu.ucsb.cs156.frontiers.models.JobLogTail;
import edu.ucsb.cs156.frontiers.repositories.CourseRepository;
import edu.ucsb.cs156.frontiers.repositories.CourseStaffRepository;
import edu.ucsb.cs156.frontiers.repositories.RosterStudentRepository;
import edu.ucsb.cs156.frontiers.repositories.TeamMemberRepository;
import edu.ucsb.cs156.frontiers.repositories.TeamRepository;
import edu.ucsb.cs156.frontiers.services.GithubTeamService;
import edu.ucsb.cs156.frontiers.services.OrganizationMemberService;
import edu.ucsb.cs156.frontiers.services.UpdateUserService;
import edu.ucsb.cs156.jobs.entities.Job;
import edu.ucsb.cs156.jobs.repositories.JobsRepository;
import edu.ucsb.cs156.jobs.services.JobService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * App-level job endpoints: launching this app's concrete jobs, plus the course-scoped jobs listing
 * and course-scoped full-log endpoint (which use this app's CourseSecurity rule). The generic admin
 * endpoints (list all / paginated / logs / delete) come from the lib-jobs library's own controller.
 */
@Tag(name = "Jobs")
@RequestMapping("/api/jobs")
@RestController
@Slf4j
public class JobsController extends ApiController {
  @Autowired private JobsRepository jobsRepository;

  @Autowired private JobService jobService;

  @Autowired private UpdateUserService updateUserService;

  @Autowired private RosterStudentRepository rosterStudentRepository;
  @Autowired private CourseRepository courseRepository;
  @Autowired private OrganizationMemberService organizationMemberService;
  @Autowired private CourseStaffRepository courseStaffRepository;
  @Autowired private TeamRepository teamRepository;
  @Autowired private TeamMemberRepository teamMemberRepository;
  @Autowired private GithubTeamService githubTeamService;

  @Operation(summary = "Launch UpdateAll job")
  @PreAuthorize("hasRole('ROLE_ADMIN')")
  @PostMapping("/launch/updateAll")
  public Job launchUpdateAllJob() {

    UpdateAllJob job = UpdateAllJob.builder().updateUserService(updateUserService).build();
    return jobService.runAsJob(job);
  }

  @Operation(summary = "Launch Audit All Courses Job")
  @PreAuthorize("hasRole('ROLE_ADMIN')")
  @PostMapping("/launch/auditAllCourses")
  public Job launchAuditAllCoursesJob() {

    MembershipAuditJob job =
        MembershipAuditJob.builder()
            .rosterStudentRepository(rosterStudentRepository)
            .courseRepository(courseRepository)
            .organizationMemberService(organizationMemberService)
            .courseStaffRepository(courseStaffRepository)
            .build();
    return jobService.runAsJob(job);
  }

  @Operation(summary = "Launch Push Teams to GitHub Job")
  @PreAuthorize("hasRole('ROLE_ADMIN')")
  @PostMapping("/launch/pushTeamsToGithub")
  public Job launchPushTeamsToGithubJob(@Parameter(name = "courseId") @RequestParam Long courseId) {

    PushTeamsToGithubJob job =
        PushTeamsToGithubJob.builder()
            .courseId(courseId)
            .courseRepository(courseRepository)
            .teamRepository(teamRepository)
            .teamMemberRepository(teamMemberRepository)
            .githubTeamService(githubTeamService)
            .build();
    return jobService.runAsJob(job);
  }

  @Operation(summary = "Launch Pull Teams from GitHub Job")
  @PreAuthorize("hasRole('ROLE_ADMIN')")
  @PostMapping("/launch/pullTeamsFromGithub")
  public Job launchPullTeamsFromGithubJob(
      @Parameter(name = "courseId") @RequestParam Long courseId) {

    PullTeamsFromGithubJob job =
        PullTeamsFromGithubJob.builder()
            .courseId(courseId)
            .courseRepository(courseRepository)
            .teamRepository(teamRepository)
            .teamMemberRepository(teamMemberRepository)
            .githubTeamService(githubTeamService)
            .build();
    return jobService.runAsJob(job);
  }

  @Operation(summary = "Launch Delete Team Member From GitHub Job")
  @PreAuthorize("@CourseSecurity.hasManagePermissions(#root, #courseId)")
  @PostMapping("/launch/deleteTeamMemberFromGithub")
  public Job launchDeleteTeamMemberFromGithubJob(
      @Parameter(name = "memberGithubLogin") @RequestParam String memberGithubLogin,
      @Parameter(name = "githubTeamId") @RequestParam Integer githubTeamId,
      @Parameter(name = "courseId") @RequestParam Long courseId) {

    DeleteTeamMemberFromGithubJob job =
        DeleteTeamMemberFromGithubJob.builder()
            .memberGithubLogin(memberGithubLogin)
            .githubTeamId(githubTeamId)
            .course(courseRepository.findById(courseId).get())
            .githubTeamService(githubTeamService)
            .build();
    return jobService.runAsJob(job);
  }

  @Operation(summary = "Launch Add Team Member To GitHub Job")
  @PreAuthorize("@CourseSecurity.hasManagePermissions(#root, #courseId)")
  @PostMapping("/launch/addTeamMemberToGithub")
  public Job launchAddTeamMemberToGithubJob(
      @Parameter(name = "memberGithubLogin") @RequestParam String memberGithubLogin,
      @Parameter(name = "githubTeamId") @RequestParam Integer githubTeamId,
      @Parameter(name = "teamMemberId") @RequestParam Long teamMemberId,
      @Parameter(name = "courseId") @RequestParam Long courseId) {

    AddTeamMemberToGithubJob job =
        AddTeamMemberToGithubJob.builder()
            .memberGithubLogin(memberGithubLogin)
            .githubTeamId(githubTeamId)
            .teamMemberId(teamMemberId)
            .course(courseRepository.findById(courseId).get())
            .githubTeamService(githubTeamService)
            .teamMemberRepository(teamMemberRepository)
            .build();
    return jobService.runAsJob(job);
  }

  @Operation(summary = "Launch Delete Team From GitHub Job")
  @PreAuthorize("@CourseSecurity.hasManagePermissions(#root, #courseId)")
  @PostMapping("/launch/deleteTeamFromGithub")
  public Job launchDeleteTeamFromGithubJob(
      @Parameter(name = "githubTeamId") @RequestParam Integer githubTeamId,
      @Parameter(name = "courseId") @RequestParam Long courseId) {

    DeleteTeamFromGithubJob job =
        DeleteTeamFromGithubJob.builder()
            .githubTeamId(githubTeamId)
            .course(courseRepository.findById(courseId).get())
            .githubTeamService(githubTeamService)
            .build();
    return jobService.runAsJob(job);
  }

  @Operation(summary = "Launch Add Team To GitHub Job")
  @PreAuthorize("@CourseSecurity.hasManagePermissions(#root, #courseId)")
  @PostMapping("/launch/addTeamToGithub")
  public Job launchAddTeamToGithubJob(
      @Parameter(name = "teamName") @RequestParam String teamName,
      @Parameter(name = "courseId") @RequestParam Long courseId) {

    AddTeamToGithubJob job =
        AddTeamToGithubJob.builder()
            .teamName(teamName)
            .course(courseRepository.findById(courseId).get())
            .teamRepository(teamRepository)
            .githubTeamService(githubTeamService)
            .build();
    return jobService.runAsJob(job);
  }

  @Operation(summary = "List jobs by courseId")
  @PreAuthorize("@CourseSecurity.hasManagePermissions(#root, #courseId)")
  @GetMapping("/course")
  public Iterable<Job> jobsByCourse(@Parameter(name = "courseId") @RequestParam Long courseId) {
    Iterable<Job> jobs = jobsRepository.findByScopeTypeAndScopeIdOrderByIdDesc("course", courseId);
    /*
     * Since lib-jobs v0.2.0, Job.log is @Transient (see job_logs) and is no
     * longer populated automatically by JPA; callers that return Job entities
     * directly must set it explicitly, same as the library's own controller
     * does for /all and /paginated.
     */
    jobs.forEach(job -> job.setLog(jobService.getJobLogPreview(job.getId())));
    return jobs;
  }

  /**
   * Returns the full log of one job that belongs to the given course.
   *
   * <p>Why this app-owned endpoint exists: the library's own {@code GET /api/jobs/logs/{id}}
   * returns the full log but is restricted to {@code ROLE_ADMIN}. The per-course jobs tab is used
   * by course instructors and staff who are not necessarily admins, and the course-scoped listing
   * above only populates {@code Job.log} with a short tail preview ({@code
   * JobService.getJobLogPreview}). This endpoint gives those users the same full log, guarded by
   * the same course-permission rule as the listing.
   *
   * <p>A job that exists but is not scoped to the given course is deliberately reported as not
   * found (404) rather than forbidden (403), so that job ids do not leak across courses.
   *
   * @param courseId the id of the course the job must belong to
   * @param jobId the id of the job
   * @return the full job log, oldest line first
   */
  @Operation(summary = "Get the full log of one job belonging to a course")
  @PreAuthorize("@CourseSecurity.hasManagePermissions(#root, #courseId)")
  @GetMapping("/course/logs")
  public String jobLogsByCourse(
      @Parameter(name = "courseId") @RequestParam Long courseId,
      @Parameter(name = "jobId") @RequestParam Long jobId) {
    findJobInCourse(courseId, jobId);
    return jobService.getJobLogs(jobId);
  }

  /**
   * Returns the log lines of one job that belong to the given course that were written after the
   * line with id {@code afterId}, together with the job's status: one poll of a "tail -f" style
   * view. The client keeps the id of the last line it has received and passes it as {@code afterId}
   * on its next poll, and stops polling once the status is one of a job that has finished.
   *
   * <p>This is the course-scoped counterpart of the library's admin-only {@code GET
   * /api/jobs/logs/{id}/tail}, for the same reason as {@link #jobLogsByCourse}. Like that endpoint
   * it reports a job of another course as not found.
   *
   * <p>The status is read <em>before</em> the lines. If the job has already finished when its
   * status is read, all of its lines have been written by the time they are read, so a client that
   * stops polling on a finished status has not missed any. Reading them the other way round could
   * miss the last lines of a job that finishes in between.
   *
   * @param courseId the id of the course the job must belong to
   * @param jobId the id of the job
   * @param afterId only lines with an id greater than this are returned; 0 for the whole log
   * @return the status of the job and its new log lines, oldest first
   */
  @Operation(summary = "Get the new log lines and the status of one job belonging to a course")
  @PreAuthorize("@CourseSecurity.hasManagePermissions(#root, #courseId)")
  @GetMapping("/course/logs/tail")
  public JobLogTail jobLogTailByCourse(
      @Parameter(name = "courseId") @RequestParam Long courseId,
      @Parameter(name = "jobId") @RequestParam Long jobId,
      @Parameter(name = "afterId", description = "only return lines with id greater than this")
          @RequestParam(defaultValue = "0")
          Long afterId) {
    String status = findJobInCourse(courseId, jobId).getStatus();
    return new JobLogTail(status, jobService.getJobLogTail(jobId, afterId));
  }

  /**
   * Finds a job, which must be a course-scoped job of the given course.
   *
   * @throws EntityNotFoundException if there is no such job, or it belongs to something else
   */
  private Job findJobInCourse(Long courseId, Long jobId) {
    Job job =
        jobsRepository
            .findById(jobId)
            .orElseThrow(() -> new EntityNotFoundException(Job.class, jobId));
    if (!"course".equals(job.getScopeType()) || !courseId.equals(job.getScopeId())) {
      throw new EntityNotFoundException(Job.class, jobId);
    }
    return job;
  }
}
