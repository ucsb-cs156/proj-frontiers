package edu.ucsb.cs156.frontiers.jobs;

import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.entities.CourseStaff;
import edu.ucsb.cs156.frontiers.entities.RosterStudent;
import edu.ucsb.cs156.frontiers.enums.OrgStatus;
import edu.ucsb.cs156.frontiers.models.OrgMember;
import edu.ucsb.cs156.frontiers.repositories.CourseRepository;
import edu.ucsb.cs156.frontiers.repositories.CourseStaffRepository;
import edu.ucsb.cs156.frontiers.repositories.RosterStudentRepository;
import edu.ucsb.cs156.frontiers.services.OrganizationMemberService;
import edu.ucsb.cs156.jobs.services.JobContext;
import edu.ucsb.cs156.jobs.services.JobContextConsumer;
import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;
import lombok.Builder;

@Builder
public class MembershipAuditJob implements JobContextConsumer {
  CourseRepository courseRepository;
  OrganizationMemberService organizationMemberService;
  RosterStudentRepository rosterStudentRepository;
  CourseStaffRepository courseStaffRepository;

  // Unscoped: audits every course with a linked GitHub org in one run, not a single course.
  // getScopeType()/getScopeId() default to null (unscoped), same as the old getCourse() did.

  @Override
  public void accept(JobContext ctx) throws Exception {
    ctx.log("Auditing membership for each course with an attached GitHub Organization...");
    Iterable<Course> courses = courseRepository.findAll();
    for (Course course : courses) {
      // Nothing in this method's whole body logs per course -- the only ctx.log() calls are
      // the opening line above and the closing "Done" below. checkCancellation() gives the
      // course loop its own checkpoint independent of course count.
      ctx.checkCancellation();
      if (course.getOrgName() != null && course.getInstallationId() != null) {
        Iterable<OrgMember> members = organizationMemberService.getOrganizationMembers(course);
        Iterable<OrgMember> admins = organizationMemberService.getOrganizationAdmins(course);
        Iterable<OrgMember> invitees = organizationMemberService.getOrganizationInvitees(course);
        List<RosterStudent> rosterStudents = course.getRosterStudents();
        List<CourseStaff> courseStaff = course.getCourseStaff();
        for (int i = 0; i < rosterStudents.size(); i++) {
          // Same reasoning as the course loop: this loop never logs per student, so a large
          // roster gives cancellation no other opportunity to fire.
          ctx.checkCancellation();
          Integer studentGithubId = rosterStudents.get(i).getGithubId();
          String studentGithubLogin = rosterStudents.get(i).getGithubLogin();
          if (studentGithubId != null && studentGithubLogin != null) {
            Optional<OrgMember> member =
                StreamSupport.stream(members.spliterator(), false)
                    .filter(s -> studentGithubId.equals(s.getGithubId()))
                    .findFirst();
            Optional<OrgMember> admin =
                StreamSupport.stream(admins.spliterator(), false)
                    .filter(s -> studentGithubId.equals(s.getGithubId()))
                    .findFirst();
            Optional<OrgMember> invitee =
                StreamSupport.stream(invitees.spliterator(), false)
                    .filter(s -> studentGithubId.equals(s.getGithubId()))
                    .findFirst();

            OrgStatus updatedStatus = OrgStatus.JOINCOURSE;

            if (admin.isPresent()) {
              updatedStatus = OrgStatus.OWNER;
            } else if (member.isPresent()) {
              updatedStatus = OrgStatus.MEMBER;
            } else if (invitee.isPresent()) {
              updatedStatus = OrgStatus.INVITED;
            }
            rosterStudents.get(i).setOrgStatus(updatedStatus);
          }
        }
        rosterStudentRepository.saveAll(rosterStudents);

        for (int i = 0; i < courseStaff.size(); i++) {
          // Same reasoning as the roster-students loop above.
          ctx.checkCancellation();
          Integer staffGithubId = courseStaff.get(i).getGithubId();
          String staffGithubLogin = courseStaff.get(i).getGithubLogin();
          if (staffGithubId != null && staffGithubLogin != null) {
            Optional<OrgMember> member =
                StreamSupport.stream(members.spliterator(), false)
                    .filter(s -> staffGithubId.equals(s.getGithubId()))
                    .findFirst();
            Optional<OrgMember> admin =
                StreamSupport.stream(admins.spliterator(), false)
                    .filter(s -> staffGithubId.equals(s.getGithubId()))
                    .findFirst();
            Optional<OrgMember> invitee =
                StreamSupport.stream(invitees.spliterator(), false)
                    .filter(s -> staffGithubId.equals(s.getGithubId()))
                    .findFirst();

            OrgStatus updatedStatus = OrgStatus.JOINCOURSE;

            if (admin.isPresent()) {
              updatedStatus = OrgStatus.OWNER;
            } else if (member.isPresent()) {
              updatedStatus = OrgStatus.MEMBER;
            } else if (invitee.isPresent()) {
              updatedStatus = OrgStatus.INVITED;
            }
            courseStaff.get(i).setOrgStatus(updatedStatus);
          }
        }
        courseStaffRepository.saveAll(courseStaff);
      }
    }
    ctx.log("Done");
  }
}
