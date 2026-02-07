package edu.ucsb.cs156.frontiers.jobs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.entities.CourseStaff;
import edu.ucsb.cs156.frontiers.entities.Job;
import edu.ucsb.cs156.frontiers.entities.RosterStudent;
import edu.ucsb.cs156.frontiers.enums.OrgStatus;
import edu.ucsb.cs156.frontiers.enums.RepositoryCreationOption;
import edu.ucsb.cs156.frontiers.enums.RepositoryPermissions;
import edu.ucsb.cs156.frontiers.services.RepositoryService;
import edu.ucsb.cs156.frontiers.services.jobs.JobContext;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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

  @Test
  public void testCreateStudentRepository_public() throws Exception {
    Course course = Course.builder().orgName("ucsb-cs156").installationId("1234").build();
    RosterStudent student =
        RosterStudent.builder().githubLogin("studentLogin").orgStatus(OrgStatus.MEMBER).build();
    course.setRosterStudents(Set.of(student));

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
    String expected = """
        Processing...
        Done""";
    assertEquals(expected, jobStarted.getLog());

    ArgumentCaptor<Course> courseCaptor = ArgumentCaptor.forClass(Course.class);
    ArgumentCaptor<RosterStudent> studentCaptor = ArgumentCaptor.forClass(RosterStudent.class);
    verify(service, times(1))
        .createStudentRepository(
            courseCaptor.capture(),
            studentCaptor.capture(),
            contains("repo-prefix"),
            eq(false),
            eq(RepositoryPermissions.WRITE));
    assertThat(courseCaptor.getValue()).usingRecursiveComparison().isEqualTo(course);
    assertThat(studentCaptor.getValue()).usingRecursiveComparison().isEqualTo(student);
  }

  @Test
  public void testCreateStudentRepository_private() throws Exception {
    Course course = Course.builder().orgName("ucsb-cs156").installationId("1234").build();
    RosterStudent student =
        RosterStudent.builder().githubLogin("studentLogin").orgStatus(OrgStatus.MEMBER).build();
    course.setRosterStudents(Set.of(student));

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
    String expected = """
        Processing...
        Done""";
    assertEquals(expected, jobStarted.getLog());

    ArgumentCaptor<Course> courseCaptor = ArgumentCaptor.forClass(Course.class);
    ArgumentCaptor<RosterStudent> studentCaptor = ArgumentCaptor.forClass(RosterStudent.class);
    verify(service, times(1))
        .createStudentRepository(
            courseCaptor.capture(),
            studentCaptor.capture(),
            contains("repo-prefix"),
            eq(true),
            eq(RepositoryPermissions.WRITE));
    assertThat(courseCaptor.getValue()).usingRecursiveComparison().isEqualTo(course);
    assertThat(studentCaptor.getValue()).usingRecursiveComparison().isEqualTo(student);
  }

  @Test
  public void testCreateStudentRepository_owner() throws Exception {
    Course course = Course.builder().orgName("ucsb-cs156").installationId("1234").build();
    RosterStudent student =
        RosterStudent.builder().githubLogin("studentLogin").orgStatus(OrgStatus.OWNER).build();
    course.setRosterStudents(Set.of(student));

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
    String expected = """
        Processing...
        Done""";
    assertEquals(expected, jobStarted.getLog());

    ArgumentCaptor<Course> courseCaptor = ArgumentCaptor.forClass(Course.class);
    ArgumentCaptor<RosterStudent> studentCaptor = ArgumentCaptor.forClass(RosterStudent.class);
    verify(service, times(1))
        .createStudentRepository(
            courseCaptor.capture(),
            studentCaptor.capture(),
            contains("repo-prefix"),
            eq(true),
            eq(RepositoryPermissions.WRITE));
    assertThat(courseCaptor.getValue()).usingRecursiveComparison().isEqualTo(course);
    assertThat(studentCaptor.getValue()).usingRecursiveComparison().isEqualTo(student);
  }

  @Test
  public void expectDoesntCallForNoLogin() throws Exception {
    Course course = Course.builder().orgName("ucsb-cs156").installationId("1234").build();
    RosterStudent student = RosterStudent.builder().build();
    course.setRosterStudents(Set.of(student));

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
    String expected = """
        Processing...
        Done""";
    assertEquals(expected, jobStarted.getLog());

    verify(service, times(0)).createStudentRepository(any(), any(), any(), any(), any());
  }

  @Test
  public void expectDoesntCallForNotMember() throws Exception {
    Course course = Course.builder().orgName("ucsb-cs156").installationId("1234").build();
    RosterStudent student =
        RosterStudent.builder().githubLogin("banana").orgStatus(OrgStatus.PENDING).build();
    course.setRosterStudents(Set.of(student));
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
    String expected = """
        Processing...
        Done""";
    assertEquals(expected, jobStarted.getLog());

    verify(service, times(0)).createStudentRepository(any(), any(), any(), any(), any());
  }

  @Test
  public void testCreateStaffRepository_staffOnly() throws Exception {
    Course course = Course.builder().orgName("ucsb-cs156").installationId("1234").build();

    RosterStudent student =
        RosterStudent.builder().githubLogin("studentLogin").orgStatus(OrgStatus.MEMBER).build();
    course.setRosterStudents(Set.of(student));

    CourseStaff staff =
        CourseStaff.builder().githubLogin("staffLogin").orgStatus(OrgStatus.MEMBER).build();
    course.setCourseStaff(Set.of(staff));

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

    String expected = """
        Processing...
        Done""";
    assertEquals(expected, jobStarted.getLog());

    verify(service, times(0)).createStudentRepository(any(), any(), any(), any(), any());

    ArgumentCaptor<Course> courseCaptor = ArgumentCaptor.forClass(Course.class);
    ArgumentCaptor<CourseStaff> staffCaptor = ArgumentCaptor.forClass(CourseStaff.class);
    verify(service, times(1))
        .createStaffRepository(
            courseCaptor.capture(),
            staffCaptor.capture(),
            contains("repo-prefix"),
            eq(false),
            eq(RepositoryPermissions.WRITE));
    assertThat(courseCaptor.getValue()).usingRecursiveComparison().isEqualTo(course);
    assertThat(staffCaptor.getValue()).usingRecursiveComparison().isEqualTo(staff);
  }

  @Test
  public void testCreateStudentAndStaffRepository_both() throws Exception {
    Course course = Course.builder().orgName("ucsb-cs156").installationId("1234").build();

    RosterStudent student =
        RosterStudent.builder().githubLogin("studentLogin").orgStatus(OrgStatus.MEMBER).build();
    course.setRosterStudents(Set.of(student));

    CourseStaff staff =
        CourseStaff.builder().githubLogin("staffLogin").orgStatus(OrgStatus.MEMBER).build();
    course.setCourseStaff(Set.of(staff));

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

    String expected = """
        Processing...
        Done""";
    assertEquals(expected, jobStarted.getLog());

    ArgumentCaptor<Course> studentCourseCaptor = ArgumentCaptor.forClass(Course.class);
    ArgumentCaptor<RosterStudent> studentCaptor = ArgumentCaptor.forClass(RosterStudent.class);
    verify(service, times(1))
        .createStudentRepository(
            studentCourseCaptor.capture(),
            studentCaptor.capture(),
            contains("repo-prefix"),
            eq(true),
            eq(RepositoryPermissions.WRITE));
    assertThat(studentCourseCaptor.getValue()).usingRecursiveComparison().isEqualTo(course);
    assertThat(studentCaptor.getValue()).usingRecursiveComparison().isEqualTo(student);

    ArgumentCaptor<Course> staffCourseCaptor = ArgumentCaptor.forClass(Course.class);
    ArgumentCaptor<CourseStaff> staffCaptor = ArgumentCaptor.forClass(CourseStaff.class);
    verify(service, times(1))
        .createStaffRepository(
            staffCourseCaptor.capture(),
            staffCaptor.capture(),
            contains("repo-prefix"),
            eq(true),
            eq(RepositoryPermissions.WRITE));
    assertThat(staffCourseCaptor.getValue()).usingRecursiveComparison().isEqualTo(course);
    assertThat(staffCaptor.getValue()).usingRecursiveComparison().isEqualTo(staff);
  }

  @Test
  public void expectDoesntCallForStaffNoLogin() throws Exception {
    Course course = Course.builder().orgName("ucsb-cs156").installationId("1234").build();

    CourseStaff staff = CourseStaff.builder().orgStatus(OrgStatus.MEMBER).build(); // no githubLogin
    course.setCourseStaff(Set.of(staff));

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

    String expected = """
        Processing...
        Done""";
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
    course.setCourseStaff(Set.of(staff));

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

    String expected = """
        Processing...
        Done""";
    assertEquals(expected, jobStarted.getLog());

    verify(service, times(0)).createStaffRepository(any(), any(), any(), any(), any());
  }

  @Test
  public void testCreateStaffRepository_owner() throws Exception {
    Course course = Course.builder().orgName("ucsb-cs156").installationId("1234").build();

    CourseStaff staff =
        CourseStaff.builder().githubLogin("staffOwner").orgStatus(OrgStatus.OWNER).build();

    course.setCourseStaff(Set.of(staff));

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

    String expected = """
        Processing...
        Done""";
    assertEquals(expected, jobStarted.getLog());

    ArgumentCaptor<Course> courseCaptor = ArgumentCaptor.forClass(Course.class);
    ArgumentCaptor<CourseStaff> staffCaptor = ArgumentCaptor.forClass(CourseStaff.class);
    verify(service, times(1))
        .createStaffRepository(
            courseCaptor.capture(),
            staffCaptor.capture(),
            contains("repo-prefix"),
            eq(false),
            eq(RepositoryPermissions.WRITE));
    assertThat(courseCaptor.getValue()).usingRecursiveComparison().isEqualTo(course);
    assertThat(staffCaptor.getValue()).usingRecursiveComparison().isEqualTo(staff);
  }
}
