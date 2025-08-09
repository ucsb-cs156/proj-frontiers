package edu.ucsb.cs156.frontiers.jobs;

import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.entities.RosterStudent;
import edu.ucsb.cs156.frontiers.enums.OrgStatus;
import edu.ucsb.cs156.frontiers.models.OrgMember;
import edu.ucsb.cs156.frontiers.repositories.RosterStudentRepository;
import edu.ucsb.cs156.frontiers.services.OrganizationMemberService;
import edu.ucsb.cs156.frontiers.services.jobs.JobContext;
import edu.ucsb.cs156.frontiers.services.jobs.JobContextConsumer;
import java.util.Optional;
import lombok.Builder;

@Builder
public class UpdateOrgMembershipJob implements JobContextConsumer {
  Course course;
  OrganizationMemberService organizationMemberService;
  RosterStudentRepository rosterStudentRepository;

  @Override
  public void accept(JobContext ctx) throws Exception {
    ctx.log("Processing...");
    Iterable<OrgMember> members = organizationMemberService.getOrganizationMembers(course);
    for (OrgMember member : members) {
      Optional<RosterStudent> student =
          rosterStudentRepository.findByCourseAndGithubId(course, member.getGithubId());
      if (student.isPresent()) {
        RosterStudent foundStudent = student.get();
        foundStudent.setOrgStatus(OrgStatus.MEMBER);
        rosterStudentRepository.save(foundStudent);
      }
    }
    ctx.log("Done");
  }
}
