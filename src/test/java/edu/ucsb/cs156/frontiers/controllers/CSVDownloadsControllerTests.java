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
import edu.ucsb.cs156.frontiers.entities.CourseOption;
import edu.ucsb.cs156.frontiers.entities.RosterStudent;
import edu.ucsb.cs156.frontiers.entities.Section;
import edu.ucsb.cs156.frontiers.enums.OrgStatus;
import edu.ucsb.cs156.frontiers.enums.RosterStatus;
import edu.ucsb.cs156.frontiers.enums.School;
import edu.ucsb.cs156.frontiers.models.RosterStudentDTO;
import edu.ucsb.cs156.frontiers.repositories.CourseOptionRepository;
import edu.ucsb.cs156.frontiers.repositories.CourseRepository;
import edu.ucsb.cs156.frontiers.repositories.RosterStudentRepository;
import edu.ucsb.cs156.frontiers.repositories.SectionRepository;
import edu.ucsb.cs156.frontiers.services.RosterStudentDTOService;
import edu.ucsb.cs156.frontiers.services.SectionTranslationService;
import edu.ucsb.cs156.frontiers.services.TeamCsvService;
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
@Import({TestConfig.class, SectionTranslationService.class, TeamCsvService.class})
public class CSVDownloadsControllerTests extends ControllerTestCase {

  @MockitoBean(answers = Answers.CALLS_REAL_METHODS)
  RosterStudentDTOService rosterStudentDTOService;

  @MockitoBean(answers = Answers.RETURNS_MOCKS)
  CourseRepository courseRepository;

  @MockitoBean(answers = Answers.RETURNS_MOCKS)
  RosterStudentRepository rosterStudentRepository;

  @MockitoBean CourseOptionRepository courseOptionRepository;

  @MockitoBean SectionRepository sectionRepository;

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
    when(rosterStudent.getSection()).thenReturn("0100");

    RosterStudent manualStudent = mock(RosterStudent.class);
    when(manualStudent.getFirstName()).thenReturn("Pat");
    when(manualStudent.getLastName()).thenReturn("Student");
    when(manualStudent.getEmail()).thenReturn("pstudent@ucsb.edu");
    when(manualStudent.getStudentId()).thenReturn("23456");
    when(manualStudent.getRosterStatus()).thenReturn(RosterStatus.MANUAL);
    when(manualStudent.getTeams()).thenReturn(Collections.emptyList());
    when(manualStudent.getSection()).thenReturn("");

    RosterStudent nullTeamsStudent = mock(RosterStudent.class);
    when(nullTeamsStudent.getFirstName()).thenReturn("Taylor");
    when(nullTeamsStudent.getLastName()).thenReturn("NoTeam");
    when(nullTeamsStudent.getEmail()).thenReturn("taylor@ucsb.edu");
    when(nullTeamsStudent.getStudentId()).thenReturn("34567");
    when(nullTeamsStudent.getRosterStatus()).thenReturn(RosterStatus.ROSTER);
    when(nullTeamsStudent.getTeams()).thenReturn(null);
    when(nullTeamsStudent.getSection()).thenReturn(null);

    doReturn(Optional.of(course)).when(courseRepository).findById(eq(1L));
    when(courseOptionRepository.findByCourseIdAndOption(eq(1L), eq("TRANSLATE_SECTIONS")))
        .thenReturn(Optional.empty());
    doReturn(List.of(rosterStudent, manualStudent, nullTeamsStudent))
        .when(rosterStudentRepository)
        .findByCourseIdAndRosterStatusInOrderByFirstNameAscLastNameAscIgnoreCase(
            eq(1L), eq(List.of(RosterStatus.ROSTER, RosterStatus.MANUAL)));

    String expectedResponse =
        """
            first,last,email,id,section,team
            Chris,Gaucho,cgaucho@ucsb.edu,12345,0100,Team Alpha
            Pat,Student,pstudent@ucsb.edu,23456,,
            Taylor,NoTeam,taylor@ucsb.edu,34567,,
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
    verify(sectionRepository, never()).findByCourseId(any());
  }

  private RosterStudent catmeStudent(String first, String last, String section) {
    return RosterStudent.builder()
        .firstName(first)
        .lastName(last)
        .email(first.toLowerCase() + "@ucsb.edu")
        .studentId("1")
        .rosterStatus(RosterStatus.ROSTER)
        .section(section)
        .build();
  }

  @Test
  @WithInstructorCoursePermissions
  public void test_catme_csv_translates_sections_when_option_enabled() throws Exception {
    Course course = Course.builder().id(1L).courseName("ucsb-cs156-s25").build();
    doReturn(Optional.of(course)).when(courseRepository).findById(eq(1L));
    doReturn(
            List.of(
                catmeStudent("Alice", "Known", "0100"),
                catmeStudent("Bob", "Unknown", "0999"),
                catmeStudent("Cara", "Blank", ""),
                catmeStudent("Dan", "Null", null)))
        .when(rosterStudentRepository)
        .findByCourseIdAndRosterStatusInOrderByFirstNameAscLastNameAscIgnoreCase(
            eq(1L), eq(List.of(RosterStatus.ROSTER, RosterStatus.MANUAL)));
    when(courseOptionRepository.findByCourseIdAndOption(eq(1L), eq("TRANSLATE_SECTIONS")))
        .thenReturn(
            Optional.of(
                CourseOption.builder()
                    .courseId(1L)
                    .option("TRANSLATE_SECTIONS")
                    .enabled(true)
                    .build()));
    when(sectionRepository.findByCourseId(eq(1L)))
        .thenReturn(
            List.of(
                Section.builder().id(10L).course(course).section("0100").label("Tue 9am").build(),
                Section.builder()
                    .id(11L)
                    .course(course)
                    .section("0200")
                    .label("Tue 10am")
                    .build()));

    String expectedResponse =
        """
            first,last,email,id,section,team
            Alice,Known,alice@ucsb.edu,1,Tue 9am,
            Bob,Unknown,bob@ucsb.edu,1,0999,
            Cara,Blank,cara@ucsb.edu,1,,
            Dan,Null,dan@ucsb.edu,1,,
            """;

    MvcResult response =
        mockMvc
            .perform(get("/api/csv/catme?courseId=1"))
            .andExpect(request().asyncStarted())
            .andDo(MvcResult::getAsyncResult)
            .andExpect(status().isOk())
            .andReturn();

    assertEquals(
        expectedResponse, response.getResponse().getContentAsString().replace("\r\n", "\n"));
    verify(courseOptionRepository, times(1))
        .findByCourseIdAndOption(eq(1L), eq("TRANSLATE_SECTIONS"));
    verify(sectionRepository, times(1)).findByCourseId(eq(1L));
  }

  @Test
  @WithInstructorCoursePermissions
  public void test_catme_csv_uses_raw_sections_when_option_explicitly_disabled() throws Exception {
    Course course = Course.builder().id(1L).courseName("ucsb-cs156-s25").build();
    doReturn(Optional.of(course)).when(courseRepository).findById(eq(1L));
    doReturn(List.of(catmeStudent("Alice", "Known", "0100")))
        .when(rosterStudentRepository)
        .findByCourseIdAndRosterStatusInOrderByFirstNameAscLastNameAscIgnoreCase(
            eq(1L), eq(List.of(RosterStatus.ROSTER, RosterStatus.MANUAL)));
    when(courseOptionRepository.findByCourseIdAndOption(eq(1L), eq("TRANSLATE_SECTIONS")))
        .thenReturn(
            Optional.of(
                CourseOption.builder()
                    .courseId(1L)
                    .option("TRANSLATE_SECTIONS")
                    .enabled(false)
                    .build()));

    String expectedResponse =
        """
            first,last,email,id,section,team
            Alice,Known,alice@ucsb.edu,1,0100,
            """;

    MvcResult response =
        mockMvc
            .perform(get("/api/csv/catme?courseId=1"))
            .andExpect(request().asyncStarted())
            .andDo(MvcResult::getAsyncResult)
            .andExpect(status().isOk())
            .andReturn();

    assertEquals(
        expectedResponse, response.getResponse().getContentAsString().replace("\r\n", "\n"));
    verify(sectionRepository, never()).findByCourseId(any());
  }

  @Test
  public void translateSection_handles_null_blank_missing_and_present() {
    Map<String, String> translations = Map.of("0100", "Tue 9am", "", "Unassigned");
    assertEquals("Tue 9am", SectionTranslationService.translateSection("0100", translations));
    assertEquals("0999", SectionTranslationService.translateSection("0999", translations));
    assertEquals("Unassigned", SectionTranslationService.translateSection("", translations));
    assertEquals("Unassigned", SectionTranslationService.translateSection(null, translations));
    assertEquals("", SectionTranslationService.translateSection(null, Map.of()));
  }

  // name2team

  @Test
  @WithMockUser(roles = {"ADMIN"})
  public void test_name2team_no_such_course() throws Exception {
    when(courseRepository.findById(eq(1L))).thenReturn(Optional.empty());

    MvcResult response = mockMvc.perform(get("/api/csv/name2team?courseId=1")).andReturn();

    Map<String, String> errorResponse =
        objectMapper.readValue(
            response.getResponse().getContentAsString(),
            new TypeReference<Map<String, String>>() {});
    Map<String, String> expectedResponse =
        Map.of("message", "Course with id 1 not found", "type", "EntityNotFoundException");
    assertEquals(expectedResponse, errorResponse);
    assertEquals(HttpStatus.NOT_FOUND.value(), response.getResponse().getStatus());
    verify(rosterStudentRepository, never())
        .findByCourseIdAndRosterStatusInOrderByFirstNameAscLastNameAscIgnoreCase(any(), any());
  }

  @Test
  @WithInstructorCoursePermissions
  public void test_name2team_rejects_columns_less_than_one() throws Exception {
    Course course = Course.builder().id(1L).courseName("ucsb-cs156-s26").build();
    doReturn(Optional.of(course)).when(courseRepository).findById(eq(1L));

    MvcResult response =
        mockMvc.perform(get("/api/csv/name2team?courseId=1&columns=0")).andReturn();

    Map<String, String> errorResponse =
        objectMapper.readValue(
            response.getResponse().getContentAsString(),
            new TypeReference<Map<String, String>>() {});
    Map<String, String> expectedResponse =
        Map.of("message", "columns must be at least 1", "type", "IllegalArgumentException");
    assertEquals(expectedResponse, errorResponse);
    assertEquals(HttpStatus.BAD_REQUEST.value(), response.getResponse().getStatus());
    verify(rosterStudentRepository, never())
        .findByCourseIdAndRosterStatusInOrderByFirstNameAscLastNameAscIgnoreCase(any(), any());
  }

  private RosterStudent name2TeamStudent(String first, String last, String team) {
    RosterStudent student = mock(RosterStudent.class);
    when(student.getFirstName()).thenReturn(first);
    when(student.getLastName()).thenReturn(last);
    when(student.getTeams()).thenReturn(team == null ? List.of() : List.of(team));
    return student;
  }

  private List<RosterStudent> name2TeamStudents() {
    return List.of(
        name2TeamStudent("ALEX", "LEE", "s26-03"),
        name2TeamStudent("ALEX", "YOUNG", "s26-10"),
        name2TeamStudent("ALEXANDER", "HAMILTON", "s26-07"),
        name2TeamStudent("DAVID", "CHANG", "s26-07"),
        name2TeamStudent("DAVID", "CHEN", "s26-02"),
        name2TeamStudent("PAT", "SMITH", "s26-01"),
        name2TeamStudent("PAT", "SMITH", "s26-04"),
        name2TeamStudent("RITAM KUMAR", "SINGH", "s26-09"),
        name2TeamStudent("ZOE", "NOTEAM", null));
  }

  @Test
  @WithInstructorCoursePermissions
  public void test_name2team_csv_default_is_four_columns() throws Exception {
    Course course = Course.builder().id(1L).courseName("ucsb-cs156-s26").build();
    doReturn(Optional.of(course)).when(courseRepository).findById(eq(1L));
    doReturn(name2TeamStudents())
        .when(rosterStudentRepository)
        .findByCourseIdAndRosterStatusInOrderByFirstNameAscLastNameAscIgnoreCase(
            eq(1L), eq(List.of(RosterStatus.ROSTER, RosterStatus.MANUAL)));

    String expectedResponse =
        """
            Name,Team,,Name,Team,,Name,Team,,Name,Team
            Alex L,s26-03,,David Chang,s26-07,,Pat Smith*,s26-01,,Ritam,s26-09
            Alex Y,s26-10,,David Chen,s26-02,,Pat Smith*,s26-04,,Zoe,
            Alexander,s26-07,,,,,,,,,
            """;

    MvcResult response =
        mockMvc
            .perform(get("/api/csv/name2team?courseId=1"))
            .andExpect(request().asyncStarted())
            .andDo(MvcResult::getAsyncResult)
            .andExpect(status().isOk())
            .andReturn();

    verify(rosterStudentRepository, times(1))
        .findByCourseIdAndRosterStatusInOrderByFirstNameAscLastNameAscIgnoreCase(
            eq(1L), eq(List.of(RosterStatus.ROSTER, RosterStatus.MANUAL)));
    assertEquals(
        expectedResponse, response.getResponse().getContentAsString().replace("\r\n", "\n"));
    assertEquals(
        "attachment;filename=ucsb-cs156-s26_name2team.csv",
        response.getResponse().getHeader("Content-Disposition"));
    assertEquals("text/csv; charset=UTF-8", response.getResponse().getContentType());
  }

  @Test
  @WithInstructorCoursePermissions
  public void test_name2team_csv_with_explicit_columns() throws Exception {
    Course course = Course.builder().id(1L).courseName("ucsb-cs156-s26").build();
    doReturn(Optional.of(course)).when(courseRepository).findById(eq(1L));
    doReturn(name2TeamStudents())
        .when(rosterStudentRepository)
        .findByCourseIdAndRosterStatusInOrderByFirstNameAscLastNameAscIgnoreCase(
            eq(1L), eq(List.of(RosterStatus.ROSTER, RosterStatus.MANUAL)));

    String expectedResponse =
        """
            Name,Team,,Name,Team
            Alex L,s26-03,,Pat Smith*,s26-01
            Alex Y,s26-10,,Pat Smith*,s26-04
            Alexander,s26-07,,Ritam,s26-09
            David Chang,s26-07,,Zoe,
            David Chen,s26-02,,,
            """;

    MvcResult response =
        mockMvc
            .perform(get("/api/csv/name2team?courseId=1&columns=2"))
            .andExpect(request().asyncStarted())
            .andDo(MvcResult::getAsyncResult)
            .andExpect(status().isOk())
            .andReturn();

    assertEquals(
        expectedResponse, response.getResponse().getContentAsString().replace("\r\n", "\n"));
  }

  @Test
  @WithInstructorCoursePermissions
  public void test_name2team_csv_with_one_column() throws Exception {
    Course course = Course.builder().id(1L).courseName("ucsb-cs156-s26").build();
    doReturn(Optional.of(course)).when(courseRepository).findById(eq(1L));
    doReturn(List.of(name2TeamStudent("RITAM", "SINGH", "s26-09")))
        .when(rosterStudentRepository)
        .findByCourseIdAndRosterStatusInOrderByFirstNameAscLastNameAscIgnoreCase(
            eq(1L), eq(List.of(RosterStatus.ROSTER, RosterStatus.MANUAL)));

    String expectedResponse =
        """
            Name,Team
            Ritam,s26-09
            """;

    MvcResult response =
        mockMvc
            .perform(get("/api/csv/name2team?courseId=1&columns=1"))
            .andExpect(request().asyncStarted())
            .andDo(MvcResult::getAsyncResult)
            .andExpect(status().isOk())
            .andReturn();

    assertEquals(
        expectedResponse, response.getResponse().getContentAsString().replace("\r\n", "\n"));
  }

  // teamtable

  @Test
  @WithMockUser(roles = {"ADMIN"})
  public void test_teamtable_no_such_course() throws Exception {
    when(courseRepository.findById(eq(1L))).thenReturn(Optional.empty());

    MvcResult response = mockMvc.perform(get("/api/csv/teamtable?courseId=1")).andReturn();

    Map<String, String> errorResponse =
        objectMapper.readValue(
            response.getResponse().getContentAsString(),
            new TypeReference<Map<String, String>>() {});
    Map<String, String> expectedResponse =
        Map.of("message", "Course with id 1 not found", "type", "EntityNotFoundException");
    assertEquals(expectedResponse, errorResponse);
    assertEquals(HttpStatus.NOT_FOUND.value(), response.getResponse().getStatus());
    verify(rosterStudentRepository, never())
        .findByCourseIdAndRosterStatusInOrderByFirstNameAscLastNameAscIgnoreCase(any(), any());
  }

  @Test
  @WithInstructorCoursePermissions
  public void test_teamtable_rejects_columns_less_than_one() throws Exception {
    Course course = Course.builder().id(1L).courseName("ucsb-cs156-s26").build();
    doReturn(Optional.of(course)).when(courseRepository).findById(eq(1L));

    MvcResult response =
        mockMvc.perform(get("/api/csv/teamtable?courseId=1&columns=0")).andReturn();

    Map<String, String> errorResponse =
        objectMapper.readValue(
            response.getResponse().getContentAsString(),
            new TypeReference<Map<String, String>>() {});
    Map<String, String> expectedResponse =
        Map.of("message", "columns must be at least 1", "type", "IllegalArgumentException");
    assertEquals(expectedResponse, errorResponse);
    assertEquals(HttpStatus.BAD_REQUEST.value(), response.getResponse().getStatus());
    verify(rosterStudentRepository, never())
        .findByCourseIdAndRosterStatusInOrderByFirstNameAscLastNameAscIgnoreCase(any(), any());
  }

  @Test
  @WithInstructorCoursePermissions
  public void test_teamtable_csv_default_is_four_columns() throws Exception {
    Course course = Course.builder().id(1L).courseName("ucsb-cs156-s26").build();
    doReturn(Optional.of(course)).when(courseRepository).findById(eq(1L));
    doReturn(name2TeamStudents())
        .when(rosterStudentRepository)
        .findByCourseIdAndRosterStatusInOrderByFirstNameAscLastNameAscIgnoreCase(
            eq(1L), eq(List.of(RosterStatus.ROSTER, RosterStatus.MANUAL)));

    // Teams in name order: s26-01, s26-02, s26-03, s26-04, s26-07, s26-09, s26-10, then the
    // unassigned student. 8 groups over 4 columns is 2 groups per column.
    String expectedResponse =
        """
            Team,Name,,Team,Name,,Team,Name,,Team,Name
            s26-01,Pat Smith*,,s26-03,Alex L,,s26-07,Alexander,,s26-10,Alex Y
            s26-02,David Chen,,s26-04,Pat Smith*,,s26-07,David Chang,,,Zoe
            "",,,,,,s26-09,Ritam,,,
            """;

    MvcResult response =
        mockMvc
            .perform(get("/api/csv/teamtable?courseId=1"))
            .andExpect(request().asyncStarted())
            .andDo(MvcResult::getAsyncResult)
            .andExpect(status().isOk())
            .andReturn();

    verify(rosterStudentRepository, times(1))
        .findByCourseIdAndRosterStatusInOrderByFirstNameAscLastNameAscIgnoreCase(
            eq(1L), eq(List.of(RosterStatus.ROSTER, RosterStatus.MANUAL)));
    assertEquals(
        expectedResponse, response.getResponse().getContentAsString().replace("\r\n", "\n"));
    assertEquals(
        "attachment;filename=ucsb-cs156-s26_teamtable.csv",
        response.getResponse().getHeader("Content-Disposition"));
    assertEquals("text/csv; charset=UTF-8", response.getResponse().getContentType());
  }

  @Test
  @WithInstructorCoursePermissions
  public void test_teamtable_csv_with_explicit_columns() throws Exception {
    Course course = Course.builder().id(1L).courseName("ucsb-cs156-s26").build();
    doReturn(Optional.of(course)).when(courseRepository).findById(eq(1L));
    doReturn(name2TeamStudents())
        .when(rosterStudentRepository)
        .findByCourseIdAndRosterStatusInOrderByFirstNameAscLastNameAscIgnoreCase(
            eq(1L), eq(List.of(RosterStatus.ROSTER, RosterStatus.MANUAL)));

    String expectedResponse =
        """
            Team,Name,,Team,Name
            s26-01,Pat Smith*,,s26-07,Alexander
            s26-02,David Chen,,s26-07,David Chang
            s26-03,Alex L,,s26-09,Ritam
            s26-04,Pat Smith*,,s26-10,Alex Y
            "",,,,Zoe
            """;

    MvcResult response =
        mockMvc
            .perform(get("/api/csv/teamtable?courseId=1&columns=2"))
            .andExpect(request().asyncStarted())
            .andDo(MvcResult::getAsyncResult)
            .andExpect(status().isOk())
            .andReturn();

    assertEquals(
        expectedResponse, response.getResponse().getContentAsString().replace("\r\n", "\n"));
  }

  @Test
  @WithInstructorCoursePermissions
  public void test_teamtable_csv_with_one_column() throws Exception {
    Course course = Course.builder().id(1L).courseName("ucsb-cs156-s26").build();
    doReturn(Optional.of(course)).when(courseRepository).findById(eq(1L));
    doReturn(
            List.of(
                name2TeamStudent("RITAM", "SINGH", "s26-09"),
                name2TeamStudent("ZOE", "LEE", "s26-01")))
        .when(rosterStudentRepository)
        .findByCourseIdAndRosterStatusInOrderByFirstNameAscLastNameAscIgnoreCase(
            eq(1L), eq(List.of(RosterStatus.ROSTER, RosterStatus.MANUAL)));

    String expectedResponse =
        """
            Team,Name
            s26-01,Zoe
            s26-09,Ritam
            """;

    MvcResult response =
        mockMvc
            .perform(get("/api/csv/teamtable?courseId=1&columns=1"))
            .andExpect(request().asyncStarted())
            .andDo(MvcResult::getAsyncResult)
            .andExpect(status().isOk())
            .andReturn();

    assertEquals(
        expectedResponse, response.getResponse().getContentAsString().replace("\r\n", "\n"));
  }
}
