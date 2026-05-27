package edu.ucsb.cs156.frontiers.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import com.opencsv.bean.StatefulBeanToCsv;
import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.entities.CourseStaff;
import edu.ucsb.cs156.frontiers.entities.User;
import edu.ucsb.cs156.frontiers.enums.OrgStatus;
import edu.ucsb.cs156.frontiers.models.CourseStaffDTO;
import edu.ucsb.cs156.frontiers.repositories.CourseStaffRepository;
import java.io.StringWriter;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/** This class contains unit tests for the CourseStaffDTOService class. */
public class CourseStaffDTOServiceTests {

  @Mock private CourseStaffRepository courseStaffRepository;

  @InjectMocks private CourseStaffDTOService courseStaffDTOService;

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  public void test_getCourseStaffDTOs_mapsLinkedUser() {
    Course course = new Course();
    course.setId(1L);

    User user = User.builder().build();
    user.setId(2L);

    CourseStaff courseStaff = new CourseStaff();
    courseStaff.setId(3L);
    courseStaff.setCourse(course);
    courseStaff.setUser(user);
    courseStaff.setFirstName("Ada");
    courseStaff.setLastName("Lovelace");
    courseStaff.setEmail("ada@example.com");
    courseStaff.setOrgStatus(OrgStatus.MEMBER);
    courseStaff.setGithubId(12345);
    courseStaff.setGithubLogin("ada");
    courseStaff.setRole("TA");

    when(courseStaffRepository.findByCourseId(1L)).thenReturn(List.of(courseStaff));

    List<CourseStaffDTO> dtos = courseStaffDTOService.getCourseStaffDTOs(1L);

    assertEquals(1, dtos.size());
    CourseStaffDTO dto = dtos.get(0);
    assertEquals(3L, dto.getId());
    assertEquals(1L, dto.getCourseId());
    assertEquals(2L, dto.getUserId());
    assertEquals("Ada", dto.getFirstName());
    assertEquals("Lovelace", dto.getLastName());
    assertEquals("ada@example.com", dto.getEmail());
    assertEquals(OrgStatus.MEMBER, dto.getOrgStatus());
    assertEquals(12345, dto.getGithubId());
    assertEquals("ada", dto.getGithubLogin());
    assertEquals("TA", dto.getRole());
  }

  @Test
  public void test_getCourseStaffDTOs_usesZeroWhenUserMissing() {
    Course course = new Course();
    course.setId(7L);

    CourseStaff courseStaff = new CourseStaff();
    courseStaff.setId(8L);
    courseStaff.setCourse(course);
    courseStaff.setFirstName("Grace");
    courseStaff.setLastName("Hopper");
    courseStaff.setEmail("grace@example.com");
    courseStaff.setOrgStatus(OrgStatus.PENDING);

    when(courseStaffRepository.findByCourseId(7L)).thenReturn(List.of(courseStaff));

    List<CourseStaffDTO> dtos = courseStaffDTOService.getCourseStaffDTOs(7L);

    assertEquals(1, dtos.size());
    assertEquals(0L, dtos.get(0).getUserId());
  }

  @Test
  public void test_getStatefulBeanToCSV_writesHeadersInExpectedOrder() throws Exception {
    StringWriter writer = new StringWriter();
    StatefulBeanToCsv<CourseStaffDTO> csvWriter =
        courseStaffDTOService.getStatefulBeanToCSV(writer);

    csvWriter.write(
        List.of(
            new CourseStaffDTO(
                3L,
                1L,
                2L,
                "Ada",
                "Lovelace",
                "ada@example.com",
                OrgStatus.MEMBER,
                12345,
                "ada",
                "TA")));

    String expected =
        """
        \"ID\",\"COURSEID\",\"USERID\",\"FIRSTNAME\",\"LASTNAME\",\"EMAIL\",\"ORGSTATUS\",\"GITHUBID\",\"GITHUBLOGIN\",\"ROLE\"
        \"3\",\"1\",\"2\",\"Ada\",\"Lovelace\",\"ada@example.com\",\"MEMBER\",\"12345\",\"ada\",\"TA\"
        """;

    assertEquals(expected, writer.toString());
  }
}
