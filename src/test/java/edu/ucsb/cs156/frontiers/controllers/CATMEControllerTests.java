package edu.ucsb.cs156.frontiers.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.ucsb.cs156.frontiers.ControllerTestCase;
import edu.ucsb.cs156.frontiers.annotations.WithInstructorCoursePermissions;
import edu.ucsb.cs156.frontiers.entities.RosterStudent;
import edu.ucsb.cs156.frontiers.repositories.RosterStudentRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;

@WebMvcTest(controllers = CATMEController.class)
public class CATMEControllerTests extends ControllerTestCase {

  @MockitoBean private RosterStudentRepository rosterStudentRepository;

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
}
