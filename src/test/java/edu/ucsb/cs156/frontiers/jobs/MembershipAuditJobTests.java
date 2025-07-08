package edu.ucsb.cs156.frontiers.jobs;

import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.entities.Job;
import edu.ucsb.cs156.frontiers.entities.RosterStudent;
import edu.ucsb.cs156.frontiers.enums.OrgStatus;
import edu.ucsb.cs156.frontiers.models.OrgMember;
import edu.ucsb.cs156.frontiers.repositories.CourseRepository;
import edu.ucsb.cs156.frontiers.repositories.RosterStudentRepository;
import edu.ucsb.cs156.frontiers.repositories.UserRepository;
import edu.ucsb.cs156.frontiers.services.OrganizationMemberService;
import edu.ucsb.cs156.frontiers.services.jobs.JobContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MembershipAuditJobTests {
    @Mock
    private UserRepository userRepository;

    @Mock
    private RosterStudentRepository rosterStudentRepository;

    @Mock
    private OrganizationMemberService organizationMemberService;

    @Mock
    private CourseRepository courseRepository;


    Job jobStarted = Job.builder().build();
    JobContext ctx = new JobContext(null, jobStarted);

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void match_students_correctly() throws Exception {
        OrgMember orgMember1 = OrgMember.builder().githubId(123456).githubLogin("division7").build();
        OrgMember orgMember2 = OrgMember.builder().githubId(123457).githubLogin("division8").build();
        List<OrgMember> orgMembers = List.of(orgMember1, orgMember2);
        OrgMember orgMember3 = OrgMember.builder().githubId(123455).githubLogin("division9").build();
        OrgMember orgMember4 = OrgMember.builder().githubId(772).githubLogin("unmatched").build();
        List<OrgMember> secondCourse = List.of(orgMember3, orgMember4);
        Course course = Course.builder().orgName("ucsb-cs156").installationId("1234").build();
        Course course2 = Course.builder().orgName("ucsb-cs156-f25").installationId("1235").build();
        RosterStudent student1 = RosterStudent.builder().studentId("banana").githubLogin("division7").githubId(123456).course(course).build();
        RosterStudent student2 = RosterStudent.builder().studentId("apple").githubLogin("division8").githubId(123457).course(course).build();
        course.setRosterStudents(List.of(student1, student2));
        RosterStudent student3 = RosterStudent.builder().studentId("banana").githubLogin("division9").githubId(123455).course(course2).build();
        RosterStudent student4 = RosterStudent.builder().studentId("apple").githubLogin("division10").githubId(123454).course(course2).build();
        course2.setRosterStudents(List.of(student3, student4));
        RosterStudent student1Updated = RosterStudent.builder().studentId("banana").githubLogin("division7").githubId(123456).course(course).orgStatus(OrgStatus.MEMBER).build();
        RosterStudent student2Updated = RosterStudent.builder().studentId("apple").githubLogin("division8").githubId(123457).course(course).orgStatus(OrgStatus.MEMBER).build();
        RosterStudent student3Updated = RosterStudent.builder().studentId("banana").githubLogin("division9").githubId(123455).course(course2).orgStatus(OrgStatus.MEMBER).build();

        doReturn(orgMembers).when(organizationMemberService).getOrganizationMembers(eq(course));
        doReturn(secondCourse).when(organizationMemberService).getOrganizationMembers(eq(course2));
        doReturn(List.of(course, course2)).when(courseRepository).findAll();

        var matchJob = spy(MembershipAuditJob.builder()
                .rosterStudentRepository(rosterStudentRepository)
                .organizationMemberService(organizationMemberService)
                .courseRepository(courseRepository)
                .build());

        matchJob.accept(ctx);
        String expected = """
                Auditing membership for each course with an attached GitHub Organization...
                Done""";
        assertEquals(expected, jobStarted.getLog());

        verify(rosterStudentRepository, times(1)).save(eq(student1Updated));
        verify(rosterStudentRepository, times(1)).save(eq(student2Updated));
        verify(rosterStudentRepository, times(1)).save(eq(student3Updated));
    }

    @Test
    public void no_roster_student() throws Exception {
        OrgMember orgMember1 = OrgMember.builder().githubId(123456).githubLogin("division7").build();
        List<OrgMember> orgMembers = List.of(orgMember1);
        Course course = Course.builder().orgName("ucsb-cs156").installationId("1234").build();

        doReturn(orgMembers).when(organizationMemberService).getOrganizationMembers(eq(course));
        doReturn(Optional.empty()).when(rosterStudentRepository).findByCourseAndGithubId(eq(course), eq(123456));

        var matchJob = spy(UpdateOrgMembershipJob.builder()
                .rosterStudentRepository(rosterStudentRepository)
                .organizationMemberService(organizationMemberService)
                .course(course)
                .build());

        matchJob.accept(ctx);
        String expected = """
                Processing...
                Done""";
        assertEquals(expected, jobStarted.getLog());

        verify(rosterStudentRepository, times(0)).save(any());
    }
}
