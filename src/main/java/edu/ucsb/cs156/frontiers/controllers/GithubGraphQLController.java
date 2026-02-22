package edu.ucsb.cs156.frontiers.controllers;

import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.exceptions.CsvFieldAssignmentException;
import edu.ucsb.cs156.frontiers.entities.Branch;
import edu.ucsb.cs156.frontiers.entities.BranchId;
import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.entities.Job;
import edu.ucsb.cs156.frontiers.errors.EntityNotFoundException;
import edu.ucsb.cs156.frontiers.jobs.LoadCommitHistoryJob;
import edu.ucsb.cs156.frontiers.models.CommitDto;
import edu.ucsb.cs156.frontiers.repositories.BranchRepository;
import edu.ucsb.cs156.frontiers.repositories.CommitRepository;
import edu.ucsb.cs156.frontiers.repositories.CourseRepository;
import edu.ucsb.cs156.frontiers.services.GithubGraphQLService;
import edu.ucsb.cs156.frontiers.services.jobs.JobService;
import edu.ucsb.cs156.frontiers.utilities.StatefulBeanToCsvBuilderFactory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.OutputStreamWriter;
import java.time.Instant;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@Tag(name = "GithubGraphQL")
@RequestMapping("/api/github/graphql/")
@RestController
@Slf4j
public class GithubGraphQLController extends ApiController {

  private final GithubGraphQLService githubGraphQLService;
  private final CourseRepository courseRepository;
  private final JobService jobService;
  private final CommitRepository commitRepository;
  private final StatefulBeanToCsvBuilderFactory statefulBeanToCsvBuilderFactory;
  private final BranchRepository branchRepository;

  public GithubGraphQLController(
      @Autowired GithubGraphQLService gitHubGraphQLService,
      @Autowired CourseRepository courseRepository,
      JobService jobService,
      CommitRepository commitRepository,
      StatefulBeanToCsvBuilderFactory statefulBeanToCsvBuilderFactory,
      BranchRepository branchRepository) {
    this.githubGraphQLService = gitHubGraphQLService;
    this.courseRepository = courseRepository;
    this.jobService = jobService;
    this.statefulBeanToCsvBuilderFactory = statefulBeanToCsvBuilderFactory;
    this.commitRepository = commitRepository;
    this.branchRepository = branchRepository;
  }

  /**
   * Return default branch name for a given repository.
   *
   * @param courseId the id of the course whose installation is being used for credentails
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
   * Returns a job to load the commit data of a number of branches
   *
   * @param courseId the id of the course whose installation is being used for credentials
   * @param branches the list of branches to load
   * @return the job identifier
   */
  @Operation(summary = "Get commits", description = "Loads commit history for the given branches")
  @PreAuthorize("@CourseSecurity.hasManagePermissions(#root, #courseId)")
  @PostMapping("history")
  public Job loadCommitHistory(@Parameter Long courseId, @RequestBody List<BranchId> branches) {
    log.debug(
        "Commit History loader called with courseId {} for the following branches: {}",
        courseId,
        branches);
    Course course =
        courseRepository
            .findById(courseId)
            .orElseThrow(() -> new EntityNotFoundException(Course.class, courseId));

    if (branches.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No branches specified");
    }

    LoadCommitHistoryJob job =
        LoadCommitHistoryJob.builder()
            .course(course)
            .branches(branches)
            .githubService(githubGraphQLService)
            .build();
    return jobService.runAsJob(job);
  }

  @Operation(
      summary = "Get commits as a CSV",
      description = "Returns preloaded commit history for the given branches as a CSV")
  @PreAuthorize("@CourseSecurity.hasManagePermissions(#root, #courseId)")
  @PostMapping(value = "csv", produces = "text/csv")
  public ResponseEntity<StreamingResponseBody> getCommitsCsv(
      @Parameter Long courseId,
      @Parameter Instant start,
      @Parameter Instant end,
      @Parameter Boolean skipMergeCommits,
      @RequestBody List<BranchId> branches)
      throws Exception {

    List<Branch> selectedBranches = branchRepository.findByIdIn(branches);

    if (branches.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No branches specified");
    }

    if (start.isAfter(end)) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Start time must be before end time.");
    }

    if (selectedBranches.size() != branches.size()) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "One or more branches not found; Please load commit history for those branches first.");
    }

    if (selectedBranches.stream().anyMatch(branch -> branch.getRetrievedTime().isBefore(end))) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "One or more branches have not been updated since the requested end time; Please load commit history for those branches first.");
    }

    StreamingResponseBody stream =
        (outputStream) -> {
          try (var writer = new OutputStreamWriter(outputStream)) {
            StatefulBeanToCsv<CommitDto> csvWriter = statefulBeanToCsvBuilderFactory.build(writer);
            List<CommitDto> commits =
                commitRepository.findByBranchIdInAndCommitTimeBetweenAndIsMergeCommitEquals(
                    branches, start, end, !skipMergeCommits);
            try {
              csvWriter.write(commits);
            } catch (CsvFieldAssignmentException ignored) {
              writer.write("Error writing CSV file");
            }
          }
        };

    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=commit_history.csv")
        .header(HttpHeaders.CONTENT_TYPE, "text/csv; charset=UTF-8")
        .header(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, HttpHeaders.CONTENT_DISPOSITION)
        .body(stream);
  }
}
