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
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpStatusCodeException;

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
          try {
            organizationMemberService.removeOrganizationMember(student);
            c.log("Removed student %s from Organization".formatted(student.getGithubLogin()));
          } catch (HttpStatusCodeException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
              c.log("Student %s not in Organization".formatted(student.getGithubLogin()));
            } else {
              throw new RuntimeException(e);
            }
          }
          student.setGithubId(null);
          student.setGithubLogin(null);
          student.setOrgStatus(OrgStatus.REMOVED);
          rosterStudentRepository.save(student);
        }
      }
    }
  }
}
