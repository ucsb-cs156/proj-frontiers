package edu.ucsb.cs156.frontiers.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.ucsb.cs156.frontiers.ControllerTestCase;
import edu.ucsb.cs156.frontiers.annotations.WithInstructorCoursePermissions;
import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.entities.CourseStaff;
import edu.ucsb.cs156.frontiers.enums.OrgStatus;
import edu.ucsb.cs156.frontiers.enums.School;
import edu.ucsb.cs156.frontiers.repositories.CourseRepository;
import edu.ucsb.cs156.frontiers.repositories.CourseStaffRepository;
import edu.ucsb.cs156.frontiers.services.CurrentUserService;
import edu.ucsb.cs156.frontiers.services.OrganizationMemberService;
import edu.ucsb.cs156.frontiers.services.UpdateUserService;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;

@Slf4j
@WebMvcTest(controllers = {CourseStaffController.class, CourseStaffCSVController.class})
public class CourseStaffCSVControllerTests extends ControllerTestCase {

  @MockitoBean private CourseRepository courseRepository;

  @MockitoBean private CourseStaffRepository courseStaffRepository;

  @MockitoBean private CurrentUserService currentUserService;

  @MockitoBean private UpdateUserService updateUserService;

  @MockitoBean private OrganizationMemberService organizationMemberService;

  private final String sampleCSV =
      """
      firstName,lastName,email
      Phill,Conrad,phtcon@ucsb.edu
      Dan,Nov,dan@ucsb.edu
      """;

  Course courseWithOrg =
      Course.builder()
          .id(1L)
          .courseName("CS156")
          .orgName("ucsb-cs156-s26")
          .term("S26")
          .school(School.UCSB)
          .installationId("id")
          .build();

  Course courseWithoutOrg =
      Course.builder()
          .id(2L)
          .courseName("CS156")
          .orgName(null)
          .term("S26")
          .school(School.UCSB)
          .installationId(null)
          .build();

  @Test
  @WithInstructorCoursePermissions
  public void instructor_can_upload_staff_csv() throws Exception {

    CourseStaff saved1 =
        CourseStaff.builder()
            .id(10L)
            .firstName("Phill")
            .lastName("Conrad")
            .email("phtcon@ucsb.edu")
            .course(courseWithOrg)
            .orgStatus(OrgStatus.JOINCOURSE)
            .build();

    CourseStaff saved2 =
        CourseStaff.builder()
            .id(11L)
            .firstName("Dan")
            .lastName("Nov")
            .email("dan@ucsb.edu")
            .course(courseWithOrg)
            .orgStatus(OrgStatus.JOINCOURSE)
            .build();

    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(courseWithOrg));
    when(courseStaffRepository.save(any(CourseStaff.class))).thenReturn(saved1, saved2);

    MockMultipartFile file =
        new MockMultipartFile(
            "file", "staff.csv", MediaType.TEXT_PLAIN_VALUE, sampleCSV.getBytes());

    MvcResult response =
        mockMvc
            .perform(
                multipart("/api/coursestaff/upload/csv")
                    .file(file)
                    .param("courseId", "1")
                    .with(csrf()))
            .andExpect(status().isOk())
            .andReturn();

    verify(courseStaffRepository, times(2)).save(any(CourseStaff.class));
    verify(updateUserService, times(2)).attachUserToCourseStaff(any(CourseStaff.class));

    String responseString = response.getResponse().getContentAsString();
    Map<String, Object> expectedMap = Map.of("inserted", 2);
    String expectedJson = mapper.writeValueAsString(expectedMap);
    assertEquals(expectedJson, responseString);
  }

  @Test
  @WithInstructorCoursePermissions
  public void org_status_is_joincourse_when_installation_id_is_set() throws Exception {

    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(courseWithOrg));
    when(courseStaffRepository.save(any(CourseStaff.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    String singleRowCSV = "firstName,lastName,email\nPhill,Conrad,phtcon@ucsb.edu\n";
    MockMultipartFile file =
        new MockMultipartFile(
            "file", "staff.csv", MediaType.TEXT_PLAIN_VALUE, singleRowCSV.getBytes());

    mockMvc
        .perform(
            multipart("/api/coursestaff/upload/csv").file(file).param("courseId", "1").with(csrf()))
        .andExpect(status().isOk());

    ArgumentCaptor<CourseStaff> captor = ArgumentCaptor.forClass(CourseStaff.class);
    verify(courseStaffRepository).save(captor.capture());
    assertEquals(OrgStatus.JOINCOURSE, captor.getValue().getOrgStatus());
  }

  @Test
  @WithInstructorCoursePermissions
  public void org_status_is_pending_when_no_installation_id() throws Exception {

    when(courseRepository.findById(eq(2L))).thenReturn(Optional.of(courseWithoutOrg));
    when(courseStaffRepository.save(any(CourseStaff.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    String singleRowCSV = "firstName,lastName,email\nPhill,Conrad,phtcon@ucsb.edu\n";
    MockMultipartFile file =
        new MockMultipartFile(
            "file", "staff.csv", MediaType.TEXT_PLAIN_VALUE, singleRowCSV.getBytes());

    mockMvc
        .perform(
            multipart("/api/coursestaff/upload/csv").file(file).param("courseId", "2").with(csrf()))
        .andExpect(status().isOk());

    ArgumentCaptor<CourseStaff> captor = ArgumentCaptor.forClass(CourseStaff.class);
    verify(courseStaffRepository).save(captor.capture());
    assertEquals(OrgStatus.PENDING, captor.getValue().getOrgStatus());
  }

  @Test
  @WithInstructorCoursePermissions
  public void instructor_cannot_upload_staff_csv_for_a_course_that_does_not_exist()
      throws Exception {

    MockMultipartFile file =
        new MockMultipartFile(
            "file", "staff.csv", MediaType.TEXT_PLAIN_VALUE, sampleCSV.getBytes());

    when(courseRepository.findById(eq(1L))).thenReturn(Optional.empty());

    MvcResult response =
        mockMvc
            .perform(
                multipart("/api/coursestaff/upload/csv")
                    .file(file)
                    .param("courseId", "1")
                    .with(csrf()))
            .andExpect(status().isNotFound())
            .andReturn();

    verify(courseRepository, atLeastOnce()).findById(eq(1L));
    String responseString = response.getResponse().getContentAsString();
    Map<String, String> expectedMap =
        Map.of(
            "type", "EntityNotFoundException",
            "message", "Course with id 1 not found");
    String expectedJson = mapper.writeValueAsString(expectedMap);
    assertEquals(expectedJson, responseString);
  }

  @Test
  @WithMockUser(roles = {"USER"})
  public void non_instructor_cannot_upload_staff_csv() throws Exception {

    MockMultipartFile file =
        new MockMultipartFile(
            "file", "staff.csv", MediaType.TEXT_PLAIN_VALUE, sampleCSV.getBytes());

    mockMvc
        .perform(
            multipart("/api/coursestaff/upload/csv").file(file).param("courseId", "1").with(csrf()))
        .andExpect(status().isForbidden());
  }
}
