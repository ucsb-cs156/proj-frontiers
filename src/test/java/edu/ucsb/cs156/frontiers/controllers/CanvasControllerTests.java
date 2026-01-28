package edu.ucsb.cs156.frontiers.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.ucsb.cs156.frontiers.ControllerTestCase;
import edu.ucsb.cs156.frontiers.annotations.WithInstructorCoursePermissions;
import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.entities.RosterStudent;
import edu.ucsb.cs156.frontiers.enums.OrgStatus;
import edu.ucsb.cs156.frontiers.enums.RosterStatus;
import edu.ucsb.cs156.frontiers.jobs.RemoveStudentsJob;
import edu.ucsb.cs156.frontiers.models.LoadResult;
import edu.ucsb.cs156.frontiers.repositories.CourseRepository;
import edu.ucsb.cs156.frontiers.repositories.RosterStudentRepository;
import edu.ucsb.cs156.frontiers.repositories.TeamRepository;
import edu.ucsb.cs156.frontiers.services.CanvasService;
import edu.ucsb.cs156.frontiers.services.OrganizationMemberService;
import edu.ucsb.cs156.frontiers.services.UpdateUserService;
import edu.ucsb.cs156.frontiers.services.jobs.JobService;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;

@WebMvcTest(controllers = CanvasController.class)
public class CanvasControllerTests extends ControllerTestCase {

  @MockitoBean private CanvasService canvasService;

  @MockitoBean private UpdateUserService updateUserService;

  @MockitoBean private CourseRepository courseRepository;

  @MockitoBean private RosterStudentRepository rosterStudentRepository;

  @MockitoBean private JobService service;

  @MockitoBean private TeamRepository teamRepository;

  @MockitoBean private OrganizationMemberService organizationMemberService;

  @Test
  @WithInstructorCoursePermissions
  public void testUploadRosterFromCanvas_insertsNewStudents() throws Exception {
    // Arrange
    Course course =
        Course.builder()
            .id(1L)
            .courseName("CS156")
            .orgName("ucsb-cs156-s25")
            .term("S25")
            .school("UCSB")
            .canvasApiToken("test-api-token")
            .canvasCourseId("12345")
            .rosterStudents(new ArrayList<>())
            .build();

    RosterStudent canvasStudent1 =
        RosterStudent.builder()
            .firstName("Alice")
            .lastName("Smith")
            .studentId("A111111")
            .email("alice@ucsb.edu")
            .build();

    RosterStudent canvasStudent2 =
        RosterStudent.builder()
            .firstName("Bob")
            .lastName("Jones")
            .studentId("A222222")
            .email("bob@ucsb.edu")
            .build();

    List<RosterStudent> canvasStudents = List.of(canvasStudent1, canvasStudent2);

    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(course));
    when(canvasService.getCanvasRoster(any(Course.class))).thenReturn(canvasStudents);

    // Act
    MvcResult response =
        mockMvc
            .perform(post("/api/courses/canvas/sync/students").with(csrf()).param("courseId", "1"))
            .andExpect(status().isOk())
            .andReturn();

    // Assert
    verify(courseRepository, atLeastOnce()).findById(eq(1L));
    verify(canvasService).getCanvasRoster(any(Course.class));
    verify(rosterStudentRepository).saveAll(any());
    verify(updateUserService).attachUsersToRosterStudents(any());
    verify(service).runAsJob(any(RemoveStudentsJob.class));

    String responseString = response.getResponse().getContentAsString();
    LoadResult expectedResult = new LoadResult(2, 0, 0, List.of());
    String expectedJson = mapper.writeValueAsString(expectedResult);
    assertEquals(expectedJson, responseString);
  }

  @Test
  @WithInstructorCoursePermissions
  public void testUploadRosterFromCanvas_updatesExistingStudents() throws Exception {
    // Arrange
    RosterStudent existingStudent =
        RosterStudent.builder()
            .id(1L)
            .firstName("Alice")
            .lastName("Smith")
            .studentId("A111111")
            .email("alice@ucsb.edu")
            .rosterStatus(RosterStatus.ROSTER)
            .orgStatus(OrgStatus.PENDING)
            .build();

    Course course =
        Course.builder()
            .id(1L)
            .courseName("CS156")
            .orgName("ucsb-cs156-s25")
            .term("S25")
            .school("UCSB")
            .canvasApiToken("test-api-token")
            .canvasCourseId("12345")
            .rosterStudents(new ArrayList<>(List.of(existingStudent)))
            .build();
    existingStudent.setCourse(course);

    RosterStudent canvasStudent =
        RosterStudent.builder()
            .firstName("Alicia")
            .lastName("Smith-Updated")
            .studentId("A111111")
            .email("alice@ucsb.edu")
            .build();

    List<RosterStudent> canvasStudents = List.of(canvasStudent);

    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(course));
    when(canvasService.getCanvasRoster(any(Course.class))).thenReturn(canvasStudents);

    // Act
    MvcResult response =
        mockMvc
            .perform(post("/api/courses/canvas/sync/students").with(csrf()).param("courseId", "1"))
            .andExpect(status().isOk())
            .andReturn();

    // Assert
    verify(courseRepository, atLeastOnce()).findById(eq(1L));
    verify(canvasService).getCanvasRoster(any(Course.class));

    String responseString = response.getResponse().getContentAsString();
    LoadResult expectedResult = new LoadResult(0, 1, 0, List.of());
    String expectedJson = mapper.writeValueAsString(expectedResult);
    assertEquals(expectedJson, responseString);
  }

  @Test
  @WithInstructorCoursePermissions
  public void testUploadRosterFromCanvas_handlesRejectedStudents() throws Exception {
    // Arrange - create a situation where student ID and email don't match same record
    RosterStudent studentWithId =
        RosterStudent.builder().id(1L).studentId("A111111").email("other@ucsb.edu").build();
    RosterStudent studentWithEmail =
        RosterStudent.builder().id(2L).studentId("B222222").email("alice@ucsb.edu").build();

    Course course =
        Course.builder()
            .id(1L)
            .courseName("CS156")
            .orgName("ucsb-cs156-s25")
            .term("S25")
            .school("UCSB")
            .canvasApiToken("test-api-token")
            .canvasCourseId("12345")
            .rosterStudents(new ArrayList<>(List.of(studentWithId, studentWithEmail)))
            .build();

    RosterStudent canvasStudent =
        RosterStudent.builder()
            .firstName("Alice")
            .lastName("Smith")
            .studentId("A111111")
            .email("alice@ucsb.edu")
            .build();

    List<RosterStudent> canvasStudents = List.of(canvasStudent);

    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(course));
    when(canvasService.getCanvasRoster(any(Course.class))).thenReturn(canvasStudents);

    // Act
    MvcResult response =
        mockMvc
            .perform(post("/api/courses/canvas/sync/students").with(csrf()).param("courseId", "1"))
            .andExpect(status().isConflict())
            .andReturn();

    // Assert
    verify(rosterStudentRepository, never()).saveAll(any());

    String responseString = response.getResponse().getContentAsString();
    RosterStudent rejectedStudent =
        RosterStudent.builder()
            .firstName("Alice")
            .lastName("Smith")
            .studentId("A111111")
            .email("alice@ucsb.edu")
            .build();
    LoadResult expectedResult = new LoadResult(0, 0, 0, List.of(rejectedStudent));
    String expectedJson = mapper.writeValueAsString(expectedResult);
    assertEquals(expectedJson, responseString);
  }

  @Test
  @WithInstructorCoursePermissions
  public void testUploadRosterFromCanvas_handlesDroppedStudents() throws Exception {
    // Arrange - existing student who will be marked as dropped
    RosterStudent existingStudent =
        RosterStudent.builder()
            .id(1L)
            .firstName("DroppedStudent")
            .lastName("ToBeDropped")
            .studentId("D999999")
            .email("dropped@ucsb.edu")
            .rosterStatus(RosterStatus.ROSTER)
            .orgStatus(OrgStatus.PENDING)
            .build();

    Course course =
        Course.builder()
            .id(1L)
            .courseName("CS156")
            .orgName("ucsb-cs156-s25")
            .term("S25")
            .school("UCSB")
            .canvasApiToken("test-api-token")
            .canvasCourseId("12345")
            .rosterStudents(new ArrayList<>(List.of(existingStudent)))
            .build();
    existingStudent.setCourse(course);

    // Canvas returns a different student, so existing student should be dropped
    RosterStudent canvasStudent =
        RosterStudent.builder()
            .firstName("NewStudent")
            .lastName("FromCanvas")
            .studentId("A111111")
            .email("new@ucsb.edu")
            .build();

    List<RosterStudent> canvasStudents = List.of(canvasStudent);

    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(course));
    when(canvasService.getCanvasRoster(any(Course.class))).thenReturn(canvasStudents);

    ArgumentCaptor<List<RosterStudent>> rosterStudentCaptor = ArgumentCaptor.forClass(List.class);

    // Act
    MvcResult response =
        mockMvc
            .perform(post("/api/courses/canvas/sync/students").with(csrf()).param("courseId", "1"))
            .andExpect(status().isOk())
            .andReturn();

    // Assert
    verify(rosterStudentRepository).saveAll(rosterStudentCaptor.capture());
    verify(service).runAsJob(any(RemoveStudentsJob.class));

    // Check that dropped student has DROPPED status in saved list
    List<RosterStudent> savedStudents = rosterStudentCaptor.getValue();
    RosterStudent droppedInSavedList =
        savedStudents.stream()
            .filter(s -> s.getStudentId().equals("D999999"))
            .findFirst()
            .orElse(null);
    assertEquals(RosterStatus.DROPPED, droppedInSavedList.getRosterStatus());

    String responseString = response.getResponse().getContentAsString();
    LoadResult expectedResult = new LoadResult(1, 0, 1, List.of());
    String expectedJson = mapper.writeValueAsString(expectedResult);
    assertEquals(expectedJson, responseString);
  }

  @Test
  @WithMockUser(roles = {"ADMIN"})
  public void testUploadRosterFromCanvas_courseNotFound() throws Exception {
    // Arrange
    when(courseRepository.findById(eq(999L))).thenReturn(Optional.empty());

    // Act
    MvcResult response =
        mockMvc
            .perform(
                post("/api/courses/canvas/sync/students").with(csrf()).param("courseId", "999"))
            .andExpect(status().isNotFound())
            .andReturn();

    // Assert
    verify(courseRepository, atLeastOnce()).findById(eq(999L));
    verify(canvasService, never()).getCanvasRoster(any(Course.class));

    String responseString = response.getResponse().getContentAsString();
    Map<String, String> expectedMap =
        Map.of(
            "type", "EntityNotFoundException",
            "message", "Course with id 999 not found");
    String expectedJson = mapper.writeValueAsString(expectedMap);
    assertEquals(expectedJson, responseString);
  }

  @Test
  @WithInstructorCoursePermissions
  public void testUploadRosterFromCanvas_setsJOINCOURSEStatusWhenInstallationIdPresent()
      throws Exception {
    // Arrange
    Course course =
        Course.builder()
            .id(1L)
            .courseName("CS156")
            .orgName("ucsb-cs156-s25")
            .term("S25")
            .school("UCSB")
            .installationId("12345")
            .canvasApiToken("test-api-token")
            .canvasCourseId("12345")
            .rosterStudents(new ArrayList<>())
            .build();

    RosterStudent canvasStudent =
        RosterStudent.builder()
            .firstName("Alice")
            .lastName("Smith")
            .studentId("A111111")
            .email("alice@ucsb.edu")
            .build();

    List<RosterStudent> canvasStudents = List.of(canvasStudent);

    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(course));
    when(canvasService.getCanvasRoster(any(Course.class))).thenReturn(canvasStudents);

    ArgumentCaptor<List<RosterStudent>> rosterStudentCaptor = ArgumentCaptor.forClass(List.class);

    // Act
    mockMvc
        .perform(post("/api/courses/canvas/sync/students").with(csrf()).param("courseId", "1"))
        .andExpect(status().isOk());

    // Assert
    verify(rosterStudentRepository).saveAll(rosterStudentCaptor.capture());

    List<RosterStudent> savedStudents = rosterStudentCaptor.getValue();
    RosterStudent savedStudent = savedStudents.get(0);
    assertEquals(OrgStatus.JOINCOURSE, savedStudent.getOrgStatus());
  }

  @Test
  @WithInstructorCoursePermissions
  public void testUploadRosterFromCanvas_setsPENDINGStatusWhenNoInstallationId() throws Exception {
    // Arrange
    Course course =
        Course.builder()
            .id(1L)
            .courseName("CS156")
            .orgName("ucsb-cs156-s25")
            .term("S25")
            .school("UCSB")
            .installationId(null)
            .canvasApiToken("test-api-token")
            .canvasCourseId("12345")
            .rosterStudents(new ArrayList<>())
            .build();

    RosterStudent canvasStudent =
        RosterStudent.builder()
            .firstName("Alice")
            .lastName("Smith")
            .studentId("A111111")
            .email("alice@ucsb.edu")
            .build();

    List<RosterStudent> canvasStudents = List.of(canvasStudent);

    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(course));
    when(canvasService.getCanvasRoster(any(Course.class))).thenReturn(canvasStudents);

    ArgumentCaptor<List<RosterStudent>> rosterStudentCaptor = ArgumentCaptor.forClass(List.class);

    // Act
    mockMvc
        .perform(post("/api/courses/canvas/sync/students").with(csrf()).param("courseId", "1"))
        .andExpect(status().isOk());

    // Assert
    verify(rosterStudentRepository).saveAll(rosterStudentCaptor.capture());

    List<RosterStudent> savedStudents = rosterStudentCaptor.getValue();
    RosterStudent savedStudent = savedStudents.get(0);
    assertEquals(OrgStatus.PENDING, savedStudent.getOrgStatus());
  }

  @Test
  @WithInstructorCoursePermissions
  public void testUploadRosterFromCanvas_manualStudentStaysManual() throws Exception {
    // Arrange - MANUAL students should not become DROPPED
    RosterStudent manualStudent =
        RosterStudent.builder()
            .id(1L)
            .firstName("ManualStudent")
            .lastName("NotDropped")
            .studentId("M999999")
            .email("manual@ucsb.edu")
            .rosterStatus(RosterStatus.MANUAL)
            .orgStatus(OrgStatus.PENDING)
            .build();

    Course course =
        Course.builder()
            .id(1L)
            .courseName("CS156")
            .orgName("ucsb-cs156-s25")
            .term("S25")
            .school("UCSB")
            .canvasApiToken("test-api-token")
            .canvasCourseId("12345")
            .rosterStudents(new ArrayList<>(List.of(manualStudent)))
            .build();
    manualStudent.setCourse(course);

    // Canvas returns a different student - manual student should NOT be dropped
    RosterStudent canvasStudent =
        RosterStudent.builder()
            .firstName("NewStudent")
            .lastName("FromCanvas")
            .studentId("A111111")
            .email("new@ucsb.edu")
            .build();

    List<RosterStudent> canvasStudents = List.of(canvasStudent);

    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(course));
    when(canvasService.getCanvasRoster(any(Course.class))).thenReturn(canvasStudents);

    ArgumentCaptor<List<RosterStudent>> rosterStudentCaptor = ArgumentCaptor.forClass(List.class);

    // Act
    MvcResult response =
        mockMvc
            .perform(post("/api/courses/canvas/sync/students").with(csrf()).param("courseId", "1"))
            .andExpect(status().isOk())
            .andReturn();

    // Assert
    verify(rosterStudentRepository).saveAll(rosterStudentCaptor.capture());

    // Check that manual student still has MANUAL status
    List<RosterStudent> savedStudents = rosterStudentCaptor.getValue();
    RosterStudent manualInSavedList =
        savedStudents.stream()
            .filter(s -> s.getStudentId().equals("M999999"))
            .findFirst()
            .orElse(null);
    assertEquals(RosterStatus.MANUAL, manualInSavedList.getRosterStatus());

    String responseString = response.getResponse().getContentAsString();
    // 1 inserted (new student), 0 dropped (manual stays manual)
    LoadResult expectedResult = new LoadResult(1, 0, 0, List.of());
    String expectedJson = mapper.writeValueAsString(expectedResult);
    assertEquals(expectedJson, responseString);
  }
}
