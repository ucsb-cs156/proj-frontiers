package edu.ucsb.cs156.frontiers.jobs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.entities.RosterStudent;
import edu.ucsb.cs156.frontiers.enums.OrgStatus;
import edu.ucsb.cs156.frontiers.models.OrgMember;
import edu.ucsb.cs156.frontiers.repositories.RosterStudentRepository;
import edu.ucsb.cs156.frontiers.repositories.UserRepository;
import edu.ucsb.cs156.frontiers.services.OrganizationMemberService;
import edu.ucsb.cs156.jobs.entities.Job;
import edu.ucsb.cs156.jobs.errors.JobCancelledException;
import edu.ucsb.cs156.jobs.repositories.JobsRepository;
import edu.ucsb.cs156.jobs.services.JobContext;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
  public void test_getScope_returnsCourseScope() {
    Course course = Course.builder().id(1L).courseName("Test Course").build();

    UpdateOrgMembershipJob job = UpdateOrgMembershipJob.builder().course(course).build();

    assertEquals("course", job.getScopeType());
    assertEquals(course.getId(), job.getScopeId());
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
    RosterStudent student1Updated =
        RosterStudent.builder()
            .studentId("banana")
            .githubLogin("division7")
            .githubId(123456)
            .course(course)
            .orgStatus(OrgStatus.MEMBER)
            .build();
    RosterStudent student2Updated =
        RosterStudent.builder()
            .studentId("apple")
            .githubLogin("division8")
            .githubId(123457)
            .course(course)
            .orgStatus(OrgStatus.MEMBER)
            .build();

    doReturn(orgMembers).when(organizationMemberService).getOrganizationMembers(eq(course));
    doReturn(Optional.of(student1))
        .when(rosterStudentRepository)
        .findByCourseAndGithubId(eq(course), eq(123456));
    doReturn(Optional.of(student2))
        .when(rosterStudentRepository)
        .findByCourseAndGithubId(eq(course), eq(123457));

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

    verify(rosterStudentRepository, times(1)).save(eq(student1Updated));
    verify(rosterStudentRepository, times(1)).save(eq(student2Updated));
  }

  @Test
  public void no_roster_student() throws Exception {
    OrgMember orgMember1 = OrgMember.builder().githubId(123456).githubLogin("division7").build();
    List<OrgMember> orgMembers = List.of(orgMember1);
    Course course = Course.builder().orgName("ucsb-cs156").installationId("1234").build();

    doReturn(orgMembers).when(organizationMemberService).getOrganizationMembers(eq(course));
    doReturn(Optional.empty())
        .when(rosterStudentRepository)
        .findByCourseAndGithubId(eq(course), eq(123456));

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

  // This loop never calls ctx.log() -- only the opening "Processing..." and closing "Done"
  // lines do. Without its own checkCancellation() checkpoint, it would give cancellation no
  // opportunity to fire no matter how many members it processes. 1 real checkpoint precedes the
  // loop's own check: accept()'s opening "Processing..." log line.
  @Test
  public void checkCancellation_stops_the_member_loop_before_calling_findByCourseAndGithubId()
      throws Exception {
    OrgMember orgMember1 = OrgMember.builder().githubId(123456).githubLogin("division7").build();
    List<OrgMember> orgMembers = List.of(orgMember1);
    Course course = Course.builder().orgName("ucsb-cs156").installationId("1234").build();
    doReturn(orgMembers).when(organizationMemberService).getOrganizationMembers(eq(course));

    JobsRepository jobsRepository = mock(JobsRepository.class);
    Job runningJob = Job.builder().id(99L).status("running").build();
    Job cancellingJob = Job.builder().id(99L).status("cancelling").build();
    when(jobsRepository.findById(99L))
        .thenReturn(Optional.of(runningJob), Optional.of(cancellingJob));
    Job job = Job.builder().id(99L).build();
    JobContext cancellingCtx = new JobContext(null, job, null, jobsRepository);

    var matchJob =
        UpdateOrgMembershipJob.builder()
            .rosterStudentRepository(rosterStudentRepository)
            .organizationMemberService(organizationMemberService)
            .course(course)
            .build();

    assertThrows(JobCancelledException.class, () -> matchJob.accept(cancellingCtx));

    verify(rosterStudentRepository, never()).findByCourseAndGithubId(any(), anyInt());
  }
}
