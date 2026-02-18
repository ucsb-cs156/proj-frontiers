package edu.ucsb.cs156.frontiers.jobs;

import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.entities.CourseStaff;
import edu.ucsb.cs156.frontiers.entities.RosterStudent;
import edu.ucsb.cs156.frontiers.enums.OrgStatus;
import edu.ucsb.cs156.frontiers.enums.RepositoryCreationOption;
import edu.ucsb.cs156.frontiers.enums.RepositoryPermissions;
import edu.ucsb.cs156.frontiers.services.RepositoryService;
import edu.ucsb.cs156.frontiers.services.jobs.JobContext;
import edu.ucsb.cs156.frontiers.services.jobs.JobContextConsumer;
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
  public Course getCourse() {
    return this.course;
  }

  @Override
  public void accept(JobContext ctx) throws Exception {
    ctx.log("Processing...");

    if (creationOption == RepositoryCreationOption.STUDENTS_ONLY
        || creationOption == RepositoryCreationOption.STUDENTS_AND_STAFF) {
      for (RosterStudent student : course.getRosterStudents()) {
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
