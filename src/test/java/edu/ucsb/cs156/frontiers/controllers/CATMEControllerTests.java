package edu.ucsb.cs156.frontiers.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.ucsb.cs156.frontiers.ControllerTestCase;
import edu.ucsb.cs156.frontiers.annotations.WithInstructorCoursePermissions;
import edu.ucsb.cs156.frontiers.entities.CourseOption;
import edu.ucsb.cs156.frontiers.entities.RosterStudent;
import edu.ucsb.cs156.frontiers.entities.Section;
import edu.ucsb.cs156.frontiers.enums.RosterStatus;
import edu.ucsb.cs156.frontiers.models.CATMEAuditResult;
import edu.ucsb.cs156.frontiers.repositories.CourseOptionRepository;
import edu.ucsb.cs156.frontiers.repositories.RosterStudentRepository;
import edu.ucsb.cs156.frontiers.repositories.SectionRepository;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MvcResult;

@WebMvcTest(controllers = CATMEController.class)
public class CATMEControllerTests extends ControllerTestCase {

  @MockitoBean private RosterStudentRepository rosterStudentRepository;

  @MockitoBean private CourseOptionRepository courseOptionRepository;

  @MockitoBean private SectionRepository sectionRepository;

  @Autowired private ObjectMapper objectMapper;

  private static final String CATME_AUDIT_HEADER =
      "Activity,Class,Term,Format,Instr,School\n"
          + "CMPSC 156,001,F25,In Person,Instructor,UCSB\n"
          + "\n"
          + "\"Name\",\"Student ID\",\"Email\",\"Section\",\"Team Name\",\"Platform\",\"Sex\",\"Java"
          + " Knowledge\",\"React Experience\",\n";

  @Test
  @WithInstructorCoursePermissions
  public void catmeAudit_returnsErrorForUnrecognizedFormat() throws Exception {
    MockMultipartFile file =
        new MockMultipartFile(
            "file",
            "catme.csv",
            "text/csv",
            "Not,The,Right,Header\n".getBytes(StandardCharsets.UTF_8));

    MvcResult response =
        mockMvc
            .perform(multipart("/api/catme/audit").file(file).with(csrf()).param("courseId", "1"))
            .andExpect(status().isUnprocessableEntity())
            .andReturn();

    Map<String, Object> body =
        objectMapper.readValue(response.getResponse().getContentAsString(), Map.class);
    assertEquals(
        "The uploaded file was not in a recognized CATME TeamMaker CSV format.",
        body.get("message"));
  }

  @Test
  @WithInstructorCoursePermissions
  public void catmeAudit_tooFewLines_returnsError() throws Exception {
    MockMultipartFile file =
        new MockMultipartFile(
            "file",
            "catme.csv",
            "text/csv",
            "Activity,Class,Term,Format,Instr,School\n".getBytes(StandardCharsets.UTF_8));

    mockMvc
        .perform(multipart("/api/catme/audit").file(file).with(csrf()).param("courseId", "1"))
        .andExpect(status().isUnprocessableEntity());
  }

  @Test
  @WithInstructorCoursePermissions
  public void catmeAudit_exactlyFourHeaderLines_returnsEmptyResult() throws Exception {
    when(rosterStudentRepository.findByCourseId(eq(1L))).thenReturn(List.of());

    MockMultipartFile file =
        new MockMultipartFile(
            "file", "catme.csv", "text/csv", CATME_AUDIT_HEADER.getBytes(StandardCharsets.UTF_8));

    MvcResult response =
        mockMvc
            .perform(multipart("/api/catme/audit").file(file).with(csrf()).param("courseId", "1"))
            .andExpect(status().isOk())
            .andReturn();

    CATMEAuditResult result =
        objectMapper.readValue(response.getResponse().getContentAsString(), CATMEAuditResult.class);
    assertEquals(0, result.studentsToUpdate().size());
    assertEquals(0, result.studentsToDrop().size());
  }

  @Test
  @WithInstructorCoursePermissions
  public void catmeAudit_rowWithExactlyFourColumns_isValid() throws Exception {
    RosterStudent student =
        RosterStudent.builder()
            .studentId("1234567")
            .firstName("Chris")
            .lastName("Gaucho")
            .email("cgaucho@ucsb.edu")
            .section("0100")
            .rosterStatus(RosterStatus.MANUAL)
            .build();
    when(rosterStudentRepository.findByCourseId(eq(1L))).thenReturn(List.of(student));

    String content =
        CATME_AUDIT_HEADER + "\"Gaucho, Chris\",\"1234567\",\"cgaucho@ucsb.edu\",\"0100\"\n";
    MockMultipartFile file =
        new MockMultipartFile(
            "file", "catme.csv", "text/csv", content.getBytes(StandardCharsets.UTF_8));

    MvcResult response =
        mockMvc
            .perform(multipart("/api/catme/audit").file(file).with(csrf()).param("courseId", "1"))
            .andExpect(status().isOk())
            .andReturn();

    CATMEAuditResult result =
        objectMapper.readValue(response.getResponse().getContentAsString(), CATMEAuditResult.class);
    assertEquals(0, result.studentsToUpdate().size());
    assertEquals(0, result.studentsToDrop().size());
  }

  @Test
  @WithInstructorCoursePermissions
  public void catmeAudit_headerWithDifferentTrailingColumns_isValid() throws Exception {
    RosterStudent student =
        RosterStudent.builder()
            .studentId("1234567")
            .firstName("Chris")
            .lastName("Gaucho")
            .email("cgaucho@ucsb.edu")
            .section("0100")
            .rosterStatus(RosterStatus.MANUAL)
            .build();
    when(rosterStudentRepository.findByCourseId(eq(1L))).thenReturn(List.of(student));

    String content =
        "Activity,Class,Term,Format,Instr,School\n"
            + "CMPSC 156,001,F25,In Person,Instructor,UCSB\n"
            + "\n"
            + "\"Name\",\"Student ID\",\"Email\",\"Section\",\"Team Name\",\"Platform\",\"Sex\",\"Java"
            + " Knowledge\",\"React Experience\",\"Tot (Max 14)\",\n"
            + "\"Gaucho, Chris\",\"1234567\",\"cgaucho@ucsb.edu\",\"0100\"\n";
    MockMultipartFile file =
        new MockMultipartFile(
            "file", "catme.csv", "text/csv", content.getBytes(StandardCharsets.UTF_8));

    MvcResult response =
        mockMvc
            .perform(multipart("/api/catme/audit").file(file).with(csrf()).param("courseId", "1"))
            .andExpect(status().isOk())
            .andReturn();

    CATMEAuditResult result =
        objectMapper.readValue(response.getResponse().getContentAsString(), CATMEAuditResult.class);
    assertEquals(0, result.studentsToUpdate().size());
    assertEquals(0, result.studentsToDrop().size());
  }

  @Test
  @WithInstructorCoursePermissions
  public void catmeAudit_duplicateCsvRows_keepsFirstRowValues() throws Exception {
    RosterStudent student =
        RosterStudent.builder()
            .studentId("1234567")
            .firstName("Chris")
            .lastName("Gaucho")
            .email("cgaucho@ucsb.edu")
            .section("0100")
            .rosterStatus(RosterStatus.MANUAL)
            .build();
    when(rosterStudentRepository.findByCourseId(eq(1L))).thenReturn(List.of(student));

    String content =
        CATME_AUDIT_HEADER
            + "\"Gaucho,"
            + " Chris\",\"1234567\",\"cgaucho@ucsb.edu\",\"0200\",\"Web\",\"M\",\"None\",\"None\",\n"
            + "\"Gaucho,"
            + " Chris\",\"1234567\",\"cgaucho@ucsb.edu\",\"0100\",\"Web\",\"M\",\"None\",\"None\",\n";
    MockMultipartFile file =
        new MockMultipartFile(
            "file", "catme.csv", "text/csv", content.getBytes(StandardCharsets.UTF_8));

    MvcResult response =
        mockMvc
            .perform(multipart("/api/catme/audit").file(file).with(csrf()).param("courseId", "1"))
            .andExpect(status().isOk())
            .andReturn();

    CATMEAuditResult result =
        objectMapper.readValue(response.getResponse().getContentAsString(), CATMEAuditResult.class);
    assertEquals(1, result.studentsToUpdate().size());
    assertEquals("Section", result.studentsToUpdate().get(0).field());
    assertEquals("0200", result.studentsToUpdate().get(0).oldValue());
    assertEquals("0100", result.studentsToUpdate().get(0).newValue());
  }

  @Test
  @WithInstructorCoursePermissions
  public void catmeAudit_wrongFirstHeaderLine_returnsError() throws Exception {
    String content =
        "Wrong,Header,Line\n"
            + "CMPSC 156,001,F25,In Person,Instructor,UCSB\n"
            + "\n"
            + "\"Name\",\"Student ID\",\"Email\",\"Section\",\"Platform\",\"Sex\",\"Java"
            + " Knowledge\",\"React Experience\",\n";
    MockMultipartFile file =
        new MockMultipartFile(
            "file", "catme.csv", "text/csv", content.getBytes(StandardCharsets.UTF_8));

    mockMvc
        .perform(multipart("/api/catme/audit").file(file).with(csrf()).param("courseId", "1"))
        .andExpect(status().isUnprocessableEntity());
  }

  @Test
  @WithInstructorCoursePermissions
  public void catmeAudit_wrongFourthHeaderLine_returnsError() throws Exception {
    String content =
        "Activity,Class,Term,Format,Instr,School\n"
            + "CMPSC 156,001,F25,In Person,Instructor,UCSB\n"
            + "\n"
            + "\"Wrong\",\"Header\"\n";
    MockMultipartFile file =
        new MockMultipartFile(
            "file", "catme.csv", "text/csv", content.getBytes(StandardCharsets.UTF_8));

    mockMvc
        .perform(multipart("/api/catme/audit").file(file).with(csrf()).param("courseId", "1"))
        .andExpect(status().isUnprocessableEntity());
  }

  @Test
  @WithInstructorCoursePermissions
  public void catmeAudit_malformedQuotedRow_returnsError() throws Exception {
    String content = CATME_AUDIT_HEADER + "\"unterminated,field\n";
    MockMultipartFile file =
        new MockMultipartFile(
            "file", "catme.csv", "text/csv", content.getBytes(StandardCharsets.UTF_8));

    mockMvc
        .perform(multipart("/api/catme/audit").file(file).with(csrf()).param("courseId", "1"))
        .andExpect(status().isUnprocessableEntity());
  }

  @Test
  @WithInstructorCoursePermissions
  public void catmeAudit_blankLine3NotBlank_returnsError() throws Exception {
    String content =
        "Activity,Class,Term,Format,Instr,School\n"
            + "CMPSC 156,001,F25,In Person,Instructor,UCSB\n"
            + "NOT BLANK\n"
            + "\"Name\",\"Student ID\",\"Email\",\"Section\",\"Platform\",\"Sex\",\"Java"
            + " Knowledge\",\"React Experience\",\n";
    MockMultipartFile file =
        new MockMultipartFile(
            "file", "catme.csv", "text/csv", content.getBytes(StandardCharsets.UTF_8));

    mockMvc
        .perform(multipart("/api/catme/audit").file(file).with(csrf()).param("courseId", "1"))
        .andExpect(status().isUnprocessableEntity());
  }

  @Test
  @WithInstructorCoursePermissions
  public void catmeAudit_rowWithTooFewColumns_returnsError() throws Exception {
    String content = CATME_AUDIT_HEADER + "\"Only\",\"Two\"\n";
    MockMultipartFile file =
        new MockMultipartFile(
            "file", "catme.csv", "text/csv", content.getBytes(StandardCharsets.UTF_8));

    mockMvc
        .perform(multipart("/api/catme/audit").file(file).with(csrf()).param("courseId", "1"))
        .andExpect(status().isUnprocessableEntity());
  }

  @Test
  @WithInstructorCoursePermissions
  public void catmeAudit_matchingStudent_producesNoUpdatesOrDrops() throws Exception {
    RosterStudent student =
        RosterStudent.builder()
            .studentId("1234567")
            .firstName("Chris")
            .lastName("Gaucho")
            .email("cgaucho@ucsb.edu")
            .section("0100")
            .rosterStatus(RosterStatus.ROSTER)
            .build();
    when(rosterStudentRepository.findByCourseId(eq(1L))).thenReturn(List.of(student));

    String content =
        CATME_AUDIT_HEADER
            + "\"Gaucho,"
            + " Chris\",\"1234567\",\"cgaucho@ucsb.edu\",\"0100\",\"Web\",\"M\",\"None\",\"None\",\n";
    MockMultipartFile file =
        new MockMultipartFile(
            "file", "catme.csv", "text/csv", content.getBytes(StandardCharsets.UTF_8));

    MvcResult response =
        mockMvc
            .perform(multipart("/api/catme/audit").file(file).with(csrf()).param("courseId", "1"))
            .andExpect(status().isOk())
            .andReturn();

    CATMEAuditResult result =
        objectMapper.readValue(response.getResponse().getContentAsString(), CATMEAuditResult.class);
    assertEquals(0, result.studentsToUpdate().size());
    assertEquals(0, result.studentsToDrop().size());
  }

  @Test
  @WithInstructorCoursePermissions
  public void catmeAudit_mismatchedNameAndSection_producesUpdates() throws Exception {
    RosterStudent student =
        RosterStudent.builder()
            .studentId("1234567")
            .firstName("Chris")
            .lastName("Gaucho")
            .email("cgaucho@ucsb.edu")
            .section("0100")
            .rosterStatus(RosterStatus.MANUAL)
            .build();
    when(rosterStudentRepository.findByCourseId(eq(1L))).thenReturn(List.of(student));

    String content =
        CATME_AUDIT_HEADER
            + "\"Wrong"
            + " Name\",\"1234567\",\"cgaucho@ucsb.edu\",\"0200\",\"Web\",\"M\",\"None\",\"None\",\n";
    MockMultipartFile file =
        new MockMultipartFile(
            "file", "catme.csv", "text/csv", content.getBytes(StandardCharsets.UTF_8));

    MvcResult response =
        mockMvc
            .perform(multipart("/api/catme/audit").file(file).with(csrf()).param("courseId", "1"))
            .andExpect(status().isOk())
            .andReturn();

    CATMEAuditResult result =
        objectMapper.readValue(response.getResponse().getContentAsString(), CATMEAuditResult.class);
    assertEquals(2, result.studentsToUpdate().size());
    assertEquals(0, result.studentsToDrop().size());
    assertEquals("Name", result.studentsToUpdate().get(0).field());
    assertEquals("Wrong Name", result.studentsToUpdate().get(0).oldValue());
    assertEquals("Gaucho, Chris", result.studentsToUpdate().get(0).newValue());
    assertEquals("Section", result.studentsToUpdate().get(1).field());
    assertEquals("0200", result.studentsToUpdate().get(1).oldValue());
    assertEquals("0100", result.studentsToUpdate().get(1).newValue());
  }

  @Test
  @WithInstructorCoursePermissions
  public void catmeAudit_missingFirstOrLastName_producesNullSafeNameUpdates() throws Exception {
    RosterStudent missingFirstName =
        RosterStudent.builder()
            .studentId("1234567")
            .firstName(null)
            .lastName("Gaucho")
            .email("gaucho@ucsb.edu")
            .section("0100")
            .rosterStatus(RosterStatus.MANUAL)
            .build();
    RosterStudent missingLastName =
        RosterStudent.builder()
            .studentId("2345678")
            .firstName("Chris")
            .lastName(null)
            .email("chris@ucsb.edu")
            .section("0200")
            .rosterStatus(RosterStatus.MANUAL)
            .build();
    when(rosterStudentRepository.findByCourseId(eq(1L)))
        .thenReturn(List.of(missingFirstName, missingLastName));

    String content =
        CATME_AUDIT_HEADER
            + "\"Wrong One\",\"1234567\",\"gaucho@ucsb.edu\",\"0100\",\"Web\",\"M\",\"None\",\"None\",\n"
            + "\"Wrong Two\",\"2345678\",\"chris@ucsb.edu\",\"0200\",\"Web\",\"M\",\"None\",\"None\",\n";
    MockMultipartFile file =
        new MockMultipartFile(
            "file", "catme.csv", "text/csv", content.getBytes(StandardCharsets.UTF_8));

    MvcResult response =
        mockMvc
            .perform(multipart("/api/catme/audit").file(file).with(csrf()).param("courseId", "1"))
            .andExpect(status().isOk())
            .andReturn();

    CATMEAuditResult result =
        objectMapper.readValue(response.getResponse().getContentAsString(), CATMEAuditResult.class);
    assertEquals(2, result.studentsToUpdate().size());
    assertEquals("Name", result.studentsToUpdate().get(0).field());
    assertEquals("Wrong One", result.studentsToUpdate().get(0).oldValue());
    assertEquals("Gaucho", result.studentsToUpdate().get(0).newValue());
    assertEquals("Name", result.studentsToUpdate().get(1).field());
    assertEquals("Wrong Two", result.studentsToUpdate().get(1).oldValue());
    assertEquals("Chris", result.studentsToUpdate().get(1).newValue());
  }

  @Test
  @WithInstructorCoursePermissions
  public void catmeAudit_translateSectionsEnabled_matchingTranslatedSection_producesNoUpdate()
      throws Exception {
    RosterStudent student =
        RosterStudent.builder()
            .studentId("1234567")
            .firstName("Chris")
            .lastName("Gaucho")
            .email("cgaucho@ucsb.edu")
            .section("0100")
            .rosterStatus(RosterStatus.MANUAL)
            .build();
    when(rosterStudentRepository.findByCourseId(eq(1L))).thenReturn(List.of(student));
    when(courseOptionRepository.findByCourseIdAndOption(eq(1L), eq("TRANSLATE_SECTIONS")))
        .thenReturn(
            Optional.of(
                CourseOption.builder()
                    .courseId(1L)
                    .option("TRANSLATE_SECTIONS")
                    .enabled(true)
                    .build()));
    when(sectionRepository.findByCourseId(eq(1L)))
        .thenReturn(List.of(Section.builder().section("0100").label("5pm").build()));

    String content =
        CATME_AUDIT_HEADER
            + "\"Gaucho,"
            + " Chris\",\"1234567\",\"cgaucho@ucsb.edu\",\"5pm\",\"Web\",\"M\",\"None\",\"None\",\n";
    MockMultipartFile file =
        new MockMultipartFile(
            "file", "catme.csv", "text/csv", content.getBytes(StandardCharsets.UTF_8));

    MvcResult response =
        mockMvc
            .perform(multipart("/api/catme/audit").file(file).with(csrf()).param("courseId", "1"))
            .andExpect(status().isOk())
            .andReturn();

    CATMEAuditResult result =
        objectMapper.readValue(response.getResponse().getContentAsString(), CATMEAuditResult.class);
    assertEquals(0, result.studentsToUpdate().size());
    assertEquals(0, result.studentsToDrop().size());
  }

  @Test
  @WithInstructorCoursePermissions
  public void catmeAudit_translateSectionsEnabled_mismatchedSection_producesTranslatedUpdate()
      throws Exception {
    RosterStudent student =
        RosterStudent.builder()
            .studentId("1234567")
            .firstName("Chris")
            .lastName("Gaucho")
            .email("cgaucho@ucsb.edu")
            .section("0200")
            .rosterStatus(RosterStatus.MANUAL)
            .build();
    when(rosterStudentRepository.findByCourseId(eq(1L))).thenReturn(List.of(student));
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
                Section.builder().section("0100").label("5pm").build(),
                Section.builder().section("0200").label("6pm").build()));

    String content =
        CATME_AUDIT_HEADER
            + "\"Gaucho,"
            + " Chris\",\"1234567\",\"cgaucho@ucsb.edu\",\"5pm\",\"Web\",\"M\",\"None\",\"None\",\n";
    MockMultipartFile file =
        new MockMultipartFile(
            "file", "catme.csv", "text/csv", content.getBytes(StandardCharsets.UTF_8));

    MvcResult response =
        mockMvc
            .perform(multipart("/api/catme/audit").file(file).with(csrf()).param("courseId", "1"))
            .andExpect(status().isOk())
            .andReturn();

    CATMEAuditResult result =
        objectMapper.readValue(response.getResponse().getContentAsString(), CATMEAuditResult.class);
    assertEquals(1, result.studentsToUpdate().size());
    assertEquals("Section", result.studentsToUpdate().get(0).field());
    assertEquals("5pm", result.studentsToUpdate().get(0).oldValue());
    assertEquals("6pm", result.studentsToUpdate().get(0).newValue());
  }

  @Test
  @WithInstructorCoursePermissions
  public void catmeAudit_translateSectionsEnabled_unknownCatmeSection_producesTranslatedUpdate()
      throws Exception {
    RosterStudent student =
        RosterStudent.builder()
            .studentId("1234567")
            .firstName("Chris")
            .lastName("Gaucho")
            .email("cgaucho@ucsb.edu")
            .section("0100")
            .rosterStatus(RosterStatus.MANUAL)
            .build();
    when(rosterStudentRepository.findByCourseId(eq(1L))).thenReturn(List.of(student));
    when(courseOptionRepository.findByCourseIdAndOption(eq(1L), eq("TRANSLATE_SECTIONS")))
        .thenReturn(
            Optional.of(
                CourseOption.builder()
                    .courseId(1L)
                    .option("TRANSLATE_SECTIONS")
                    .enabled(true)
                    .build()));
    when(sectionRepository.findByCourseId(eq(1L)))
        .thenReturn(List.of(Section.builder().section("0100").label("5pm").build()));

    String content =
        CATME_AUDIT_HEADER
            + "\"Gaucho,"
            + " Chris\",\"1234567\",\"cgaucho@ucsb.edu\",\"unknown\",\"Web\",\"M\",\"None\",\"None\",\n";
    MockMultipartFile file =
        new MockMultipartFile(
            "file", "catme.csv", "text/csv", content.getBytes(StandardCharsets.UTF_8));

    MvcResult response =
        mockMvc
            .perform(multipart("/api/catme/audit").file(file).with(csrf()).param("courseId", "1"))
            .andExpect(status().isOk())
            .andReturn();

    CATMEAuditResult result =
        objectMapper.readValue(response.getResponse().getContentAsString(), CATMEAuditResult.class);
    assertEquals(1, result.studentsToUpdate().size());
    assertEquals("Section", result.studentsToUpdate().get(0).field());
    assertEquals("unknown", result.studentsToUpdate().get(0).oldValue());
    assertEquals("5pm", result.studentsToUpdate().get(0).newValue());
  }

  @Test
  @WithInstructorCoursePermissions
  public void catmeAudit_translateSectionsDisabled_ignoresSectionsTable() throws Exception {
    RosterStudent student =
        RosterStudent.builder()
            .studentId("1234567")
            .firstName("Chris")
            .lastName("Gaucho")
            .email("cgaucho@ucsb.edu")
            .section("0100")
            .rosterStatus(RosterStatus.MANUAL)
            .build();
    when(rosterStudentRepository.findByCourseId(eq(1L))).thenReturn(List.of(student));
    when(courseOptionRepository.findByCourseIdAndOption(eq(1L), eq("TRANSLATE_SECTIONS")))
        .thenReturn(
            Optional.of(
                CourseOption.builder()
                    .courseId(1L)
                    .option("TRANSLATE_SECTIONS")
                    .enabled(false)
                    .build()));

    String content =
        CATME_AUDIT_HEADER
            + "\"Gaucho,"
            + " Chris\",\"1234567\",\"cgaucho@ucsb.edu\",\"0100\",\"Web\",\"M\",\"None\",\"None\",\n";
    MockMultipartFile file =
        new MockMultipartFile(
            "file", "catme.csv", "text/csv", content.getBytes(StandardCharsets.UTF_8));

    MvcResult response =
        mockMvc
            .perform(multipart("/api/catme/audit").file(file).with(csrf()).param("courseId", "1"))
            .andExpect(status().isOk())
            .andReturn();

    CATMEAuditResult result =
        objectMapper.readValue(response.getResponse().getContentAsString(), CATMEAuditResult.class);
    assertEquals(0, result.studentsToUpdate().size());
    verify(sectionRepository, never()).findByCourseId(any());
  }

  @Test
  @WithInstructorCoursePermissions
  public void catmeAudit_studentWithNullStudentIdIsIgnored() throws Exception {
    RosterStudent studentWithNullId =
        RosterStudent.builder()
            .studentId(null)
            .firstName("Chris")
            .lastName("Gaucho")
            .email("cgaucho@ucsb.edu")
            .section("0100")
            .rosterStatus(RosterStatus.MANUAL)
            .build();
    when(rosterStudentRepository.findByCourseId(eq(1L))).thenReturn(List.of(studentWithNullId));

    String content =
        CATME_AUDIT_HEADER
            + "\"Gaucho,"
            + " Chris\",\"1234567\",\"cgaucho@ucsb.edu\",\"0100\",\"Web\",\"M\",\"None\",\"None\",\n";
    MockMultipartFile file =
        new MockMultipartFile(
            "file", "catme.csv", "text/csv", content.getBytes(StandardCharsets.UTF_8));

    MvcResult response =
        mockMvc
            .perform(multipart("/api/catme/audit").file(file).with(csrf()).param("courseId", "1"))
            .andExpect(status().isOk())
            .andReturn();

    CATMEAuditResult result =
        objectMapper.readValue(response.getResponse().getContentAsString(), CATMEAuditResult.class);
    assertEquals(0, result.studentsToUpdate().size());
    assertEquals(1, result.studentsToDrop().size());
  }

  @Test
  @WithInstructorCoursePermissions
  public void catmeAudit_studentWithNullSection_producesSectionUpdate() throws Exception {
    RosterStudent student =
        RosterStudent.builder()
            .studentId("1234567")
            .firstName("Chris")
            .lastName("Gaucho")
            .email("cgaucho@ucsb.edu")
            .section(null)
            .rosterStatus(RosterStatus.MANUAL)
            .build();
    when(rosterStudentRepository.findByCourseId(eq(1L))).thenReturn(List.of(student));

    String content =
        CATME_AUDIT_HEADER
            + "\"Gaucho,"
            + " Chris\",\"1234567\",\"cgaucho@ucsb.edu\",\"0100\",\"Web\",\"M\",\"None\",\"None\",\n";
    MockMultipartFile file =
        new MockMultipartFile(
            "file", "catme.csv", "text/csv", content.getBytes(StandardCharsets.UTF_8));

    MvcResult response =
        mockMvc
            .perform(multipart("/api/catme/audit").file(file).with(csrf()).param("courseId", "1"))
            .andExpect(status().isOk())
            .andReturn();

    CATMEAuditResult result =
        objectMapper.readValue(response.getResponse().getContentAsString(), CATMEAuditResult.class);
    assertEquals(1, result.studentsToUpdate().size());
    assertEquals(0, result.studentsToDrop().size());
    assertEquals("Section", result.studentsToUpdate().get(0).field());
    assertEquals("0100", result.studentsToUpdate().get(0).oldValue());
    assertEquals("", result.studentsToUpdate().get(0).newValue());
  }

  @Test
  @WithInstructorCoursePermissions
  public void catmeAudit_studentNotOnRoster_producesDrop() throws Exception {
    when(rosterStudentRepository.findByCourseId(eq(1L))).thenReturn(List.of());

    String content =
        CATME_AUDIT_HEADER
            + "\"Gaucho,"
            + " Chris\",\"1234567\",\"cgaucho@ucsb.edu\",\"0100\",\"Web\",\"M\",\"None\",\"None\",\n";
    MockMultipartFile file =
        new MockMultipartFile(
            "file", "catme.csv", "text/csv", content.getBytes(StandardCharsets.UTF_8));

    MvcResult response =
        mockMvc
            .perform(multipart("/api/catme/audit").file(file).with(csrf()).param("courseId", "1"))
            .andExpect(status().isOk())
            .andReturn();

    CATMEAuditResult result =
        objectMapper.readValue(response.getResponse().getContentAsString(), CATMEAuditResult.class);
    assertEquals(0, result.studentsToUpdate().size());
    assertEquals(1, result.studentsToDrop().size());
    assertEquals("1234567", result.studentsToDrop().get(0).studentId());
  }

  @Test
  @WithInstructorCoursePermissions
  public void catmeAudit_duplicateStudentIds_keepsFirstOccurrence() throws Exception {
    RosterStudent student1 =
        RosterStudent.builder()
            .studentId("1234567")
            .firstName("Chris")
            .lastName("Gaucho")
            .email("cgaucho@ucsb.edu")
            .section("0100")
            .rosterStatus(RosterStatus.ROSTER)
            .build();
    RosterStudent student2 =
        RosterStudent.builder()
            .studentId("1234567")
            .firstName("Chris")
            .lastName("Gaucho")
            .email("cgaucho@ucsb.edu")
            .section("0100")
            .rosterStatus(RosterStatus.MANUAL)
            .build();
    when(rosterStudentRepository.findByCourseId(eq(1L))).thenReturn(List.of(student1, student2));

    String content =
        CATME_AUDIT_HEADER
            + "\"Gaucho,"
            + " Chris\",\"1234567\",\"cgaucho@ucsb.edu\",\"0100\",\"Web\",\"M\",\"None\",\"None\",\n"
            + "\"Gaucho,"
            + " Chris\",\"1234567\",\"cgaucho@ucsb.edu\",\"0100\",\"Web\",\"M\",\"None\",\"None\",\n";
    MockMultipartFile file =
        new MockMultipartFile(
            "file", "catme.csv", "text/csv", content.getBytes(StandardCharsets.UTF_8));

    MvcResult response =
        mockMvc
            .perform(multipart("/api/catme/audit").file(file).with(csrf()).param("courseId", "1"))
            .andExpect(status().isOk())
            .andReturn();

    CATMEAuditResult result =
        objectMapper.readValue(response.getResponse().getContentAsString(), CATMEAuditResult.class);
    assertEquals(0, result.studentsToUpdate().size());
    assertEquals(0, result.studentsToDrop().size());
  }

  @Test
  @WithInstructorCoursePermissions
  public void catmeAudit_rosterStudentNotInCatme_isIgnored() throws Exception {
    RosterStudent student =
        RosterStudent.builder()
            .studentId("9999999")
            .firstName("Not")
            .lastName("Uploaded")
            .email("notuploaded@ucsb.edu")
            .section("0100")
            .rosterStatus(RosterStatus.ROSTER)
            .build();
    when(rosterStudentRepository.findByCourseId(eq(1L))).thenReturn(List.of(student));

    String content =
        CATME_AUDIT_HEADER
            + "\"Gaucho,"
            + " Chris\",\"1234567\",\"cgaucho@ucsb.edu\",\"0100\",\"Web\",\"M\",\"None\",\"None\",\n";
    MockMultipartFile file =
        new MockMultipartFile(
            "file", "catme.csv", "text/csv", content.getBytes(StandardCharsets.UTF_8));

    MvcResult response =
        mockMvc
            .perform(multipart("/api/catme/audit").file(file).with(csrf()).param("courseId", "1"))
            .andExpect(status().isOk())
            .andReturn();

    CATMEAuditResult result =
        objectMapper.readValue(response.getResponse().getContentAsString(), CATMEAuditResult.class);
    assertEquals(0, result.studentsToUpdate().size());
    assertEquals(1, result.studentsToDrop().size());
    assertEquals("1234567", result.studentsToDrop().get(0).studentId());
  }

  @Test
  @WithInstructorCoursePermissions
  public void catmeAudit_droppedStudentInFrontiersIsNotAudited() throws Exception {
    RosterStudent droppedStudent =
        RosterStudent.builder()
            .studentId("1234567")
            .firstName("Chris")
            .lastName("Gaucho")
            .email("cgaucho@ucsb.edu")
            .section("0100")
            .rosterStatus(RosterStatus.DROPPED)
            .build();
    when(rosterStudentRepository.findByCourseId(eq(1L))).thenReturn(List.of(droppedStudent));

    String content =
        CATME_AUDIT_HEADER
            + "\"Gaucho,"
            + " Chris\",\"1234567\",\"cgaucho@ucsb.edu\",\"0100\",\"Web\",\"M\",\"None\",\"None\",\n";
    MockMultipartFile file =
        new MockMultipartFile(
            "file", "catme.csv", "text/csv", content.getBytes(StandardCharsets.UTF_8));

    MvcResult response =
        mockMvc
            .perform(multipart("/api/catme/audit").file(file).with(csrf()).param("courseId", "1"))
            .andExpect(status().isOk())
            .andReturn();

    CATMEAuditResult result =
        objectMapper.readValue(response.getResponse().getContentAsString(), CATMEAuditResult.class);
    assertEquals(0, result.studentsToUpdate().size());
    assertEquals(1, result.studentsToDrop().size());
  }

  @Test
  @WithInstructorCoursePermissions
  public void catmeAudit_blankLineEndsDataSection() throws Exception {
    RosterStudent student =
        RosterStudent.builder()
            .studentId("1234567")
            .firstName("Chris")
            .lastName("Gaucho")
            .email("cgaucho@ucsb.edu")
            .section("0100")
            .rosterStatus(RosterStatus.ROSTER)
            .build();
    when(rosterStudentRepository.findByCourseId(eq(1L))).thenReturn(List.of(student));

    String content =
        CATME_AUDIT_HEADER
            + "\"Gaucho,"
            + " Chris\",\"1234567\",\"cgaucho@ucsb.edu\",\"0100\",\"Web\",\"M\",\"None\",\"None\",\n"
            + "\n"
            + "some trailing footer text that should be ignored\n";
    MockMultipartFile file =
        new MockMultipartFile(
            "file", "catme.csv", "text/csv", content.getBytes(StandardCharsets.UTF_8));

    MvcResult response =
        mockMvc
            .perform(multipart("/api/catme/audit").file(file).with(csrf()).param("courseId", "1"))
            .andExpect(status().isOk())
            .andReturn();

    CATMEAuditResult result =
        objectMapper.readValue(response.getResponse().getContentAsString(), CATMEAuditResult.class);
    assertEquals(0, result.studentsToUpdate().size());
    assertEquals(0, result.studentsToDrop().size());
  }

  @Test
  @WithInstructorCoursePermissions
  public void catmeEmails_returnsMatchedEmailsAndNoEmailMessage() throws Exception {
    RosterStudent student1 =
        RosterStudent.builder()
            .lastName("Karimi")
            .firstName("Milad Arash")
            .email("milad@ucsb.edu")
            .build();

    RosterStudent student2 =
        RosterStudent.builder()
            .lastName("O'Connor")
            .firstName("Emily Elizabeth")
            .email("emily@ucsb.edu")
            .build();

    when(rosterStudentRepository.findByCourseId(eq(1L))).thenReturn(List.of(student1, student2));

    String payload =
        "O'CONNOR,   EMILY   ELIZABETH     0     2026-06-02 19:07:08.064856\n"
            + "KARIMI, MILAD ARASH\t14\t2026-06-04 23:10:58.045544\n"
            + "MISSING, STUDENT     0     2026-06-02 18:27:28.353824";

    MvcResult response =
        mockMvc
            .perform(
                post("/api/catme/emails")
                    .with(csrf())
                    .param("courseId", "1")
                    .contentType(MediaType.TEXT_PLAIN)
                    .content(payload))
            .andExpect(status().isOk())
            .andReturn();

    assertEquals(
        "emily@ucsb.edu\nmilad@ucsb.edu\n# NO EMAIL FOUND FOR MISSING, STUDENT",
        response.getResponse().getContentAsString());

    verify(rosterStudentRepository, times(1)).findByCourseId(eq(1L));
  }

  @Test
  @WithInstructorCoursePermissions
  public void catmeEmails_blankPayload_returnsBlankResponse() throws Exception {
    MvcResult response =
        mockMvc
            .perform(
                post("/api/catme/emails")
                    .with(csrf())
                    .param("courseId", "1")
                    .contentType(MediaType.TEXT_PLAIN)
                    .content(""))
            .andExpect(status().isOk())
            .andReturn();

    assertEquals("", response.getResponse().getContentAsString());

    verifyNoInteractions(rosterStudentRepository);
  }

  @Test
  @WithInstructorCoursePermissions
  public void catmeEmails_nullPayload_returnsBlankResponse() throws Exception {
    MvcResult response =
        mockMvc
            .perform(
                post("/api/catme/emails")
                    .with(csrf())
                    .param("courseId", "1")
                    .contentType(MediaType.TEXT_PLAIN))
            .andExpect(status().isOk())
            .andReturn();

    assertEquals("", response.getResponse().getContentAsString());
    verifyNoInteractions(rosterStudentRepository);
  }

  @Test
  @WithInstructorCoursePermissions
  public void catmeEmails_ignoresRosterEntriesMissingRequiredFields() throws Exception {
    RosterStudent valid =
        RosterStudent.builder().lastName("Smith").firstName("Pat").email("pat@ucsb.edu").build();
    RosterStudent missingFirst =
        RosterStudent.builder().lastName("Smith").firstName(null).email("null1@ucsb.edu").build();
    RosterStudent missingLast =
        RosterStudent.builder().lastName(null).firstName("Pat").email("null2@ucsb.edu").build();
    RosterStudent missingEmail =
        RosterStudent.builder().lastName("Smith").firstName("Pat").email(null).build();

    when(rosterStudentRepository.findByCourseId(eq(1L)))
        .thenReturn(List.of(valid, missingFirst, missingLast, missingEmail));

    MvcResult response =
        mockMvc
            .perform(
                post("/api/catme/emails")
                    .with(csrf())
                    .param("courseId", "1")
                    .contentType(MediaType.TEXT_PLAIN)
                    .content("SMITH, PAT"))
            .andExpect(status().isOk())
            .andReturn();

    assertEquals("pat@ucsb.edu", response.getResponse().getContentAsString());
  }

  @Test
  @WithInstructorCoursePermissions
  public void catmeEmails_handlesNameWithoutCommaAsUnmatched() throws Exception {
    when(rosterStudentRepository.findByCourseId(eq(1L))).thenReturn(List.of());

    MvcResult response =
        mockMvc
            .perform(
                post("/api/catme/emails")
                    .with(csrf())
                    .param("courseId", "1")
                    .contentType(MediaType.TEXT_PLAIN)
                    .content("SINGLETOKENNAME"))
            .andExpect(status().isOk())
            .andReturn();

    assertEquals(
        "# NO EMAIL FOUND FOR SINGLETOKENNAME", response.getResponse().getContentAsString());
  }

  @Test
  @WithInstructorCoursePermissions
  public void catmeEmails_matchesWhenDateTokenStartsAtLastPossibleIndex() throws Exception {
    RosterStudent student =
        RosterStudent.builder()
            .lastName("Boundary")
            .firstName("Case")
            .email("boundary@ucsb.edu")
            .build();
    when(rosterStudentRepository.findByCourseId(eq(1L))).thenReturn(List.of(student));

    String payload = "BOUNDARY, CASE 0 2026-06-02";
    MvcResult response =
        mockMvc
            .perform(
                post("/api/catme/emails")
                    .with(csrf())
                    .param("courseId", "1")
                    .contentType(MediaType.TEXT_PLAIN)
                    .content(payload))
            .andExpect(status().isOk())
            .andReturn();

    assertEquals("boundary@ucsb.edu", response.getResponse().getContentAsString());
  }

  @Test
  @WithInstructorCoursePermissions
  public void catmeEmails_doesNotTreatNonDigitDateTokenAsDate() throws Exception {
    when(rosterStudentRepository.findByCourseId(eq(1L))).thenReturn(List.of());

    String payload = "NOTADATE, CASE 0 202A-06-02";
    MvcResult response =
        mockMvc
            .perform(
                post("/api/catme/emails")
                    .with(csrf())
                    .param("courseId", "1")
                    .contentType(MediaType.TEXT_PLAIN)
                    .content(payload))
            .andExpect(status().isOk())
            .andReturn();

    assertEquals(
        "# NO EMAIL FOUND FOR NOTADATE, CASE 0 202A-06-02",
        response.getResponse().getContentAsString());
  }

  @Test
  public void catmeEmails_directCall_nullPayload_returnsBlankAndSkipsRepoLookup() {
    CATMEController controller = new CATMEController();
    ReflectionTestUtils.setField(controller, "rosterStudentRepository", rosterStudentRepository);

    String response = controller.getCourseEmailsFromCatme(1L, null);

    assertEquals("", response);
    verifyNoInteractions(rosterStudentRepository);
  }

  @Test
  public void catmeEmails_directCall_blankPayload_returnsBlankAndSkipsRepoLookup() {
    CATMEController controller = new CATMEController();
    ReflectionTestUtils.setField(controller, "rosterStudentRepository", rosterStudentRepository);

    String response = controller.getCourseEmailsFromCatme(1L, "   ");

    assertEquals("", response);
    verifyNoInteractions(rosterStudentRepository);
  }

  @Test
  @WithInstructorCoursePermissions
  public void catmeEmails_coversDateAndDuplicateEdgeCases() throws Exception {
    RosterStudent firstDuplicate =
        RosterStudent.builder()
            .lastName("Dup")
            .firstName("Case")
            .email("first-dup@ucsb.edu")
            .build();
    RosterStudent secondDuplicate =
        RosterStudent.builder()
            .lastName("Dup")
            .firstName("Case")
            .email("second-dup@ucsb.edu")
            .build();
    when(rosterStudentRepository.findByCourseId(eq(1L)))
        .thenReturn(List.of(firstDuplicate, secondDuplicate));

    String payload =
        String.join(
            "\n",
            "DUP, CASE",
            "",
            "NOSPACE,NAME 2026-06-02",
            "LETTERSCORE, EXAMPLE X 2026-06-02",
            "BADDATE1, EXAMPLE 0 2026/06-02",
            "BADDATE2, EXAMPLE 0 2026-A6-02",
            "BADDATE3, EXAMPLE 0 2026-0A-02",
            "BADDATE4, EXAMPLE 0 2026-06/02",
            "BADDATE5, EXAMPLE 0 2026-06-A2",
            "BADDATE6, EXAMPLE 0 2026-06-2A");

    MvcResult response =
        mockMvc
            .perform(
                post("/api/catme/emails")
                    .with(csrf())
                    .param("courseId", "1")
                    .contentType(MediaType.TEXT_PLAIN)
                    .content(payload))
            .andExpect(status().isOk())
            .andReturn();

    assertEquals(
        String.join(
            "\n",
            "first-dup@ucsb.edu",
            "",
            "# NO EMAIL FOUND FOR NOSPACE,NAME",
            "# NO EMAIL FOUND FOR LETTERSCORE, EXAMPLE X",
            "# NO EMAIL FOUND FOR BADDATE1, EXAMPLE 0 2026/06-02",
            "# NO EMAIL FOUND FOR BADDATE2, EXAMPLE 0 2026-A6-02",
            "# NO EMAIL FOUND FOR BADDATE3, EXAMPLE 0 2026-0A-02",
            "# NO EMAIL FOUND FOR BADDATE4, EXAMPLE 0 2026-06/02",
            "# NO EMAIL FOUND FOR BADDATE5, EXAMPLE 0 2026-06-A2",
            "# NO EMAIL FOUND FOR BADDATE6, EXAMPLE 0 2026-06-2A"),
        response.getResponse().getContentAsString());
  }
}
