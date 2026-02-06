package edu.ucsb.cs156.frontiers.jobs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.entities.Job;
import edu.ucsb.cs156.frontiers.entities.RosterStudent;
import edu.ucsb.cs156.frontiers.enums.OrgStatus;
import edu.ucsb.cs156.frontiers.models.OrgMember;
import edu.ucsb.cs156.frontiers.repositories.RosterStudentRepository;
import edu.ucsb.cs156.frontiers.repositories.UserRepository;
import edu.ucsb.cs156.frontiers.services.OrganizationMemberService;
import edu.ucsb.cs156.frontiers.services.jobs.JobContext;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class UpdateOrgMembershipJobTests {

  @Mock private UserRepository userRepository;

  @Mock private RosterStudentRepository rosterStudentRepository;

  @Mock private OrganizationMemberService organizationMemberService;

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
    Course course = Course.builder().orgName("ucsb-cs156").installationId("1234").build();
    RosterStudent student1 =
        RosterStudent.builder()
            .studentId("banana")
            .githubLogin("division7")
            .githubId(123456)
            .course(course)
            .build();
    RosterStudent student2 =
        RosterStudent.builder()
            .studentId("apple")
            .githubLogin("division8")
            .githubId(123457)
            .course(course)
            .build();
    doReturn(orgMembers).when(organizationMemberService).getOrganizationMembers(any(Course.class));
    doReturn(Optional.of(student1))
        .when(rosterStudentRepository)
        .findByCourseAndGithubId(any(Course.class), eq(123456));
    doReturn(Optional.of(student2))
        .when(rosterStudentRepository)
        .findByCourseAndGithubId(any(Course.class), eq(123457));

    var matchJob =
        spy(
            UpdateOrgMembershipJob.builder()
                .rosterStudentRepository(rosterStudentRepository)
                .organizationMemberService(organizationMemberService)
                .course(course)
                .build());

    matchJob.accept(ctx);
    String expected = """
                Processing...
                Done""";
    assertEquals(expected, jobStarted.getLog());

    ArgumentCaptor<RosterStudent> captor = ArgumentCaptor.forClass(RosterStudent.class);
    verify(rosterStudentRepository, times(2)).save(captor.capture());
    List<RosterStudent> savedStudents = captor.getAllValues();
    assertThat(savedStudents.get(0).getOrgStatus()).isEqualTo(OrgStatus.MEMBER);
    assertThat(savedStudents.get(0).getGithubId()).isEqualTo(123456);
    assertThat(savedStudents.get(0).getStudentId()).isEqualTo("banana");

    assertThat(savedStudents.get(1).getOrgStatus()).isEqualTo(OrgStatus.MEMBER);
    assertThat(savedStudents.get(1).getGithubId()).isEqualTo(123457);
    assertThat(savedStudents.get(1).getStudentId()).isEqualTo("apple");
  }

  @Test
  public void no_roster_student() throws Exception {
    OrgMember orgMember1 = OrgMember.builder().githubId(123456).githubLogin("division7").build();
    List<OrgMember> orgMembers = List.of(orgMember1);
    Course course = Course.builder().orgName("ucsb-cs156").installationId("1234").build();

    doReturn(orgMembers).when(organizationMemberService).getOrganizationMembers(any(Course.class));
    doReturn(Optional.empty())
        .when(rosterStudentRepository)
        .findByCourseAndGithubId(any(Course.class), eq(123456));

    var matchJob =
        spy(
            UpdateOrgMembershipJob.builder()
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
