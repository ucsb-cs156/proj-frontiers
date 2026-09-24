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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.ucsb.cs156.frontiers.ControllerTestCase;
import edu.ucsb.cs156.frontiers.annotations.WithInstructorCoursePermissions;
import edu.ucsb.cs156.frontiers.annotations.WithStaffCoursePermissions;
import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.entities.DokkuAccountTranslation;
import edu.ucsb.cs156.frontiers.repositories.CourseRepository;
import edu.ucsb.cs156.frontiers.repositories.DokkuAccountTranslationRepository;
import edu.ucsb.cs156.frontiers.testconfig.TestConfig;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.server.ResponseStatusException;

@WebMvcTest(controllers = {DokkuAccountTranslationsController.class})
@Import({TestConfig.class})
public class DokkuAccountTranslationsControllerTests extends ControllerTestCase {

  @MockitoBean CourseRepository courseRepository;

  @MockitoBean DokkuAccountTranslationRepository dokkuAccountTranslationRepository;

  private static final String URL = "/api/dokku/translations";

  private Course course;

  @BeforeEach
  public void setUp() {
    course = Course.builder().id(1L).build();
  }

  private static DokkuAccountTranslation translation(String email, String username) {
    return DokkuAccountTranslation.builder().email(email).username(username).build();
  }

  private Map<String, String> errorBody(MvcResult response) throws Exception {
    return mapper.readValue(response.getResponse().getContentAsString(), Map.class);
  }

  // ---- security ----

  @Test
  public void logged_out_users_cannot_get() throws Exception {
    mockMvc.perform(get(URL).param("courseId", "1")).andExpect(status().is(403));
    verify(courseRepository, never()).findById(any());
  }

  @Test
  @WithMockUser(roles = {"USER"})
  public void users_without_course_permissions_cannot_get() throws Exception {
    mockMvc.perform(get(URL).param("courseId", "1")).andExpect(status().is(403));
  }

  @Test
  @WithMockUser(roles = {"USER"})
  public void users_without_course_permissions_cannot_post_put_or_delete() throws Exception {
    mockMvc
        .perform(
            post(URL)
                .with(csrf())
                .param("courseId", "1")
                .param("email", "a@ucsb.edu")
                .param("username", "a"))
        .andExpect(status().is(403));
    mockMvc
        .perform(
            put(URL)
                .with(csrf())
                .param("courseId", "1")
                .param("email", "a@ucsb.edu")
                .param("username", "a"))
        .andExpect(status().is(403));
    mockMvc
        .perform(delete(URL).with(csrf()).param("courseId", "1").param("email", "a@ucsb.edu"))
        .andExpect(status().is(403));
    verify(dokkuAccountTranslationRepository, never()).save(any());
    verify(dokkuAccountTranslationRepository, never()).delete(any());
  }

  // ---- GET ----

  @Test
  @WithStaffCoursePermissions
  public void get_returns_all_translations_sorted_by_email() throws Exception {
    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(course));
    List<DokkuAccountTranslation> translations =
        List.of(translation("a@ucsb.edu", "alpha"), translation("b@ucsb.edu", "bravo"));
    when(dokkuAccountTranslationRepository.findAllByOrderByEmailAsc()).thenReturn(translations);

    MvcResult response =
        mockMvc.perform(get(URL).param("courseId", "1")).andExpect(status().isOk()).andReturn();

    assertEquals(
        mapper.writeValueAsString(translations), response.getResponse().getContentAsString());
    verify(dokkuAccountTranslationRepository, times(1)).findAllByOrderByEmailAsc();
  }

  @Test
  @WithInstructorCoursePermissions
  public void get_returns_404_when_course_does_not_exist() throws Exception {
    when(courseRepository.findById(eq(1L))).thenReturn(Optional.empty());

    MvcResult response =
        mockMvc
            .perform(get(URL).param("courseId", "1"))
            .andExpect(status().isNotFound())
            .andReturn();

    assertEquals(
        Map.of("message", "Course with id 1 not found", "type", "EntityNotFoundException"),
        errorBody(response));
    verify(dokkuAccountTranslationRepository, never()).findAllByOrderByEmailAsc();
  }

  // ---- POST ----

  @Test
  @WithInstructorCoursePermissions
  public void post_creates_a_translation_with_canonical_email_and_stripped_fields()
      throws Exception {
    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(course));
    when(dokkuAccountTranslationRepository.findById(eq("cgaucho@ucsb.edu")))
        .thenReturn(Optional.empty());
    DokkuAccountTranslation saved = translation("cgaucho@ucsb.edu", "chris");
    when(dokkuAccountTranslationRepository.save(eq(saved))).thenReturn(saved);

    MvcResult response =
        mockMvc
            .perform(
                post(URL)
                    .with(csrf())
                    .param("courseId", "1")
                    .param("email", "  CGaucho@umail.ucsb.edu ")
                    .param("username", "  chris "))
            .andExpect(status().isOk())
            .andReturn();

    verify(dokkuAccountTranslationRepository, times(1)).save(eq(saved));
    assertEquals(mapper.writeValueAsString(saved), response.getResponse().getContentAsString());
  }

  @Test
  @WithInstructorCoursePermissions
  public void post_returns_409_when_email_already_has_a_translation() throws Exception {
    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(course));
    when(dokkuAccountTranslationRepository.findById(eq("cgaucho@ucsb.edu")))
        .thenReturn(Optional.of(translation("cgaucho@ucsb.edu", "old")));

    MvcResult response =
        mockMvc
            .perform(
                post(URL)
                    .with(csrf())
                    .param("courseId", "1")
                    .param("email", "cgaucho@ucsb.edu")
                    .param("username", "new"))
            .andExpect(status().isConflict())
            .andReturn();

    ResponseStatusException e = (ResponseStatusException) response.getResolvedException();
    assertEquals("Dokku account translation for cgaucho@ucsb.edu already exists", e.getReason());
    verify(dokkuAccountTranslationRepository, never()).save(any());
  }

  @Test
  @WithInstructorCoursePermissions
  public void post_returns_400_when_email_is_blank() throws Exception {
    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(course));

    MvcResult response =
        mockMvc
            .perform(
                post(URL)
                    .with(csrf())
                    .param("courseId", "1")
                    .param("email", "   ")
                    .param("username", "chris"))
            .andExpect(status().isBadRequest())
            .andReturn();

    assertEquals(
        Map.of("message", "email must not be blank", "type", "IllegalArgumentException"),
        errorBody(response));
    verify(dokkuAccountTranslationRepository, never()).save(any());
  }

  @Test
  @WithInstructorCoursePermissions
  public void post_returns_400_when_username_is_blank() throws Exception {
    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(course));

    MvcResult response =
        mockMvc
            .perform(
                post(URL)
                    .with(csrf())
                    .param("courseId", "1")
                    .param("email", "cgaucho@ucsb.edu")
                    .param("username", ""))
            .andExpect(status().isBadRequest())
            .andReturn();

    assertEquals(
        Map.of("message", "username must not be blank", "type", "IllegalArgumentException"),
        errorBody(response));
    verify(dokkuAccountTranslationRepository, never()).findById(any());
    verify(dokkuAccountTranslationRepository, never()).save(any());
  }

  @Test
  @WithInstructorCoursePermissions
  public void post_returns_404_when_course_does_not_exist() throws Exception {
    when(courseRepository.findById(eq(1L))).thenReturn(Optional.empty());

    mockMvc
        .perform(
            post(URL)
                .with(csrf())
                .param("courseId", "1")
                .param("email", "cgaucho@ucsb.edu")
                .param("username", "chris"))
        .andExpect(status().isNotFound());

    verify(dokkuAccountTranslationRepository, never()).save(any());
  }

  // ---- PUT ----

  @Test
  @WithInstructorCoursePermissions
  public void put_updates_the_username_of_an_existing_translation() throws Exception {
    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(course));
    DokkuAccountTranslation existing = translation("cgaucho@ucsb.edu", "old");
    when(dokkuAccountTranslationRepository.findById(eq("cgaucho@ucsb.edu")))
        .thenReturn(Optional.of(existing));
    when(dokkuAccountTranslationRepository.save(any())).thenReturn(existing);

    MvcResult response =
        mockMvc
            .perform(
                put(URL)
                    .with(csrf())
                    .param("courseId", "1")
                    .param("email", "CGAUCHO@umail.ucsb.edu")
                    .param("username", " newname "))
            .andExpect(status().isOk())
            .andReturn();

    assertEquals("newname", existing.getUsername());
    assertEquals("cgaucho@ucsb.edu", existing.getEmail());
    verify(dokkuAccountTranslationRepository, times(1)).save(eq(existing));
    assertEquals(
        mapper.writeValueAsString(translation("cgaucho@ucsb.edu", "newname")),
        response.getResponse().getContentAsString());
  }

  @Test
  @WithInstructorCoursePermissions
  public void put_returns_404_when_translation_does_not_exist() throws Exception {
    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(course));
    when(dokkuAccountTranslationRepository.findById(eq("nobody@ucsb.edu")))
        .thenReturn(Optional.empty());

    MvcResult response =
        mockMvc
            .perform(
                put(URL)
                    .with(csrf())
                    .param("courseId", "1")
                    .param("email", "nobody@ucsb.edu")
                    .param("username", "x"))
            .andExpect(status().isNotFound())
            .andReturn();

    assertEquals(
        Map.of(
            "message",
            "DokkuAccountTranslation with id nobody@ucsb.edu not found",
            "type",
            "EntityNotFoundException"),
        errorBody(response));
    verify(dokkuAccountTranslationRepository, never()).save(any());
  }

  @Test
  @WithInstructorCoursePermissions
  public void put_returns_400_when_username_is_blank() throws Exception {
    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(course));

    MvcResult response =
        mockMvc
            .perform(
                put(URL)
                    .with(csrf())
                    .param("courseId", "1")
                    .param("email", "cgaucho@ucsb.edu")
                    .param("username", "  "))
            .andExpect(status().isBadRequest())
            .andReturn();

    assertEquals(
        Map.of("message", "username must not be blank", "type", "IllegalArgumentException"),
        errorBody(response));
    verify(dokkuAccountTranslationRepository, never()).findById(any());
  }

  @Test
  @WithInstructorCoursePermissions
  public void put_returns_404_when_course_does_not_exist() throws Exception {
    when(courseRepository.findById(eq(1L))).thenReturn(Optional.empty());

    mockMvc
        .perform(
            put(URL)
                .with(csrf())
                .param("courseId", "1")
                .param("email", "cgaucho@ucsb.edu")
                .param("username", "x"))
        .andExpect(status().isNotFound());

    verify(dokkuAccountTranslationRepository, never()).findById(any());
  }

  // ---- DELETE ----

  @Test
  @WithInstructorCoursePermissions
  public void delete_removes_an_existing_translation() throws Exception {
    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(course));
    DokkuAccountTranslation existing = translation("cgaucho@ucsb.edu", "chris");
    when(dokkuAccountTranslationRepository.findById(eq("cgaucho@ucsb.edu")))
        .thenReturn(Optional.of(existing));

    MvcResult response =
        mockMvc
            .perform(
                delete(URL)
                    .with(csrf())
                    .param("courseId", "1")
                    .param("email", " CGaucho@umail.ucsb.edu "))
            .andExpect(status().isOk())
            .andReturn();

    verify(dokkuAccountTranslationRepository, times(1)).delete(eq(existing));
    assertEquals(
        Map.of("message", "Dokku account translation for cgaucho@ucsb.edu deleted"),
        errorBody(response));
  }

  @Test
  @WithInstructorCoursePermissions
  public void delete_returns_404_when_translation_does_not_exist() throws Exception {
    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(course));
    when(dokkuAccountTranslationRepository.findById(eq("nobody@ucsb.edu")))
        .thenReturn(Optional.empty());

    MvcResult response =
        mockMvc
            .perform(
                delete(URL).with(csrf()).param("courseId", "1").param("email", "nobody@ucsb.edu"))
            .andExpect(status().isNotFound())
            .andReturn();

    assertEquals(
        Map.of(
            "message",
            "DokkuAccountTranslation with id nobody@ucsb.edu not found",
            "type",
            "EntityNotFoundException"),
        errorBody(response));
    verify(dokkuAccountTranslationRepository, never()).delete(any());
  }

  @Test
  @WithInstructorCoursePermissions
  public void delete_returns_400_when_email_is_blank() throws Exception {
    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(course));

    mockMvc
        .perform(delete(URL).with(csrf()).param("courseId", "1").param("email", " "))
        .andExpect(status().isBadRequest());

    verify(dokkuAccountTranslationRepository, never()).delete(any());
  }

  @Test
  @WithInstructorCoursePermissions
  public void delete_returns_404_when_course_does_not_exist() throws Exception {
    when(courseRepository.findById(eq(1L))).thenReturn(Optional.empty());
    // even though the translation exists, the missing course must be a 404 with no delete
    when(dokkuAccountTranslationRepository.findById(eq("a@ucsb.edu")))
        .thenReturn(Optional.of(translation("a@ucsb.edu", "a")));

    MvcResult response =
        mockMvc
            .perform(delete(URL).with(csrf()).param("courseId", "1").param("email", "a@ucsb.edu"))
            .andExpect(status().isNotFound())
            .andReturn();

    assertEquals(
        Map.of("message", "Course with id 1 not found", "type", "EntityNotFoundException"),
        errorBody(response));
    verify(dokkuAccountTranslationRepository, never()).findById(any());
    verify(dokkuAccountTranslationRepository, never()).delete(any());
  }
}
