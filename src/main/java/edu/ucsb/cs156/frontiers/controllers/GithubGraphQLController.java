package edu.ucsb.cs156.frontiers.controllers;

import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.entities.Job;
import edu.ucsb.cs156.frontiers.errors.EntityNotFoundException;
import edu.ucsb.cs156.frontiers.jobs.ReturnCsvCommitHistoryJob;
import edu.ucsb.cs156.frontiers.redis.CommitCsvResult;
import edu.ucsb.cs156.frontiers.redis.JobResult;
import edu.ucsb.cs156.frontiers.redis.JobResultRepository;
import edu.ucsb.cs156.frontiers.repositories.CourseRepository;
import edu.ucsb.cs156.frontiers.services.GithubGraphQLService;
import edu.ucsb.cs156.frontiers.services.jobs.JobService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "GithubGraphQL")
@RequestMapping("/api/github/graphql/")
@RestController
@Slf4j
public class GithubGraphQLController extends ApiController {

  private final GithubGraphQLService githubGraphQLService;
  private final CourseRepository courseRepository;
  private final JobService jobService;
  private final JobResultRepository jobResultRepository;

  public GithubGraphQLController(
      @Autowired GithubGraphQLService gitHubGraphQLService,
      @Autowired CourseRepository courseRepository,
      JobService jobService,
      JobResultRepository jobResultRepository) {
    this.githubGraphQLService = gitHubGraphQLService;
    this.courseRepository = courseRepository;
    this.jobService = jobService;
    this.jobResultRepository = jobResultRepository;
  }

  /**
   * Return default branch name for a given repository.
   *
   * @param courseId the id of the course whose installation is being used for credentials
   * @param owner the owner of the repository
   * @param repo the name of the repository
   * @return the default branch name
   */
  @Operation(summary = "Get default branch name")
  @PreAuthorize("@CourseSecurity.hasManagePermissions(#root, #courseId)")
  @GetMapping("defaultBranchName")
  public String getDefaultBranchName(
      @Parameter Long courseId, @Parameter String owner, @Parameter String repo) throws Exception {
    log.info(
        "getDefaultBranchName called with courseId: {}, owner: {}, repo: {}",
        courseId,
        owner,
        repo);
    Course course =
        courseRepository
            .findById(courseId)
            .orElseThrow(() -> new EntityNotFoundException(Course.class, courseId));

    log.info("Found course: {}", course);

    log.info("Current user is authorized to access course: {}", course.getId());

    String result = this.githubGraphQLService.getDefaultBranchName(course, owner, repo);

    log.info("Result from getDefaultBranchName: {}", result);

    return result;
  }

  /**
   * Return default branch name for a given repository.
   *
   * @param courseId the id of the course whose installation is being used for credentails
   * @param owner the owner of the repository
   * @param repo the name of the repository
   * @return the default branch name
   */
  @Operation(summary = "Get commits")
  @PreAuthorize("@CourseSecurity.hasManagePermissions(#root, #courseId)")
  @GetMapping("commits")
  public String getCommits(
      @Parameter Long courseId,
      @Parameter String owner,
      @Parameter String repo,
      @Parameter String branch,
      @Parameter Integer first,
      @RequestParam(name = "after", required = false) @Parameter String after)
      throws Exception {
    log.info(
        "getCommits called with courseId: {}, owner: {}, repo: {}, branch: {}, first: {}, after: {} ",
        courseId,
        owner,
        repo,
        branch,
        first,
        after);
    Course course =
        courseRepository
            .findById(courseId)
            .orElseThrow(() -> new EntityNotFoundException(Course.class, courseId));

    log.info("Found course: {}", course);

    log.info("Current user is authorized to access course: {}", course.getId());

    String result = this.githubGraphQLService.getCommits(course, owner, repo, branch, first, after);

    log.info("Result from getCommits: {}", result);

    return result;
  }

  /**
   * Return default branch name for a given repository.
   *
   * @param courseId the id of the course whose installation is being used for credentails
   * @param owner the owner of the repository
   * @param repo the name of the repository
   * @return the default branch name
   */
  @Operation(summary = "Get commits")
  @PreAuthorize("@CourseSecurity.hasManagePermissions(#root, #courseId)")
  @PostMapping("csvCommits")
  public Job commitCsv(
      @Parameter Long courseId,
      @Parameter String owner,
      @Parameter String repo,
      @Parameter String branch,
      @Parameter Integer count)
      throws Exception {
    log.info(
        "commitCsv called with courseId: {}, owner: {}, repo: {}, branch: {}, first: {}",
        courseId,
        owner,
        repo,
        branch,
        count);
    Course course =
        courseRepository
            .findById(courseId)
            .orElseThrow(() -> new EntityNotFoundException(Course.class, courseId));

    ReturnCsvCommitHistoryJob job =
        ReturnCsvCommitHistoryJob.builder()
            .course(course)
            .owner(owner)
            .repo(repo)
            .branch(branch)
            .count(count)
            .githubService(githubGraphQLService)
            .jobResultRepository(jobResultRepository)
            .build();
    return jobService.runAsJob(job);
  }

  @GetMapping("csvResult/{jobId}")
  public ResponseEntity<byte[]> returnCsv(@PathVariable Long jobId) throws Exception {
    CommitCsvResult result =
        jobService
            .returnTypedResultById(jobId, CommitCsvResult.class)
            .orElseThrow(() -> new EntityNotFoundException(JobResult.class, jobId));
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
    headers.setContentDispositionFormData("attachment", "commits.csv");
    return ResponseEntity.ok().headers(headers).body(result.getJobData().getCsvData());
  }
}
