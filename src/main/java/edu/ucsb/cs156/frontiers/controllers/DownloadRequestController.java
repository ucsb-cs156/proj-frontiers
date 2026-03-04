package edu.ucsb.cs156.frontiers.controllers;

import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.entities.DownloadRequest;
import edu.ucsb.cs156.frontiers.entities.DownloadedCommit;
import edu.ucsb.cs156.frontiers.entities.Job;
import edu.ucsb.cs156.frontiers.enums.DownloadRequestType;
import edu.ucsb.cs156.frontiers.errors.EntityNotFoundException;
import edu.ucsb.cs156.frontiers.jobs.CommitDownloadRequestJob;
import edu.ucsb.cs156.frontiers.repositories.CourseRepository;
import edu.ucsb.cs156.frontiers.repositories.DownloadRequestRepository;
import edu.ucsb.cs156.frontiers.repositories.DownloadedCommitRepository;
import edu.ucsb.cs156.frontiers.services.GithubGraphQLService;
import edu.ucsb.cs156.frontiers.services.jobs.JobService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.time.Instant;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/downloads")
@Tag(name = "Download Management")
@Slf4j
public class DownloadRequestController extends ApiController {

  private final DownloadedCommitRepository downloadedCommitRepository;
  private final DownloadRequestRepository downloadRequestRepository;
  private final CourseRepository courseRepository;
  private final JobService jobService;
  private final GithubGraphQLService githubGraphQLService;

  public DownloadRequestController(
      DownloadedCommitRepository downloadedCommitRepository,
      DownloadRequestRepository downloadRequestRepository,
      CourseRepository courseRepository,
      JobService jobService,
      GithubGraphQLService githubGraphQLService) {
    this.downloadedCommitRepository = downloadedCommitRepository;
    this.downloadRequestRepository = downloadRequestRepository;
    this.courseRepository = courseRepository;
    this.jobService = jobService;
    this.githubGraphQLService = githubGraphQLService;
  }

  @PostMapping("/create")
  @PreAuthorize("@CourseSecurity.hasManagePermissions(#root, #courseId)")
  public ResponseEntity<DownloadRequest> createDownloadRequest(
      @Parameter Long courseId,
      @RequestParam String org,
      @RequestParam String repo,
      @RequestParam(required = false) String branch,
      @RequestParam DownloadRequestType type,
      @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) Instant startDate,
      @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) Instant endDate) {

    Course course =
        courseRepository
            .findById(courseId)
            .orElseThrow(() -> new EntityNotFoundException(Course.class, courseId));
    DownloadRequest request =
        DownloadRequest.builder()
            .course(course)
            .org(org)
            .repo(repo)
            .downloadType(type)
            .startDate(startDate)
            .endDate(endDate)
            .build();

    if (branch != null) {
      request.setBranch(branch);
    }

    request = downloadRequestRepository.save(request);

    CommitDownloadRequestJob job =
        CommitDownloadRequestJob.builder()
            .request(request)
            .githubService(githubGraphQLService)
            .build();

    Job startedJob = jobService.runAsJob(job);

    request.setJob(startedJob);

    return ResponseEntity.status(HttpStatus.ACCEPTED).body(downloadRequestRepository.save(request));
  }

  @PostMapping("{downloadRequestId}")
  @PreAuthorize("@CourseSecurity.hasDownloadPermissions(#root, #downloadRequestId)")
  public ResponseEntity<Object> getDownload(@PathVariable Long downloadRequestId) throws Exception {
    DownloadRequest request =
        downloadRequestRepository
            .findById(downloadRequestId)
            .orElseThrow(
                () -> new EntityNotFoundException(DownloadRequest.class, downloadRequestId));

    if (request.getJob().getStatus().equals("error")) {
      Map<String, String> errorResponse =
          Map.of(
              "message",
              "Download request failed with error; please check the job log for more information.");
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    if (!request.getJob().getStatus().equals("complete")) {
      return ResponseEntity.status(HttpStatus.ACCEPTED)
          .body(Map.of("message", "Download request is not complete. Please try again later."));
    }

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try (OutputStreamWriter writer = new OutputStreamWriter(baos)) {
      StatefulBeanToCsv<DownloadedCommit> csvWriter =
          new StatefulBeanToCsvBuilder<DownloadedCommit>(writer).build();
      csvWriter.write(downloadedCommitRepository.findByRequest(request).iterator());
    }

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.parseMediaType("text/csv"));
    headers.setContentDisposition(
        ContentDisposition.builder("attachment")
            .filename(request.getOrg() + "_" + request.getRepo() + ".csv")
            .build());

    return ResponseEntity.ok().headers(headers).body(new ByteArrayResource(baos.toByteArray()));
  }
}
