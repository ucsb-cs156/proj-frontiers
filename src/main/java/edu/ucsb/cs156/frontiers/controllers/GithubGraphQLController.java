package edu.ucsb.cs156.frontiers.controllers;

import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.entities.Job;
import edu.ucsb.cs156.frontiers.errors.EntityNotFoundException;
import edu.ucsb.cs156.frontiers.jobs.ReturnCsvCommitHistoryJob;
import edu.ucsb.cs156.frontiers.models.Branch;
import edu.ucsb.cs156.frontiers.mongo.documents.AggregatedCommit;
import edu.ucsb.cs156.frontiers.mongo.documents.Commit;
import edu.ucsb.cs156.frontiers.mongo.documents.CommitHistory;
import edu.ucsb.cs156.frontiers.mongo.documents.TemporaryCommitCollection;
import edu.ucsb.cs156.frontiers.mongo.repositories.AggregatedCommitRepository;
import edu.ucsb.cs156.frontiers.mongo.repositories.CommitRepository;
import edu.ucsb.cs156.frontiers.mongo.repositories.TemporaryCommitCollectionRepository;
import edu.ucsb.cs156.frontiers.repositories.CourseRepository;
import edu.ucsb.cs156.frontiers.services.GithubGraphQLService;
import edu.ucsb.cs156.frontiers.services.jobs.JobService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
  private final CommitRepository commitRepository;
  private final TemporaryCommitCollectionRepository temporaryCommitCollectionRepository;
  private final AggregatedCommitRepository aggregatedCommitRepository;

  public GithubGraphQLController(
      @Autowired GithubGraphQLService gitHubGraphQLService,
      @Autowired CourseRepository courseRepository,
      JobService jobService,
      CommitRepository commitRepository,
      TemporaryCommitCollectionRepository temporaryCommitCollectionRepository,
      AggregatedCommitRepository aggregatedCommitRepository) {
    this.githubGraphQLService = gitHubGraphQLService;
    this.courseRepository = courseRepository;
    this.jobService = jobService;
    this.commitRepository = commitRepository;
    this.temporaryCommitCollectionRepository = temporaryCommitCollectionRepository;
    this.aggregatedCommitRepository = aggregatedCommitRepository;
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
   * Return default branch name for a given repository.
   *
   * @param courseId the id of the course whose installation is being used for credentails
   * @param owner the owner of the repository
   * @param repo the name of the repository
   * @return the default branch name
   */
  @Operation(summary = "Get commits")
  @PreAuthorize("@CourseSecurity.hasManagePermissions(#root, #courseId)")
  @GetMapping("csvCommits")
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
            .build();
    return jobService.runAsJob(job);
  }

  @GetMapping("/pagedCommits")
  @PreAuthorize("@CourseSecurity.hasManagePermissions(#root, #courseId)")
  public Page<Commit> returnCommits(
      @Parameter Long courseId,
      @Parameter String owner,
      @Parameter String repo,
      @Parameter String branch,
      Pageable pageable)
      throws Exception {
    Course course =
        courseRepository
            .findById(courseId)
            .orElseThrow(() -> new EntityNotFoundException(Course.class, courseId));
    CommitHistory returnedBranch =
        githubGraphQLService.updateCommitHistory(course, owner, repo, branch);
    Page<Commit> returnedCommits =
        commitRepository.findByParentBranch(returnedBranch.getId(), pageable);
    return returnedCommits;
  }

  @PostMapping("/create_commit_session")
  @PreAuthorize("@CourseSecurity.hasManagePermissions(#root, #courseId)")
  public TemporaryCommitCollection staticTest(
      @Parameter Long courseId, @RequestBody List<Branch> branches) {
    Course course =
        courseRepository
            .findById(courseId)
            .orElseThrow(() -> new EntityNotFoundException(Course.class, courseId));
    TemporaryCommitCollection starter =
        TemporaryCommitCollection.builder().user(getCurrentUser().getUser().getId()).build();
    return temporaryCommitCollectionRepository.createSession(starter, branches);
  }

  @GetMapping("/aggregated_commits")
  @PreAuthorize("hasRole('ROLE_ADMIN')")
  public Page<AggregatedCommit> aggregatedCommits(
      @Parameter Pageable pageable, @Parameter String sessionId) {
    TemporaryCommitCollection session =
        temporaryCommitCollectionRepository.findBySessionId(sessionId);
    return aggregatedCommitRepository.findAllBySessionId(session.getId(), pageable);
  }
}
