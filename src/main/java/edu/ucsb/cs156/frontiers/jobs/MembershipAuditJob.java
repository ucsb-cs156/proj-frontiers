package edu.ucsb.cs156.frontiers.jobs;

import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.entities.RosterStudent;
import edu.ucsb.cs156.frontiers.enums.OrgStatus;
import edu.ucsb.cs156.frontiers.models.OrgMember;
import edu.ucsb.cs156.frontiers.repositories.CourseRepository;
import edu.ucsb.cs156.frontiers.repositories.RosterStudentRepository;
import edu.ucsb.cs156.frontiers.services.OrganizationMemberService;
import edu.ucsb.cs156.frontiers.services.jobs.JobContext;
import edu.ucsb.cs156.frontiers.services.jobs.JobContextConsumer;
import lombok.Builder;

import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;

@Builder
public class MembershipAuditJob implements JobContextConsumer {
    CourseRepository courseRepository;
    OrganizationMemberService organizationMemberService;
    RosterStudentRepository rosterStudentRepository;

    @Override
    public void accept(JobContext ctx) throws Exception {
        ctx.log("Auditing membership for each course with an attached GitHub Organization...");
        Iterable<Course> courses = courseRepository.findAll();
        for(Course course : courses){
            if (course.getOrgName() != null && course.getInstallationId() != null) {
                Iterable<OrgMember> members = organizationMemberService.getOrganizationMembers(course);
                Iterable<OrgMember> admins = organizationMemberService.getOrganizationAdmins(course);
                List<RosterStudent> rosterStudents = course.getRosterStudents();
                for (int i = 0; i < rosterStudents.size(); i++ ) {
                    Integer studentGithubId = rosterStudents.get(i).getGithubId();
                    String studentGithubLogin = rosterStudents.get(i).getGithubLogin();
                    if(studentGithubId != null && studentGithubLogin != null){
                        Optional<OrgMember> member = StreamSupport.stream(members.spliterator(), false).filter(s -> studentGithubId.equals(s.getGithubId())).findFirst();
                        Optional<OrgMember> admin = StreamSupport.stream(admins.spliterator(), false).filter(s -> studentGithubId.equals(s.getGithubId())).findFirst();
                        OrgStatus updatedStatus = OrgStatus.NONE;
                        if (admin.isPresent()) {
                            updatedStatus = OrgStatus.OWNER;
                        } else if (member.isPresent()) {
                            updatedStatus = OrgStatus.MEMBER;
                        }
                        rosterStudents.get(i).setOrgStatus(updatedStatus);
                    }
                }
                rosterStudentRepository.saveAll(rosterStudents);
            }
        }
        ctx.log("Done");
    }
}
