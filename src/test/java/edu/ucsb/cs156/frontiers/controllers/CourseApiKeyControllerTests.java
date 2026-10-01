package edu.ucsb.cs156.frontiers.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.ucsb.cs156.frontiers.ControllerTestCase;
import edu.ucsb.cs156.frontiers.annotations.WithInstructorCoursePermissions;
import edu.ucsb.cs156.frontiers.annotations.WithStaffCoursePermissions;
import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.models.CourseApiKeyView;
import edu.ucsb.cs156.frontiers.models.CourseApiKeyView.Status;
import edu.ucsb.cs156.frontiers.repositories.CourseRepository;
import edu.ucsb.cs156.frontiers.services.ApiKeyService;
import edu.ucsb.cs156.frontiers.services.ApiKeyService.ExpirationChoice;
import edu.ucsb.cs156.frontiers.services.ApiKeyService.IssuedCourseApiKey;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;

@WebMvcTest(CourseApiKeyController.class)
public class CourseApiKeyControllerTests extends ControllerTestCase {

  @MockitoBean private ApiKeyService apiKeyService;
  private final ZonedDateTime staticZD =
      Instant.parse("2026-03-11T08:00:00.00Z").atZone(ZoneId.of("America/Los_Angeles"));
  @MockitoBean private CourseRepository courseRepository;

  @Test
  @WithInstructorCoursePermissions
  public void test_create_course_api_key() throws Exception {
    IssuedCourseApiKey created = new IssuedCourseApiKey("key", 1L, staticZD, staticZD, null);

    Course course = Course.builder().id(1L).build();

    when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
    when(apiKeyService.apiKeysEnabled(1L)).thenReturn(true);
    when(apiKeyService.createApiKey(course, ExpirationChoice.DAYS_90, null)).thenReturn(created);

    MvcResult result =
        mockMvc
            .perform(
                post("/api/courses/key")
                    .param("courseId", "1")
                    .param("choice", ExpirationChoice.DAYS_90.name())
                    .with(csrf()))
            .andExpect(status().isOk())
            .andReturn();

    verify(apiKeyService).createApiKey(course, ExpirationChoice.DAYS_90, null);
    assertEquals(mapper.writeValueAsString(created), result.getResponse().getContentAsString());
  }

  @Test
  @WithInstructorCoursePermissions
  public void test_create_course_api_key_with_label() throws Exception {
    IssuedCourseApiKey created =
        new IssuedCourseApiKey("key", 1L, staticZD, staticZD, "jpa02 autograder");
    Course course = Course.builder().id(1L).build();
    when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
    when(apiKeyService.apiKeysEnabled(1L)).thenReturn(true);
    when(apiKeyService.createApiKey(course, ExpirationChoice.MONTHS_6, "jpa02 autograder"))
        .thenReturn(created);

    MvcResult result =
        mockMvc
            .perform(
                post("/api/courses/key")
                    .param("courseId", "1")
                    .param("choice", ExpirationChoice.MONTHS_6.name())
                    .param("label", "jpa02 autograder")
                    .with(csrf()))
            .andExpect(status().isOk())
            .andReturn();

    assertEquals(mapper.writeValueAsString(created), result.getResponse().getContentAsString());
  }

  @Test
  @WithInstructorCoursePermissions
  public void test_create_course_api_key_rejects_long_label() throws Exception {
    mockMvc
        .perform(
            post("/api/courses/key")
                .param("courseId", "1")
                .param("choice", ExpirationChoice.DAYS_90.name())
                .param("label", "x".repeat(61))
                .with(csrf()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("label must be at most 60 characters"));

    verify(apiKeyService, never()).createApiKey(any(), any(), any());
  }

  @Test
  @WithInstructorCoursePermissions
  public void list_api_keys_for_course() throws Exception {
    Course course = Course.builder().id(1L).build();
    when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
    List<CourseApiKeyView> views =
        List.of(
            new CourseApiKeyView(
                7L,
                "jpa02 autograder",
                "abc123",
                "phtcon@ucsb.edu",
                staticZD,
                staticZD.plusDays(90),
                null,
                0L,
                false,
                Status.ACTIVE),
            new CourseApiKeyView(
                6L,
                null,
                "def456",
                "phtcon@ucsb.edu",
                staticZD.minusDays(1),
                staticZD.plusDays(89),
                staticZD,
                12L,
                true,
                Status.REVOKED));
    when(apiKeyService.listApiKeys(1L)).thenReturn(views);

    MvcResult result =
        mockMvc
            .perform(get("/api/courses/key").param("courseId", "1"))
            .andExpect(status().isOk())
            .andReturn();

    assertEquals(mapper.writeValueAsString(views), result.getResponse().getContentAsString());
  }

  @Test
  @WithInstructorCoursePermissions
  public void list_api_keys_for_unknown_course_is_not_found() throws Exception {
    mockMvc
        .perform(get("/api/courses/key").param("courseId", "1"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Course with id 1 not found"));
    verify(apiKeyService, never()).listApiKeys(any());
  }

  @Test
  @WithStaffCoursePermissions
  public void staff_cannot_list_api_keys() throws Exception {
    mockMvc
        .perform(get("/api/courses/key").param("courseId", "1"))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithInstructorCoursePermissions
  public void revoke_by_id_success() throws Exception {
    when(apiKeyService.revokeApiKeyById(5L, 1L)).thenReturn(true);

    mockMvc
        .perform(delete("/api/courses/key").param("courseId", "1").param("id", "5").with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("API key with id 5 revoked"));
  }

  @Test
  @WithInstructorCoursePermissions
  public void revoke_by_id_not_found() throws Exception {
    when(apiKeyService.revokeApiKeyById(5L, 1L)).thenReturn(false);

    mockMvc
        .perform(delete("/api/courses/key").param("courseId", "1").param("id", "5").with(csrf()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("CourseApiKey with id 5 not found"));
  }

  @Test
  @WithStaffCoursePermissions
  public void staff_cannot_revoke_by_id() throws Exception {
    mockMvc
        .perform(delete("/api/courses/key").param("courseId", "1").param("id", "5").with(csrf()))
        .andExpect(status().isForbidden());
    verify(apiKeyService, never()).revokeApiKeyById(any(), any());
  }

  @Test
  @WithInstructorCoursePermissions
  public void test_create_course_api_key_with_invalid_course_id() throws Exception {
    mockMvc
        .perform(
            post("/api/courses/key")
                .param("courseId", "1")
                .param("choice", ExpirationChoice.DAYS_90.name())
                .with(csrf()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Course with id 1 not found"));

    verify(apiKeyService, never()).createApiKey(any(), any(), any());
  }

  @Test
  @WithInstructorCoursePermissions
  public void test_create_course_api_key_requires_enable_api_keys_option() throws Exception {
    Course course = Course.builder().id(1L).build();
    when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
    when(apiKeyService.apiKeysEnabled(1L)).thenReturn(false);

    mockMvc
        .perform(
            post("/api/courses/key")
                .param("courseId", "1")
                .param("choice", ExpirationChoice.DAYS_90.name())
                .with(csrf()))
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.message")
                .value("The course option ENABLE_API_KEYS must be enabled to create an API key."));

    verify(apiKeyService, never()).createApiKey(any(), any(), any());
  }

  @Test
  public void revoke_success() throws Exception {
    when(apiKeyService.revokeApiKey("banana")).thenReturn(true);

    mockMvc
        .perform(delete("/api/courses/key/revoke").param("apiKey", "banana").with(csrf()))
        .andExpect(status().isNoContent());
  }

  @Test
  public void revoke_not_key() throws Exception {
    when(apiKeyService.revokeApiKey("banana")).thenReturn(false);

    mockMvc
        .perform(delete("/api/courses/key/revoke").param("apiKey", "banana").with(csrf()))
        .andExpect(status().isNotFound());
  }
}
