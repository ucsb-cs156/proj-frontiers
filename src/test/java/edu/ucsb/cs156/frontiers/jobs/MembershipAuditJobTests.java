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

        List<OrgMember> emptyAdmins = List.of();

        Course course = Course.builder().orgName("ucsb-cs156").installationId("1234").build();
        Course course2 = Course.builder().orgName("ucsb-cs156-f25").installationId("1235").build();
        Course course3 = Course.builder().build();
        Course course4 = Course.builder().orgName("ucsb-cs156-f25").build();
        RosterStudent student1 = RosterStudent.builder().studentId("banana").githubLogin("division7").githubId(123456).course(course).build();
        RosterStudent student2 = RosterStudent.builder().studentId("apple").githubLogin("division8").githubId(123457).course(course).build();
        course.setRosterStudents(List.of(student1, student2));
        RosterStudent student3 = RosterStudent.builder().studentId("banana").githubLogin("division9").githubId(123455).course(course2).build();
        RosterStudent student4 = RosterStudent.builder().studentId("apple").githubLogin("division10").githubId(123454).course(course2).build();
        RosterStudent student5 = RosterStudent.builder().studentId("orange").githubLogin(null).githubId(null).course(course2).build();
        RosterStudent student6 = RosterStudent.builder().studentId("grape").githubLogin(null).githubId(123455).course(course3).build();
        course2.setRosterStudents(List.of(student3, student4, student5, student6));
        RosterStudent student1Updated = RosterStudent.builder().studentId("banana").githubLogin("division7").githubId(123456).course(course).orgStatus(OrgStatus.MEMBER).build();
        RosterStudent student2Updated = RosterStudent.builder().studentId("apple").githubLogin("division8").githubId(123457).course(course).orgStatus(OrgStatus.MEMBER).build();
        RosterStudent student3Updated = RosterStudent.builder().studentId("banana").githubLogin("division9").githubId(123455).course(course2).orgStatus(OrgStatus.MEMBER).build();
        RosterStudent student4Updated = RosterStudent.builder().studentId("apple").githubLogin("division10").githubId(123454).course(course2).orgStatus(OrgStatus.PENDING).build();
        RosterStudent student5Updated = RosterStudent.builder().studentId("orange").githubLogin(null).githubId(null).course(course2).build();
        RosterStudent student6Updated = RosterStudent.builder().studentId("grape").githubLogin(null).githubId(123455).course(course3).build();



        doReturn(orgMembers).when(organizationMemberService).getOrganizationMembers(eq(course));
        doReturn(secondCourse).when(organizationMemberService).getOrganizationMembers(eq(course2));
        doReturn(emptyAdmins).when(organizationMemberService).getOrganizationAdmins(eq(course));
        doReturn(emptyAdmins).when(organizationMemberService).getOrganizationAdmins(eq(course2));
        doReturn(List.of(course, course2, course3, course4)).when(courseRepository).findAll();

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

        verify(rosterStudentRepository, atLeastOnce()).saveAll(eq(List.of(student1Updated, student2Updated)));
        verify(rosterStudentRepository, atLeastOnce()).saveAll(eq(List.of(student3Updated, student4Updated, student5Updated, student6Updated)));
        verify(rosterStudentRepository, times(2)).saveAll(any());
    }


    @Test
    public void no_match_on_any_member() throws Exception{
        OrgMember orgMember1 = OrgMember.builder().githubId(123455).githubLogin("unmatched-a").build();
        OrgMember orgMember2 = OrgMember.builder().githubId(772).githubLogin("unmatched-b").build();
        List<OrgMember> orgMembers = List.of(orgMember1, orgMember2);
        List<OrgMember> emptyAdmins = List.of();
        Course course = Course.builder().orgName("ucsb-cs156").installationId("1234").build();
        RosterStudent student1 = RosterStudent.builder().studentId("banana").githubLogin("division7").githubId(123456).course(course).build();
        course.setRosterStudents(List.of(student1));
        RosterStudent student1Updated = RosterStudent.builder().studentId("banana").githubLogin("division7").githubId(123456).course(course).orgStatus(OrgStatus.PENDING).build();
        when(organizationMemberService.getOrganizationMembers(course)).thenReturn(orgMembers);
        when(organizationMemberService.getOrganizationAdmins(course)).thenReturn(emptyAdmins);
        when(courseRepository.findAll()).thenReturn(List.of(course));

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

        verify(rosterStudentRepository, times(1)).saveAll(eq(List.of(student1Updated)));
    }

    @Test
    public void match_admin_students_correctly() throws Exception {
        OrgMember orgMember2 = OrgMember.builder().githubId(123457).githubLogin("division8").build();
        List<OrgMember> orgMembers = List.of(orgMember2);

        OrgMember orgAdmin2 = OrgMember.builder().githubId(123455).githubLogin("division9").build();
        List<OrgMember> orgAdmins = List.of(orgAdmin2);

        Course course = Course.builder().orgName("ucsb-cs156").installationId("1234").build();
        RosterStudent student2 = RosterStudent.builder().studentId("apple").githubLogin("division8").githubId(123457).course(course).build();
        RosterStudent student3 = RosterStudent.builder().studentId("orange").githubLogin("division9").githubId(123455).course(course).build();
        course.setRosterStudents(List.of(student2, student3));

        RosterStudent student2Updated = RosterStudent.builder().studentId("apple").githubLogin("division8").githubId(123457).course(course).orgStatus(OrgStatus.MEMBER).build();
        RosterStudent student3Updated = RosterStudent.builder().studentId("orange").githubLogin("division9").githubId(123455).course(course).orgStatus(OrgStatus.OWNER).build();

        doReturn(orgMembers).when(organizationMemberService).getOrganizationMembers(eq(course));
        doReturn(orgAdmins).when(organizationMemberService).getOrganizationAdmins(eq(course));
        doReturn(List.of(course)).when(courseRepository).findAll();

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

        verify(rosterStudentRepository, times(1)).saveAll(eq(List.of(student2Updated, student3Updated)));
    }
    @Test
    public void match_invited_students_correctly() throws Exception {
        List<OrgMember> emptyMembers = List.of();
        List<OrgMember> emptyAdmins = List.of();
        OrgMember invitee = OrgMember.builder().githubId(123456).githubLogin("division7").build();
        List<OrgMember> orgInvitees = List.of(invitee);

        Course course = Course.builder().orgName("ucsb-cs156").installationId("1234").build();

        RosterStudent student = RosterStudent.builder()
            .studentId("banana")
            .githubLogin("division7")
            .githubId(123456)
            .course(course)
            .orgStatus(OrgStatus.MEMBER)
            .build();

        RosterStudent student2 = RosterStudent.builder()
                .studentId("banana")
                .githubLogin("division11")
                .githubId(241789)
                .course(course)
                .orgStatus(OrgStatus.PENDING)
                .build();

        course.setRosterStudents(List.of(student, student2));

        RosterStudent studentUpdated = RosterStudent.builder()
            .studentId("banana")
            .githubLogin("division7")
            .githubId(123456)
            .course(course)
            .orgStatus(OrgStatus.INVITED)
            .build();

        RosterStudent student2NotUpdated = RosterStudent.builder()
                .studentId("banana")
                .githubLogin("division11")
                .githubId(241789)
                .course(course)
                .orgStatus(OrgStatus.PENDING)
                .build();

        doReturn(emptyMembers).when(organizationMemberService).getOrganizationMembers(eq(course));
        doReturn(emptyAdmins).when(organizationMemberService).getOrganizationAdmins(eq(course));
        doReturn(orgInvitees).when(organizationMemberService).getOrganizationInvitees(eq(course));
        doReturn(List.of(course)).when(courseRepository).findAll();

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

        verify(rosterStudentRepository, times(1)).saveAll(eq(List.of(studentUpdated, student2NotUpdated)));
    }
}
