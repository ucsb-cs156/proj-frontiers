package edu.ucsb.cs156.frontiers.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.ucsb.cs156.frontiers.ControllerTestCase;
import edu.ucsb.cs156.frontiers.entities.Assignment;
import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.enums.AssignmentType;
import edu.ucsb.cs156.frontiers.enums.Permission;
import edu.ucsb.cs156.frontiers.enums.Visibility;
import edu.ucsb.cs156.frontiers.repositories.AssignmentRepository;
import edu.ucsb.cs156.frontiers.repositories.CourseRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;

@Slf4j
@WebMvcTest(controllers = AssignmentController.class)
public class AssignmentControllerTests extends ControllerTestCase {
  @MockitoBean private CourseRepository courseRepository;

  @MockitoBean private AssignmentRepository assignmentRepository;

  /** Test that ROLE_INSTRUCTOR can create an assignment */
  @Test
  @WithMockUser(roles = {"INSTRUCTOR"})
  public void testPostAssignment_byInstructor() throws Exception {
    User user = currentUserService.getCurrentUser().getUser();

    // Arrange
    Course course =
        Course.builder()
            .courseName("CS156")
            .term("S25")
            .school("UCSB")
            .instructorEmail(user.getEmail())
            .build();

    when(courseRepository.save(any(Course.class))).thenReturn(course);

    Assignment assignment =
        Assignment.builder()
            .courseId(course)
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
}
