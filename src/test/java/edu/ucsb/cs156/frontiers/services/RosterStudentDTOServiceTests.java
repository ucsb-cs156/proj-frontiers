package edu.ucsb.cs156.frontiers.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.entities.RosterStudent;
import edu.ucsb.cs156.frontiers.entities.User;
import edu.ucsb.cs156.frontiers.enums.OrgStatus;
import edu.ucsb.cs156.frontiers.enums.RosterStatus;
import edu.ucsb.cs156.frontiers.models.RosterStudentDTO;
import edu.ucsb.cs156.frontiers.repositories.RosterStudentRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/** This class contains unit tests for the RosterStudentDTOService class. */
public class RosterStudentDTOServiceTests {

  @Mock private RosterStudentRepository rosterStudentRepository;

  @InjectMocks private RosterStudentDTOService rosterStudentDTOService;

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  public void test_getRosterStudentDTOById_success() {
    // Arrange
    Course course = new Course();
    course.setId(1L);

    User user = User.builder().build();
    user.setId(2L);

    RosterStudent rosterStudent = new RosterStudent();
    rosterStudent.setId(3L);
    rosterStudent.setCourse(course);
    rosterStudent.setStudentId("U123456");
    rosterStudent.setFirstName("John");
    rosterStudent.setLastName("Doe");
    rosterStudent.setEmail("johndoe@example.com");
    rosterStudent.setGithubId(12345);
    rosterStudent.setGithubLogin("testuser");
    rosterStudent.setUser(user);
    rosterStudent.setRosterStatus(RosterStatus.ROSTER);
    rosterStudent.setOrgStatus(OrgStatus.PENDING);

    when(rosterStudentRepository.findByCourseId(1L)).thenReturn(List.of(rosterStudent));

    // Act
    List<RosterStudentDTO> dtos = rosterStudentDTOService.getRosterStudentDTOs(1L);
    // Assert
    assertEquals(1, dtos.size());
    RosterStudentDTO dto = dtos.get(0);
    assertEquals(3L, dto.id());
    assertEquals(1L, dto.courseId());
    assertEquals("U123456", dto.studentId());
    assertEquals("John", dto.firstName());
    assertEquals("Doe", dto.lastName());
    assertEquals("johndoe@example.com", dto.email());
    assertEquals(2L, dto.userId());
    assertEquals(12345, dto.userGithubId());
    assertEquals("testuser", dto.userGithubLogin());
    assertEquals(RosterStatus.ROSTER, dto.rosterStatus());
    assertEquals(OrgStatus.PENDING, dto.orgStatus());
  }
}
