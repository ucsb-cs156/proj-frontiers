package edu.ucsb.cs156.frontiers.jobs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.entities.RosterStudent;
import edu.ucsb.cs156.frontiers.enums.OrgStatus;
import edu.ucsb.cs156.frontiers.repositories.RosterStudentRepository;
import edu.ucsb.cs156.frontiers.services.OrganizationMemberService;
import edu.ucsb.cs156.jobs.entities.Job;
import edu.ucsb.cs156.jobs.errors.JobCancelledException;
import edu.ucsb.cs156.jobs.repositories.JobsRepository;
import edu.ucsb.cs156.jobs.services.JobContext;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

public class RemoveStudentsJobTests {

  @Mock private OrganizationMemberService organizationMemberService;
  @Mock private JobContext jobContext;
  @Mock private RosterStudentRepository rosterStudentRepository;

  private RemoveStudentsJob removeStudentsJob;
  private List<RosterStudent> validStudents;

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  public void test_getScope_returnsUnscoped() {
    RemoveStudentsJob job = RemoveStudentsJob.builder().build();

    assertNull(job.getScopeType());
    assertNull(job.getScopeId());
  }

  @Test
  public void testAccept_validStudents_callsRemoveOrganizationMember() throws Exception {
    Course course = Course.builder().orgName("testOrg").installationId("123456").build();
    RosterStudent student1 =
        RosterStudent.builder()
            .course(course)
            .githubLogin("testLogin1")
            .githubId(123545)
            .orgStatus(OrgStatus.MEMBER)
            .build();
    RosterStudent student2 =
        RosterStudent.builder()
            .course(course)
            .githubLogin("testLogin2")
            .orgStatus(OrgStatus.OWNER)
            .githubId(123456)
            .build();

    RosterStudent student1Updated =
        RosterStudent.builder().course(course).orgStatus(OrgStatus.REMOVED).build();
    RosterStudent student2Updated =
        RosterStudent.builder().course(course).orgStatus(OrgStatus.REMOVED).build();

    validStudents = List.of(student1, student2);

    removeStudentsJob =
        RemoveStudentsJob.builder()
            .organizationMemberService(organizationMemberService)
            .rosterStudentRepository(rosterStudentRepository)
            .students(validStudents)
            .build();

    // Act
    removeStudentsJob.accept(jobContext);

    // Assert
    verify(organizationMemberService, times(2)).removeOrganizationMember(any(RosterStudent.class));
    verify(rosterStudentRepository, times(2)).save(any(RosterStudent.class));
    verify(rosterStudentRepository, atLeastOnce()).save(student1Updated);
    verify(rosterStudentRepository, atLeastOnce()).save(student2Updated);
    verify(jobContext).log("Removed student testLogin1 from Organization");
    verify(jobContext).log("Removed student testLogin2 from Organization");
  }

  @Test
  public void not_in_organization_handled_correctly() throws Exception {
    Course course = Course.builder().orgName("testOrg").installationId("123456").build();
    RosterStudent student1 =
        RosterStudent.builder()
            .course(course)
            .githubLogin("testLogin1")
            .githubId(123545)
            .orgStatus(OrgStatus.MEMBER)
            .build();
    RosterStudent student1Updated =
        RosterStudent.builder().course(course).orgStatus(OrgStatus.REMOVED).build();

    removeStudentsJob =
        RemoveStudentsJob.builder()
            .organizationMemberService(organizationMemberService)
            .rosterStudentRepository(rosterStudentRepository)
            .students(List.of(student1))
            .build();

    doThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND))
        .when(organizationMemberService)
        .removeOrganizationMember(eq(student1));
    removeStudentsJob.accept(jobContext);
    verify(organizationMemberService, times(1)).removeOrganizationMember(eq(student1));
    verify(rosterStudentRepository, times(1)).save(student1Updated);
    verify(jobContext).log("Student testLogin1 not in Organization");
  }

  @Test
  public void spammed_stops_job() throws Exception {
    Course course = Course.builder().orgName("testOrg").installationId("123456").build();
    RosterStudent student1 =
        RosterStudent.builder()
            .course(course)
            .githubLogin("testLogin1")
            .githubId(123545)
            .orgStatus(OrgStatus.MEMBER)
            .build();
    RosterStudent student1Updated =
        RosterStudent.builder().course(course).orgStatus(OrgStatus.REMOVED).build();

    removeStudentsJob =
        RemoveStudentsJob.builder()
            .organizationMemberService(organizationMemberService)
            .rosterStudentRepository(rosterStudentRepository)
            .students(List.of(student1))
            .build();

    doThrow(new HttpClientErrorException(HttpStatus.TOO_MANY_REQUESTS))
        .when(organizationMemberService)
        .removeOrganizationMember(eq(student1));
    IllegalStateException e =
        assertThrows(IllegalStateException.class, () -> removeStudentsJob.accept(jobContext));
    verify(organizationMemberService, times(1)).removeOrganizationMember(eq(student1));
    verify(rosterStudentRepository, never()).save(student1Updated);
    assertEquals(
        "Failed to remove student testLogin1 from Organization: 429 TOO_MANY_REQUESTS",
        e.getMessage());
  }

  @Test
  public void testAccept_incompleteCourseData_doesNotCallRemoveOrganizationMember()
      throws Exception {
    // Arrange
    Course unlinkedCourse = Course.builder().orgName(null).installationId(null).build();
    RosterStudent unlinkedCourseStudent1 =
        RosterStudent.builder()
            .course(unlinkedCourse)
            .githubLogin("testLogin")
            .githubId(123456)
            .build();
    Course incompleteCourse = Course.builder().orgName("org-1").installationId(null).build();
    RosterStudent unlinkedCourseStudent2 =
        RosterStudent.builder()
            .course(incompleteCourse)
            .githubLogin("testLogin")
            .githubId(123456)
            .build();
    removeStudentsJob =
        RemoveStudentsJob.builder()
            .organizationMemberService(organizationMemberService)
            .rosterStudentRepository(rosterStudentRepository)
            .students(List.of(unlinkedCourseStudent1, unlinkedCourseStudent2))
            .build();

    // Act
    removeStudentsJob.accept(jobContext);

    // Assert
    verify(organizationMemberService, never()).removeOrganizationMember(any(RosterStudent.class));
    verify(rosterStudentRepository, never()).save(any(RosterStudent.class));
  }

  @Test
  public void testAccept_incompleteGithubData_doesNotCallRemoveOrganizationMember()
      throws Exception {
    // Arrange
    Course course = Course.builder().orgName("testOrg").installationId("123456").build();
    RosterStudent studentNoGithubData =
        RosterStudent.builder().course(course).githubLogin(null).githubId(null).build();
    RosterStudent studentNoGithubId =
        RosterStudent.builder().course(course).githubLogin("fakeusername").githubId(null).build();
    removeStudentsJob =
        RemoveStudentsJob.builder()
            .organizationMemberService(organizationMemberService)
            .rosterStudentRepository(rosterStudentRepository)
            .students(List.of(studentNoGithubData, studentNoGithubId))
            .build();

    // Act
    removeStudentsJob.accept(jobContext);

    // Assert
    verify(organizationMemberService, never()).removeOrganizationMember(any(RosterStudent.class));
    verify(rosterStudentRepository, never()).save(any(RosterStudent.class));
  }

  // A student whose course has no linked org, or who has no GitHub login/id, never calls
  // c.log() -- without its own checkCancellation() checkpoint, this loop would give
  // cancellation no opportunity to fire no matter how many students it skips. This test uses a
  // real JobContext (not the mocked jobContext field above, which can't observe the checkpoint)
  // backed by a JobsRepository that reports "cancelling" immediately -- the loop's own check is
  // the very first checkCancellation-consuming call, since accept() never logs before it.
  @Test
  public void checkCancellation_stops_the_student_loop_before_calling_removeOrganizationMember()
      throws Exception {
    Course course = Course.builder().orgName("testOrg").installationId("123456").build();
    RosterStudent student1 =
        RosterStudent.builder()
            .course(course)
            .githubLogin("testLogin1")
            .githubId(123545)
            .orgStatus(OrgStatus.MEMBER)
            .build();

    removeStudentsJob =
        RemoveStudentsJob.builder()
            .organizationMemberService(organizationMemberService)
            .rosterStudentRepository(rosterStudentRepository)
            .students(List.of(student1))
            .build();

    JobsRepository jobsRepository = mock(JobsRepository.class);
    Job cancellingJob = Job.builder().id(99L).status("cancelling").build();
    when(jobsRepository.findById(99L)).thenReturn(Optional.of(cancellingJob));
    Job job = Job.builder().id(99L).build();
    JobContext cancellingCtx = new JobContext(null, job, null, jobsRepository);

    assertThrows(JobCancelledException.class, () -> removeStudentsJob.accept(cancellingCtx));

    verify(organizationMemberService, never()).removeOrganizationMember(any(RosterStudent.class));
  }
}
