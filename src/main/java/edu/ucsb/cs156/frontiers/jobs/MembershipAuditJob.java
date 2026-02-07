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
import edu.ucsb.cs156.frontiers.services.jobs.JobContext;
import edu.ucsb.cs156.frontiers.services.jobs.JobContextConsumer;
import java.util.Optional;
import java.util.Set;
import java.util.stream.StreamSupport;
import lombok.Builder;

@Builder
public class MembershipAuditJob implements JobContextConsumer {
  CourseRepository courseRepository;
  OrganizationMemberService organizationMemberService;
  RosterStudentRepository rosterStudentRepository;
  CourseStaffRepository courseStaffRepository;

  @Override
  public void accept(JobContext ctx) throws Exception {
    ctx.log("Auditing membership for each course with an attached GitHub Organization...");
    Iterable<Course> courses = courseRepository.findAll();
    for (Course course : courses) {
      if (course.getOrgName() != null && course.getInstallationId() != null) {
        Iterable<OrgMember> members = organizationMemberService.getOrganizationMembers(course);
        Iterable<OrgMember> admins = organizationMemberService.getOrganizationAdmins(course);
        Iterable<OrgMember> invitees = organizationMemberService.getOrganizationInvitees(course);
        Set<RosterStudent> rosterStudents = course.getRosterStudents();
        Set<CourseStaff> courseStaff = course.getCourseStaff();
        for (RosterStudent student : rosterStudents) {
          Integer studentGithubId = student.getGithubId();
          String studentGithubLogin = student.getGithubLogin();
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
            student.setOrgStatus(updatedStatus);
          }
        }
        rosterStudentRepository.saveAll(rosterStudents);

        for (CourseStaff staff : courseStaff) {
          Integer staffGithubId = staff.getGithubId();
          String staffGithubLogin = staff.getGithubLogin();
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
            staff.setOrgStatus(updatedStatus);
          }
        }
        courseStaffRepository.saveAll(courseStaff);
      }
    }
    ctx.log("Done");
  }
}
