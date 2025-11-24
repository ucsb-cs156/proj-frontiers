package edu.ucsb.cs156.frontiers.controllers;

import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.entities.Job;
import edu.ucsb.cs156.frontiers.enums.RepositoryCreationTarget;
import edu.ucsb.cs156.frontiers.enums.RepositoryPermissions;
import edu.ucsb.cs156.frontiers.errors.EntityNotFoundException;
import edu.ucsb.cs156.frontiers.errors.NoLinkedOrganizationException;
import edu.ucsb.cs156.frontiers.jobs.CreateStudentRepositoriesJob;
import edu.ucsb.cs156.frontiers.repositories.CourseRepository;
import edu.ucsb.cs156.frontiers.services.RepositoryService;
import edu.ucsb.cs156.frontiers.services.jobs.JobService;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Repository Controller")
@RestController
@RequestMapping("/api/repos")
public class RepositoryController extends ApiController {

  @Autowired RepositoryService repositoryService;

  @Autowired JobService jobService;

  @Autowired CourseRepository courseRepository;

  /**
   * Fires a job that creates a repo for students and/or staff members with GitHub accounts.
   *
   * @param courseId ID of course to create repos for
   * @param repoPrefix each repo created will begin with this prefix, followed by a dash and the
   *     GitHub username
   * @param isPrivate determines whether the repository being created is private
   * @param permissions the permissions level to grant to the repository collaborators
   * @param creationTarget determines who gets repositories: STUDENTS_ONLY (default), STAFF_ONLY, or
   *     STUDENTS_AND_STAFF
   * @return the {@link edu.ucsb.cs156.frontiers.entities.Job Job} started to create the repos.
   */
  @PostMapping("/createRepos")
  @PreAuthorize("@CourseSecurity.hasManagePermissions(#root, #courseId)")
  public Job createRepos(
      @RequestParam Long courseId,
      @RequestParam String repoPrefix,
      @RequestParam Optional<Boolean> isPrivate,
      @RequestParam RepositoryPermissions permissions,
      @RequestParam Optional<RepositoryCreationTarget> creationTarget) {
    Course course =
        courseRepository
            .findById(courseId)
            .orElseThrow(() -> new EntityNotFoundException(Course.class, courseId));
    if (course.getOrgName() == null || course.getInstallationId() == null) {
      throw new NoLinkedOrganizationException(course.getCourseName());
    } else {
      CreateStudentRepositoriesJob job =
          CreateStudentRepositoriesJob.builder()
              .repositoryPrefix(repoPrefix)
              .isPrivate(isPrivate.orElse(false))
              .repositoryService(repositoryService)
              .course(course)
              .permissions(permissions)
              .creationTarget(creationTarget.orElse(RepositoryCreationTarget.STUDENTS_ONLY))
              .build();
      return jobService.runAsJob(job);
    }
  }
}
