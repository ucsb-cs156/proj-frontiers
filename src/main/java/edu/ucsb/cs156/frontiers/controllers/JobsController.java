package edu.ucsb.cs156.frontiers.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import edu.ucsb.cs156.frontiers.entities.Job;
import edu.ucsb.cs156.frontiers.errors.EntityNotFoundException;
import edu.ucsb.cs156.frontiers.jobs.MembershipAuditJob;
import edu.ucsb.cs156.frontiers.jobs.UpdateAllJob;
import edu.ucsb.cs156.frontiers.repositories.CourseRepository;
import edu.ucsb.cs156.frontiers.repositories.JobsRepository;
import edu.ucsb.cs156.frontiers.repositories.RosterStudentRepository;
import edu.ucsb.cs156.frontiers.services.OrganizationMemberService;
import edu.ucsb.cs156.frontiers.services.UpdateUserService;
import edu.ucsb.cs156.frontiers.services.jobs.JobService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Jobs")
@RequestMapping("/api/jobs")
@RestController
@Slf4j
public class JobsController extends ApiController {
  @Autowired private JobsRepository jobsRepository;

  @Autowired private JobService jobService;

  @Autowired private UpdateUserService updateUserService; 
  
  @Autowired ObjectMapper mapper;
    @Autowired
    private RosterStudentRepository rosterStudentRepository;
    @Autowired
    private CourseRepository courseRepository;
    @Autowired
    private OrganizationMemberService organizationMemberService;

  @Operation(summary = "List all jobs")
  @PreAuthorize("hasRole('ROLE_ADMIN')")
  @GetMapping("/all")
  public Iterable<Job> allJobs() {
    Iterable<Job> jobs = jobsRepository.findAll();
    return jobs;
  }

  @Operation(summary = "Delete all job records")
  @PreAuthorize("hasRole('ROLE_ADMIN')")
  @DeleteMapping("/all")
  public Map<String, String> deleteAllJobs() {
    jobsRepository.deleteAll();
    return Map.of("message", "All jobs deleted");
  }

  @Operation(summary = "Get a specific Job Log by ID if it is in the database")
  @PreAuthorize("hasRole('ROLE_ADMIN')")
  @GetMapping("")
  public Job getJobLogById(
      @Parameter(name = "id", description = "ID of the job") @RequestParam Long id)
      throws JsonProcessingException {

    Job job =
        jobsRepository.findById(id).orElseThrow(() -> new EntityNotFoundException(Job.class, id));

    return job;
  }

  @Operation(summary = "Delete specific job record")
  @PreAuthorize("hasRole('ROLE_ADMIN')")
  @DeleteMapping("")
  public Map<String, String> deleteAllJobs(@Parameter(name = "id") @RequestParam Long id) {
    if (!jobsRepository.existsById(id)) {
      return Map.of("message", String.format("Job with id %d not found", id));
    }
    jobsRepository.deleteById(id);
    return Map.of("message", String.format("Job with id %d deleted", id));
  }



  @Operation(summary = "Get long job logs")
  @PreAuthorize("hasRole('ROLE_ADMIN')")
  @GetMapping("/logs/{id}")
  public String getJobLogs(@Parameter(name = "id", description = "Job ID") @PathVariable Long id) {

    return jobService.getJobLogs(id);
  }

  @Operation(summary = "Launch UpdateAll job")
  @PreAuthorize("hasRole('ROLE_ADMIN')")
  @PostMapping("/launch/updateAll")
  public Job launchUpdateAllJob() {

    UpdateAllJob job = 
        UpdateAllJob.builder()
            .updateUserService(updateUserService) 
            .build();
    return jobService.runAsJob(job);
  }

  @Operation(summary = "Launch Audit All Courses Job")
  @PreAuthorize("hasRole('ROLE_ADMIN')")
  @PostMapping("/launch/auditAllCourses")
  public Job launchAuditAllCoursesJob() {

    MembershipAuditJob job = MembershipAuditJob.builder()
            .rosterStudentRepository(rosterStudentRepository)
            .courseRepository(courseRepository)
            .organizationMemberService(organizationMemberService)
            .build();
    return jobService.runAsJob(job);
  }
}
