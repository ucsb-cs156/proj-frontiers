package edu.ucsb.cs156.frontiers.jobs;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.entities.RosterStudent;
import edu.ucsb.cs156.frontiers.enums.OrgStatus;
import edu.ucsb.cs156.frontiers.repositories.RosterStudentRepository;
import edu.ucsb.cs156.frontiers.services.OrganizationMemberService;
import edu.ucsb.cs156.frontiers.services.jobs.JobContext;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

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
}
