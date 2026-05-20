package edu.ucsb.cs156.frontiers.jobs;

import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.repositories.CourseRepository;
import edu.ucsb.cs156.frontiers.services.RepositoryService;
import edu.ucsb.cs156.frontiers.services.jobs.JobContext;
import edu.ucsb.cs156.frontiers.services.jobs.JobContextConsumer;
import java.util.List;
import java.util.Optional;
import lombok.Builder;

@Builder
public class DeleteReposJob implements JobContextConsumer {
  Long courseId;
  String prefix;
  CourseRepository courseRepository;
  RepositoryService repositoryService;

  @Builder.Default long sleepMillis = 100L;

  @Override
  public Course getCourse() {
    if (courseId == null || courseRepository == null) {
      return null;
    }
    return courseRepository.findById(courseId).orElse(null);
  }

  @Override
  public void accept(JobContext ctx) throws Exception {
    Optional<Course> courseOpt = courseRepository.findById(courseId);
    if (courseOpt.isEmpty()) {
      ctx.log("ERROR: Course with ID " + courseId + " not found");
      return;
    }

    Course course = courseOpt.get();
    if (course.getOrgName() == null || course.getInstallationId() == null) {
      ctx.log("ERROR: Course has no linked GitHub organization");
      return;
    }

    List<String> repos = repositoryService.getRepositoryNamesWithPrefix(course, prefix);
    ctx.log(repos.size() + " repos found with prefix " + prefix);

    int reposDeleted = 0;
    int reposRetained = 0;
    int repoErrors = 0;

    for (String repoName : repos) {
      try {
        if (repositoryService.repositoryHasCommits(course, repoName)) {
          reposRetained++;
          ctx.log("Repo " + repoName + " not deleted; commits exist.");
        } else {
          repositoryService.deleteRepository(course, repoName);
          reposDeleted++;
        }
      } catch (Exception e) {
        repoErrors++;
        ctx.log("ERROR deleting repo " + repoName + ": " + e.getMessage());
      }

      if (sleepMillis > 0) {
        Thread.sleep(sleepMillis);
      }
    }

    ctx.log(reposDeleted + " repos deleted");
    ctx.log(reposRetained + " repos retained");
    ctx.log(repoErrors + " errors");
  }
}
