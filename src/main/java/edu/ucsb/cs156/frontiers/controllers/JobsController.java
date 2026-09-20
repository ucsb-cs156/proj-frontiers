package edu.ucsb.cs156.frontiers.controllers;

import edu.ucsb.cs156.frontiers.jobs.AddTeamMemberToGithubJob;
import edu.ucsb.cs156.frontiers.jobs.AddTeamToGithubJob;
import edu.ucsb.cs156.frontiers.jobs.DeleteTeamFromGithubJob;
import edu.ucsb.cs156.frontiers.jobs.DeleteTeamMemberFromGithubJob;
import edu.ucsb.cs156.frontiers.jobs.MembershipAuditJob;
import edu.ucsb.cs156.frontiers.jobs.PullTeamsFromGithubJob;
import edu.ucsb.cs156.frontiers.jobs.PushTeamsToGithubJob;
import edu.ucsb.cs156.frontiers.jobs.UpdateAllJob;
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
 * (which uses this app's CourseSecurity rule). The generic admin endpoints (list all / paginated /
 * logs / delete) come from the lib-jobs library's own controller.
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
}
