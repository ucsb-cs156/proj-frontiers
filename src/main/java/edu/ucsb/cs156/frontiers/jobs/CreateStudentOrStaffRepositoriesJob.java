package edu.ucsb.cs156.frontiers.jobs;

import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.entities.CourseStaff;
import edu.ucsb.cs156.frontiers.entities.RosterStudent;
import edu.ucsb.cs156.frontiers.enums.OrgStatus;
import edu.ucsb.cs156.frontiers.enums.RepositoryCreationOption;
import edu.ucsb.cs156.frontiers.enums.RepositoryPermissions;
import edu.ucsb.cs156.frontiers.services.RepositoryService;
import edu.ucsb.cs156.jobs.services.JobContext;
import edu.ucsb.cs156.jobs.services.JobContextConsumer;
import lombok.Builder;

@Builder
public class CreateStudentOrStaffRepositoriesJob implements JobContextConsumer {
  Course course;
  RepositoryService repositoryService;
  String repositoryPrefix;
  Boolean isPrivate;
  RepositoryPermissions permissions;

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
          repositoryService.createStudentRepository(
              course, student, repositoryPrefix, isPrivate, permissions);
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
          repositoryService.createStaffRepository(
              course, staff, repositoryPrefix, isPrivate, permissions);
        }
      }
    }

    ctx.log("Done");
  }
}
