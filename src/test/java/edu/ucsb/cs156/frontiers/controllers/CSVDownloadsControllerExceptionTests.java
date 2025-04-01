package edu.ucsb.cs156.frontiers.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureDataJpa;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import edu.ucsb.cs156.frontiers.ControllerTestCase;
import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.repositories.CourseRepository;
import edu.ucsb.cs156.frontiers.services.RosterStudentDTOService;
import edu.ucsb.cs156.frontiers.testconfig.TestConfig;

@WebMvcTest(controllers = {CSVDownloadsController.class})
@Import(TestConfig.class)
@AutoConfigureDataJpa
public class CSVDownloadsControllerExceptionTests extends ControllerTestCase {

  @MockitoBean RosterStudentDTOService rosterStudentDTOService;
  @MockitoBean CourseRepository courseRepository;

  @Autowired
  ObjectMapper objectMapper;

  @Test
  public void test_no_such_course() throws Exception {

    // arrange

    when(courseRepository.findById(eq(1L))).thenReturn(Optional.empty());

    // act

    MvcResult response =
        mockMvc
            .perform(
                get(
                    "/api/csv/rosterstudents?courseId=1"))
            .andReturn();

    // assert
    String actualResponse = response.getResponse().getContentAsString();

    objectMapper.readValue(
      response.getResponse().getContentAsString(),
      new TypeReference<Map<String, String>>() {});


    Map<String, String> errorResponse = objectMapper.readValue(actualResponse, new TypeReference<Map<String,String>>() {});
    Map<String,String> expectedResponse = Map.of("message", "Course with id 1 not found", "type", "EntityNotFoundException");
    assertEquals(expectedResponse, errorResponse);
    assertEquals(HttpStatus.NOT_FOUND.value(), response.getResponse().getStatus());
  }

  @Test
  public void test_csv_exception() throws Exception {

    // arrange

    Course course = Course.builder().id(1L).build();
    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(course));  

    when(rosterStudentDTOService.getRosterStudentDTOs(eq(1L)))
        .thenReturn(List.of());

    // act

    MvcResult response =
        mockMvc
            .perform(
                get(
                    "/api/csv/rosterstudents?courseId=1&testException=CsvDataTypeMismatchException"))
            .andReturn();

    // assert
    String actualResponse = response.getResponse().getContentAsString();
    String expectedMessage = "";
    assertEquals(expectedMessage, actualResponse);
  }
}
