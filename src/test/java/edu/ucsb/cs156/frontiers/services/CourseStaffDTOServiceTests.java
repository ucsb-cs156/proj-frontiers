package edu.ucsb.cs156.frontiers.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.entities.CourseStaff;
import edu.ucsb.cs156.frontiers.entities.User;
import edu.ucsb.cs156.frontiers.enums.OrgStatus;
import edu.ucsb.cs156.frontiers.models.CourseStaffDTO;
import edu.ucsb.cs156.frontiers.repositories.CourseStaffRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class CourseStaffDTOServiceTests {

  @Mock private CourseStaffRepository courseStaffRepository;

  @InjectMocks private CourseStaffDTOService courseStaffDTOService;

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  public void test_getCourseStaffDTOs_success() {
    // Arrange
    Course course = new Course();
    course.setId(1L);

    User user = User.builder().build();
    user.setId(2L);

    CourseStaff courseStaff = new CourseStaff();
    courseStaff.setId(3L);
    courseStaff.setCourse(course);
    courseStaff.setFirstName("Chris");
    courseStaff.setLastName("Gaucho");
    courseStaff.setEmail("cgaucho@ucsb.edu");
    courseStaff.setRole("TA");
    courseStaff.setGithubId(12345);
    courseStaff.setGithubLogin("cgaucho");
    courseStaff.setUser(user);
    courseStaff.setOrgStatus(OrgStatus.MEMBER);

    when(courseStaffRepository.findByCourseId(1L)).thenReturn(List.of(courseStaff));

    // Act
    List<CourseStaffDTO> dtos = courseStaffDTOService.getCourseStaffDTOs(1L);

    // Assert
    assertEquals(1, dtos.size());
    CourseStaffDTO dto = dtos.get(0);
    assertEquals(3L, dto.id());
    assertEquals(1L, dto.courseId());
    assertEquals("Chris", dto.firstName());
    assertEquals("Gaucho", dto.lastName());
    assertEquals("cgaucho@ucsb.edu", dto.email());
    assertEquals("TA", dto.role());
    assertEquals(2L, dto.userId());
    assertEquals(12345, dto.githubId());
    assertEquals("cgaucho", dto.githubLogin());
    assertEquals(OrgStatus.MEMBER, dto.orgStatus());
  }

  @Test
  public void test_getCourseStaffDTOs_nullUser() {
    // Arrange
    Course course = new Course();
    course.setId(1L);

    CourseStaff courseStaff = new CourseStaff();
    courseStaff.setId(3L);
    courseStaff.setCourse(course);
    courseStaff.setFirstName("Chris");
    courseStaff.setLastName("Gaucho");
    courseStaff.setEmail("cgaucho@ucsb.edu");
    courseStaff.setRole("TA");
    courseStaff.setUser(null);
    courseStaff.setOrgStatus(OrgStatus.PENDING);

    when(courseStaffRepository.findByCourseId(1L)).thenReturn(List.of(courseStaff));

    // Act
    List<CourseStaffDTO> dtos = courseStaffDTOService.getCourseStaffDTOs(1L);

    // Assert
    assertEquals(1, dtos.size());
    CourseStaffDTO dto = dtos.get(0);
    assertEquals(0L, dto.userId());
  }
}
