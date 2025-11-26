package edu.ucsb.cs156.frontiers.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.ucsb.cs156.frontiers.ControllerTestCase;
import edu.ucsb.cs156.frontiers.annotations.WithInstructorCoursePermissions;
import edu.ucsb.cs156.frontiers.entities.Assignment;
import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.entities.User;
import edu.ucsb.cs156.frontiers.enums.AssignmentType;
import edu.ucsb.cs156.frontiers.enums.Permission;
import edu.ucsb.cs156.frontiers.enums.Visibility;
import edu.ucsb.cs156.frontiers.repositories.AssignmentRepository;
import edu.ucsb.cs156.frontiers.repositories.CourseRepository;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;

@Slf4j
@WebMvcTest(controllers = AssignmentsController.class)
public class AssignmentsControllerTests extends ControllerTestCase {
  @MockitoBean private CourseRepository courseRepository;

  @MockitoBean private AssignmentRepository assignmentRepository;

  // Tests for the POST endpoint
  @Test
  public void logged_out_users_cannot_post() throws Exception {
    mockMvc
        .perform(post("/api/admin/assignments/post"))
        .andExpect(status().is(403)); // logged out users cannot post
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void logged_in_users_cannot_post() throws Exception {
    mockMvc
        .perform(post("/api/admin/assignments/post"))
        .andExpect(status().is(403)); // logged in users cannot post
  }

  /** Test that ROLE_INSTRUCTOR can create an assignment */
  @Test
  @WithInstructorCoursePermissions
  public void testPostAssignment_byInstructor() throws Exception {
    User user = currentUserService.getCurrentUser().getUser();

    // Arrange
    Course course =
        Course.builder()
            .id(1L)
            .courseName("CS156")
            .term("S25")
            .school("UCSB")
            .instructorEmail(user.getEmail())
            .build();

    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(course));

    Assignment assignment =
        Assignment.builder()
            .course(course)
            .name("HW1")
            .asnType(AssignmentType.INDIVIDUAL)
            .visibility(Visibility.PUBLIC)
            .permission(Permission.READ)
            .build();

    when(assignmentRepository.save(any(Assignment.class))).thenReturn(assignment);

    // Act
    MvcResult response =
        mockMvc
            .perform(
                post("/api/assignments/post")
                    .with(csrf())
                    .param("courseId", "1")
                    .param("name", "HW1")
                    .param("asnType", "INDIVIDUAL")
                    .param("visibility", "PUBLIC")
                    .param("permission", "READ"))
            .andExpect(status().isOk())
            .andReturn();

    // Assert repository save
    verify(assignmentRepository, times(1)).save(any(Assignment.class));

    // Assert returned JSON
    String expectedJson = mapper.writeValueAsString(assignment);
    assertEquals(expectedJson, response.getResponse().getContentAsString());
  }

  /**
   * Test that you cannot post a single roster student for a course that does not exist
   *
   * @throws Exception
   */
  @Test
  @WithMockUser(roles = {"ADMIN"})
  public void test_AdminCannotPostAssignmentForCourseThatDoesNotExist() throws Exception {
    when(courseRepository.findById(eq(1L))).thenReturn(Optional.empty());

    // act

    MvcResult response =
        mockMvc
            .perform(
                post("/api/assignments/post")
                    .with(csrf())
                    .param("courseId", "1")
                    .param("name", "HW1")
                    .param("asnType", "INDIVIDUAL")
                    .param("visibility", "PUBLIC")
                    .param("permission", "READ"))
            .andExpect(status().isNotFound())
            .andReturn();

    // assert

    String responseString = response.getResponse().getContentAsString();
    Map<String, String> expectedMap =
        Map.of(
            "type", "EntityNotFoundException",
            "message", "Course with id 1 not found");
    String expectedJson = mapper.writeValueAsString(expectedMap);
    assertEquals(expectedJson, responseString);
  }

  @Test
  @WithInstructorCoursePermissions
  public void testUpdateAssignment_success() throws Exception {
    User user = currentUserService.getCurrentUser().getUser();

    Course course =
        Course.builder()
            .id(1L)
            .courseName("CS156")
            .term("S25")
            .school("UCSB")
            .instructorEmail(user.getEmail())
            .build();

    Assignment assignment =
        Assignment.builder()
            .course(course)
            .name("HW1")
            .asnType(AssignmentType.INDIVIDUAL)
            .visibility(Visibility.PUBLIC)
            .permission(Permission.READ)
            .build();

    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(course));
    when(assignmentRepository.findById(eq(1L))).thenReturn(Optional.of(assignment));

    Assignment updatedAssignment =
        Assignment.builder()
            .course(course)
            .name("HW1")
            .asnType(AssignmentType.TEAM)
            .visibility(Visibility.PRIVATE)
            .permission(Permission.WRITE)
            .build();

    when(assignmentRepository.save(any(Assignment.class))).thenReturn(updatedAssignment);

    MvcResult response =
        mockMvc
            .perform(
                put("/api/assignments/put")
                    .with(csrf())
                    .param("assignmentId", "1")
                    .param("name", "HW1")
                    .param("asnType", "TEAM")
                    .param("visibility", "PRIVATE")
                    .param("permission", "WRITE"))
            .andExpect(status().isOk())
            .andReturn();

    ArgumentCaptor<Assignment> captor = ArgumentCaptor.forClass(Assignment.class);
    verify(assignmentRepository).save(captor.capture());
    Assignment saved = captor.getValue();
    assertEquals(AssignmentType.TEAM, saved.getAsnType());
    assertEquals(Visibility.PRIVATE, saved.getVisibility());
    assertEquals(Permission.WRITE, saved.getPermission());

    String responseString = response.getResponse().getContentAsString();
    String expectedJson = mapper.writeValueAsString(updatedAssignment);
    assertEquals(expectedJson, responseString);
  }

  // DELETE endpoint tests
  @Test
  public void logged_out_users_cannot_delete() throws Exception {
    mockMvc
        .perform(delete("/api/assignments/10").param("courseId", "1").with(csrf()))
        .andExpect(status().is(403)); // forbidden
  }

  @Test
  @WithMockUser(roles = {"USER"})
  public void logged_in_users_without_permission_cannot_delete() throws Exception {
    mockMvc
        .perform(delete("/api/assignments/10").param("courseId", "1").with(csrf()))
        .andExpect(status().is(403));
  }

  @Test
  @WithInstructorCoursePermissions
  public void instructor_can_delete_assignment() throws Exception {
    // Arrange course
    Course course = Course.builder().id(1L).build();
    when(courseRepository.findById(1L)).thenReturn(Optional.of(course));

    // Arrange assignment
    Assignment assignment = Assignment.builder().id(10L).name("HW1").course(course).build();
    when(assignmentRepository.findById(10L)).thenReturn(Optional.of(assignment));

    // Act
    MvcResult response =
        mockMvc
            .perform(delete("/api/assignments/10").with(csrf()).param("courseId", "1"))
            .andExpect(status().isOk())
            .andReturn();

    // Assert interactions
    verify(courseRepository, times(1)).findById(1L);
    verify(assignmentRepository, times(1)).findById(10L);
    verify(assignmentRepository, times(1)).delete(assignment);

    // Assert message
    String expectedJson =
        mapper.writeValueAsString(Map.of("message", "Assignment with id 10 deleted"));
    assertEquals(expectedJson, response.getResponse().getContentAsString());
  }

  @Test
  @WithInstructorCoursePermissions
  public void testUpdateAssignment_course_not_found() throws Exception {

    when(courseRepository.findById(eq(42L))).thenReturn(Optional.empty());

    MvcResult response =
        mockMvc
            .perform(
                put("/api/assignments/put")
                    .with(csrf())
                    .param("assignmentId", "42")
                    .param("name", "HW1")
                    .param("asnType", "INDIVIDUAL")
                    .param("visibility", "PUBLIC")
                    .param("permission", "READ"))
            .andExpect(status().isNotFound())
            .andReturn();

    // assert

    String responseString = response.getResponse().getContentAsString();
    Map<String, String> expectedMap =
        Map.of(
            "type", "EntityNotFoundException",
            "message", "Assignment with id 42 not found");
    String expectedJson = mapper.writeValueAsString(expectedMap);
    assertEquals(expectedJson, responseString);
  }

  @Test
  @WithInstructorCoursePermissions
  public void delete_assignment_returns_404_when_course_not_found() throws Exception {
    when(courseRepository.findById(999L)).thenReturn(Optional.empty());

    mockMvc
        .perform(delete("/api/assignments/10").with(csrf()).param("courseId", "999"))
        .andExpect(status().isNotFound());

    verify(courseRepository, times(1)).findById(999L);
    verify(assignmentRepository, never()).findById(any());
    verify(assignmentRepository, never()).delete(any());
  }

  @Test
  @WithInstructorCoursePermissions
  public void delete_assignment_returns_404_when_assignment_not_found() throws Exception {
    Course course = Course.builder().id(1L).build();
    when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
    when(assignmentRepository.findById(10L)).thenReturn(Optional.empty());

    mockMvc
        .perform(delete("/api/assignments/10").with(csrf()).param("courseId", "1"))
        .andExpect(status().isNotFound());

    verify(courseRepository, times(1)).findById(1L);
    verify(assignmentRepository, times(1)).findById(10L);
    verify(assignmentRepository, never()).delete(any());
  }
}
