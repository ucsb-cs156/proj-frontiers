package edu.ucsb.cs156.frontiers.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.ucsb.cs156.frontiers.ControllerTestCase;
import edu.ucsb.cs156.frontiers.annotations.WithInstructorCoursePermissions;
import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.repositories.CourseRepository;
import edu.ucsb.cs156.frontiers.services.ApiKeyService;
import edu.ucsb.cs156.frontiers.services.ApiKeyService.ExpirationChoice;
import edu.ucsb.cs156.frontiers.services.ApiKeyService.IssuedCourseApiKey;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
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
    IssuedCourseApiKey created = new IssuedCourseApiKey("key", 1L, staticZD, staticZD);

    Course course = Course.builder().id(1L).build();

    when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
    when(apiKeyService.createApiKey(course, ExpirationChoice.DAYS_90)).thenReturn(created);

    MvcResult result =
        mockMvc
            .perform(
                post("/api/courses/key")
                    .param("courseId", "1")
                    .param("choice", ExpirationChoice.DAYS_90.name())
                    .with(csrf()))
            .andExpect(status().isOk())
            .andReturn();

    verify(apiKeyService).createApiKey(course, ExpirationChoice.DAYS_90);
    assertEquals(mapper.writeValueAsString(created), result.getResponse().getContentAsString());
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

    verify(apiKeyService, never()).createApiKey(any(), any());
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
