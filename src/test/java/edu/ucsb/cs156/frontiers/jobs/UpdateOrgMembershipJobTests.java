package edu.ucsb.cs156.frontiers.jobs;

import com.fasterxml.jackson.core.JsonProcessingException;
import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.entities.Job;
import edu.ucsb.cs156.frontiers.entities.RosterStudent;
import edu.ucsb.cs156.frontiers.entities.User;
import edu.ucsb.cs156.frontiers.enums.OrgStatus;
import edu.ucsb.cs156.frontiers.models.OrgMember;
import edu.ucsb.cs156.frontiers.repositories.CourseRepository;
import edu.ucsb.cs156.frontiers.repositories.RosterStudentRepository;
import edu.ucsb.cs156.frontiers.repositories.UserRepository;
import edu.ucsb.cs156.frontiers.services.OrganizationLinkerService;
import edu.ucsb.cs156.frontiers.services.OrganizationMemberService;
import edu.ucsb.cs156.frontiers.services.jobs.JobContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UpdateOrgMembershipJobTests {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RosterStudentRepository rosterStudentRepository;

    @Mock
    private OrganizationMemberService organizationMemberService;

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
        User user1 = User.builder().githubLogin("division7").githubId(123456).id(1L).build();
        User user2 = User.builder().githubLogin("division8").githubId(123457).id(2L).build();
        List<OrgMember> orgMembers = List.of(orgMember1, orgMember2);
        Course course = Course.builder().orgName("ucsb-cs156").installationId("1234").build();
        RosterStudent student1 = RosterStudent.builder().studentId("banana").course(course).user(user1).build();
        RosterStudent student2 = RosterStudent.builder().studentId("apple").course(course).user(user2).build();
        RosterStudent student1Updated = RosterStudent.builder().studentId("banana").course(course).user(user1).orgStatus(OrgStatus.MEMBER).build();
        RosterStudent student2Updated = RosterStudent.builder().studentId("apple").course(course).user(user2).orgStatus(OrgStatus.MEMBER).build();

        doReturn(orgMembers).when(organizationMemberService).getOrganizationMembers(eq(course));
        doReturn(Optional.of(user1)).when(userRepository).findByGithubId(eq(123456));
        doReturn(Optional.of(user2)).when(userRepository).findByGithubId(eq(123457));
        doReturn(Optional.of(student1)).when(rosterStudentRepository).findByCourseAndUser(eq(course), eq(user1));
        doReturn(Optional.of(student2)).when(rosterStudentRepository).findByCourseAndUser(eq(course), eq(user2));

        var matchJob = spy(UpdateOrgMembershipJob.builder()
                .rosterStudentRepository(rosterStudentRepository)
                .userRepository(userRepository)
                .organizationMemberService(organizationMemberService)
                .course(course)
                .build());

        matchJob.accept(ctx);
        String expected = """
                Processing...
                Done""";
        assertEquals(expected, jobStarted.getLog());

        verify(rosterStudentRepository, times(1)).save(eq(student1Updated));
        verify(rosterStudentRepository, times(1)).save(eq(student2Updated));
    }

    @Test
    public void no_user() throws Exception {
        OrgMember orgMember1 = OrgMember.builder().githubId(123456).githubLogin("division7").build();
        List<OrgMember> orgMembers = List.of(orgMember1);
        Course course = Course.builder().orgName("ucsb-cs156").installationId("1234").build();

        doReturn(orgMembers).when(organizationMemberService).getOrganizationMembers(eq(course));
        doReturn(Optional.empty()).when(userRepository).findByGithubId(eq(123456));

        var matchJob = spy(UpdateOrgMembershipJob.builder()
                .rosterStudentRepository(rosterStudentRepository)
                .userRepository(userRepository)
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

    @Test
    public void no_roster_student() throws Exception {
        OrgMember orgMember1 = OrgMember.builder().githubId(123456).githubLogin("division7").build();
        User user1 = User.builder().githubLogin("division7").githubId(123456).id(1L).build();
        List<OrgMember> orgMembers = List.of(orgMember1);
        Course course = Course.builder().orgName("ucsb-cs156").installationId("1234").build();

        doReturn(orgMembers).when(organizationMemberService).getOrganizationMembers(eq(course));
        doReturn(Optional.of(user1)).when(userRepository).findByGithubId(eq(123456));
        doReturn(Optional.empty()).when(rosterStudentRepository).findByCourseAndUser(eq(course), eq(user1));

        var matchJob = spy(UpdateOrgMembershipJob.builder()
                .rosterStudentRepository(rosterStudentRepository)
                .userRepository(userRepository)
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
