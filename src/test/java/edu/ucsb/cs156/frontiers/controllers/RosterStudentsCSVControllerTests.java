package edu.ucsb.cs156.frontiers.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import edu.ucsb.cs156.frontiers.services.CurrentUserService;
import edu.ucsb.cs156.frontiers.services.OrganizationMemberService;
import edu.ucsb.cs156.frontiers.services.UpdateUserService;
import edu.ucsb.cs156.frontiers.services.jobs.JobService;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@WebMvcTest(controllers = {RosterStudentsController.class, RosterStudentsCSVController.class})
public class RosterStudentsCSVControllerTests extends ControllerTestCase {

  @MockitoBean private CourseRepository courseRepository;

  @MockitoBean private RosterStudentRepository rosterStudentRepository;

  @Autowired private CurrentUserService currentUserService;

  @MockitoBean private UpdateUserService updateUserService;

  @MockitoBean private OrganizationMemberService organizationMemberService;

  @MockitoBean private JobService service;

  @Autowired private ObjectMapper objectMapper;

  /** Test whether instructor can upload students */
  private final String sampleCSVContentsUCSB =
      """
            Enrl Cd,Perm #,Grade,Final Units,Student Last,Student First Middle,Quarter,Course ID,Section,Meeting Time(s) / Location(s),Email,ClassLevel,Major1,Major2,Date/Time,Pronoun

            08235,A123456,,4.0,GAUCHO,CHRIS FAKE,F23,CMPSC156,0100,T R   2:00- 3:15 SH 1431     W    5:00- 5:50 PHELP 3525  W    6:00- 6:50 PHELP 3525  W    7:00- 7:50 PHELP 3525  ,cgaucho@ucsb.edu,SR,CMPSC,,9/27/2023 9:39:25 AM,
            08250,A987654,,4.0,DEL PLAYA,LAUREN,F23,CMPSC156,0100,T R   2:00- 3:15 SH 1431     W    5:00- 5:50 PHELP 3525  W    6:00- 6:50 PHELP 3525  W    7:00- 7:50 PHELP 3525  ,ldelplaya@umail.ucsb.edu,SR,CMPSC,,9/27/2023 9:39:25 AM,She (She/Her/Hers)
            08243,1234567,,4.0,TARDE,SABADO,F23,CMPSC156,0100,T R   2:00- 3:15 SH 1431     W    5:00- 5:50 PHELP 3525  W    6:00- 6:50 PHELP 3525  W    7:00- 7:50 PHELP 3525  ,sabadotarde@umail.ucsb.edu,SR,CMPSC,,9/27/2023 9:39:25 AM,He (He/Him/His)
            """;

  private final String sampleCSVContentsChico =
      """
            Student Name,Student ID,Student SIS ID,Email,Section Name
            Marge Simpson,88200,013228559,msimpson@csuchico.edu,CSED 500 - 362 Computational Thinking Summer 2025
            Homer Simpson,88001,013205354,hsimpson@csuchico.edu,CSED 500 - 362 Computational Thinking Summer 2025
            Ralph Wiggum,88003,013251642,rwiggum@csuchico.edu,CSED 500 - 362 Computational Thinking Summer 2025
            """;

  private final String sampleCSVContentsOregonState =
      """
            Full name,Sortable name,Canvas user id,Overall course grade,Assignment on time percent,Last page view time,Last participation time,Last logged out,Email,SIS Id
            Tom Smith,"Smith, Tom",6056208,96.25,80.4,2-Jul-25,11-Jun-25,21-May-25,tomsmith@oregonstate.edu,931551625
            Martha Washington,"Washington, Martha",9876543,100,100,8-Aug-25,12-Dec-25,5-May-25,martha@oregonstate.edu,123456789
            John Doe,"Doe, John",1234567,88.5,75.0,15-Jul-25,10-Jun-25,5-May-25,johndoe@oregonstate.edu,987654321
            Alice Johnson,"Johnson, Alice",7654321,92.0,85.5,20-Jun-25,18-Jun-25,10-Jun-25,alicejohnson@oregonstate.edu,192837465
            Bob Lee,"Lee, Bob",2468135,78.75,60.0,5-May-25,2-May-25,1-May-25,boblee@oregonstate.edu,564738291
            """;

  private final String sampleUnknownCSVFormat =
      """
            Name,ID,SIS ID,University Email,Invalid Column Name
            Marge Simpson,88200,013228559,msimpson@csuchico.edu,CSED 500 - 362 Computational Thinking Summer 2025
            """;
  @Autowired private JobService jobService;

  @Test
  @WithInstructorCoursePermissions
  public void instructor_can_upload_students_for_an_existing_course_chico() throws Exception {

    // arrange

    Course course1 =
        Course.builder()
            .id(1L)
            .courseName("CSED 500")
            .orgName("csed-500-s25")
            .term("S25")
            .school("CSU Chico")
            .build();

    RosterStudent rs1BeforeWithId =
        RosterStudent.builder()
            .id(1L)
            .firstName("MARGE")
            .lastName("SIMPSON")
            .studentId("013228559")
            .email("msimpson@csuchico.edu")
            .course(course1)
            .rosterStatus(RosterStatus.MANUAL)
            .orgStatus(OrgStatus.PENDING)
            .build();

    RosterStudent rs1AfterWithId =
        RosterStudent.builder()
            .id(1L)
            .firstName("Marge")
            .lastName("Simpson")
            .studentId("013228559")
            .email("msimpson@csuchico.edu")
            .course(course1)
            .rosterStatus(RosterStatus.ROSTER)
            .orgStatus(OrgStatus.PENDING)
            .build();

    RosterStudent rs2BeforeWithId =
        RosterStudent.builder()
            .id(2L)
            .firstName("Homer")
            .lastName("Simpson")
            .studentId("013205354")
            .email("hsimpson@csuchico.edu")
            .course(course1)
            .rosterStatus(RosterStatus.ROSTER)
            .orgStatus(OrgStatus.PENDING)
            .build();

    RosterStudent rs2AfterWithId =
        RosterStudent.builder()
            .id(2L)
            .course(course1)
            .firstName("Homer")
            .lastName("Simpson")
            .email("hsimpson@csuchico.edu")
            .studentId("013205354")
            .rosterStatus(RosterStatus.ROSTER)
            .orgStatus(OrgStatus.PENDING)
            .build();

    RosterStudent rs3NoId =
        RosterStudent.builder()
            .course(course1)
            .firstName("Ralph")
            .lastName("Wiggum")
            .email("rwiggum@csuchico.edu")
            .studentId("013251642")
            .rosterStatus(RosterStatus.ROSTER)
            .orgStatus(OrgStatus.PENDING)
            .build();

    RosterStudent rs3WithId =
        RosterStudent.builder()
            .id(3L)
            .course(course1)
            .firstName("Ralph")
            .lastName("Wiggum")
            .email("rwiggum@csuchico.edu")
            .studentId("013251642")
            .rosterStatus(RosterStatus.ROSTER)
            .orgStatus(OrgStatus.PENDING)
            .build();

    course1.setRosterStudents(new ArrayList<>(List.of(rs1BeforeWithId, rs2BeforeWithId)));

    MockMultipartFile file =
        new MockMultipartFile(
            "file", "roster.csv", MediaType.TEXT_PLAIN_VALUE, sampleCSVContentsChico.getBytes());

    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(course1));

    when(rosterStudentRepository.saveAll(List.of(rs1AfterWithId, rs2AfterWithId, rs3NoId)))
        .thenReturn(List.of(rs1AfterWithId, rs2AfterWithId, rs3WithId));

    // act

    MvcResult response =
        mockMvc
            .perform(
                multipart("/api/rosterstudents/upload/csv")
                    .file(file)
                    .param("courseId", "1")
                    .with(csrf()))
            .andExpect(status().isOk())
            .andReturn();

    // assert

    verify(courseRepository, atLeastOnce()).findById(eq(1L));
    verify(rosterStudentRepository, times(1))
        .saveAll(new ArrayList<>(List.of(rs1AfterWithId, rs2AfterWithId, rs3NoId)));
    verify(updateUserService, times(1))
        .attachUsersToRosterStudents(List.of(rs1AfterWithId, rs2AfterWithId, rs3NoId));

    String responseString = response.getResponse().getContentAsString();
    LoadResult expectedResult = new LoadResult(1, 2, 0, List.of());
    String expectedJson = mapper.writeValueAsString(expectedResult);
    assertEquals(expectedJson, responseString);
  }

  @Test
  @WithInstructorCoursePermissions
  public void students_with_non_matching_student_id_and_email_are_rejected() throws Exception {

    RosterStudent student1ID = RosterStudent.builder().id(1L).studentId("A123456").build();
    RosterStudent student1Email = RosterStudent.builder().id(2L).email("cgaucho@ucsb.edu").build();
    RosterStudent student2 =
        RosterStudent.builder().id(3L).studentId("A987654").email("ldelplaya@ucsb.edu").build();
    Course course1 =
        Course.builder()
            .id(1L)
            .rosterStudents(new ArrayList<>(List.of(student1ID, student1Email, student2)))
            .build();
    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(course1));
    MockMultipartFile file =
        new MockMultipartFile(
            "file", "roster.csv", MediaType.TEXT_PLAIN_VALUE, sampleCSVContentsUCSB.getBytes());
    MvcResult response =
        mockMvc
            .perform(
                multipart("/api/rosterstudents/upload/csv")
                    .file(file)
                    .param("courseId", "1")
                    .with(csrf()))
            .andExpect(status().isConflict())
            .andReturn();

    RosterStudent rosterStudentRejected =
        RosterStudent.builder()
            .firstName("CHRIS FAKE")
            .lastName("GAUCHO")
            .studentId("A123456")
            .email("cgaucho@ucsb.edu")
            .section("08235")
            .build();
    RosterStudent rosterStudent2Updated =
        RosterStudent.builder()
            .id(3L)
            .firstName("LAUREN")
            .lastName("DEL PLAYA")
            .email("ldelplaya@ucsb.edu")
            .studentId("A987654")
            .section("08250")
            .rosterStatus(RosterStatus.ROSTER)
            .build();

    RosterStudent rosterStudent3Updated =
        RosterStudent.builder()
            .id(4L)
            .firstName("SABADO")
            .lastName("TARDE")
            .email("sabadotarde@ucsb.edu")
            .studentId("1234567")
            .section("08243")
            .rosterStatus(RosterStatus.ROSTER)
            .build();

    verify(rosterStudentRepository, never()).saveAll(List.of());
    String responseString = response.getResponse().getContentAsString();
    LoadResult expectedResult = new LoadResult(0, 0, 0, List.of(rosterStudentRejected));
    String expectedJson = mapper.writeValueAsString(expectedResult);
    assertEquals(expectedJson, responseString);
  }

  @Test
  @WithInstructorCoursePermissions
  public void drops_handled_correctly() throws Exception {
    RosterStudent noUpdateNoDrop =
        RosterStudent.builder()
            .id(5L)
            .studentId("A123489138507")
            .email("stayincourse")
            .rosterStatus(RosterStatus.MANUAL)
            .build();

    RosterStudent droppedStudent =
        RosterStudent.builder()
            .id(4L)
            .studentId("3323748")
            .email("dropped@ucsb.edu")
            .rosterStatus(RosterStatus.ROSTER)
            .build();

    RosterStudent droppedStudentUpdated =
        RosterStudent.builder()
            .id(4L)
            .studentId("3323748")
            .email("dropped@ucsb.edu")
            .rosterStatus(RosterStatus.DROPPED)
            .build();

    Course course =
        Course.builder()
            .id(1L)
            .rosterStudents(new ArrayList<>(List.of(noUpdateNoDrop, droppedStudent)))
            .build();

    when(rosterStudentRepository.saveAll(any()))
        .thenReturn(List.of(droppedStudentUpdated, noUpdateNoDrop));

    ArgumentCaptor<List<RosterStudent>> rosterStudentCaptor = ArgumentCaptor.forClass(List.class);

    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(course));
    MockMultipartFile file =
        new MockMultipartFile(
            "file", "roster.csv", MediaType.TEXT_PLAIN_VALUE, sampleCSVContentsUCSB.getBytes());

    MvcResult response =
        mockMvc
            .perform(
                multipart("/api/rosterstudents/upload/csv")
                    .file(file)
                    .param("courseId", "1")
                    .with(csrf()))
            .andExpect(status().isOk())
            .andReturn();

    verify(rosterStudentRepository, times(1)).saveAll(rosterStudentCaptor.capture());
    verify(jobService, times(1)).runAsJob(any(RemoveStudentsJob.class));

    System.out.println("rosterStudentCaptor.getValue() = " + rosterStudentCaptor.getValue());
    assertTrue(rosterStudentCaptor.getValue().contains(droppedStudentUpdated));
    assertTrue(rosterStudentCaptor.getValue().contains(noUpdateNoDrop));
    String responseString = response.getResponse().getContentAsString();
    LoadResult expectedResult = new LoadResult(3, 0, 1, List.of());
    String expectedJson = mapper.writeValueAsString(expectedResult);
    assertEquals(expectedJson, responseString);
  }

  @WithInstructorCoursePermissions
  @Test
  public void unrecognized_csv_format_throws_an_exception() throws Exception {

    Course course1 =
        Course.builder()
            .id(1L)
            .courseName("OSU 101")
            .orgName("osu-101-s25")
            .term("S25")
            .school("Oregon State")
            .rosterStudents(List.of())
            .build();
    MockMultipartFile file =
        new MockMultipartFile(
            "file", "roster.csv", MediaType.TEXT_PLAIN_VALUE, sampleUnknownCSVFormat.getBytes());

    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(course1));

    // act

    MvcResult response =
        mockMvc
            .perform(
                multipart("/api/rosterstudents/upload/csv")
                    .file(file)
                    .param("courseId", "1")
                    .with(csrf()))
            .andExpect(status().is4xxClientError())
            .andReturn();

    String responseString = response.getResponse().getErrorMessage();
    assertEquals("Unknown Roster Source Type", responseString);
  }

  /** Test that you cannot upload a roster for a course that does not exist */
  @WithMockUser(roles = {"ADMIN"})
  @Test
  public void instructor_cannot_upload_students_for_a_course_that_does_not_exist()
      throws Exception {

    // arrange

    MockMultipartFile file =
        new MockMultipartFile(
            "file", "roster.csv", MediaType.TEXT_PLAIN_VALUE, sampleCSVContentsUCSB.getBytes());

    when(courseRepository.findById(eq(1L))).thenReturn(Optional.empty());

    // act

    MvcResult response =
        mockMvc
            .perform(
                multipart("/api/rosterstudents/upload/csv")
                    .file(file)
                    .param("courseId", "1")
                    .with(csrf()))
            .andExpect(status().isNotFound())
            .andReturn();

    // assert

    verify(courseRepository, atLeastOnce()).findById(eq(1L));
    String responseString = response.getResponse().getContentAsString();
    Map<String, String> expectedMap =
        Map.of(
            "type", "EntityNotFoundException",
            "message", "Course with id 1 not found");
    String expectedJson = mapper.writeValueAsString(expectedMap);
    assertEquals(expectedJson, responseString);
  }

  @Test
  public void test_getLastName() {
    assertEquals("Gaucho", RosterStudentsCSVController.getLastName("Chris Gaucho"));
    assertEquals("Milnes", RosterStudentsCSVController.getLastName("Christopher Robin Milnes"));
    assertEquals("Cher", RosterStudentsCSVController.getLastName("Cher"));
  }

  @Test
  public void test_getFirstname() {
    assertEquals("Chris", RosterStudentsCSVController.getFirstName("Chris Gaucho"));
    assertEquals(
        "Christopher Robin", RosterStudentsCSVController.getFirstName("Christopher Robin Milnes"));
    assertEquals("", RosterStudentsCSVController.getFirstName("Cher"));
  }

  @Test
  public void test_getRosterSourceType() {
    // Test with UCSB Egrades header
    String[] ucsbEgradesHeader = RosterStudentsCSVController.UCSB_EGRADES_HEADERS.split(",");
    assertEquals(
        RosterStudentsCSVController.RosterSourceType.UCSB_EGRADES,
        RosterStudentsCSVController.getRosterSourceType(ucsbEgradesHeader));

    // Test with Chico Canvas header
    String[] chicoCanvasHeader = RosterStudentsCSVController.CHICO_CANVAS_HEADERS.split(",");
    assertEquals(
        RosterStudentsCSVController.RosterSourceType.CHICO_CANVAS,
        RosterStudentsCSVController.getRosterSourceType(chicoCanvasHeader));

    // Test with Oregon State header
    String[] oregonStateHeader = RosterStudentsCSVController.OREGON_STATE_HEADERS.split(",");
    assertEquals(
        RosterStudentsCSVController.RosterSourceType.OREGON_STATE,
        RosterStudentsCSVController.getRosterSourceType(oregonStateHeader));

    // Test with unknown header
    String[] unknownHeader = {"Unknown Header 1", "Unknown Header 2"};
    assertEquals(
        RosterStudentsCSVController.RosterSourceType.UNKNOWN,
        RosterStudentsCSVController.getRosterSourceType(unknownHeader));
  }

  @Test
  public void test_fromCSVRow_UnrecognizedSourceType() {
    assertThrows(
        ResponseStatusException.class,
        () -> {
          String[] row = {"Unknown Header 1", "Unknown Header 2"};
          RosterStudentsCSVController.fromCSVRow(
              row, RosterStudentsCSVController.RosterSourceType.UNKNOWN);
        });
  }

  @Test
  public void test_fromCSVRowOregonState() {
    String row[] = {
      "Martha Washington",
      "Washington, Martha",
      "9876543",
      "100",
      "100",
      "8-Aug-25",
      "12-Dec-25",
      "5-May-25",
      "martha@oregonstate.edu",
      "123456789"
    };

    RosterStudent rs =
        RosterStudentsCSVController.fromCSVRow(
            row, RosterStudentsCSVController.RosterSourceType.OREGON_STATE);

    assertEquals("Martha", rs.getFirstName());
    assertEquals("Washington", rs.getLastName());
    assertEquals("123456789", rs.getStudentId());
    assertEquals("martha@oregonstate.edu", rs.getEmail());
  }

  @Test
  public void test_fromCSVRowOregonState_noComma() {
    String row[] = {
      "Sting",
      "Sting",
      "1234567",
      "100",
      "100",
      "8-Aug-25",
      "12-Dec-25",
      "5-May-25",
      "sting@oregonstate.edu",
      "999999999"
    };

    RosterStudent rs =
        RosterStudentsCSVController.fromCSVRow(
            row, RosterStudentsCSVController.RosterSourceType.OREGON_STATE);

    assertEquals("", rs.getFirstName());
    assertEquals("Sting", rs.getLastName());
    assertEquals("999999999", rs.getStudentId());
    assertEquals("sting@oregonstate.edu", rs.getEmail());
  }

  @Test
  public void test_checkRowLength_throwsException() {
    String row[] = {"a", "b", "c"};
    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class,
            () ->
                RosterStudentsCSVController.checkRowLength(
                    row, 5, RosterStudentsCSVController.RosterSourceType.CHICO_CANVAS));
    assertEquals(org.springframework.http.HttpStatus.BAD_REQUEST, ex.getStatusCode());
    assertEquals(
        "CHICO_CANVAS CSV row does not have enough columns. Length = 3 Row content = [[a, b, c]]",
        ex.getReason());
  }

  @Test
  public void test_fromCSVRowOregonState_notEnoughColumns() {
    String row[] = {
      "Sting",
      "Sting",
      "1234567",
      "100",
      "100",
      "8-Aug-25",
      "12-Dec-25",
      "5-May-25",
      "sting@oregonstate.edu"
      // missing SIS Id column
    };

    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class,
            () ->
                RosterStudentsCSVController.fromCSVRow(
                    row, RosterStudentsCSVController.RosterSourceType.OREGON_STATE));
    assertEquals(org.springframework.http.HttpStatus.BAD_REQUEST, ex.getStatusCode());
    assertEquals(
        "OREGON_STATE CSV row does not have enough columns. Length = 9 Row content = [[Sting, Sting, 1234567, 100, 100, 8-Aug-25, 12-Dec-25, 5-May-25, sting@oregonstate.edu]]",
        ex.getReason());
  }

  @Test
  public void test_fromCSVRowChico_notEnoughColumns() {
    String row[] = {
      "Marge Simpson", "88200", "013228559",
    };

    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class,
            () ->
                RosterStudentsCSVController.fromCSVRow(
                    row, RosterStudentsCSVController.RosterSourceType.CHICO_CANVAS));
    assertEquals(org.springframework.http.HttpStatus.BAD_REQUEST, ex.getStatusCode());
    assertEquals(
        "CHICO_CANVAS CSV row does not have enough columns. Length = 3 Row content = [[Marge Simpson, 88200, 013228559]]",
        ex.getReason());
  }

  @Test
  public void test_fromCSVRowUCSB_notEnoughColumns() {
    String row[] = {"08235", "A123456", "", "4.0", "GAUCHO", "CHRIS FAKE", "F23", "CMPSC156"
      // missing rest of columns
    };

    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class,
            () ->
                RosterStudentsCSVController.fromCSVRow(
                    row, RosterStudentsCSVController.RosterSourceType.UCSB_EGRADES));
    assertEquals(org.springframework.http.HttpStatus.BAD_REQUEST, ex.getStatusCode());
    assertEquals(
        "UCSB_EGRADES CSV row does not have enough columns. Length = 8 Row content = [[08235, A123456, , 4.0, GAUCHO, CHRIS FAKE, F23, CMPSC156]]",
        ex.getReason());
  }

  @Test
  public void test_fromCSVRowUCSB_sectionField() {
    String row[] = {
      "08235", // Enrl Cd - position 0 (should become section)
      "A123456", // Perm #
      "", // Grade
      "4.0", // Final Units
      "GAUCHO", // Student Last
      "CHRIS FAKE", // Student First Middle
      "F23", // Quarter
      "CMPSC156", // Course ID
      "0100", // Section
      "T R 2:00- 3:15 SH 1431", // Meeting Time(s) / Location(s)
      "cgaucho@ucsb.edu", // Email
      "SR", // ClassLevel
      "CMPSC", // Major1
      "", // Major2
      "9/27/2023 9:39:25 AM", // Date/Time
      "" // Pronoun
    };

    RosterStudent rs =
        RosterStudentsCSVController.fromCSVRow(
            row, RosterStudentsCSVController.RosterSourceType.UCSB_EGRADES);

    assertEquals("CHRIS FAKE", rs.getFirstName());
    assertEquals("GAUCHO", rs.getLastName());
    assertEquals("A123456", rs.getStudentId());
    assertEquals("cgaucho@ucsb.edu", rs.getEmail());
    assertEquals(
        "08235", rs.getSection()); // This is the key test - section should be Enrl Cd (position 0)
  }

  @Test
  public void test_fromCSVRowChico_sectionField() {
    String row[] = {
      "Marge Simpson", // Student Name
      "88200", // Student ID
      "013228559", // Student SIS ID
      "msimpson@csuchico.edu" // Email
    };

    RosterStudent rs =
        RosterStudentsCSVController.fromCSVRow(
            row, RosterStudentsCSVController.RosterSourceType.CHICO_CANVAS);

    assertEquals("Marge", rs.getFirstName());
    assertEquals("Simpson", rs.getLastName());
    assertEquals("013228559", rs.getStudentId());
    assertEquals("msimpson@csuchico.edu", rs.getEmail());
    assertEquals("", rs.getSection()); // This is the key test - section should be empty string
  }

  @Test
  public void test_fromCSVRowOregonState_sectionField() {
    String row[] = {
      "Martha Washington", // Full name
      "Washington, Martha", // Sortable name
      "9876543", // Canvas user id
      "100", // Overall course grade
      "100", // Assignment on time percent
      "8-Aug-25", // Last page view time
      "12-Dec-25", // Last participation time
      "5-May-25", // Last logged out
      "martha@oregonstate.edu", // Email
      "123456789" // SIS Id
    };

    RosterStudent rs =
        RosterStudentsCSVController.fromCSVRow(
            row, RosterStudentsCSVController.RosterSourceType.OREGON_STATE);

    assertEquals("Martha", rs.getFirstName());
    assertEquals("Washington", rs.getLastName());
    assertEquals("123456789", rs.getStudentId());
    assertEquals("martha@oregonstate.edu", rs.getEmail());
    assertEquals("", rs.getSection()); // This is the key test - section should be empty string
  }
}
