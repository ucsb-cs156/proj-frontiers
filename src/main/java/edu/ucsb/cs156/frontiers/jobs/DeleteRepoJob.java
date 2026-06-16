package edu.ucsb.cs156.frontiers.jobs;

import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.repositories.CourseRepository;
import edu.ucsb.cs156.frontiers.services.RepositoryService;
import edu.ucsb.cs156.frontiers.services.RepositoryService.GithubRepository;
import edu.ucsb.cs156.frontiers.services.jobs.JobContext;
import edu.ucsb.cs156.frontiers.services.jobs.JobContextConsumer;
import java.util.List;
import java.util.Optional;
import lombok.Builder;

@Builder
public class DeleteRepoJob implements JobContextConsumer {
  Long courseId;
  String prefix;
  CourseRepository courseRepository;
  RepositoryService repositoryService;

  @Override
  public Course getCourse() {
    Optional<Course> courseOpt = courseRepository.findById(courseId);
    return courseOpt.orElse(null);
  }

  @Override
  public void accept(JobContext ctx) throws Exception {
    ctx.log(String.format("Starting delete repository job for course ID: %s", courseId));
    ctx.log(String.format("prefix=%s", prefix));

    Optional<Course> courseOpt = courseRepository.findById(courseId);
    if (courseOpt.isEmpty()) {
      ctx.log(String.format("ERROR: Course with ID %s not found", courseId));
      return;
    }

    Course course = courseOpt.get();
    if (course.getOrgName() == null || course.getInstallationId() == null) {
      ctx.log("ERROR: Course has no linked GitHub organization");
      return;
    }

    List<GithubRepository> repositories;
    int errors = 0;
    try {
      repositories = repositoryService.getRepositoriesMatchingPrefix(course, prefix);
    } catch (Exception e) {
      ctx.log(String.format("ERROR: Failed to list repositories: %s", e.getMessage()));
      errors++;
      ctx.log(String.format("Summary: found=0, deleted=0, retained=0, errors=%d", errors));
      return;
    }

    int deleted = 0;
    int retained = 0;

    for (GithubRepository repository : repositories) {
      try {
        boolean deletedRepository =
            repositoryService.deleteRepositoryIfEmpty(course, repository.name());
        if (deletedRepository) {
          deleted++;
          ctx.log(String.format("Deleted empty repository: %s", repository.name()));
        } else {
          retained++;
          ctx.log(String.format("Retained repository with commits: %s", repository.name()));
        }
      } catch (Exception e) {
        errors++;
        ctx.log(
            String.format(
                "ERROR: Failed to delete/check repository %s: %s",
                repository.name(), e.getMessage()));
      }
    }

    ctx.log(
        String.format(
            "Summary: found=%d, deleted=%d, retained=%d, errors=%d",
            repositories.size(), deleted, retained, errors));
    ctx.log("Done");
  }
}
