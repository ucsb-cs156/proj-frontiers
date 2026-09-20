package edu.ucsb.cs156.frontiers.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
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
import edu.ucsb.cs156.frontiers.entities.Section;
import edu.ucsb.cs156.frontiers.enums.School;
import edu.ucsb.cs156.frontiers.repositories.CourseRepository;
import edu.ucsb.cs156.frontiers.repositories.SectionRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.server.ResponseStatusException;

@WebMvcTest(controllers = SectionsController.class)
public class SectionsControllerTests extends ControllerTestCase {

  @MockitoBean private SectionRepository sectionRepository;
  @MockitoBean private CourseRepository courseRepository;

  private final Course course =
      Course.builder()
          .id(1L)
          .courseName("CS156")
          .term("S25")
          .school(School.UCSB)
          .instructorEmail("instructor@ucsb.edu")
          .build();

  private final Course otherCourse =
      Course.builder()
          .id(2L)
          .courseName("CS148")
          .term("S25")
          .school(School.UCSB)
          .instructorEmail("someoneelse@ucsb.edu")
          .build();

  private final Section section1 =
      Section.builder().id(10L).course(course).section("0100").label("Tue 9am").build();

  private final Section section2 =
      Section.builder().id(11L).course(course).section("0200").label("Tue 10am").build();

  private final Section sectionInOtherCourse =
      Section.builder().id(12L).course(otherCourse).section("0100").label("Wed 9am").build();

  private final Map<String, String> courseNotFound =
      Map.of("type", "EntityNotFoundException", "message", "Course with id 99 not found");

  private final Map<String, String> sectionNotFound =
      Map.of("type", "EntityNotFoundException", "message", "Section with id 12 not found");

  // ---------- Authorization ----------

  @Test
  public void logged_out_users_cannot_get() throws Exception {
    mockMvc.perform(get("/api/courses/1/sections")).andExpect(status().is(403));
  }

  @Test
  public void logged_out_users_cannot_post() throws Exception {
    mockMvc
        .perform(
            post("/api/courses/1/sections")
                .with(csrf())
                .param("section", "0100")
                .param("label", "Tue 9am"))
        .andExpect(status().is(403));
  }

  @Test
  public void logged_out_users_cannot_put() throws Exception {
    mockMvc
        .perform(
            put("/api/courses/1/sections/10")
                .with(csrf())
                .param("section", "0100")
                .param("label", "Tue 9am"))
        .andExpect(status().is(403));
  }

  @Test
  public void logged_out_users_cannot_delete() throws Exception {
    mockMvc.perform(delete("/api/courses/1/sections/10").with(csrf())).andExpect(status().is(403));
  }

  @Test
  @WithMockUser(roles = {"USER"})
  public void users_without_course_permissions_cannot_access_sections() throws Exception {
    mockMvc.perform(get("/api/courses/1/sections")).andExpect(status().is(403));
    mockMvc
        .perform(
            post("/api/courses/1/sections")
                .with(csrf())
                .param("section", "0100")
                .param("label", "Tue 9am"))
        .andExpect(status().is(403));
    mockMvc
        .perform(
            put("/api/courses/1/sections/10")
                .with(csrf())
                .param("section", "0100")
                .param("label", "Tue 9am"))
        .andExpect(status().is(403));
    mockMvc.perform(delete("/api/courses/1/sections/10").with(csrf())).andExpect(status().is(403));
  }

  @Test
  @WithStaffCoursePermissions
  public void staff_with_course_permissions_can_get_sections() throws Exception {
    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(course));
    when(sectionRepository.findByCourseIdOrderBySectionAsc(eq(1L)))
        .thenReturn(List.of(section1, section2));

    MvcResult response =
        mockMvc.perform(get("/api/courses/1/sections")).andExpect(status().isOk()).andReturn();

    assertEquals(
        mapper.writeValueAsString(List.of(section1, section2)),
        response.getResponse().getContentAsString());
  }

  @Test
  @WithMockUser(roles = {"ADMIN"})
  public void admin_can_get_sections() throws Exception {
    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(course));
    when(sectionRepository.findByCourseIdOrderBySectionAsc(eq(1L))).thenReturn(List.of());

    MvcResult response =
        mockMvc.perform(get("/api/courses/1/sections")).andExpect(status().isOk()).andReturn();

    assertEquals("[]", response.getResponse().getContentAsString());
  }

  // ---------- GET ----------

  @Test
  @WithInstructorCoursePermissions
  public void get_sections_returns_sections_for_course() throws Exception {
    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(course));
    when(sectionRepository.findByCourseIdOrderBySectionAsc(eq(1L)))
        .thenReturn(List.of(section1, section2));

    MvcResult response =
        mockMvc.perform(get("/api/courses/1/sections")).andExpect(status().isOk()).andReturn();

    verify(sectionRepository).findByCourseIdOrderBySectionAsc(eq(1L));
    assertEquals(
        mapper.writeValueAsString(List.of(section1, section2)),
        response.getResponse().getContentAsString());
  }

  @Test
  @WithInstructorCoursePermissions
  public void get_sections_missing_course_returns_404() throws Exception {
    when(courseRepository.findById(eq(99L))).thenReturn(Optional.empty());

    MvcResult response =
        mockMvc
            .perform(get("/api/courses/99/sections"))
            .andExpect(status().isNotFound())
            .andReturn();

    assertEquals(
        mapper.writeValueAsString(courseNotFound), response.getResponse().getContentAsString());
    verify(sectionRepository, never()).findByCourseIdOrderBySectionAsc(any());
  }

  // ---------- POST ----------

  @Test
  @WithInstructorCoursePermissions
  public void post_section_creates_section() throws Exception {
    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(course));
    when(sectionRepository.findByCourseIdAndSection(eq(1L), eq("0100")))
        .thenReturn(Optional.empty());
    when(sectionRepository.save(any(Section.class)))
        .thenAnswer(
            invocation -> {
              Section s = invocation.getArgument(0);
              s.setId(10L);
              return s;
            });

    MvcResult response =
        mockMvc
            .perform(
                post("/api/courses/1/sections")
                    .with(csrf())
                    .param("section", "  0100 ")
                    .param("label", " Tue 9am  "))
            .andExpect(status().isOk())
            .andReturn();

    ArgumentCaptor<Section> captor = ArgumentCaptor.forClass(Section.class);
    verify(sectionRepository).save(captor.capture());
    assertEquals(course, captor.getValue().getCourse());
    assertEquals("0100", captor.getValue().getSection());
    assertEquals("Tue 9am", captor.getValue().getLabel());
    assertEquals(mapper.writeValueAsString(section1), response.getResponse().getContentAsString());
  }

  @Test
  @WithInstructorCoursePermissions
  public void post_section_creates_section_with_slack_channel_name() throws Exception {
    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(course));
    when(sectionRepository.findByCourseIdAndSection(eq(1L), eq("0100")))
        .thenReturn(Optional.empty());
    when(sectionRepository.save(any(Section.class)))
        .thenAnswer(
            invocation -> {
              Section s = invocation.getArgument(0);
              s.setId(10L);
              return s;
            });

    mockMvc
        .perform(
            post("/api/courses/1/sections")
                .with(csrf())
                .param("section", "0100")
                .param("label", "Tue 9am")
                .param("slackChannelName", "  #cs156-0100  "))
        .andExpect(status().isOk())
        .andReturn();

    ArgumentCaptor<Section> captor = ArgumentCaptor.forClass(Section.class);
    verify(sectionRepository).save(captor.capture());
    assertEquals("#cs156-0100", captor.getValue().getSlackChannelName());
  }

  @Test
  @WithInstructorCoursePermissions
  public void post_section_blank_slack_channel_name_is_stored_as_null() throws Exception {
    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(course));
    when(sectionRepository.findByCourseIdAndSection(eq(1L), eq("0100")))
        .thenReturn(Optional.empty());
    when(sectionRepository.save(any(Section.class)))
        .thenAnswer(
            invocation -> {
              Section s = invocation.getArgument(0);
              s.setId(10L);
              return s;
            });

    mockMvc
        .perform(
            post("/api/courses/1/sections")
                .with(csrf())
                .param("section", "0100")
                .param("label", "Tue 9am")
                .param("slackChannelName", "   "))
        .andExpect(status().isOk())
        .andReturn();

    ArgumentCaptor<Section> captor = ArgumentCaptor.forClass(Section.class);
    verify(sectionRepository).save(captor.capture());
    assertEquals(null, captor.getValue().getSlackChannelName());
  }

  @Test
  @WithInstructorCoursePermissions
  public void post_section_missing_course_returns_404() throws Exception {
    when(courseRepository.findById(eq(99L))).thenReturn(Optional.empty());

    MvcResult response =
        mockMvc
            .perform(
                post("/api/courses/99/sections")
                    .with(csrf())
                    .param("section", "0100")
                    .param("label", "Tue 9am"))
            .andExpect(status().isNotFound())
            .andReturn();

    assertEquals(
        mapper.writeValueAsString(courseNotFound), response.getResponse().getContentAsString());
    verify(sectionRepository, never()).save(any());
  }

  @Test
  @WithInstructorCoursePermissions
  public void post_section_duplicate_returns_409() throws Exception {
    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(course));
    when(sectionRepository.findByCourseIdAndSection(eq(1L), eq("0100")))
        .thenReturn(Optional.of(section1));

    MvcResult response =
        mockMvc
            .perform(
                post("/api/courses/1/sections")
                    .with(csrf())
                    .param("section", "0100")
                    .param("label", "Another label"))
            .andExpect(status().isConflict())
            .andReturn();

    ResponseStatusException e = (ResponseStatusException) response.getResolvedException();
    assertEquals("Section 0100 already exists for course 1", e.getReason());
    verify(sectionRepository, never()).save(any());
  }

  @Test
  @WithInstructorCoursePermissions
  public void post_section_blank_section_returns_400() throws Exception {
    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(course));

    MvcResult response =
        mockMvc
            .perform(
                post("/api/courses/1/sections")
                    .with(csrf())
                    .param("section", "   ")
                    .param("label", "Tue 9am"))
            .andExpect(status().isBadRequest())
            .andReturn();

    assertEquals(
        mapper.writeValueAsString(
            Map.of("type", "IllegalArgumentException", "message", "section must not be blank")),
        response.getResponse().getContentAsString());
    verify(sectionRepository, never()).findByCourseIdAndSection(any(), any());
    verify(sectionRepository, never()).save(any());
  }

  @Test
  @WithInstructorCoursePermissions
  public void post_section_blank_label_returns_400() throws Exception {
    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(course));

    MvcResult response =
        mockMvc
            .perform(
                post("/api/courses/1/sections")
                    .with(csrf())
                    .param("section", "0100")
                    .param("label", ""))
            .andExpect(status().isBadRequest())
            .andReturn();

    assertEquals(
        mapper.writeValueAsString(
            Map.of("type", "IllegalArgumentException", "message", "label must not be blank")),
        response.getResponse().getContentAsString());
    verify(sectionRepository, never()).save(any());
  }

  // ---------- PUT ----------

  @Test
  @WithInstructorCoursePermissions
  public void put_section_updates_section() throws Exception {
    Section existing =
        Section.builder().id(10L).course(course).section("0100").label("Tue 9am").build();
    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(course));
    when(sectionRepository.findById(eq(10L))).thenReturn(Optional.of(existing));
    when(sectionRepository.findByCourseIdAndSection(eq(1L), eq("0300")))
        .thenReturn(Optional.empty());
    when(sectionRepository.save(any(Section.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    MvcResult response =
        mockMvc
            .perform(
                put("/api/courses/1/sections/10")
                    .with(csrf())
                    .param("section", " 0300 ")
                    .param("label", " Thu 1pm "))
            .andExpect(status().isOk())
            .andReturn();

    ArgumentCaptor<Section> captor = ArgumentCaptor.forClass(Section.class);
    verify(sectionRepository).save(captor.capture());
    assertEquals(10L, captor.getValue().getId());
    assertEquals(course, captor.getValue().getCourse());
    assertEquals("0300", captor.getValue().getSection());
    assertEquals("Thu 1pm", captor.getValue().getLabel());

    Section expected =
        Section.builder().id(10L).course(course).section("0300").label("Thu 1pm").build();
    assertEquals(mapper.writeValueAsString(expected), response.getResponse().getContentAsString());
  }

  @Test
  @WithInstructorCoursePermissions
  public void put_section_updates_slack_channel_name() throws Exception {
    Section existing =
        Section.builder()
            .id(10L)
            .course(course)
            .section("0100")
            .label("Tue 9am")
            .slackChannelName("#old-channel")
            .build();
    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(course));
    when(sectionRepository.findById(eq(10L))).thenReturn(Optional.of(existing));
    when(sectionRepository.findByCourseIdAndSection(eq(1L), eq("0100")))
        .thenReturn(Optional.of(existing));
    when(sectionRepository.save(any(Section.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    mockMvc
        .perform(
            put("/api/courses/1/sections/10")
                .with(csrf())
                .param("section", "0100")
                .param("label", "Tue 9am")
                .param("slackChannelName", "  #new-channel  "))
        .andExpect(status().isOk())
        .andReturn();

    ArgumentCaptor<Section> captor = ArgumentCaptor.forClass(Section.class);
    verify(sectionRepository).save(captor.capture());
    assertEquals("#new-channel", captor.getValue().getSlackChannelName());
  }

  @Test
  @WithInstructorCoursePermissions
  public void put_section_omitted_slack_channel_name_clears_it() throws Exception {
    Section existing =
        Section.builder()
            .id(10L)
            .course(course)
            .section("0100")
            .label("Tue 9am")
            .slackChannelName("#old-channel")
            .build();
    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(course));
    when(sectionRepository.findById(eq(10L))).thenReturn(Optional.of(existing));
    when(sectionRepository.findByCourseIdAndSection(eq(1L), eq("0100")))
        .thenReturn(Optional.of(existing));
    when(sectionRepository.save(any(Section.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    mockMvc
        .perform(
            put("/api/courses/1/sections/10")
                .with(csrf())
                .param("section", "0100")
                .param("label", "Tue 9am"))
        .andExpect(status().isOk())
        .andReturn();

    ArgumentCaptor<Section> captor = ArgumentCaptor.forClass(Section.class);
    verify(sectionRepository).save(captor.capture());
    assertEquals(null, captor.getValue().getSlackChannelName());
  }

  @Test
  @WithInstructorCoursePermissions
  public void put_section_keeping_same_section_value_is_allowed() throws Exception {
    Section existing =
        Section.builder().id(10L).course(course).section("0100").label("Tue 9am").build();
    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(course));
    when(sectionRepository.findById(eq(10L))).thenReturn(Optional.of(existing));
    // The "duplicate" found is the very row being edited, so it is not a conflict
    when(sectionRepository.findByCourseIdAndSection(eq(1L), eq("0100")))
        .thenReturn(Optional.of(existing));
    when(sectionRepository.save(any(Section.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    MvcResult response =
        mockMvc
            .perform(
                put("/api/courses/1/sections/10")
                    .with(csrf())
                    .param("section", "0100")
                    .param("label", "New label"))
            .andExpect(status().isOk())
            .andReturn();

    ArgumentCaptor<Section> captor = ArgumentCaptor.forClass(Section.class);
    verify(sectionRepository).save(captor.capture());
    assertEquals("0100", captor.getValue().getSection());
    assertEquals("New label", captor.getValue().getLabel());

    Section expected =
        Section.builder().id(10L).course(course).section("0100").label("New label").build();
    assertEquals(mapper.writeValueAsString(expected), response.getResponse().getContentAsString());
  }

  @Test
  @WithInstructorCoursePermissions
  public void put_section_duplicate_of_other_row_returns_409() throws Exception {
    Section existing =
        Section.builder().id(10L).course(course).section("0100").label("Tue 9am").build();
    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(course));
    when(sectionRepository.findById(eq(10L))).thenReturn(Optional.of(existing));
    when(sectionRepository.findByCourseIdAndSection(eq(1L), eq("0200")))
        .thenReturn(Optional.of(section2));

    MvcResult response =
        mockMvc
            .perform(
                put("/api/courses/1/sections/10")
                    .with(csrf())
                    .param("section", "0200")
                    .param("label", "Tue 9am"))
            .andExpect(status().isConflict())
            .andReturn();

    ResponseStatusException e = (ResponseStatusException) response.getResolvedException();
    assertEquals("Section 0200 already exists for course 1", e.getReason());
    verify(sectionRepository, never()).save(any());
    assertEquals("0100", existing.getSection());
    assertEquals("Tue 9am", existing.getLabel());
  }

  @Test
  @WithInstructorCoursePermissions
  public void put_section_missing_course_returns_404() throws Exception {
    when(courseRepository.findById(eq(99L))).thenReturn(Optional.empty());

    MvcResult response =
        mockMvc
            .perform(
                put("/api/courses/99/sections/10")
                    .with(csrf())
                    .param("section", "0100")
                    .param("label", "Tue 9am"))
            .andExpect(status().isNotFound())
            .andReturn();

    assertEquals(
        mapper.writeValueAsString(courseNotFound), response.getResponse().getContentAsString());
    verify(sectionRepository, never()).findById(any());
    verify(sectionRepository, never()).save(any());
  }

  @Test
  @WithInstructorCoursePermissions
  public void put_section_missing_section_returns_404() throws Exception {
    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(course));
    when(sectionRepository.findById(eq(12L))).thenReturn(Optional.empty());

    MvcResult response =
        mockMvc
            .perform(
                put("/api/courses/1/sections/12")
                    .with(csrf())
                    .param("section", "0100")
                    .param("label", "Tue 9am"))
            .andExpect(status().isNotFound())
            .andReturn();

    assertEquals(
        mapper.writeValueAsString(sectionNotFound), response.getResponse().getContentAsString());
    verify(sectionRepository, never()).save(any());
  }

  @Test
  @WithInstructorCoursePermissions
  public void put_section_belonging_to_other_course_returns_404() throws Exception {
    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(course));
    when(sectionRepository.findById(eq(12L))).thenReturn(Optional.of(sectionInOtherCourse));

    MvcResult response =
        mockMvc
            .perform(
                put("/api/courses/1/sections/12")
                    .with(csrf())
                    .param("section", "0100")
                    .param("label", "Tue 9am"))
            .andExpect(status().isNotFound())
            .andReturn();

    assertEquals(
        mapper.writeValueAsString(sectionNotFound), response.getResponse().getContentAsString());
    verify(sectionRepository, never()).save(any());
  }

  @Test
  @WithInstructorCoursePermissions
  public void put_section_blank_section_returns_400() throws Exception {
    Section existing =
        Section.builder().id(10L).course(course).section("0100").label("Tue 9am").build();
    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(course));
    when(sectionRepository.findById(eq(10L))).thenReturn(Optional.of(existing));

    MvcResult response =
        mockMvc
            .perform(
                put("/api/courses/1/sections/10")
                    .with(csrf())
                    .param("section", " ")
                    .param("label", "Tue 9am"))
            .andExpect(status().isBadRequest())
            .andReturn();

    assertEquals(
        mapper.writeValueAsString(
            Map.of("type", "IllegalArgumentException", "message", "section must not be blank")),
        response.getResponse().getContentAsString());
    verify(sectionRepository, never()).save(any());
    assertEquals("0100", existing.getSection());
  }

  @Test
  @WithInstructorCoursePermissions
  public void put_section_blank_label_returns_400() throws Exception {
    Section existing =
        Section.builder().id(10L).course(course).section("0100").label("Tue 9am").build();
    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(course));
    when(sectionRepository.findById(eq(10L))).thenReturn(Optional.of(existing));

    MvcResult response =
        mockMvc
            .perform(
                put("/api/courses/1/sections/10")
                    .with(csrf())
                    .param("section", "0100")
                    .param("label", "  "))
            .andExpect(status().isBadRequest())
            .andReturn();

    assertEquals(
        mapper.writeValueAsString(
            Map.of("type", "IllegalArgumentException", "message", "label must not be blank")),
        response.getResponse().getContentAsString());
    verify(sectionRepository, never()).save(any());
    assertEquals("Tue 9am", existing.getLabel());
  }

  // ---------- DELETE ----------

  @Test
  @WithInstructorCoursePermissions
  public void delete_section_deletes_section() throws Exception {
    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(course));
    when(sectionRepository.findById(eq(10L))).thenReturn(Optional.of(section1));

    MvcResult response =
        mockMvc
            .perform(delete("/api/courses/1/sections/10").with(csrf()))
            .andExpect(status().isOk())
            .andReturn();

    verify(sectionRepository).delete(eq(section1));
    assertEquals(
        mapper.writeValueAsString(Map.of("message", "Section with id 10 deleted")),
        response.getResponse().getContentAsString());
  }

  @Test
  @WithInstructorCoursePermissions
  public void delete_section_missing_course_returns_404() throws Exception {
    when(courseRepository.findById(eq(99L))).thenReturn(Optional.empty());

    MvcResult response =
        mockMvc
            .perform(delete("/api/courses/99/sections/10").with(csrf()))
            .andExpect(status().isNotFound())
            .andReturn();

    assertEquals(
        mapper.writeValueAsString(courseNotFound), response.getResponse().getContentAsString());
    verify(sectionRepository, never()).findById(any());
    verify(sectionRepository, never()).delete(any());
  }

  @Test
  @WithInstructorCoursePermissions
  public void delete_section_missing_section_returns_404() throws Exception {
    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(course));
    when(sectionRepository.findById(eq(12L))).thenReturn(Optional.empty());

    MvcResult response =
        mockMvc
            .perform(delete("/api/courses/1/sections/12").with(csrf()))
            .andExpect(status().isNotFound())
            .andReturn();

    assertEquals(
        mapper.writeValueAsString(sectionNotFound), response.getResponse().getContentAsString());
    verify(sectionRepository, never()).delete(any());
  }

  @Test
  @WithInstructorCoursePermissions
  public void delete_section_belonging_to_other_course_returns_404() throws Exception {
    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(course));
    when(sectionRepository.findById(eq(12L))).thenReturn(Optional.of(sectionInOtherCourse));

    MvcResult response =
        mockMvc
            .perform(delete("/api/courses/1/sections/12").with(csrf()))
            .andExpect(status().isNotFound())
            .andReturn();

    assertEquals(
        mapper.writeValueAsString(sectionNotFound), response.getResponse().getContentAsString());
    verify(sectionRepository, never()).delete(any());
  }
}
