package edu.ucsb.cs156.frontiers.jobs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.entities.CourseStaff;
import edu.ucsb.cs156.frontiers.entities.RosterStudent;
import edu.ucsb.cs156.frontiers.enums.OrgStatus;
import edu.ucsb.cs156.frontiers.enums.RepositoryCreationOption;
import edu.ucsb.cs156.frontiers.enums.RepositoryPermissions;
import edu.ucsb.cs156.frontiers.services.RepositoryService;
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
public class CreateStudentOrStaffRepositoriesJobTest {

  @Mock private RepositoryService service;

  Job jobStarted = Job.builder().build();
  JobContext ctx = new JobContext(null, jobStarted);

  @BeforeEach
  public void setup() {
    MockitoAnnotations.openMocks(this);
  }

  private String expectedLog(Boolean isPrivate, RepositoryCreationOption creationOption) {
    return """
        repositoryPrefix=repo-prefix
        isPrivate=%s
        permissions=WRITE
        creationOption=%s
        Done"""
        .formatted(isPrivate, creationOption);
  }

  @Test
  public void test_getScope_returnsCourseScope() {
    Course course = Course.builder().id(1L).courseName("Test Course").build();

    CreateStudentOrStaffRepositoriesJob job =
        CreateStudentOrStaffRepositoriesJob.builder().course(course).build();

    assertEquals("course", job.getScopeType());
    assertEquals(course.getId(), job.getScopeId());
  }

  @Test
  public void testCreateStudentRepository_public() throws Exception {
    Course course = Course.builder().orgName("ucsb-cs156").installationId("1234").build();
    RosterStudent student =
        RosterStudent.builder().githubLogin("studentLogin").orgStatus(OrgStatus.MEMBER).build();
    course.setRosterStudents(List.of(student));

    var repoJob =
        spy(
            CreateStudentOrStaffRepositoriesJob.builder()
                .repositoryService(service)
                .repositoryPrefix("repo-prefix")
                .course(course)
                .isPrivate(false)
                .permissions(RepositoryPermissions.WRITE)
                .build());

    repoJob.accept(ctx);
    String expected = expectedLog(false, RepositoryCreationOption.STUDENTS_ONLY);
    assertEquals(expected, jobStarted.getLog());

    verify(service, times(1))
        .createStudentRepository(
            eq(course),
            eq(student),
            contains("repo-prefix"),
            eq(false),
            eq(RepositoryPermissions.WRITE));
  }

  @Test
  public void testCreateStudentRepository_private() throws Exception {
    Course course = Course.builder().orgName("ucsb-cs156").installationId("1234").build();
    RosterStudent student =
        RosterStudent.builder().githubLogin("studentLogin").orgStatus(OrgStatus.MEMBER).build();
    course.setRosterStudents(List.of(student));

    var repoJob =
        spy(
            CreateStudentOrStaffRepositoriesJob.builder()
                .repositoryService(service)
                .repositoryPrefix("repo-prefix")
                .course(course)
                .isPrivate(true)
                .permissions(RepositoryPermissions.WRITE)
                .build());

    repoJob.accept(ctx);
    String expected = expectedLog(true, RepositoryCreationOption.STUDENTS_ONLY);
    assertEquals(expected, jobStarted.getLog());

    verify(service, times(1))
        .createStudentRepository(
            eq(course),
            eq(student),
            contains("repo-prefix"),
            eq(true),
            eq(RepositoryPermissions.WRITE));
  }

  @Test
  public void testCreateStudentRepository_owner() throws Exception {
    Course course = Course.builder().orgName("ucsb-cs156").installationId("1234").build();
    RosterStudent student =
        RosterStudent.builder().githubLogin("studentLogin").orgStatus(OrgStatus.OWNER).build();
    course.setRosterStudents(List.of(student));

    var repoJob =
        spy(
            CreateStudentOrStaffRepositoriesJob.builder()
                .repositoryService(service)
                .repositoryPrefix("repo-prefix")
                .course(course)
                .isPrivate(true)
                .permissions(RepositoryPermissions.WRITE)
                .build());

    repoJob.accept(ctx);
    String expected = expectedLog(true, RepositoryCreationOption.STUDENTS_ONLY);
    assertEquals(expected, jobStarted.getLog());

    verify(service, times(1))
        .createStudentRepository(
            eq(course),
            eq(student),
            contains("repo-prefix"),
            eq(true),
            eq(RepositoryPermissions.WRITE));
  }

  @Test
  public void expectDoesntCallForNoLogin() throws Exception {
    Course course = Course.builder().orgName("ucsb-cs156").installationId("1234").build();
    RosterStudent student = RosterStudent.builder().build();
    course.setRosterStudents(List.of(student));

    var repoJob =
        spy(
            CreateStudentOrStaffRepositoriesJob.builder()
                .repositoryService(service)
                .repositoryPrefix("repo-prefix")
                .isPrivate(false)
                .course(course)
                .permissions(RepositoryPermissions.WRITE)
                .build());

    repoJob.accept(ctx);
    String expected = expectedLog(false, RepositoryCreationOption.STUDENTS_ONLY);
    assertEquals(expected, jobStarted.getLog());

    verify(service, times(0)).createStudentRepository(any(), any(), any(), any(), any());
  }

  @Test
  public void expectDoesntCallForNotMember() throws Exception {
    Course course = Course.builder().orgName("ucsb-cs156").installationId("1234").build();
    RosterStudent student =
        RosterStudent.builder().githubLogin("banana").orgStatus(OrgStatus.PENDING).build();
    course.setRosterStudents(List.of(student));
    var repoJob =
        spy(
            CreateStudentOrStaffRepositoriesJob.builder()
                .repositoryService(service)
                .repositoryPrefix("repo-prefix")
                .isPrivate(false)
                .course(course)
                .permissions(RepositoryPermissions.WRITE)
                .build());

    repoJob.accept(ctx);
    String expected = expectedLog(false, RepositoryCreationOption.STUDENTS_ONLY);
    assertEquals(expected, jobStarted.getLog());

    verify(service, times(0)).createStudentRepository(any(), any(), any(), any(), any());
  }

  @Test
  public void testCreateStaffRepository_staffOnly() throws Exception {
    Course course = Course.builder().orgName("ucsb-cs156").installationId("1234").build();

    RosterStudent student =
        RosterStudent.builder().githubLogin("studentLogin").orgStatus(OrgStatus.MEMBER).build();
    course.setRosterStudents(List.of(student));

    CourseStaff staff =
        CourseStaff.builder().githubLogin("staffLogin").orgStatus(OrgStatus.MEMBER).build();
    course.setCourseStaff(List.of(staff));

    var repoJob =
        spy(
            CreateStudentOrStaffRepositoriesJob.builder()
                .repositoryService(service)
                .repositoryPrefix("repo-prefix")
                .course(course)
                .isPrivate(false)
                .permissions(RepositoryPermissions.WRITE)
                .creationOption(RepositoryCreationOption.STAFF_ONLY)
                .build());

    repoJob.accept(ctx);

    String expected = expectedLog(false, RepositoryCreationOption.STAFF_ONLY);
    assertEquals(expected, jobStarted.getLog());

    verify(service, times(0)).createStudentRepository(any(), any(), any(), any(), any());

    verify(service, times(1))
        .createStaffRepository(
            eq(course),
            eq(staff),
            contains("repo-prefix"),
            eq(false),
            eq(RepositoryPermissions.WRITE));
  }

  @Test
  public void testCreateStudentAndStaffRepository_both() throws Exception {
    Course course = Course.builder().orgName("ucsb-cs156").installationId("1234").build();

    RosterStudent student =
        RosterStudent.builder().githubLogin("studentLogin").orgStatus(OrgStatus.MEMBER).build();
    course.setRosterStudents(List.of(student));

    CourseStaff staff =
        CourseStaff.builder().githubLogin("staffLogin").orgStatus(OrgStatus.MEMBER).build();
    course.setCourseStaff(List.of(staff));

    var repoJob =
        spy(
            CreateStudentOrStaffRepositoriesJob.builder()
                .repositoryService(service)
                .repositoryPrefix("repo-prefix")
                .course(course)
                .isPrivate(true)
                .permissions(RepositoryPermissions.WRITE)
                .creationOption(RepositoryCreationOption.STUDENTS_AND_STAFF)
                .build());

    repoJob.accept(ctx);

    String expected = expectedLog(true, RepositoryCreationOption.STUDENTS_AND_STAFF);
    assertEquals(expected, jobStarted.getLog());

    verify(service, times(1))
        .createStudentRepository(
            eq(course),
            eq(student),
            contains("repo-prefix"),
            eq(true),
            eq(RepositoryPermissions.WRITE));

    verify(service, times(1))
        .createStaffRepository(
            eq(course),
            eq(staff),
            contains("repo-prefix"),
            eq(true),
            eq(RepositoryPermissions.WRITE));
  }

  @Test
  public void expectDoesntCallForStaffNoLogin() throws Exception {
    Course course = Course.builder().orgName("ucsb-cs156").installationId("1234").build();

    CourseStaff staff = CourseStaff.builder().orgStatus(OrgStatus.MEMBER).build(); // no githubLogin
    course.setCourseStaff(List.of(staff));

    var repoJob =
        spy(
            CreateStudentOrStaffRepositoriesJob.builder()
                .repositoryService(service)
                .repositoryPrefix("repo-prefix")
                .isPrivate(false)
                .course(course)
                .permissions(RepositoryPermissions.WRITE)
                .creationOption(RepositoryCreationOption.STAFF_ONLY)
                .build());

    repoJob.accept(ctx);

    String expected = expectedLog(false, RepositoryCreationOption.STAFF_ONLY);
    assertEquals(expected, jobStarted.getLog());

    verify(service, times(0)).createStaffRepository(any(), any(), any(), any(), any());
  }

  @Test
  public void expectDoesntCallForNotStaffMember() throws Exception {
    Course course = Course.builder().orgName("ucsb-cs156").installationId("1234").build();

    CourseStaff staff =
        CourseStaff.builder()
            .githubLogin("staffLogin")
            .orgStatus(OrgStatus.PENDING) // not MEMBER or OWNER
            .build();
    course.setCourseStaff(List.of(staff));

    var repoJob =
        spy(
            CreateStudentOrStaffRepositoriesJob.builder()
                .repositoryService(service)
                .repositoryPrefix("repo-prefix")
                .isPrivate(false)
                .course(course)
                .permissions(RepositoryPermissions.WRITE)
                .creationOption(RepositoryCreationOption.STAFF_ONLY)
                .build());

    repoJob.accept(ctx);

    String expected = expectedLog(false, RepositoryCreationOption.STAFF_ONLY);
    assertEquals(expected, jobStarted.getLog());

    verify(service, times(0)).createStaffRepository(any(), any(), any(), any(), any());
  }

  @Test
  public void testCreateStaffRepository_owner() throws Exception {
    Course course = Course.builder().orgName("ucsb-cs156").installationId("1234").build();

    CourseStaff staff =
        CourseStaff.builder().githubLogin("staffOwner").orgStatus(OrgStatus.OWNER).build();

    course.setCourseStaff(List.of(staff));

    var repoJob =
        spy(
            CreateStudentOrStaffRepositoriesJob.builder()
                .repositoryService(service)
                .repositoryPrefix("repo-prefix")
                .course(course)
                .isPrivate(false)
                .permissions(RepositoryPermissions.WRITE)
                .creationOption(RepositoryCreationOption.STAFF_ONLY)
                .build());

    repoJob.accept(ctx);

    String expected = expectedLog(false, RepositoryCreationOption.STAFF_ONLY);
    assertEquals(expected, jobStarted.getLog());

    verify(service, times(1))
        .createStaffRepository(
            eq(course),
            eq(staff),
            contains("repo-prefix"),
            eq(false),
            eq(RepositoryPermissions.WRITE));
  }

  // ────────────────────── checkCancellation checkpoints ──────────────────────
  // A student/staff with no GitHub login, or not yet an org member, never calls
  // repositoryService and never logs -- without their own ctx.checkCancellation() checkpoints,
  // these loops would give cancellation no opportunity to fire no matter how many students or
  // staff they skip over. accept() always logs 4 lines (repositoryPrefix/isPrivate/permissions/
  // creationOption) before either loop, and each ctx.log() call internally checks cancellation
  // too -- so these tests report "running" for exactly those 4 preceding checkpoints, then
  // "cancelling" from the loop's own check onward, and assert both that JobCancelledException is
  // thrown AND that repositoryService was never called -- the second assertion is what actually
  // distinguishes the real code from a mutant that removes the checkpoint, since ctx.log("Done")
  // at the end would otherwise throw the same exception type.

  private static Job runningJob() {
    return Job.builder().id(99L).status("running").build();
  }

  private static Job cancellingJob() {
    return Job.builder().id(99L).status("cancelling").build();
  }

  @Test
  public void checkCancellation_stops_the_student_loop_before_calling_repositoryService()
      throws Exception {
    Course course = Course.builder().orgName("ucsb-cs156").installationId("1234").build();
    RosterStudent student =
        RosterStudent.builder().githubLogin("studentLogin").orgStatus(OrgStatus.MEMBER).build();
    course.setRosterStudents(List.of(student));

    JobsRepository jobsRepository = mock(JobsRepository.class);
    when(jobsRepository.findById(99L))
        .thenReturn(
            Optional.of(runningJob()),
            Optional.of(runningJob()),
            Optional.of(runningJob()),
            Optional.of(runningJob()),
            Optional.of(cancellingJob()));
    Job job = Job.builder().id(99L).build();
    JobContext cancellingCtx = new JobContext(null, job, null, jobsRepository);

    var repoJob =
        CreateStudentOrStaffRepositoriesJob.builder()
            .repositoryService(service)
            .repositoryPrefix("repo-prefix")
            .course(course)
            .isPrivate(false)
            .permissions(RepositoryPermissions.WRITE)
            .creationOption(RepositoryCreationOption.STUDENTS_ONLY)
            .build();

    assertThrows(JobCancelledException.class, () -> repoJob.accept(cancellingCtx));

    verify(service, never()).createStudentRepository(any(), any(), any(), any(), any());
  }

  @Test
  public void checkCancellation_stops_the_staff_loop_before_calling_repositoryService()
      throws Exception {
    Course course = Course.builder().orgName("ucsb-cs156").installationId("1234").build();
    CourseStaff staff =
        CourseStaff.builder().githubLogin("staffLogin").orgStatus(OrgStatus.MEMBER).build();
    course.setCourseStaff(List.of(staff));

    JobsRepository jobsRepository = mock(JobsRepository.class);
    when(jobsRepository.findById(99L))
        .thenReturn(
            Optional.of(runningJob()),
            Optional.of(runningJob()),
            Optional.of(runningJob()),
            Optional.of(runningJob()),
            Optional.of(cancellingJob()));
    Job job = Job.builder().id(99L).build();
    JobContext cancellingCtx = new JobContext(null, job, null, jobsRepository);

    var repoJob =
        CreateStudentOrStaffRepositoriesJob.builder()
            .repositoryService(service)
            .repositoryPrefix("repo-prefix")
            .course(course)
            .isPrivate(false)
            .permissions(RepositoryPermissions.WRITE)
            .creationOption(RepositoryCreationOption.STAFF_ONLY)
            .build();

    assertThrows(JobCancelledException.class, () -> repoJob.accept(cancellingCtx));

    verify(service, never()).createStaffRepository(any(), any(), any(), any(), any());
  }
}
