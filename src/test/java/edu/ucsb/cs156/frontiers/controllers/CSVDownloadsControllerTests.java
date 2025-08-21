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
import edu.ucsb.cs156.frontiers.enums.OrgStatus;
import edu.ucsb.cs156.frontiers.enums.RosterStatus;
import edu.ucsb.cs156.frontiers.models.RosterStudentDTO;
import edu.ucsb.cs156.frontiers.repositories.CourseRepository;
import edu.ucsb.cs156.frontiers.services.RosterStudentDTOService;
import edu.ucsb.cs156.frontiers.testconfig.TestConfig;
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
        Course.builder().id(1L).courseName("ucsb-cs156-s25").term("S25").school("UCSB").build();

    RosterStudentDTO rosterStudentDTO =
        new RosterStudentDTO(
            42L,
            course.getId(),
            "12345",
            "Chris",
            "Gaucho",
            "cgaucho@ucsb.edu",
            102L,
            12345,
            "cgaucho",
            RosterStatus.ROSTER,
            OrgStatus.PENDING);

    doReturn(Optional.of(course)).when(courseRepository).findById(eq(1L));
    doReturn(List.of(rosterStudentDTO)).when(rosterStudentDTOService).getRosterStudentDTOs(eq(1L));

    String expectedResponse =
        """
                "COURSEID","EMAIL","FIRSTNAME","ID","LASTNAME","ORGSTATUS","ROSTERSTATUS","STUDENTID","USERGITHUBID","USERGITHUBLOGIN","USERID"
                "1","cgaucho@ucsb.edu","Chris","42","Gaucho","PENDING","ROSTER","12345","12345","cgaucho","102"
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
}
