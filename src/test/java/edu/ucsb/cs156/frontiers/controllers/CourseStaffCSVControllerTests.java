package edu.ucsb.cs156.frontiers.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.ucsb.cs156.frontiers.ControllerTestCase;
import edu.ucsb.cs156.frontiers.annotations.WithInstructorCoursePermissions;
import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.entities.CourseStaff;
import edu.ucsb.cs156.frontiers.enums.OrgStatus;
import edu.ucsb.cs156.frontiers.enums.School;
import edu.ucsb.cs156.frontiers.repositories.CourseRepository;
import edu.ucsb.cs156.frontiers.repositories.CourseStaffRepository;
import edu.ucsb.cs156.frontiers.services.CurrentUserService;
import edu.ucsb.cs156.frontiers.services.UpdateUserService;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;

@Slf4j
@WebMvcTest(controllers = CourseStaffCSVController.class)
public class CourseStaffCSVControllerTests extends ControllerTestCase {

  @MockitoBean private CourseRepository courseRepository;

  @MockitoBean private CourseStaffRepository courseStaffRepository;

  @Autowired private CurrentUserService currentUserService;

  @MockitoBean private UpdateUserService updateUserService;

  @Autowired private ObjectMapper objectMapper;

  private final String sampleCSV =
      """
      firstName,lastName,email
      Chris,Gaucho,cgaucho@example.org
      Lauren,Del Playa,ldelplaya@ucsb.edu
      """;

  private final String sampleCSVWithExtraSpaces =
      """
      firstName,lastName,email
       Chris , Gaucho , cgaucho@example.org
      """;

  @Test
  @WithInstructorCoursePermissions
  public void instructor_can_upload_staff_csv() throws Exception {

    Course course =
        Course.builder()
            .id(1L)
            .courseName("CS156")
            .orgName("ucsb-cs156-s25")
            .term("S25")
            .school(School.UCSB)
            .build();

    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(course));

    when(courseStaffRepository.save(any(CourseStaff.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

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

    // Verify the saved staff members
    ArgumentCaptor<CourseStaff> captor = ArgumentCaptor.forClass(CourseStaff.class);
    verify(courseStaffRepository, times(2)).save(captor.capture());

    CourseStaff first = captor.getAllValues().get(0);
    assertEquals("Chris", first.getFirstName());
    assertEquals("Gaucho", first.getLastName());
    assertEquals("cgaucho@example.org", first.getEmail());
    assertEquals(OrgStatus.PENDING, first.getOrgStatus());

    CourseStaff second = captor.getAllValues().get(1);
    assertEquals("Lauren", second.getFirstName());
    assertEquals("Del Playa", second.getLastName());
    assertEquals("ldelplaya@ucsb.edu", second.getEmail());

    String responseString = response.getResponse().getContentAsString();
    String expectedJson = objectMapper.writeValueAsString(Map.of("count", 2));
    assertEquals(expectedJson, responseString);
  }

  @Test
  @WithInstructorCoursePermissions
  public void instructor_can_upload_staff_csv_with_installation_id() throws Exception {

    Course course =
        Course.builder()
            .id(1L)
            .courseName("CS156")
            .orgName("ucsb-cs156-s25")
            .installationId("12345")
            .term("S25")
            .school(School.UCSB)
            .build();

    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(course));

    when(courseStaffRepository.save(any(CourseStaff.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    MockMultipartFile file =
        new MockMultipartFile(
            "file", "staff.csv", MediaType.TEXT_PLAIN_VALUE, sampleCSV.getBytes());

    mockMvc
        .perform(
            multipart("/api/coursestaff/upload/csv").file(file).param("courseId", "1").with(csrf()))
        .andExpect(status().isOk());

    ArgumentCaptor<CourseStaff> captor = ArgumentCaptor.forClass(CourseStaff.class);
    verify(courseStaffRepository, times(2)).save(captor.capture());

    // When installationId is present, orgStatus should be JOINCOURSE
    assertEquals(OrgStatus.JOINCOURSE, captor.getAllValues().get(0).getOrgStatus());
    assertEquals(OrgStatus.JOINCOURSE, captor.getAllValues().get(1).getOrgStatus());
  }

  @Test
  @WithInstructorCoursePermissions
  public void upload_strips_whitespace_from_fields() throws Exception {

    Course course =
        Course.builder()
            .id(1L)
            .courseName("CS156")
            .orgName("ucsb-cs156-s25")
            .term("S25")
            .school(School.UCSB)
            .build();

    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(course));

    when(courseStaffRepository.save(any(CourseStaff.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    MockMultipartFile file =
        new MockMultipartFile(
            "file", "staff.csv", MediaType.TEXT_PLAIN_VALUE, sampleCSVWithExtraSpaces.getBytes());

    mockMvc
        .perform(
            multipart("/api/coursestaff/upload/csv").file(file).param("courseId", "1").with(csrf()))
        .andExpect(status().isOk());

    ArgumentCaptor<CourseStaff> captor = ArgumentCaptor.forClass(CourseStaff.class);
    verify(courseStaffRepository, times(1)).save(captor.capture());

    CourseStaff saved = captor.getValue();
    assertEquals("Chris", saved.getFirstName());
    assertEquals("Gaucho", saved.getLastName());
    assertEquals("cgaucho@example.org", saved.getEmail());
  }

  @Test
  @WithInstructorCoursePermissions
  public void upload_returns_bad_request_for_invalid_headers() throws Exception {

    Course course =
        Course.builder()
            .id(1L)
            .courseName("CS156")
            .orgName("ucsb-cs156-s25")
            .term("S25")
            .school(School.UCSB)
            .build();

    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(course));

    String badCSV = "name,address,phone\nChris,123 Main,555-1234\n";

    MockMultipartFile file =
        new MockMultipartFile("file", "staff.csv", MediaType.TEXT_PLAIN_VALUE, badCSV.getBytes());

    mockMvc
        .perform(
            multipart("/api/coursestaff/upload/csv").file(file).param("courseId", "1").with(csrf()))
        .andExpect(status().isBadRequest());

    verify(courseStaffRepository, never()).save(any(CourseStaff.class));
  }

  @Test
  @WithInstructorCoursePermissions
  public void upload_returns_bad_request_for_empty_csv() throws Exception {

    Course course =
        Course.builder()
            .id(1L)
            .courseName("CS156")
            .orgName("ucsb-cs156-s25")
            .term("S25")
            .school(School.UCSB)
            .build();

    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(course));

    MockMultipartFile file =
        new MockMultipartFile("file", "staff.csv", MediaType.TEXT_PLAIN_VALUE, "".getBytes());

    mockMvc
        .perform(
            multipart("/api/coursestaff/upload/csv").file(file).param("courseId", "1").with(csrf()))
        .andExpect(status().isBadRequest());

    verify(courseStaffRepository, never()).save(any(CourseStaff.class));
  }

  @Test
  @WithInstructorCoursePermissions
  public void upload_returns_bad_request_for_short_row() throws Exception {

    Course course =
        Course.builder()
            .id(1L)
            .courseName("CS156")
            .orgName("ucsb-cs156-s25")
            .term("S25")
            .school(School.UCSB)
            .build();

    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(course));

    String shortRowCSV = "firstName,lastName,email\nChris,Gaucho\n";

    MockMultipartFile file =
        new MockMultipartFile(
            "file", "staff.csv", MediaType.TEXT_PLAIN_VALUE, shortRowCSV.getBytes());

    mockMvc
        .perform(
            multipart("/api/coursestaff/upload/csv").file(file).param("courseId", "1").with(csrf()))
        .andExpect(status().isBadRequest());
  }

  @Test
  @WithInstructorCoursePermissions
  public void upload_returns_not_found_for_missing_course() throws Exception {

    when(courseRepository.findById(eq(99L))).thenReturn(Optional.empty());

    MockMultipartFile file =
        new MockMultipartFile(
            "file", "staff.csv", MediaType.TEXT_PLAIN_VALUE, sampleCSV.getBytes());

    mockMvc
        .perform(
            multipart("/api/coursestaff/upload/csv")
                .file(file)
                .param("courseId", "99")
                .with(csrf()))
        .andExpect(status().isNotFound());

    verify(courseStaffRepository, never()).save(any(CourseStaff.class));
  }

  @Test
  @WithMockUser(roles = {"USER"})
  public void regular_user_cannot_upload_staff_csv() throws Exception {

    MockMultipartFile file =
        new MockMultipartFile(
            "file", "staff.csv", MediaType.TEXT_PLAIN_VALUE, sampleCSV.getBytes());

    mockMvc
        .perform(
            multipart("/api/coursestaff/upload/csv").file(file).param("courseId", "1").with(csrf()))
        .andExpect(status().isForbidden());

    verify(courseStaffRepository, never()).save(any(CourseStaff.class));
  }

  @Test
  @WithInstructorCoursePermissions
  public void upload_returns_bad_request_for_too_few_headers() throws Exception {

    Course course =
        Course.builder()
            .id(1L)
            .courseName("CS156")
            .orgName("ucsb-cs156-s25")
            .term("S25")
            .school(School.UCSB)
            .build();

    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(course));

    String fewHeadersCSV = "firstName,lastName\nChris,Gaucho\n";

    MockMultipartFile file =
        new MockMultipartFile(
            "file", "staff.csv", MediaType.TEXT_PLAIN_VALUE, fewHeadersCSV.getBytes());

    mockMvc
        .perform(
            multipart("/api/coursestaff/upload/csv").file(file).param("courseId", "1").with(csrf()))
        .andExpect(status().isBadRequest());

    verify(courseStaffRepository, never()).save(any(CourseStaff.class));
  }
}
