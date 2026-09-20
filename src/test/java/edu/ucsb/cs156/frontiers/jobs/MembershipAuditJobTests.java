package edu.ucsb.cs156.frontiers.jobs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.entities.CourseStaff;
import edu.ucsb.cs156.frontiers.entities.RosterStudent;
import edu.ucsb.cs156.frontiers.enums.OrgStatus;
import edu.ucsb.cs156.frontiers.models.OrgMember;
import edu.ucsb.cs156.frontiers.repositories.CourseRepository;
import edu.ucsb.cs156.frontiers.repositories.CourseStaffRepository;
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
public class MembershipAuditJobTests {
  @Mock private UserRepository userRepository;

  @Mock private RosterStudentRepository rosterStudentRepository;

  @Mock private OrganizationMemberService organizationMemberService;

  @Mock private CourseRepository courseRepository;

  @Mock CourseStaffRepository courseStaffRepository;

  Job jobStarted = Job.builder().build();
  JobContext ctx = new JobContext(null, jobStarted);

  @BeforeEach
  public void setup() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  public void test_getScope_returnsUnscoped() {
    MembershipAuditJob job = MembershipAuditJob.builder().build();

    assertNull(job.getScopeType());
    assertNull(job.getScopeId());
  }

  @Test
  public void match_students_and_staff_correctly() throws Exception {
    OrgMember orgMember1 = OrgMember.builder().githubId(123456).githubLogin("division7").build();
    OrgMember orgMember2 = OrgMember.builder().githubId(123457).githubLogin("division8").build();
    OrgMember orgMember5 = OrgMember.builder().githubId(781).githubLogin("division11").build();
    List<OrgMember> orgMembers = List.of(orgMember1, orgMember2, orgMember5);
    OrgMember orgMember3 = OrgMember.builder().githubId(123455).githubLogin("division9").build();
    OrgMember orgMember4 = OrgMember.builder().githubId(772).githubLogin("unmatched").build();
    OrgMember orgMember6 = OrgMember.builder().githubId(738).githubLogin("division6").build();
    List<OrgMember> secondCourse = List.of(orgMember3, orgMember4, orgMember6);

    List<OrgMember> emptyAdmins = List.of();

    Course course = Course.builder().orgName("ucsb-cs156").installationId("1234").build();
    Course course2 = Course.builder().orgName("ucsb-cs156-f25").installationId("1235").build();
    Course course3 = Course.builder().build();
    Course course4 = Course.builder().orgName("ucsb-cs156-f25").build();
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
    course.setRosterStudents(List.of(student1, student2));

    CourseStaff courseStaff1 =
        CourseStaff.builder().githubLogin("division11").githubId(781).course(course).build();
    course.setCourseStaff(List.of(courseStaff1));

    RosterStudent student3 =
        RosterStudent.builder()
            .studentId("banana")
            .githubLogin("division9")
            .githubId(123455)
            .course(course2)
            .build();
    RosterStudent student4 =
        RosterStudent.builder()
            .studentId("apple")
            .githubLogin("division10")
            .githubId(123454)
            .course(course2)
            .build();
    RosterStudent student5 =
        RosterStudent.builder()
            .studentId("orange")
            .githubLogin(null)
            .githubId(null)
            .course(course2)
            .build();
    RosterStudent student6 =
        RosterStudent.builder()
            .studentId("grape")
            .githubLogin(null)
            .githubId(123455)
            .course(course3)
            .build();
    course2.setRosterStudents(List.of(student3, student4, student5, student6));

    CourseStaff courseStaff2 =
        CourseStaff.builder().githubLogin("division6").githubId(738).course(course2).build();
    CourseStaff courseStaff3 =
        CourseStaff.builder().githubLogin(null).githubId(null).course(course2).build();
    CourseStaff courseStaff4 =
        CourseStaff.builder().githubLogin(null).githubId(722).course(course2).build();
    course2.setCourseStaff(List.of(courseStaff2, courseStaff3, courseStaff4));

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
    RosterStudent student3Updated =
        RosterStudent.builder()
            .studentId("banana")
            .githubLogin("division9")
            .githubId(123455)
            .course(course2)
            .orgStatus(OrgStatus.MEMBER)
            .build();
    RosterStudent student4Updated =
        RosterStudent.builder()
            .studentId("apple")
            .githubLogin("division10")
            .githubId(123454)
            .course(course2)
            .orgStatus(OrgStatus.JOINCOURSE)
            .build();
    RosterStudent student5Updated =
        RosterStudent.builder()
            .studentId("orange")
            .githubLogin(null)
            .githubId(null)
            .course(course2)
            .build();
    RosterStudent student6Updated =
        RosterStudent.builder()
            .studentId("grape")
            .githubLogin(null)
            .githubId(123455)
            .course(course3)
            .build();
    CourseStaff courseStaff1Updated =
        CourseStaff.builder()
            .githubLogin("division11")
            .githubId(781)
            .course(course)
            .orgStatus(OrgStatus.MEMBER)
            .build();
    CourseStaff courseStaff2Updated =
        CourseStaff.builder()
            .githubLogin("division6")
            .githubId(738)
            .course(course2)
            .orgStatus(OrgStatus.MEMBER)
            .build();
    CourseStaff courseStaff3Updated =
        CourseStaff.builder().githubLogin(null).githubId(null).course(course2).build();
    CourseStaff courseStaff4Updated =
        CourseStaff.builder().githubLogin(null).githubId(722).course(course2).build();

    doReturn(orgMembers).when(organizationMemberService).getOrganizationMembers(eq(course));
    doReturn(secondCourse).when(organizationMemberService).getOrganizationMembers(eq(course2));
    doReturn(emptyAdmins).when(organizationMemberService).getOrganizationAdmins(eq(course));
    doReturn(emptyAdmins).when(organizationMemberService).getOrganizationAdmins(eq(course2));
    doReturn(List.of(course, course2, course3, course4)).when(courseRepository).findAll();

    var matchJob =
        spy(
            MembershipAuditJob.builder()
                .rosterStudentRepository(rosterStudentRepository)
                .organizationMemberService(organizationMemberService)
                .courseRepository(courseRepository)
                .courseStaffRepository(courseStaffRepository)
                .build());

    matchJob.accept(ctx);
    String expected =
        """
                Auditing membership for each course with an attached GitHub Organization...
                Done""";
    assertEquals(expected, jobStarted.getLog());

    verify(rosterStudentRepository, atLeastOnce())
        .saveAll(eq(List.of(student1Updated, student2Updated)));
    verify(courseStaffRepository, atLeastOnce()).saveAll(eq(List.of(courseStaff1Updated)));
    verify(courseStaffRepository, atLeastOnce())
        .saveAll(eq(List.of(courseStaff2Updated, courseStaff3Updated, courseStaff4Updated)));
    verify(rosterStudentRepository, atLeastOnce())
        .saveAll(eq(List.of(student3Updated, student4Updated, student5Updated, student6Updated)));
    verify(rosterStudentRepository, times(2)).saveAll(any());
    verify(courseStaffRepository, times(2)).saveAll(any());
    verifyNoMoreInteractions(courseStaffRepository, rosterStudentRepository);
  }

  @Test
  public void no_match_on_any_member() throws Exception {
    OrgMember orgMember1 = OrgMember.builder().githubId(123455).githubLogin("unmatched-a").build();
    OrgMember orgMember2 = OrgMember.builder().githubId(772).githubLogin("unmatched-b").build();
    List<OrgMember> orgMembers = List.of(orgMember1, orgMember2);
    List<OrgMember> emptyAdmins = List.of();
    Course course = Course.builder().orgName("ucsb-cs156").installationId("1234").build();
    RosterStudent student1 =
        RosterStudent.builder()
            .studentId("banana")
            .githubLogin("division7")
            .githubId(123456)
            .course(course)
            .build();
    course.setRosterStudents(List.of(student1));
    CourseStaff courseStaff1 =
        CourseStaff.builder().githubLogin("apple").githubId(123457).course(course).build();
    course.setCourseStaff(List.of(courseStaff1));
    RosterStudent student1Updated =
        RosterStudent.builder()
            .studentId("banana")
            .githubLogin("division7")
            .githubId(123456)
            .course(course)
            .orgStatus(OrgStatus.JOINCOURSE)
            .build();
    CourseStaff courseStaff1Updated =
        CourseStaff.builder()
            .githubLogin("apple")
            .githubId(123457)
            .course(course)
            .orgStatus(OrgStatus.JOINCOURSE)
            .build();
    when(organizationMemberService.getOrganizationMembers(course)).thenReturn(orgMembers);
    when(organizationMemberService.getOrganizationAdmins(course)).thenReturn(emptyAdmins);
    when(organizationMemberService.getOrganizationInvitees(course)).thenReturn(List.of());
    when(courseRepository.findAll()).thenReturn(List.of(course));

    var matchJob =
        spy(
            MembershipAuditJob.builder()
                .rosterStudentRepository(rosterStudentRepository)
                .organizationMemberService(organizationMemberService)
                .courseRepository(courseRepository)
                .courseStaffRepository(courseStaffRepository)
                .build());

    matchJob.accept(ctx);
    String expected =
        """
                Auditing membership for each course with an attached GitHub Organization...
                Done""";
    assertEquals(expected, jobStarted.getLog());

    verify(rosterStudentRepository, times(1)).saveAll(eq(List.of(student1Updated)));
    verify(courseStaffRepository, times(1)).saveAll(eq(List.of(courseStaff1Updated)));
    verifyNoMoreInteractions(courseStaffRepository, rosterStudentRepository);
  }

  @Test
  public void match_admin_students_and_staff_correctly() throws Exception {
    OrgMember orgMember2 = OrgMember.builder().githubId(123457).githubLogin("division8").build();
    OrgMember orgMember4 = OrgMember.builder().githubId(752).githubLogin("division11").build();
    List<OrgMember> orgMembers = List.of(orgMember2, orgMember4);

    OrgMember orgAdmin2 = OrgMember.builder().githubId(123455).githubLogin("division9").build();
    OrgMember orgAdmin3 = OrgMember.builder().githubId(772).githubLogin("division6").build();
    List<OrgMember> orgAdmins = List.of(orgAdmin2, orgAdmin3);

    Course course = Course.builder().orgName("ucsb-cs156").installationId("1234").build();
    RosterStudent student2 =
        RosterStudent.builder()
            .studentId("apple")
            .githubLogin("division8")
            .githubId(123457)
            .course(course)
            .build();
    RosterStudent student3 =
        RosterStudent.builder()
            .studentId("orange")
            .githubLogin("division9")
            .githubId(123455)
            .course(course)
            .build();
    course.setRosterStudents(List.of(student2, student3));

    CourseStaff courseStaff1 =
        CourseStaff.builder().githubLogin("division6").githubId(772).course(course).build();
    CourseStaff courseStaff2 =
        CourseStaff.builder().githubLogin("division11").githubId(752).course(course).build();
    course.setCourseStaff(List.of(courseStaff1, courseStaff2));

    RosterStudent student2Updated =
        RosterStudent.builder()
            .studentId("apple")
            .githubLogin("division8")
            .githubId(123457)
            .course(course)
            .orgStatus(OrgStatus.MEMBER)
            .build();
    RosterStudent student3Updated =
        RosterStudent.builder()
            .studentId("orange")
            .githubLogin("division9")
            .githubId(123455)
            .course(course)
            .orgStatus(OrgStatus.OWNER)
            .build();
    CourseStaff courseStaff1Updated =
        CourseStaff.builder()
            .githubLogin("division6")
            .githubId(772)
            .course(course)
            .orgStatus(OrgStatus.OWNER)
            .build();
    CourseStaff courseStaff2Updated =
        CourseStaff.builder()
            .githubLogin("division11")
            .githubId(752)
            .course(course)
            .orgStatus(OrgStatus.MEMBER)
            .build();

    doReturn(orgMembers).when(organizationMemberService).getOrganizationMembers(eq(course));
    doReturn(orgAdmins).when(organizationMemberService).getOrganizationAdmins(eq(course));
    doReturn(List.of(course)).when(courseRepository).findAll();

    var matchJob =
        spy(
            MembershipAuditJob.builder()
                .rosterStudentRepository(rosterStudentRepository)
                .organizationMemberService(organizationMemberService)
                .courseRepository(courseRepository)
                .courseStaffRepository(courseStaffRepository)
                .build());

    matchJob.accept(ctx);
    String expected =
        """
                Auditing membership for each course with an attached GitHub Organization...
                Done""";
    assertEquals(expected, jobStarted.getLog());

    verify(rosterStudentRepository, times(1))
        .saveAll(eq(List.of(student2Updated, student3Updated)));
    verify(courseStaffRepository, times(1))
        .saveAll(eq(List.of(courseStaff1Updated, courseStaff2Updated)));
    verifyNoMoreInteractions(courseStaffRepository, rosterStudentRepository);
  }

  @Test
  public void match_invited_students_and_staff_correctly() throws Exception {
    List<OrgMember> emptyMembers = List.of();
    List<OrgMember> emptyAdmins = List.of();
    OrgMember invitee = OrgMember.builder().githubId(123456).githubLogin("division7").build();
    OrgMember invitee2 = OrgMember.builder().githubId(777).githubLogin("division6").build();
    List<OrgMember> orgInvitees = List.of(invitee, invitee2);

    Course course = Course.builder().orgName("ucsb-cs156").installationId("1234").build();

    RosterStudent student =
        RosterStudent.builder()
            .studentId("banana")
            .githubLogin("division7")
            .githubId(123456)
            .course(course)
            .orgStatus(OrgStatus.MEMBER)
            .build();

    RosterStudent student2 =
        RosterStudent.builder()
            .studentId("banana")
            .githubLogin("division11")
            .githubId(241789)
            .course(course)
            .orgStatus(OrgStatus.JOINCOURSE)
            .build();

    CourseStaff courseStaff1 =
        CourseStaff.builder()
            .githubLogin("division6")
            .orgStatus(OrgStatus.MEMBER)
            .githubId(777)
            .course(course)
            .build();

    CourseStaff courseStaff2 =
        CourseStaff.builder()
            .githubLogin("division14")
            .githubId(7310)
            .course(course)
            .orgStatus(OrgStatus.JOINCOURSE)
            .build();

    course.setRosterStudents(List.of(student, student2));

    course.setCourseStaff(List.of(courseStaff1, courseStaff2));

    RosterStudent studentUpdated =
        RosterStudent.builder()
            .studentId("banana")
            .githubLogin("division7")
            .githubId(123456)
            .course(course)
            .orgStatus(OrgStatus.INVITED)
            .build();

    RosterStudent student2NotUpdated =
        RosterStudent.builder()
            .studentId("banana")
            .githubLogin("division11")
            .githubId(241789)
            .course(course)
            .orgStatus(OrgStatus.JOINCOURSE)
            .build();

    CourseStaff courseStaff1Updated =
        CourseStaff.builder()
            .githubLogin("division6")
            .orgStatus(OrgStatus.INVITED)
            .githubId(777)
            .course(course)
            .build();
    CourseStaff courseStaff2NotUpdated =
        CourseStaff.builder()
            .githubLogin("division14")
            .githubId(7310)
            .course(course)
            .orgStatus(OrgStatus.JOINCOURSE)
            .build();

    doReturn(emptyMembers).when(organizationMemberService).getOrganizationMembers(eq(course));
    doReturn(emptyAdmins).when(organizationMemberService).getOrganizationAdmins(eq(course));
    doReturn(orgInvitees).when(organizationMemberService).getOrganizationInvitees(eq(course));
    doReturn(List.of(course)).when(courseRepository).findAll();

    var matchJob =
        spy(
            MembershipAuditJob.builder()
                .rosterStudentRepository(rosterStudentRepository)
                .organizationMemberService(organizationMemberService)
                .courseRepository(courseRepository)
                .courseStaffRepository(courseStaffRepository)
                .build());

    matchJob.accept(ctx);

    String expected =
        """
                Auditing membership for each course with an attached GitHub Organization...
                Done""";
    assertEquals(expected, jobStarted.getLog());

    verify(rosterStudentRepository, times(1))
        .saveAll(eq(List.of(studentUpdated, student2NotUpdated)));
    verify(courseStaffRepository).saveAll(eq(List.of(courseStaff1Updated, courseStaff2NotUpdated)));
    verifyNoMoreInteractions(courseStaffRepository, rosterStudentRepository);
  }

  // ────────────────────── checkCancellation checkpoints ──────────────────────
  // Nothing in this method's whole body logs per course, per student, or per staff member --
  // the only ctx.log() calls are the opening line and the closing "Done". Without their own
  // checkCancellation() checkpoints, the course/student/staff loops would give cancellation no
  // opportunity to fire no matter how many courses/students/staff they process. These tests
  // mock a JobsRepository that reports "running" for exactly the calls known to precede the
  // checkpoint under test, then "cancelling" from then on, and assert both that
  // JobCancelledException is thrown AND that a downstream call the checkpoint should have
  // pre-empted was never made.

  private static Job runningJob() {
    return Job.builder().id(99L).status("running").build();
  }

  private static Job cancellingJob() {
    return Job.builder().id(99L).status("cancelling").build();
  }

  @Test
  public void checkCancellation_stops_the_course_loop_before_fetching_org_members()
      throws Exception {
    Course course = Course.builder().orgName("ucsb-cs156").installationId("1234").build();
    doReturn(List.of(course)).when(courseRepository).findAll();

    JobsRepository jobsRepository = mock(JobsRepository.class);
    // 1 real checkpoint precedes the course loop's own check: accept()'s opening log line.
    when(jobsRepository.findById(99L))
        .thenReturn(Optional.of(runningJob()), Optional.of(cancellingJob()));
    Job job = Job.builder().id(99L).build();
    JobContext cancellingCtx = new JobContext(null, job, null, jobsRepository);

    var matchJob =
        MembershipAuditJob.builder()
            .rosterStudentRepository(rosterStudentRepository)
            .organizationMemberService(organizationMemberService)
            .courseRepository(courseRepository)
            .courseStaffRepository(courseStaffRepository)
            .build();

    assertThrows(JobCancelledException.class, () -> matchJob.accept(cancellingCtx));

    verify(organizationMemberService, never()).getOrganizationMembers(any());
  }

  @Test
  public void checkCancellation_stops_the_student_loop_before_saving_the_roster() throws Exception {
    Course course = Course.builder().orgName("ucsb-cs156").installationId("1234").build();
    RosterStudent student =
        RosterStudent.builder().studentId("banana").githubId(123456).course(course).build();
    course.setRosterStudents(List.of(student));
    course.setCourseStaff(List.of());
    doReturn(List.of(course)).when(courseRepository).findAll();
    doReturn(List.of()).when(organizationMemberService).getOrganizationMembers(eq(course));
    doReturn(List.of()).when(organizationMemberService).getOrganizationAdmins(eq(course));
    doReturn(List.of()).when(organizationMemberService).getOrganizationInvitees(eq(course));

    JobsRepository jobsRepository = mock(JobsRepository.class);
    // 2 real checkpoints precede the student loop's own check under this setup: accept()'s
    // opening log line, and the course loop's own checkCancellation() (checked above).
    when(jobsRepository.findById(99L))
        .thenReturn(
            Optional.of(runningJob()), Optional.of(runningJob()), Optional.of(cancellingJob()));
    Job job = Job.builder().id(99L).build();
    JobContext cancellingCtx = new JobContext(null, job, null, jobsRepository);

    var matchJob =
        MembershipAuditJob.builder()
            .rosterStudentRepository(rosterStudentRepository)
            .organizationMemberService(organizationMemberService)
            .courseRepository(courseRepository)
            .courseStaffRepository(courseStaffRepository)
            .build();

    assertThrows(JobCancelledException.class, () -> matchJob.accept(cancellingCtx));

    verify(rosterStudentRepository, never()).saveAll(any());
  }

  @Test
  public void checkCancellation_stops_the_staff_loop_before_saving_the_staff_list()
      throws Exception {
    Course course = Course.builder().orgName("ucsb-cs156").installationId("1234").build();
    CourseStaff staff = CourseStaff.builder().githubId(781).course(course).build();
    course.setRosterStudents(List.of());
    course.setCourseStaff(List.of(staff));
    doReturn(List.of(course)).when(courseRepository).findAll();
    doReturn(List.of()).when(organizationMemberService).getOrganizationMembers(eq(course));
    doReturn(List.of()).when(organizationMemberService).getOrganizationAdmins(eq(course));
    doReturn(List.of()).when(organizationMemberService).getOrganizationInvitees(eq(course));

    JobsRepository jobsRepository = mock(JobsRepository.class);
    // 2 real checkpoints precede the staff loop's own check under this setup (an empty roster
    // means the student loop never runs, and rosterStudentRepository.saveAll() doesn't consume
    // a checkpoint): accept()'s opening log line, and the course loop's own checkCancellation().
    when(jobsRepository.findById(99L))
        .thenReturn(
            Optional.of(runningJob()), Optional.of(runningJob()), Optional.of(cancellingJob()));
    Job job = Job.builder().id(99L).build();
    JobContext cancellingCtx = new JobContext(null, job, null, jobsRepository);

    var matchJob =
        MembershipAuditJob.builder()
            .rosterStudentRepository(rosterStudentRepository)
            .organizationMemberService(organizationMemberService)
            .courseRepository(courseRepository)
            .courseStaffRepository(courseStaffRepository)
            .build();

    assertThrows(JobCancelledException.class, () -> matchJob.accept(cancellingCtx));

    verify(courseStaffRepository, never()).saveAll(any());
  }
}
