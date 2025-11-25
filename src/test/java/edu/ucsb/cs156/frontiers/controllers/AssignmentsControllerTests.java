package edu.ucsb.cs156.frontiers.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.ucsb.cs156.frontiers.ControllerTestCase;
import edu.ucsb.cs156.frontiers.annotations.WithInstructorCoursePermissions;
import edu.ucsb.cs156.frontiers.controllers.AssignmentsController.InstructorAssignmentView;
import edu.ucsb.cs156.frontiers.entities.Assignment;
import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.entities.User;
import edu.ucsb.cs156.frontiers.repositories.AdminRepository;
import edu.ucsb.cs156.frontiers.repositories.AssignmentRepository;
import edu.ucsb.cs156.frontiers.repositories.CourseRepository;
import edu.ucsb.cs156.frontiers.repositories.CourseStaffRepository;
import edu.ucsb.cs156.frontiers.repositories.InstructorRepository;
import edu.ucsb.cs156.frontiers.repositories.RosterStudentRepository;
import edu.ucsb.cs156.frontiers.repositories.UserRepository;
import edu.ucsb.cs156.frontiers.services.CurrentUserService;
import edu.ucsb.cs156.frontiers.services.OrganizationLinkerService;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;

@Slf4j
@WebMvcTest(controllers = AssignmentsController.class)
public class AssignmentsControllerTests extends ControllerTestCase {

  @MockitoBean private AssignmentRepository assignmentRepository;

  @MockitoBean private CourseRepository courseRepository;

  @Autowired private CurrentUserService currentUserService;

  @MockitoBean private OrganizationLinkerService linkerService;

  @MockitoBean private UserRepository userRepository;

  @MockitoBean private RosterStudentRepository rosterStudentRepository;

  @MockitoBean private CourseStaffRepository courseStaffRepository;

  @MockitoBean private InstructorRepository instructorRepository;

  @MockitoBean private AdminRepository adminRepository;

  /** Test that ROLE_ADMIN can create an assignment */
  @Test
  @WithMockUser(roles = {"ADMIN"})
  public void testPostAssignment_byAdmin() throws Exception {

    User user = currentUserService.getCurrentUser().getUser();

    // arrange
    Course course =
        Course.builder()
            .id(1L)
            .courseName("CS156")
            .term("F25")
            .school("UCSB")
            .instructorEmail(user.getEmail())
            .build();

    when(courseRepository.findById(1L)).thenReturn(Optional.of(course));

    Assignment assignment =
        Assignment.builder()
            .course(course)
            .name("Assignment 1")
            .asnType("individual")
            .visibility("public")
            .permission("write")
            .build();

    when(assignmentRepository.save(any(Assignment.class))).thenReturn(assignment);

    // act

    MvcResult response =
        mockMvc
            .perform(
                post("/api/assignments/post")
                    .with(csrf())
                    .param("courseId", "1")
                    .param("name", "Assignment 1")
                    .param("asn_type", "individual")
                    .param("visibility", "public")
                    .param("permission", "write"))
            .andExpect(status().isOk())
            .andReturn();

    // assert

    verify(assignmentRepository, times(1)).save(eq(assignment));

    String responseString = response.getResponse().getContentAsString();
    String expectedJson = mapper.writeValueAsString(new InstructorAssignmentView(assignment));
    assertEquals(expectedJson, responseString);
  }

  /** Test that ROLE_INSTRUCTOR can create an assignment */
  @Test
  @WithMockUser(roles = {"INSTRUCTOR"})
  public void testPostAssignment_byInstructor() throws Exception {

    User user = currentUserService.getCurrentUser().getUser();

    // arrange
    Course course =
        Course.builder()
            .id(1L)
            .courseName("CS156")
            .term("F25")
            .school("UCSB")
            .instructorEmail(user.getEmail())
            .build();

    when(courseRepository.findById(1L)).thenReturn(Optional.of(course));

    Assignment assignment =
        Assignment.builder()
            .course(course)
            .name("Assignment 1")
            .asnType("individual")
            .visibility("public")
            .permission("write")
            .build();

    when(assignmentRepository.save(any(Assignment.class))).thenReturn(assignment);

    // act

    MvcResult response =
        mockMvc
            .perform(
                post("/api/assignments/post")
                    .with(csrf())
                    .param("courseId", "1")
                    .param("name", "Assignment 1")
                    .param("asn_type", "individual")
                    .param("visibility", "public")
                    .param("permission", "write"))
            .andExpect(status().isOk())
            .andReturn();

    // assert
    verify(assignmentRepository, times(1)).save(eq(assignment));

    String responseString = response.getResponse().getContentAsString();
    String expectedJson = mapper.writeValueAsString(new InstructorAssignmentView(assignment));
    assertEquals(expectedJson, responseString);
  }

  @Test
  @WithMockUser(roles = {"ADMIN"})
  public void testPostAssigment_courseNotFound_throwsException() throws Exception {
    when(courseRepository.findById(999L)).thenReturn(Optional.empty());

    MvcResult response =
        mockMvc
            .perform(
                post("/api/assignments/post")
                    .with(csrf())
                    .param("courseId", "999")
                    .param("name", "Assignment X")
                    .param("asn_type", "individual")
                    .param("visibility", "public")
                    .param("permission", "write"))
            .andExpect(status().isNotFound())
            .andReturn();

    String responseBody = response.getResponse().getContentAsString();
    assertTrue(responseBody.contains("Course with id 999 not found"));
  }

  @Test
  @WithMockUser(roles = {"ADMIN"})
  public void updateAssignment_success_admin() throws Exception {

    Assignment assignment =
        Assignment.builder()
            .id(1L)
            .name("Assignment 1")
            .asnType("individual")
            .visibility("public")
            .permission("write")
            .build();

    Assignment updatedAssignment =
        Assignment.builder()
            .id(1L)
            .name("Assignment 1")
            .asnType("group")
            .visibility("private")
            .permission("read")
            .build();

    when(assignmentRepository.findById(eq(1L))).thenReturn(Optional.of(assignment));
    when(assignmentRepository.save(any(Assignment.class))).thenReturn(updatedAssignment);

    MvcResult result =
        mockMvc
            .perform(
                put("/api/assignments")
                    .param("id", "1")
                    .param("asn_type", "group")
                    .param("visibility", "private")
                    .param("permission", "read")
                    .with(csrf()))
            .andExpect(status().isOk())
            .andReturn();

    verify(assignmentRepository, times(1)).findById(eq(1L));
    verify(assignmentRepository, times(1)).save(any(Assignment.class));

    String expectedJson =
        mapper.writeValueAsString(new InstructorAssignmentView(updatedAssignment));
    assertEquals(expectedJson, result.getResponse().getContentAsString());
  }

  @Test
  @WithMockUser(roles = {"INSTRUCTOR"})
  public void updateAssignment_notFound() throws Exception {
    when(assignmentRepository.findById(eq(2L))).thenReturn(Optional.empty());

    MvcResult result =
        mockMvc
            .perform(
                put("/api/assignments")
                    .param("id", "2")
                    .param("asn_type", "individual")
                    .param("visibility", "public")
                    .param("permission", "write")
                    .with(csrf()))
            .andExpect(status().isForbidden())
            .andReturn();

    verify(assignmentRepository, never()).save(any(Assignment.class));
  }

  @Test
  @WithMockUser(roles = {"USER"})
  public void updateAssignment_forbidden_for_non_instructor() throws Exception {
    mockMvc
        .perform(
            put("/api/assignments")
                .param("id", "1")
                .param("asn_type", "group")
                .param("visibility", "private")
                .param("permission", "read")
                .with(csrf()))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithInstructorCoursePermissions
  public void update_assignment_not_found_returns_not_found() throws Exception {
    when(assignmentRepository.findById(eq(1L))).thenReturn(Optional.empty());
    MvcResult response =
        mockMvc
            .perform(
                put("/api/assignments")
                    .param("id", "1")
                    .param("asn_type", "group")
                    .param("visibility", "private")
                    .param("permission", "read")
                    .with(csrf()))
            .andExpect(status().isNotFound())
            .andReturn();
    String responseString = response.getResponse().getContentAsString();
    Map<String, String> expectedMap =
        Map.of(
            "type", "EntityNotFoundException",
            "message", "Assignment with id 1 not found");
    String expectedJson = mapper.writeValueAsString(expectedMap);
    assertEquals(expectedJson, responseString);
    verify(assignmentRepository).findById(eq(1L));
    verifyNoMoreInteractions(assignmentRepository);
  }

  @Test
  @WithInstructorCoursePermissions
  public void update_assignment_success_returns_ok() throws Exception {
    User user = currentUserService.getCurrentUser().getUser();

    Assignment originalAssignment =
        Assignment.builder()
            .id(1L)
            .name("Assignment")
            .asnType("individual")
            .visibility("public")
            .permission("write")
            .build();

    Assignment updatedAssignment =
        Assignment.builder()
            .id(1L)
            .name("Assignment")
            .asnType("group")
            .visibility("private")
            .permission("read")
            .build();

    when(assignmentRepository.findById(eq(1L))).thenReturn(Optional.of(originalAssignment));
    when(assignmentRepository.save(any(Assignment.class))).thenReturn(updatedAssignment);

    MvcResult response =
        mockMvc
            .perform(
                put("/api/assignments")
                    .param("id", "1")
                    .param("asn_type", "group")
                    .param("visibility", "private")
                    .param("permission", "read")
                    .with(csrf()))
            .andExpect(status().isOk())
            .andReturn();

    verify(assignmentRepository).findById(eq(1L));
    verify(assignmentRepository).save(updatedAssignment);

    String responseString = response.getResponse().getContentAsString();
    String expectedJson =
        mapper.writeValueAsString(new InstructorAssignmentView(updatedAssignment));
    assertEquals(expectedJson, responseString);
  }
}
