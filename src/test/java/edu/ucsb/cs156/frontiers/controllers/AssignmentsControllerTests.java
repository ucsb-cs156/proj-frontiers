package edu.ucsb.cs156.frontiers.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.ucsb.cs156.frontiers.ControllerTestCase;
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
}
