package edu.ucsb.cs156.frontiers.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import edu.ucsb.cs156.frontiers.ControllerTestCase;
import edu.ucsb.cs156.frontiers.annotations.WithInstructorCoursePermissions;
import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.entities.RosterStudent;
import edu.ucsb.cs156.frontiers.enums.OrgStatus;
import edu.ucsb.cs156.frontiers.enums.RosterStatus;
import edu.ucsb.cs156.frontiers.enums.School;
import edu.ucsb.cs156.frontiers.models.RosterStudentDTO;
import edu.ucsb.cs156.frontiers.repositories.CourseRepository;
import edu.ucsb.cs156.frontiers.repositories.RosterStudentRepository;
import edu.ucsb.cs156.frontiers.services.RosterStudentDTOService;
import edu.ucsb.cs156.frontiers.testconfig.TestConfig;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;

@WebMvcTest(controllers = {CSVDownloadsController.class})
@Import(TestConfig.class)
public class CSVDownloadsControllerTests extends ControllerTestCase {

  @MockitoBean(answers = Answers.CALLS_REAL_METHODS)
  RosterStudentDTOService rosterStudentDTOService;

  @MockitoBean(answers = Answers.RETURNS_MOCKS)
  CourseRepository courseRepository;

  @MockitoBean(answers = Answers.RETURNS_MOCKS)
  RosterStudentRepository rosterStudentRepository;

  @Mock(answer = Answers.CALLS_REAL_METHODS)
  StatefulBeanToCsv<RosterStudentDTO> csvWriter;

  @Autowired ObjectMapper objectMapper;

  @Test
  @WithMockUser(roles = {"ADMIN"})
  public void test_no_such_course() throws Exception {

    // arrange

    when(courseRepository.findById(eq(1L))).thenReturn(Optional.empty());

    // act

    MvcResult response = mockMvc.perform(get("/api/csv/rosterstudents?courseId=1")).andReturn();

    // assert
    String actualResponse = response.getResponse().getContentAsString();

    objectMapper.readValue(
        response.getResponse().getContentAsString(), new TypeReference<Map<String, String>>() {});

    Map<String, String> errorResponse =
        objectMapper.readValue(actualResponse, new TypeReference<Map<String, String>>() {});
    Map<String, String> expectedResponse =
        Map.of("message", "Course with id 1 not found", "type", "EntityNotFoundException");
    assertEquals(expectedResponse, errorResponse);
    assertEquals(HttpStatus.NOT_FOUND.value(), response.getResponse().getStatus());
  }

  @Test
  @WithInstructorCoursePermissions
  public void test_csv_exception() throws Exception {

    // arrange

    Course course = Course.builder().id(1L).build();
    doReturn(Optional.of(course)).when(courseRepository).findById(eq(1L));
    doReturn(List.of()).when(rosterStudentDTOService).getRosterStudentDTOs(eq(1L));
    doReturn(csvWriter).when(rosterStudentDTOService).getStatefulBeanToCSV(any());

    doThrow(new CsvDataTypeMismatchException()).when(csvWriter).write(anyList());

    // act

    MvcResult response =
        mockMvc
            .perform(get("/api/csv/rosterstudents?courseId=1"))
            .andExpect(request().asyncStarted())
            .andDo(MvcResult::getAsyncResult)
            .andExpect(status().isOk())
            .andReturn();

    // assert
    String actualResponse = response.getResponse().getContentAsString();
    String expectedMessage = "";
    assertEquals(expectedMessage, actualResponse);
  }

  @Test
  @DirtiesContext(methodMode = DirtiesContext.MethodMode.BEFORE_METHOD)
  @WithInstructorCoursePermissions
  public void mockMvcSRBTest() throws Exception {
    Course course =
        Course.builder()
            .id(1L)
            .courseName("ucsb-cs156-s25")
            .term("S25")
            .school(School.UCSB)
            .build();

    RosterStudentDTO rosterStudentDTO =
        new RosterStudentDTO(
            42L,
            course.getId(),
            "12345",
            "Chris",
            "Gaucho",
            "cgaucho@ucsb.edu",
            "Section A",
            102L,
            12345,
            "cgaucho",
            RosterStatus.ROSTER,
            OrgStatus.PENDING,
            List.of("Team Alpha", "Team Beta"));

    doReturn(Optional.of(course)).when(courseRepository).findById(eq(1L));
    doReturn(List.of(rosterStudentDTO)).when(rosterStudentDTOService).getRosterStudentDTOs(eq(1L));

    String expectedResponse =
        """
            "COURSEID","EMAIL","FIRSTNAME","GITHUBID","GITHUBLOGIN","ID","LASTNAME","ORGSTATUS","ROSTERSTATUS","SECTION","STUDENTID","TEAMS","USERID"
            "1","cgaucho@ucsb.edu","Chris","12345","cgaucho","42","Gaucho","PENDING","ROSTER","Section A","12345","Team Alpha","102"
            """;

    MvcResult response =
        mockMvc
            .perform(get("/api/csv/rosterstudents?courseId=1"))
            .andExpect(request().asyncStarted())
            .andDo(MvcResult::getAsyncResult)
            .andExpect(status().isOk())
            .andReturn();

    verify(rosterStudentDTOService, times(1)).getRosterStudentDTOs(eq(1L));
    verify(rosterStudentDTOService, times(1)).getStatefulBeanToCSV(any());

    assertEquals(expectedResponse, response.getResponse().getContentAsString());
  }

  @Test
  @WithMockUser(roles = {"ADMIN"})
  public void test_catme_no_such_course() throws Exception {

    // arrange

    when(courseRepository.findById(eq(1L))).thenReturn(Optional.empty());

    // act

    MvcResult response = mockMvc.perform(get("/api/csv/catme?courseId=1")).andReturn();

    // assert
    String actualResponse = response.getResponse().getContentAsString();

    Map<String, String> errorResponse =
        objectMapper.readValue(actualResponse, new TypeReference<Map<String, String>>() {});
    Map<String, String> expectedResponse =
        Map.of("message", "Course with id 1 not found", "type", "EntityNotFoundException");
    assertEquals(expectedResponse, errorResponse);
    assertEquals(HttpStatus.NOT_FOUND.value(), response.getResponse().getStatus());
  }

  @Test
  @WithInstructorCoursePermissions
  public void test_catme_csv_filters_to_roster_and_manual() throws Exception {
    Course course =
        Course.builder()
            .id(1L)
            .courseName("ucsb-cs156-s25")
            .term("S25")
            .school(School.UCSB)
            .build();

    RosterStudent rosterStudent = mock(RosterStudent.class);
    when(rosterStudent.getFirstName()).thenReturn("Chris");
    when(rosterStudent.getLastName()).thenReturn("Gaucho");
    when(rosterStudent.getEmail()).thenReturn("cgaucho@ucsb.edu");
    when(rosterStudent.getStudentId()).thenReturn("12345");
    when(rosterStudent.getRosterStatus()).thenReturn(RosterStatus.ROSTER);
    when(rosterStudent.getTeams()).thenReturn(List.of("Team Alpha"));

    RosterStudent manualStudent = mock(RosterStudent.class);
    when(manualStudent.getFirstName()).thenReturn("Pat");
    when(manualStudent.getLastName()).thenReturn("Student");
    when(manualStudent.getEmail()).thenReturn("pstudent@ucsb.edu");
    when(manualStudent.getStudentId()).thenReturn("23456");
    when(manualStudent.getRosterStatus()).thenReturn(RosterStatus.MANUAL);
    when(manualStudent.getTeams()).thenReturn(Collections.emptyList());

    RosterStudent nullTeamsStudent = mock(RosterStudent.class);
    when(nullTeamsStudent.getFirstName()).thenReturn("Taylor");
    when(nullTeamsStudent.getLastName()).thenReturn("NoTeam");
    when(nullTeamsStudent.getEmail()).thenReturn("taylor@ucsb.edu");
    when(nullTeamsStudent.getStudentId()).thenReturn("34567");
    when(nullTeamsStudent.getRosterStatus()).thenReturn(RosterStatus.ROSTER);
    when(nullTeamsStudent.getTeams()).thenReturn(null);

    doReturn(Optional.of(course)).when(courseRepository).findById(eq(1L));
    doReturn(List.of(rosterStudent, manualStudent, nullTeamsStudent))
        .when(rosterStudentRepository)
        .findByCourseIdAndRosterStatusInOrderByFirstNameAscLastNameAscIgnoreCase(
            eq(1L), eq(List.of(RosterStatus.ROSTER, RosterStatus.MANUAL)));

    String expectedResponse =
        """
            first,last,email,id,team
            Chris,Gaucho,cgaucho@ucsb.edu,12345,Team Alpha
            Pat,Student,pstudent@ucsb.edu,23456,
            Taylor,NoTeam,taylor@ucsb.edu,34567,
            """;

    MvcResult response =
        mockMvc
            .perform(get("/api/csv/catme?courseId=1"))
            .andExpect(request().asyncStarted())
            .andDo(MvcResult::getAsyncResult)
            .andExpect(status().isOk())
            .andReturn();

    verify(rosterStudentRepository, times(1))
        .findByCourseIdAndRosterStatusInOrderByFirstNameAscLastNameAscIgnoreCase(
            eq(1L), eq(List.of(RosterStatus.ROSTER, RosterStatus.MANUAL)));
    assertEquals(
        expectedResponse, response.getResponse().getContentAsString().replace("\r\n", "\n"));
  }
}
