package edu.ucsb.cs156.frontiers.jobs;

import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.entities.RosterStudent;
import edu.ucsb.cs156.frontiers.entities.User;
import edu.ucsb.cs156.frontiers.enums.OrgStatus;
import edu.ucsb.cs156.frontiers.models.OrgMember;
import edu.ucsb.cs156.frontiers.repositories.RosterStudentRepository;
import edu.ucsb.cs156.frontiers.repositories.UserRepository;
import edu.ucsb.cs156.frontiers.services.OrganizationLinkerService;
import edu.ucsb.cs156.frontiers.services.OrganizationMemberService;
import edu.ucsb.cs156.frontiers.services.jobs.JobContext;
import edu.ucsb.cs156.frontiers.services.jobs.JobContextConsumer;
import lombok.Builder;

import java.util.Optional;
@Builder
public class UpdateOrgMembershipJob implements JobContextConsumer {
    Course course;
    OrganizationMemberService organizationMemberService;
    UserRepository userRepository;
    RosterStudentRepository rosterStudentRepository;


    @Override
    public void accept(JobContext ctx) throws Exception {
        ctx.log("Processing...");
        Iterable<OrgMember> members = organizationMemberService.getOrganizationMembers(course);
        for(OrgMember member : members){
            Optional<User> user = userRepository.findByGithubId(member.getGithubId());
            if(user.isPresent()){
                Optional<RosterStudent> student = rosterStudentRepository.findByCourseAndUser(course, user.get());
                if(student.isPresent()){
                    RosterStudent foundStudent = student.get();
                    foundStudent.setOrgStatus(OrgStatus.MEMBER);
                    rosterStudentRepository.save(foundStudent);
                }
            }
        }
        ctx.log("Done");
    }
}
