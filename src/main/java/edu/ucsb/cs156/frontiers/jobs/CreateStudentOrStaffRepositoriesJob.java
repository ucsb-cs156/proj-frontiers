package edu.ucsb.cs156.frontiers.jobs;

import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.entities.CourseStaff;
import edu.ucsb.cs156.frontiers.entities.RosterStudent;
import edu.ucsb.cs156.frontiers.enums.OrgStatus;
import edu.ucsb.cs156.frontiers.enums.RepositoryCreationOption;
import edu.ucsb.cs156.frontiers.enums.RepositoryPermissions;
import edu.ucsb.cs156.frontiers.services.RepositoryService;
import edu.ucsb.cs156.frontiers.services.RepositoryService.RepositoryCreationResult;
import edu.ucsb.cs156.jobs.services.JobContext;
import edu.ucsb.cs156.jobs.services.JobContextConsumer;
import java.util.Optional;
import lombok.Builder;
import org.springframework.web.client.HttpStatusCodeException;

@Builder
public class CreateStudentOrStaffRepositoriesJob implements JobContextConsumer {
  Course course;
  RepositoryService repositoryService;
  String repositoryPrefix;
  Boolean isPrivate;
  RepositoryPermissions permissions;

  /**
   * Whether the repositories must require signed commits: true gives each repository the ruleset
   * that requires them, false removes it. Null, as for a caller that does not manage this, leaves
   * the repositories' rulesets alone.
   */
  Boolean requireSignedCommit;

  @Builder.Default RepositoryCreationOption creationOption = RepositoryCreationOption.STUDENTS_ONLY;

  @Override
  public String getScopeType() {
    return "course";
  }

  @Override
  public Long getScopeId() {
    return course.getId();
  }

  @Override
  public void accept(JobContext ctx) throws Exception {
    ctx.log("repositoryPrefix=" + repositoryPrefix);
    ctx.log("isPrivate=" + isPrivate);
    ctx.log("permissions=" + permissions);
    ctx.log("creationOption=" + creationOption);
    if (requireSignedCommit != null) {
      ctx.log("requireSignedCommit=" + requireSignedCommit);
    }

    int reposCreated = 0;
    int reposUpdated = 0;
    int signedCommitsFailures = 0;

    if (creationOption == RepositoryCreationOption.STUDENTS_ONLY
        || creationOption == RepositoryCreationOption.STUDENTS_AND_STAFF) {
      for (RosterStudent student : course.getRosterStudents()) {
        // A student not on GitHub or not yet an org member never calls repositoryService,
        // and neither of those branches calls ctx.log() -- checkCancellation() gives this
        // loop its own checkpoint independent of whether an iteration does anything at all.
        ctx.checkCancellation();
        if (student.getGithubLogin() != null
            && (student.getOrgStatus() == OrgStatus.MEMBER
                || student.getOrgStatus() == OrgStatus.OWNER)) {
          Optional<RepositoryCreationResult> result =
              repositoryService.createStudentRepository(
                  course, student, repositoryPrefix, isPrivate, permissions);
          if (result.isPresent()) {
            if (result.get().created()) {
              ctx.log(" created repo " + result.get().repoName());
              reposCreated++;
            } else {
              ctx.log("  updated repo " + result.get().repoName());
              reposUpdated++;
            }
            if (!applySignedCommits(ctx, result.get().repoName())) {
              signedCommitsFailures++;
            }
          }
        }
      }
    }

    if (creationOption == RepositoryCreationOption.STAFF_ONLY
        || creationOption == RepositoryCreationOption.STUDENTS_AND_STAFF) {
      for (CourseStaff staff : course.getCourseStaff()) {
        // Same reasoning as the student loop above: a staff member not on GitHub or not yet
        // an org member never logs, so this loop needs its own cancellation checkpoint too.
        ctx.checkCancellation();
        if (staff.getGithubLogin() != null
            && (staff.getOrgStatus() == OrgStatus.MEMBER
                || staff.getOrgStatus() == OrgStatus.OWNER)) {
          Optional<RepositoryCreationResult> result =
              repositoryService.createStaffRepository(
                  course, staff, repositoryPrefix, isPrivate, permissions);
          if (result.isPresent()) {
            if (result.get().created()) {
              ctx.log(" created repo " + result.get().repoName());
              reposCreated++;
            } else {
              ctx.log("  updated repo " + result.get().repoName());
              reposUpdated++;
            }
            if (!applySignedCommits(ctx, result.get().repoName())) {
              signedCommitsFailures++;
            }
          }
        }
      }
    }

    ctx.log("Summary:");
    ctx.log(String.format("%4d repos created", reposCreated));
    ctx.log(String.format("%4d repos updated", reposUpdated));
    ctx.log(String.format("%4d repos total", reposCreated + reposUpdated));
    if (signedCommitsFailures > 0) {
      ctx.log(
          String.format("%4d repos where signed commits could not be set", signedCommitsFailures));
    }
    ctx.log("Done");
  }

  /**
   * Makes a repository require signed commits, or not, as this job is set to; does nothing if it is
   * not set either way. A refusal by GitHub is logged, and does not stop the job.
   *
   * @return false if GitHub refused, true otherwise
   */
  private boolean applySignedCommits(JobContext ctx, String repoName) throws Exception {
    if (requireSignedCommit == null) {
      return true;
    }
    try {
      repositoryService.setSignedCommitsRequired(course, repoName, requireSignedCommit);
      return true;
    } catch (HttpStatusCodeException e) {
      ctx.log(
          "  could not "
              + (requireSignedCommit ? "require" : "stop requiring")
              + " signed commits on "
              + repoName
              + ": "
              + e.getStatusCode()
              + " "
              + e.getResponseBodyAsString());
      return false;
    }
  }
}
