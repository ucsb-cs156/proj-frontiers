package edu.ucsb.cs156.frontiers.jobs;

import edu.ucsb.cs156.frontiers.entities.RosterStudent;
import edu.ucsb.cs156.frontiers.enums.OrgStatus;
import edu.ucsb.cs156.frontiers.repositories.RosterStudentRepository;
import edu.ucsb.cs156.frontiers.services.OrganizationMemberService;
import edu.ucsb.cs156.frontiers.services.jobs.JobContext;
import edu.ucsb.cs156.frontiers.services.jobs.JobContextConsumer;
import java.util.List;
import lombok.Builder;
import lombok.EqualsAndHashCode;

@Builder
@EqualsAndHashCode
public class RemoveStudentsJob implements JobContextConsumer {
  private OrganizationMemberService organizationMemberService;
  private List<RosterStudent> students;
  private RosterStudentRepository rosterStudentRepository;

  @Override
  public void accept(JobContext c) throws Exception {
    for (RosterStudent student : students) {
      if (student.getCourse().getOrgName() != null
          && student.getCourse().getInstallationId() != null) {
        if (student.getGithubLogin() != null && student.getGithubId() != null) {
          organizationMemberService.removeOrganizationMember(student);
          c.log("Removed student %s from Organization".formatted(student.getGithubLogin()));
          student.setOrgStatus(OrgStatus.REMOVED);
          student.setGithubId(null);
          student.setGithubLogin(null);
          rosterStudentRepository.save(student);
        }
      }
    }
  }
}
