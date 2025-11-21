package edu.ucsb.cs156.frontiers.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.ucsb.cs156.frontiers.ControllerTestCase;
import edu.ucsb.cs156.frontiers.annotations.WithInstructorCoursePermissions;
import edu.ucsb.cs156.frontiers.entities.Assignment;
import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.enums.AssignmentType;
import edu.ucsb.cs156.frontiers.enums.RepositoryPermissions;
import edu.ucsb.cs156.frontiers.enums.Visibility;
import edu.ucsb.cs156.frontiers.repositories.AssignmentRepository;
import edu.ucsb.cs156.frontiers.repositories.CourseRepository;
import edu.ucsb.cs156.frontiers.repositories.RosterStudentRepository;
import edu.ucsb.cs156.frontiers.services.OrganizationMemberService;
import edu.ucsb.cs156.frontiers.services.UpdateUserService;
import edu.ucsb.cs156.frontiers.services.jobs.JobService;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;

@Slf4j
@WebMvcTest(controllers = AssignmentsController.class)
public class AssignmentsControllerTests extends ControllerTestCase {

  @MockitoBean private CourseRepository courseRepository;

  @MockitoBean private AssignmentRepository assignmentRepository;

  @MockitoBean private RosterStudentRepository rosterStudentRepository;

  @MockitoBean private UpdateUserService updateUserService;

  @MockitoBean private OrganizationMemberService organizationMemberService;

  @MockitoBean private JobService service;

  final String assignmentPostEndpoint = "/api/assignments/post";
  final String assignmentGetEndpoint = "/api/assignments/";
  final String assignmentGetAllEndpoint = "/api/assignments/get";

  Course course1 =
      Course.builder()
          .id(1L)
          .courseName("CS156")
          .rosterStudents(List.of())
          .orgName("ucsb-cs156-f25")
          .term("F25")
          .school("UCSB")
          .assignments(null)
          .build();

  // POST /api/assignments/post
  @Test
  @WithInstructorCoursePermissions
  public void logged_in_instructor_can_post() throws Exception {
    Assignment expectedAssignment =
        Assignment.builder()
            .course(course1)
            .name("Test Assignment")
            .asn_type(AssignmentType.TEAM)
            .visibility(Visibility.PRIVATE)
            .permission(RepositoryPermissions.WRITE)
            .build();

    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(course1));
    when(assignmentRepository.save(any(Assignment.class))).thenReturn(expectedAssignment);

    MvcResult response =
        mockMvc
            .perform(
                post(assignmentPostEndpoint)
                    .with(csrf())
                    .param("courseId", "1")
                    .param("name", "Test Assignment")
                    .param("asn_type", "TEAM")
                    .param("visibility", "PRIVATE")
                    .param("permission", "WRITE"))
            .andExpect(status().isOk())
            .andReturn();

    verify(courseRepository, times(1)).findById(eq(1L));
    verify(assignmentRepository, times(1)).save(any(Assignment.class));

    String expectedJson = mapper.writeValueAsString(expectedAssignment);
    String responseString = response.getResponse().getContentAsString();
    assertEquals(expectedJson, responseString);
  }

  @Test
  public void logged_out_users_cannot_post() throws Exception {
    mockMvc.perform(post(assignmentPostEndpoint)).andExpect(status().is(403));
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void logged_in_users_cannot_post() throws Exception {
    mockMvc.perform(post(assignmentPostEndpoint)).andExpect(status().is(403));
  }

  @Test
  @WithInstructorCoursePermissions
  public void instructor_tries_to_create_assignment_for_nonexistent_course_and_gets_404()
      throws Exception {
    long nonExistentCourseId = 99L;
    when(courseRepository.findById(eq(nonExistentCourseId))).thenReturn(Optional.empty());

    mockMvc
        .perform(
            post(assignmentPostEndpoint)
                .with(csrf())
                .param("courseId", String.valueOf(nonExistentCourseId))
                .param("name", "Test Assignment")
                .param("asn_type", "TEAM")
                .param("visibility", "PRIVATE")
                .param("permission", "WRITE"))
        .andExpect(status().isNotFound())
        .andExpect(
            result ->
                assertEquals(
                    "Course with id 99 not found", result.getResolvedException().getMessage()));

    verify(courseRepository, times(1)).findById(eq(nonExistentCourseId));
    verify(assignmentRepository, times(0)).save(any(Assignment.class));
  }
}
