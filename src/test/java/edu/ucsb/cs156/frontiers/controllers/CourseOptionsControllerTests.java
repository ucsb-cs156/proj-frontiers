package edu.ucsb.cs156.frontiers.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.ucsb.cs156.frontiers.ControllerTestCase;
import edu.ucsb.cs156.frontiers.annotations.WithInstructorCoursePermissions;
import edu.ucsb.cs156.frontiers.annotations.WithStaffCoursePermissions;
import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.entities.CourseOption;
import edu.ucsb.cs156.frontiers.enums.School;
import edu.ucsb.cs156.frontiers.repositories.CourseOptionRepository;
import edu.ucsb.cs156.frontiers.repositories.CourseRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;

@WebMvcTest(controllers = CourseOptionsController.class)
public class CourseOptionsControllerTests extends ControllerTestCase {

  @MockitoBean private CourseOptionRepository courseOptionRepository;
  @MockitoBean private CourseRepository courseRepository;

  private final Course course =
      Course.builder()
          .id(1L)
          .courseName("CS156")
          .term("S25")
          .school(School.UCSB)
          .instructorEmail("instructor@ucsb.edu")
          .build();

  @Test
  @WithInstructorCoursePermissions
  public void getCourseOptions_returnsAllOptionsWithDefaultsAndOverrides() throws Exception {
    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(course));
    when(courseOptionRepository.findByCourseId(eq(1L)))
        .thenReturn(
            List.of(
                CourseOption.builder().courseId(1L).option("ENABLE_CANVAS").enabled(true).build(),
                CourseOption.builder()
                    .courseId(1L)
                    .option("TRANSLATE_SECTIONS")
                    .enabled(false)
                    .build()));

    MvcResult response =
        mockMvc
            .perform(get("/api/course/options").param("courseId", "1"))
            .andExpect(status().isOk())
            .andReturn();

    LinkedHashMap<String, Boolean> expected = new LinkedHashMap<>();
    expected.put("ENABLE_CANVAS", true);
    expected.put("TRANSLATE_SECTIONS", false);
    assertEquals(mapper.writeValueAsString(expected), response.getResponse().getContentAsString());
  }

  @Test
  @WithInstructorCoursePermissions
  public void getCourseOptions_withOptionParam_returnsSingleOption() throws Exception {
    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(course));
    when(courseOptionRepository.findByCourseIdAndOption(eq(1L), eq("ENABLE_CANVAS")))
        .thenReturn(
            Optional.of(
                CourseOption.builder().courseId(1L).option("ENABLE_CANVAS").enabled(true).build()));

    MvcResult response =
        mockMvc
            .perform(
                get("/api/course/options").param("courseId", "1").param("option", "enable_canvas"))
            .andExpect(status().isOk())
            .andReturn();

    assertEquals(
        mapper.writeValueAsString(Map.of("ENABLE_CANVAS", true)),
        response.getResponse().getContentAsString());
  }

  @Test
  @WithInstructorCoursePermissions
  public void setCourseOption_createsNewValue() throws Exception {
    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(course));
    when(courseOptionRepository.findByCourseIdAndOption(eq(1L), eq("ENABLE_CANVAS")))
        .thenReturn(Optional.empty());
    when(courseOptionRepository.save(any(CourseOption.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    MvcResult response =
        mockMvc
            .perform(
                post("/api/course/options")
                    .with(csrf())
                    .param("courseId", "1")
                    .param("option", "ENABLE_CANVAS")
                    .param("enabled", "true"))
            .andExpect(status().isOk())
            .andReturn();

    ArgumentCaptor<CourseOption> captor = ArgumentCaptor.forClass(CourseOption.class);
    verify(courseOptionRepository).save(captor.capture());
    assertEquals(1L, captor.getValue().getCourseId());
    assertEquals("ENABLE_CANVAS", captor.getValue().getOption());
    assertEquals(true, captor.getValue().getEnabled());
    assertEquals(
        mapper.writeValueAsString(Map.of("ENABLE_CANVAS", true)),
        response.getResponse().getContentAsString());
  }

  @Test
  @WithInstructorCoursePermissions
  public void setCourseOption_updatesExistingValue() throws Exception {
    CourseOption existing =
        CourseOption.builder().courseId(1L).option("ENABLE_CANVAS").enabled(false).build();

    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(course));
    when(courseOptionRepository.findByCourseIdAndOption(eq(1L), eq("ENABLE_CANVAS")))
        .thenReturn(Optional.of(existing));
    when(courseOptionRepository.save(any(CourseOption.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    mockMvc
        .perform(
            post("/api/course/options")
                .with(csrf())
                .param("courseId", "1")
                .param("option", "ENABLE_CANVAS")
                .param("enabled", "true"))
        .andExpect(status().isOk());

    ArgumentCaptor<CourseOption> captor = ArgumentCaptor.forClass(CourseOption.class);
    verify(courseOptionRepository).save(captor.capture());
    assertEquals(true, captor.getValue().getEnabled());
  }

  @Test
  @WithInstructorCoursePermissions
  public void getCourseOptions_invalidOption_returnsBadRequest() throws Exception {
    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(course));

    MvcResult response =
        mockMvc
            .perform(
                get("/api/course/options")
                    .param("courseId", "1")
                    .param("option", "NOT_A_REAL_OPTION"))
            .andExpect(status().isBadRequest())
            .andReturn();

    assertEquals(
        mapper.writeValueAsString(
            Map.of(
                "type",
                "IllegalArgumentException",
                "message",
                "Invalid course option: NOT_A_REAL_OPTION")),
        response.getResponse().getContentAsString());
    verify(courseOptionRepository, never()).findByCourseIdAndOption(any(), any());
  }

  @Test
  @WithInstructorCoursePermissions
  public void setCourseOption_missingCourse_returnsNotFound() throws Exception {
    when(courseRepository.findById(eq(99L))).thenReturn(Optional.empty());

    MvcResult response =
        mockMvc
            .perform(
                post("/api/course/options")
                    .with(csrf())
                    .param("courseId", "99")
                    .param("option", "ENABLE_CANVAS")
                    .param("enabled", "true"))
            .andExpect(status().isNotFound())
            .andReturn();

    assertEquals(
        mapper.writeValueAsString(
            Map.of("type", "EntityNotFoundException", "message", "Course with id 99 not found")),
        response.getResponse().getContentAsString());
    verify(courseOptionRepository, never()).save(any());
  }

  @Test
  @WithStaffCoursePermissions
  public void staffCannotGetOrSetCourseOptions() throws Exception {
    mockMvc
        .perform(get("/api/course/options").param("courseId", "1"))
        .andExpect(status().isForbidden());

    mockMvc
        .perform(
            post("/api/course/options")
                .with(csrf())
                .param("courseId", "1")
                .param("option", "ENABLE_CANVAS")
                .param("enabled", "true"))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(roles = {"ADMIN"})
  public void adminCanGetCourseOptions() throws Exception {
    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(course));
    when(courseOptionRepository.findByCourseId(eq(1L))).thenReturn(List.of());

    mockMvc.perform(get("/api/course/options").param("courseId", "1")).andExpect(status().isOk());
  }

  @Test
  @WithInstructorCoursePermissions
  public void getCourseOptions_withBlankOptionParam_returnsAllOptions() throws Exception {
    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(course));
    when(courseOptionRepository.findByCourseId(eq(1L))).thenReturn(List.of());

    MvcResult response =
        mockMvc
            .perform(get("/api/course/options").param("courseId", "1").param("option", "   "))
            .andExpect(status().isOk())
            .andReturn();

    LinkedHashMap<String, Boolean> expected = new LinkedHashMap<>();
    expected.put("ENABLE_CANVAS", false);
    expected.put("TRANSLATE_SECTIONS", false);
    assertEquals(mapper.writeValueAsString(expected), response.getResponse().getContentAsString());
  }

  @Test
  @WithInstructorCoursePermissions
  public void getCourseOptions_withUnknownOptionInDb_skipsUnknownOption() throws Exception {
    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(course));
    when(courseOptionRepository.findByCourseId(eq(1L)))
        .thenReturn(
            List.of(
                CourseOption.builder()
                    .courseId(1L)
                    .option("OLD_REMOVED_OPTION")
                    .enabled(true)
                    .build()));

    MvcResult response =
        mockMvc
            .perform(get("/api/course/options").param("courseId", "1"))
            .andExpect(status().isOk())
            .andReturn();

    LinkedHashMap<String, Boolean> expected = new LinkedHashMap<>();
    expected.put("ENABLE_CANVAS", false);
    expected.put("TRANSLATE_SECTIONS", false);
    assertEquals(mapper.writeValueAsString(expected), response.getResponse().getContentAsString());
  }
}
