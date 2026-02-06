package edu.ucsb.cs156.frontiers.jobs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.entities.CourseStaff;
import edu.ucsb.cs156.frontiers.entities.Job;
import edu.ucsb.cs156.frontiers.entities.RosterStudent;
import edu.ucsb.cs156.frontiers.enums.OrgStatus;
import edu.ucsb.cs156.frontiers.models.OrgMember;
import edu.ucsb.cs156.frontiers.repositories.CourseRepository;
import edu.ucsb.cs156.frontiers.repositories.CourseStaffRepository;
import edu.ucsb.cs156.frontiers.repositories.RosterStudentRepository;
import edu.ucsb.cs156.frontiers.repositories.UserRepository;
import edu.ucsb.cs156.frontiers.services.OrganizationMemberService;
import edu.ucsb.cs156.frontiers.services.jobs.JobContext;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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

    doAnswer(
            invocation -> {
              Course c = invocation.getArgument(0);
              if (c == course) return orgMembers;
              if (c == course2) return secondCourse;
              return List.of();
            })
        .when(organizationMemberService)
        .getOrganizationMembers(any(Course.class));
    doReturn(emptyAdmins).when(organizationMemberService).getOrganizationAdmins(any(Course.class));
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

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<RosterStudent>> rosterCaptor = ArgumentCaptor.forClass(List.class);
    verify(rosterStudentRepository, times(2)).saveAll(rosterCaptor.capture());
    List<List<RosterStudent>> rosterSaves = rosterCaptor.getAllValues();
    assertThat(rosterSaves.get(0).get(0).getGithubId()).isEqualTo(123456);
    assertThat(rosterSaves.get(0).get(0).getStudentId()).isEqualTo("banana");
    assertThat(rosterSaves.get(0).get(0).getOrgStatus()).isEqualTo(OrgStatus.MEMBER);
    assertThat(rosterSaves.get(0).get(1).getGithubId()).isEqualTo(123457);
    assertThat(rosterSaves.get(0).get(1).getStudentId()).isEqualTo("apple");
    assertThat(rosterSaves.get(0).get(1).getOrgStatus()).isEqualTo(OrgStatus.MEMBER);
    assertThat(rosterSaves.get(1).get(0).getGithubId()).isEqualTo(123455);
    assertThat(rosterSaves.get(1).get(0).getStudentId()).isEqualTo("banana");
    assertThat(rosterSaves.get(1).get(0).getOrgStatus()).isEqualTo(OrgStatus.MEMBER);
    assertThat(rosterSaves.get(1).get(1).getGithubId()).isEqualTo(123454);
    assertThat(rosterSaves.get(1).get(1).getStudentId()).isEqualTo("apple");
    assertThat(rosterSaves.get(1).get(1).getOrgStatus()).isEqualTo(OrgStatus.JOINCOURSE);
    assertThat(rosterSaves.get(1).get(2).getStudentId()).isEqualTo("orange");
    assertThat(rosterSaves.get(1).get(2).getGithubId()).isNull();
    assertThat(rosterSaves.get(1).get(2).getOrgStatus()).isNull();
    assertThat(rosterSaves.get(1).get(3).getStudentId()).isEqualTo("grape");
    assertThat(rosterSaves.get(1).get(3).getGithubId()).isEqualTo(123455);
    assertThat(rosterSaves.get(1).get(3).getOrgStatus()).isNull();

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<CourseStaff>> staffCaptor = ArgumentCaptor.forClass(List.class);
    verify(courseStaffRepository, times(2)).saveAll(staffCaptor.capture());
    List<List<CourseStaff>> staffSaves = staffCaptor.getAllValues();
    assertThat(staffSaves.get(0).get(0).getGithubId()).isEqualTo(781);
    assertThat(staffSaves.get(0).get(0).getGithubLogin()).isEqualTo("division11");
    assertThat(staffSaves.get(0).get(0).getOrgStatus()).isEqualTo(OrgStatus.MEMBER);
    assertThat(staffSaves.get(1).get(0).getGithubId()).isEqualTo(738);
    assertThat(staffSaves.get(1).get(0).getGithubLogin()).isEqualTo("division6");
    assertThat(staffSaves.get(1).get(0).getOrgStatus()).isEqualTo(OrgStatus.MEMBER);
    assertThat(staffSaves.get(1).get(1).getGithubId()).isNull();
    assertThat(staffSaves.get(1).get(1).getGithubLogin()).isNull();
    assertThat(staffSaves.get(1).get(1).getOrgStatus()).isNull();
    assertThat(staffSaves.get(1).get(2).getGithubId()).isEqualTo(722);
    assertThat(staffSaves.get(1).get(2).getGithubLogin()).isNull();
    assertThat(staffSaves.get(1).get(2).getOrgStatus()).isNull();
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
    when(organizationMemberService.getOrganizationMembers(any(Course.class)))
        .thenReturn(orgMembers);
    when(organizationMemberService.getOrganizationAdmins(any(Course.class)))
        .thenReturn(emptyAdmins);
    when(organizationMemberService.getOrganizationInvitees(any(Course.class)))
        .thenReturn(List.of());
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

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<RosterStudent>> rosterCaptor = ArgumentCaptor.forClass(List.class);
    verify(rosterStudentRepository, times(1)).saveAll(rosterCaptor.capture());
    assertThat(rosterCaptor.getValue().get(0).getGithubId()).isEqualTo(123456);
    assertThat(rosterCaptor.getValue().get(0).getStudentId()).isEqualTo("banana");
    assertThat(rosterCaptor.getValue().get(0).getOrgStatus()).isEqualTo(OrgStatus.JOINCOURSE);

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<CourseStaff>> staffCaptor = ArgumentCaptor.forClass(List.class);
    verify(courseStaffRepository, times(1)).saveAll(staffCaptor.capture());
    assertThat(staffCaptor.getValue().get(0).getGithubId()).isEqualTo(123457);
    assertThat(staffCaptor.getValue().get(0).getGithubLogin()).isEqualTo("apple");
    assertThat(staffCaptor.getValue().get(0).getOrgStatus()).isEqualTo(OrgStatus.JOINCOURSE);
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

    doReturn(orgMembers).when(organizationMemberService).getOrganizationMembers(any(Course.class));
    doReturn(orgAdmins).when(organizationMemberService).getOrganizationAdmins(any(Course.class));
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

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<RosterStudent>> rosterCaptor = ArgumentCaptor.forClass(List.class);
    verify(rosterStudentRepository, times(1)).saveAll(rosterCaptor.capture());
    assertThat(rosterCaptor.getValue().get(0).getGithubId()).isEqualTo(123457);
    assertThat(rosterCaptor.getValue().get(0).getStudentId()).isEqualTo("apple");
    assertThat(rosterCaptor.getValue().get(0).getOrgStatus()).isEqualTo(OrgStatus.MEMBER);
    assertThat(rosterCaptor.getValue().get(1).getGithubId()).isEqualTo(123455);
    assertThat(rosterCaptor.getValue().get(1).getStudentId()).isEqualTo("orange");
    assertThat(rosterCaptor.getValue().get(1).getOrgStatus()).isEqualTo(OrgStatus.OWNER);

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<CourseStaff>> staffCaptor = ArgumentCaptor.forClass(List.class);
    verify(courseStaffRepository, times(1)).saveAll(staffCaptor.capture());
    assertThat(staffCaptor.getValue().get(0).getGithubId()).isEqualTo(772);
    assertThat(staffCaptor.getValue().get(0).getGithubLogin()).isEqualTo("division6");
    assertThat(staffCaptor.getValue().get(0).getOrgStatus()).isEqualTo(OrgStatus.OWNER);
    assertThat(staffCaptor.getValue().get(1).getGithubId()).isEqualTo(752);
    assertThat(staffCaptor.getValue().get(1).getGithubLogin()).isEqualTo("division11");
    assertThat(staffCaptor.getValue().get(1).getOrgStatus()).isEqualTo(OrgStatus.MEMBER);
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

    doReturn(emptyMembers)
        .when(organizationMemberService)
        .getOrganizationMembers(any(Course.class));
    doReturn(emptyAdmins).when(organizationMemberService).getOrganizationAdmins(any(Course.class));
    doReturn(orgInvitees)
        .when(organizationMemberService)
        .getOrganizationInvitees(any(Course.class));
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

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<RosterStudent>> rosterCaptor = ArgumentCaptor.forClass(List.class);
    verify(rosterStudentRepository, times(1)).saveAll(rosterCaptor.capture());
    assertThat(rosterCaptor.getValue().get(0).getGithubId()).isEqualTo(123456);
    assertThat(rosterCaptor.getValue().get(0).getStudentId()).isEqualTo("banana");
    assertThat(rosterCaptor.getValue().get(0).getOrgStatus()).isEqualTo(OrgStatus.INVITED);
    assertThat(rosterCaptor.getValue().get(1).getGithubId()).isEqualTo(241789);
    assertThat(rosterCaptor.getValue().get(1).getStudentId()).isEqualTo("banana");
    assertThat(rosterCaptor.getValue().get(1).getOrgStatus()).isEqualTo(OrgStatus.JOINCOURSE);

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<CourseStaff>> staffCaptor = ArgumentCaptor.forClass(List.class);
    verify(courseStaffRepository).saveAll(staffCaptor.capture());
    assertThat(staffCaptor.getValue().get(0).getGithubId()).isEqualTo(777);
    assertThat(staffCaptor.getValue().get(0).getGithubLogin()).isEqualTo("division6");
    assertThat(staffCaptor.getValue().get(0).getOrgStatus()).isEqualTo(OrgStatus.INVITED);
    assertThat(staffCaptor.getValue().get(1).getGithubId()).isEqualTo(7310);
    assertThat(staffCaptor.getValue().get(1).getGithubLogin()).isEqualTo("division14");
    assertThat(staffCaptor.getValue().get(1).getOrgStatus()).isEqualTo(OrgStatus.JOINCOURSE);
    verifyNoMoreInteractions(courseStaffRepository, rosterStudentRepository);
  }
}
