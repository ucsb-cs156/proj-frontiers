package edu.ucsb.cs156.frontiers.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.ucsb.cs156.frontiers.ControllerTestCase;
import edu.ucsb.cs156.frontiers.annotations.WithStaffCoursePermissions;
import edu.ucsb.cs156.frontiers.entities.Assignment;
import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.repositories.AssignmentRepository;
import edu.ucsb.cs156.frontiers.repositories.CourseRepository;
import edu.ucsb.cs156.frontiers.repositories.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;

@WebMvcTest(controllers = AssignmentController.class)
public class AssignmentControllerTests extends ControllerTestCase {

  @MockitoBean private AssignmentRepository assignmentRepository;

  @MockitoBean private CourseRepository courseRepository;

  @MockitoBean private UserRepository userRepository;

  @Test
  public void logged_out_users_cannot_post() throws Exception {
    mockMvc
        .perform(
            post("/api/assignments")
                .with(csrf())
                .param("courseId", "1")
                .param("name", "HW1")
                .param("asn_type", "individual")
                .param("visibility", "public")
                .param("permission", "read"))
        .andExpect(status().is(403));
  }

  @Test
  @WithStaffCoursePermissions
  public void post_assignment_individual_returns_success() throws Exception {

    Course course =
        Course.builder()
            .id(1L)
            .installationId("INST123")
            .orgName("UCSB")
            .instructorEmail("prof@ucsb.edu")
            .courseName("CMPSC156")
            .term("F25")
            .school("Engineering")
            .canvasCourseId("12345")
            .build();

    when(courseRepository.findById(1L)).thenReturn(Optional.of(course));

    Assignment assignment =
        Assignment.builder()
            .id(1L)
            .course(course)
            .name("HW1")
            .asn_type("individual")
            .visibility("public")
            .permission("read")
            .build();

    when(assignmentRepository.save(any(Assignment.class))).thenReturn(assignment);

    MvcResult response =
        mockMvc
            .perform(
                post("/api/assignments")
                    .with(csrf())
                    .param("courseId", "1")
                    .param("name", "HW1")
                    .param("asn_type", "individual")
                    .param("visibility", "public")
                    .param("permission", "read"))
            .andExpect(status().isOk())
            .andReturn();

    verify(assignmentRepository, times(1)).save(any(Assignment.class));

    String responseString = response.getResponse().getContentAsString();
    String expectedJson = mapper.writeValueAsString(assignment);
    assertEquals(expectedJson, responseString);
  }

  @Test
  @WithStaffCoursePermissions
  public void post_assignment_team_with_admin_permission_returns_success() throws Exception {

    Course course =
        Course.builder()
            .id(1L)
            .installationId("INST123")
            .orgName("UCSB")
            .instructorEmail("prof@ucsb.edu")
            .courseName("CMPSC156")
            .term("F25")
            .school("Engineering")
            .canvasCourseId("12345")
            .build();

    when(courseRepository.findById(1L)).thenReturn(Optional.of(course));

    Assignment assignment =
        Assignment.builder()
            .id(2L)
            .course(course)
            .name("Team Project")
            .asn_type("team")
            .visibility("private")
            .permission("admin")
            .build();

    when(assignmentRepository.save(any(Assignment.class))).thenReturn(assignment);

    MvcResult response =
        mockMvc
            .perform(
                post("/api/assignments")
                    .with(csrf())
                    .param("courseId", "1")
                    .param("name", "Team Project")
                    .param("asn_type", "team")
                    .param("visibility", "private")
                    .param("permission", "admin"))
            .andExpect(status().isOk())
            .andReturn();

    verify(assignmentRepository, times(1)).save(any(Assignment.class));

    String responseString = response.getResponse().getContentAsString();
    String expectedJson = mapper.writeValueAsString(assignment);
    assertEquals(expectedJson, responseString);
  }

  @Test
  @WithStaffCoursePermissions
  public void post_assignment_returns_404_when_course_not_found() throws Exception {

    when(courseRepository.findById(999L)).thenReturn(Optional.empty());

    MvcResult response =
        mockMvc
            .perform(
                post("/api/assignments")
                    .with(csrf())
                    .param("courseId", "999")
                    .param("name", "HW1")
                    .param("asn_type", "individual")
                    .param("visibility", "public")
                    .param("permission", "write"))
            .andExpect(status().isNotFound())
            .andReturn();

    verify(assignmentRepository, never()).save(any(Assignment.class));
  }

  // PUT Tests
  @Test
  public void logged_out_users_cannot_edit() throws Exception {
    mockMvc
        .perform(
            put("/api/assignments/{id}", 1L)
                .with(csrf())
                .param("courseId", "1")
                .param("asn_type", "individual")
                .param("visibility", "public")
                .param("permission", "read"))
        .andExpect(status().is(403));
  }

  @Test
  @WithStaffCoursePermissions
  public void edit_assignment_individual_returns_success() throws Exception {

    Course course =
        Course.builder()
            .id(1L)
            .installationId("INST123")
            .orgName("UCSB")
            .instructorEmail("prof@ucsb.edu")
            .courseName("CMPSC156")
            .term("F25")
            .school("Engineering")
            .canvasCourseId("12345")
            .build();

    when(courseRepository.findById(1L)).thenReturn(Optional.of(course));

    Assignment assignment =
        Assignment.builder()
            .id(1L)
            .course(course)
            .name("HW1")
            .asn_type("team")
            .visibility("private")
            .permission("write")
            .build();

    when(assignmentRepository.findById(1L)).thenReturn(Optional.of(assignment));

    Assignment updatedAssignment =
        Assignment.builder()
            .id(1L)
            .course(course)
            .name("HW1")
            .asn_type("individual")
            .visibility("public")
            .permission("read")
            .build();

    when(assignmentRepository.save(any(Assignment.class))).thenReturn(updatedAssignment);

    MvcResult response =
        mockMvc
            .perform(
                put("/api/assignments/1")
                    .with(csrf())
                    .param("courseId", "1")
                    .param("asn_type", "individual")
                    .param("visibility", "public")
                    .param("permission", "read"))
            .andExpect(status().isOk())
            .andReturn();

    verify(assignmentRepository, times(1)).findById(1L);
    verify(assignmentRepository, times(1)).save(any(Assignment.class));

    assertEquals(updatedAssignment, assignment);

    String responseString = response.getResponse().getContentAsString();
    String expectedJson = mapper.writeValueAsString(updatedAssignment);
    assertEquals(expectedJson, responseString);
  }

  @Test
  @WithStaffCoursePermissions
  public void edit_assignment_team_with_admin_permission_returns_success() throws Exception {

    Course course =
        Course.builder()
            .id(1L)
            .installationId("INST123")
            .orgName("UCSB")
            .instructorEmail("prof@ucsb.edu")
            .courseName("CMPSC156")
            .term("F25")
            .school("Engineering")
            .canvasCourseId("12345")
            .build();

    when(courseRepository.findById(1L)).thenReturn(Optional.of(course));

    Assignment assignment =
        Assignment.builder()
            .id(1L)
            .course(course)
            .name("HW1")
            .asn_type("team")
            .visibility("private")
            .permission("admin")
            .build();

    when(assignmentRepository.findById(1L)).thenReturn(Optional.of(assignment));

    Assignment updatedAssignment =
        Assignment.builder()
            .id(1L)
            .course(course)
            .name("HW1")
            .asn_type("individual")
            .visibility("public")
            .permission("admin")
            .build();

    when(assignmentRepository.save(any(Assignment.class))).thenReturn(updatedAssignment);

    MvcResult response =
        mockMvc
            .perform(
                put("/api/assignments/1")
                    .with(csrf())
                    .param("courseId", "1")
                    .param("asn_type", "individual")
                    .param("visibility", "public")
                    .param("permission", "admin"))
            .andExpect(status().isOk())
            .andReturn();

    verify(assignmentRepository, times(1)).findById(1L);
    verify(assignmentRepository, times(1)).save(any(Assignment.class));

    assertEquals(updatedAssignment, assignment);

    String responseString = response.getResponse().getContentAsString();
    String expectedJson = mapper.writeValueAsString(updatedAssignment);
    assertEquals(expectedJson, responseString);
  }

  @Test
  @WithStaffCoursePermissions
  public void edit_assignment_returns_404_when_course_not_found() throws Exception {

    when(courseRepository.findById(999L)).thenReturn(Optional.empty());

    MvcResult response =
        mockMvc
            .perform(
                put("/api/assignments/1")
                    .with(csrf())
                    .param("courseId", "999")
                    .param("asn_type", "individual")
                    .param("visibility", "public")
                    .param("permission", "write"))
            .andExpect(status().isNotFound())
            .andReturn();

    verify(assignmentRepository, never()).save(any(Assignment.class));
  }

  // Add case edit assignment returns 404 when assignemnt not found
  @Test
  @WithStaffCoursePermissions
  public void edit_assignment_returns_404_when_assignment_not_found() throws Exception {

    Course course =
        Course.builder()
            .id(1L)
            .installationId("INST123")
            .orgName("UCSB")
            .instructorEmail("prof@ucsb.edu")
            .courseName("CMPSC156")
            .term("F25")
            .school("Engineering")
            .canvasCourseId("12345")
            .build();

    when(courseRepository.findById(1L)).thenReturn(Optional.of(course));

    when(assignmentRepository.findById(999L)).thenReturn(Optional.empty());

    mockMvc
        .perform(
            put("/api/assignments/999")
                .with(csrf())
                .param("courseId", "1")
                .param("asn_type", "individual")
                .param("visibility", "public")
                .param("permission", "read"))
        .andExpect(status().isNotFound());

    verify(assignmentRepository, never()).save(any(Assignment.class));
  }

  // DELETE tests
  @Test
  public void logged_out_users_cannot_delete() throws Exception {
    mockMvc
        .perform(delete("/api/assignments/1").with(csrf()).param("courseId", "1"))
        .andExpect(status().is(403));
  }

  @Test
  @WithStaffCoursePermissions
  public void delete_assignment_returns_success() throws Exception {

    Course course =
        Course.builder()
            .id(1L)
            .installationId("INST123")
            .orgName("UCSB")
            .instructorEmail("prof@ucsb.edu")
            .courseName("CMPSC156")
            .term("F25")
            .school("Engineering")
            .canvasCourseId("12345")
            .build();

    when(courseRepository.findById(1L)).thenReturn(Optional.of(course));

    Assignment assignment =
        Assignment.builder()
            .id(1L)
            .course(course)
            .name("HW1")
            .asn_type("individual")
            .visibility("public")
            .permission("read")
            .build();

    when(assignmentRepository.findById(1L)).thenReturn(Optional.of(assignment));

    MvcResult response =
        mockMvc
            .perform(delete("/api/assignments/1").with(csrf()).param("courseId", "1"))
            .andExpect(status().isOk())
            .andReturn();

    verify(courseRepository, times(1)).findById(1L);
    verify(assignmentRepository, times(1)).findById(1L);
    verify(assignmentRepository, times(1)).delete(assignment);

    String responseString = response.getResponse().getContentAsString();
    String expectedJson = "{\"message\":\"Assignment with id 1 deleted\"}";
    assertEquals(expectedJson, responseString);
  }

  @Test
  @WithStaffCoursePermissions
  public void delete_assignment_returns_404_when_course_not_found() throws Exception {

    when(courseRepository.findById(999L)).thenReturn(Optional.empty());

    mockMvc
        .perform(delete("/api/assignments/1").with(csrf()).param("courseId", "999"))
        .andExpect(status().isNotFound());

    verify(courseRepository, times(1)).findById(999L);
    verify(assignmentRepository, never()).findById(any(Long.class));
    verify(assignmentRepository, never()).delete(any(Assignment.class));
  }

  @Test
  @WithStaffCoursePermissions
  public void delete_assignment_returns_404_when_assignment_not_found() throws Exception {

    Course course =
        Course.builder()
            .id(1L)
            .installationId("INST123")
            .orgName("UCSB")
            .instructorEmail("prof@ucsb.edu")
            .courseName("CMPSC156")
            .term("F25")
            .school("Engineering")
            .canvasCourseId("12345")
            .build();

    when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
    when(assignmentRepository.findById(999L)).thenReturn(Optional.empty());

    mockMvc
        .perform(delete("/api/assignments/999").with(csrf()).param("courseId", "1"))
        .andExpect(status().isNotFound());

    verify(courseRepository, times(1)).findById(1L);
    verify(assignmentRepository, never()).delete(any(Assignment.class));
  }
}
