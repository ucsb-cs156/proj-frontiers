package edu.ucsb.cs156.frontiers.jobs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import edu.ucsb.cs156.frontiers.services.RepositoryService.RepositoryCreationResult;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

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
    return expectedLog(isPrivate, creationOption, "", 0, 0);
  }

  private String expectedLog(
      Boolean isPrivate,
      RepositoryCreationOption creationOption,
      String repoLines,
      int created,
      int updated) {
    return """
        repositoryPrefix=repo-prefix
        isPrivate=%s
        permissions=WRITE
        creationOption=%s
        %sSummary:
        %4d repos created
        %4d repos updated
        %4d repos total
        Done"""
        .formatted(isPrivate, creationOption, repoLines, created, updated, created + updated);
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

    when(service.createStudentRepository(
            eq(course), eq(student), eq("repo-prefix"), eq(false), eq(RepositoryPermissions.WRITE)))
        .thenReturn(Optional.of(new RepositoryCreationResult("repo-prefix-studentLogin", true)));

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
    String expected =
        expectedLog(
            false,
            RepositoryCreationOption.STUDENTS_ONLY,
            " created repo repo-prefix-studentLogin\n",
            1,
            0);
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

    when(service.createStudentRepository(
            eq(course), eq(student), eq("repo-prefix"), eq(true), eq(RepositoryPermissions.WRITE)))
        .thenReturn(Optional.of(new RepositoryCreationResult("repo-prefix-studentLogin", false)));

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
    String expected =
        expectedLog(
            true,
            RepositoryCreationOption.STUDENTS_ONLY,
            "  updated repo repo-prefix-studentLogin\n",
            0,
            1);
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

    when(service.createStaffRepository(
            eq(course), eq(staff), eq("repo-prefix"), eq(false), eq(RepositoryPermissions.WRITE)))
        .thenReturn(Optional.of(new RepositoryCreationResult("repo-prefix-staffLogin", true)));

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

    String expected =
        expectedLog(
            false,
            RepositoryCreationOption.STAFF_ONLY,
            " created repo repo-prefix-staffLogin\n",
            1,
            0);
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

    when(service.createStudentRepository(
            eq(course), eq(student), eq("repo-prefix"), eq(true), eq(RepositoryPermissions.WRITE)))
        .thenReturn(Optional.of(new RepositoryCreationResult("repo-prefix-studentLogin", true)));
    when(service.createStaffRepository(
            eq(course), eq(staff), eq("repo-prefix"), eq(true), eq(RepositoryPermissions.WRITE)))
        .thenReturn(Optional.of(new RepositoryCreationResult("repo-prefix-staffLogin", false)));

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

    String expected =
        expectedLog(
            true,
            RepositoryCreationOption.STUDENTS_AND_STAFF,
            " created repo repo-prefix-studentLogin\n  updated repo repo-prefix-staffLogin\n",
            1,
            1);
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

    when(service.createStaffRepository(
            eq(course), eq(staff), eq("repo-prefix"), eq(false), eq(RepositoryPermissions.WRITE)))
        .thenReturn(Optional.of(new RepositoryCreationResult("repo-prefix-staffOwner", false)));

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

    String expected =
        expectedLog(
            false,
            RepositoryCreationOption.STAFF_ONLY,
            "  updated repo repo-prefix-staffOwner\n",
            0,
            1);
    assertEquals(expected, jobStarted.getLog());

    verify(service, times(1))
        .createStaffRepository(
            eq(course),
            eq(staff),
            contains("repo-prefix"),
            eq(false),
            eq(RepositoryPermissions.WRITE));
  }

  @Test
  public void testCreateStaffRepository_emptyResultIsNotCountedOrLogged() throws Exception {
    Course course = Course.builder().orgName("ucsb-cs156").installationId("1234").build();

    CourseStaff staff =
        CourseStaff.builder().githubLogin("staffLogin").orgStatus(OrgStatus.MEMBER).build();
    course.setCourseStaff(List.of(staff));

    when(service.createStaffRepository(
            eq(course), eq(staff), eq("repo-prefix"), eq(false), eq(RepositoryPermissions.WRITE)))
        .thenReturn(Optional.empty());

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

  private static final String SIGNED_PARAMS =
      """
      repositoryPrefix=repo-prefix
      isPrivate=false
      permissions=WRITE
      creationOption=%s
      """;

  private CreateStudentOrStaffRepositoriesJob.CreateStudentOrStaffRepositoriesJobBuilder
      signedCommitsJob(
          Course course, RepositoryCreationOption creationOption, Boolean requireSignedCommit) {
    return CreateStudentOrStaffRepositoriesJob.builder()
        .repositoryService(service)
        .repositoryPrefix("repo-prefix")
        .course(course)
        .isPrivate(false)
        .permissions(RepositoryPermissions.WRITE)
        .creationOption(creationOption)
        .requireSignedCommit(requireSignedCommit);
  }

  /**
   * A course with one student and one staff member, whose repositories were created and updated.
   */
  private Course courseWithStudentAndStaff() throws Exception {
    Course course = Course.builder().orgName("ucsb-cs156").installationId("1234").build();
    RosterStudent student =
        RosterStudent.builder().githubLogin("studentLogin").orgStatus(OrgStatus.MEMBER).build();
    CourseStaff staff =
        CourseStaff.builder().githubLogin("staffLogin").orgStatus(OrgStatus.OWNER).build();
    course.setRosterStudents(List.of(student));
    course.setCourseStaff(List.of(staff));
    lenient()
        .when(
            service.createStudentRepository(
                eq(course), eq(student), any(), any(), eq(RepositoryPermissions.WRITE)))
        .thenReturn(Optional.of(new RepositoryCreationResult("repo-prefix-studentLogin", true)));
    when(service.createStaffRepository(
            eq(course), eq(staff), any(), any(), eq(RepositoryPermissions.WRITE)))
        .thenReturn(Optional.of(new RepositoryCreationResult("repo-prefix-staffLogin", false)));
    return course;
  }

  @Test
  public void requireSignedCommit_true_is_logged_and_applied_to_student_and_staff_repos()
      throws Exception {
    Course course = courseWithStudentAndStaff();

    signedCommitsJob(course, RepositoryCreationOption.STUDENTS_AND_STAFF, true).build().accept(ctx);

    String expected =
        (SIGNED_PARAMS.formatted(RepositoryCreationOption.STUDENTS_AND_STAFF)
            + """
            requireSignedCommit=true
             created repo repo-prefix-studentLogin
              updated repo repo-prefix-staffLogin
            Summary:
               1 repos created
               1 repos updated
               2 repos total
            Done""");
    assertEquals(expected, jobStarted.getLog());
    verify(service, times(1))
        .setSignedCommitsRequired(eq(course), eq("repo-prefix-studentLogin"), eq(true));
    verify(service, times(1))
        .setSignedCommitsRequired(eq(course), eq("repo-prefix-staffLogin"), eq(true));
  }

  @Test
  public void requireSignedCommit_false_is_logged_and_clears_the_rule() throws Exception {
    Course course = courseWithStudentAndStaff();

    signedCommitsJob(course, RepositoryCreationOption.STUDENTS_AND_STAFF, false)
        .build()
        .accept(ctx);

    assertTrue(jobStarted.getLog().contains("\nrequireSignedCommit=false\n"));
    verify(service, times(1))
        .setSignedCommitsRequired(eq(course), eq("repo-prefix-studentLogin"), eq(false));
    verify(service, times(1))
        .setSignedCommitsRequired(eq(course), eq("repo-prefix-staffLogin"), eq(false));
  }

  @Test
  public void requireSignedCommit_only_applies_to_the_kind_of_repos_that_are_created()
      throws Exception {
    Course course = courseWithStudentAndStaff();

    signedCommitsJob(course, RepositoryCreationOption.STAFF_ONLY, true).build().accept(ctx);

    verify(service, times(1))
        .setSignedCommitsRequired(eq(course), eq("repo-prefix-staffLogin"), eq(true));
    verify(service, never())
        .setSignedCommitsRequired(eq(course), eq("repo-prefix-studentLogin"), anyBoolean());
  }

  @Test
  public void requireSignedCommit_null_leaves_the_rulesets_alone_and_is_not_logged()
      throws Exception {
    Course course = courseWithStudentAndStaff();

    signedCommitsJob(course, RepositoryCreationOption.STUDENTS_AND_STAFF, null).build().accept(ctx);

    assertFalse(jobStarted.getLog().contains("requireSignedCommit"));
    verify(service, never()).setSignedCommitsRequired(any(), any(), anyBoolean());
  }

  @Test
  public void a_repo_that_github_refuses_the_rule_for_is_logged_and_the_job_carries_on()
      throws Exception {
    Course course = courseWithStudentAndStaff();
    doThrow(
            HttpClientErrorException.create(
                HttpStatus.FORBIDDEN,
                "Forbidden",
                HttpHeaders.EMPTY,
                "{\"message\":\"Resource not accessible by integration\"}".getBytes(),
                null))
        .when(service)
        .setSignedCommitsRequired(eq(course), eq("repo-prefix-studentLogin"), eq(true));

    signedCommitsJob(course, RepositoryCreationOption.STUDENTS_AND_STAFF, true).build().accept(ctx);

    String expected =
        (SIGNED_PARAMS.formatted(RepositoryCreationOption.STUDENTS_AND_STAFF)
            + """
            requireSignedCommit=true
             created repo repo-prefix-studentLogin
              could not require signed commits on repo-prefix-studentLogin: 403 FORBIDDEN {"message":"Resource not accessible by integration"}
              updated repo repo-prefix-staffLogin
            Summary:
               1 repos created
               1 repos updated
               2 repos total
               1 repos where signed commits could not be set
            Done""");
    assertEquals(expected, jobStarted.getLog());
    verify(service, times(1))
        .setSignedCommitsRequired(eq(course), eq("repo-prefix-staffLogin"), eq(true));
  }

  @Test
  public void refusals_to_clear_the_rule_on_staff_repos_are_counted_too() throws Exception {
    Course course = courseWithStudentAndStaff();
    lenient()
        .doThrow(
            HttpClientErrorException.create(
                HttpStatus.NOT_FOUND, "Not Found", HttpHeaders.EMPTY, new byte[0], null))
        .when(service)
        .setSignedCommitsRequired(eq(course), eq("repo-prefix-staffLogin"), eq(false));

    signedCommitsJob(course, RepositoryCreationOption.STUDENTS_AND_STAFF, false)
        .build()
        .accept(ctx);

    assertTrue(
        jobStarted
            .getLog()
            .contains(
                "  could not stop requiring signed commits on repo-prefix-staffLogin: 404 NOT_FOUND \n"));
    assertTrue(jobStarted.getLog().contains("   1 repos where signed commits could not be set\n"));
  }

  @Test
  public void a_repo_whose_creation_failed_gets_no_signed_commits_call() throws Exception {
    Course course = Course.builder().orgName("ucsb-cs156").installationId("1234").build();
    RosterStudent student =
        RosterStudent.builder().githubLogin("studentLogin").orgStatus(OrgStatus.MEMBER).build();
    course.setRosterStudents(List.of(student));
    when(service.createStudentRepository(eq(course), eq(student), any(), any(), any()))
        .thenReturn(Optional.empty());

    signedCommitsJob(course, RepositoryCreationOption.STUDENTS_ONLY, true).build().accept(ctx);

    verify(service, never()).setSignedCommitsRequired(any(), any(), anyBoolean());
    assertFalse(jobStarted.getLog().contains("could not"));
  }
}
