package edu.ucsb.cs156.frontiers.controllers;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import edu.ucsb.cs156.frontiers.ControllerTestCase;
import edu.ucsb.cs156.frontiers.repositories.AssignmentRepository;
import edu.ucsb.cs156.frontiers.repositories.CourseRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@WebMvcTest(controllers = AssignmentController.class)
public class AssignmentControllerTests extends ControllerTestCase {

  @MockitoBean AssignmentRepository assignmentRepository;

  @MockitoBean CourseRepository courseRepository;

  @Test
  public void test_get_assignment_by_id_returns_404_when_not_found() throws Exception {
    when(assignmentRepository.findById(1L)).thenReturn(Optional.empty());

    mockMvc.perform(get("/api/assignments/1")).andExpect(status().isNotFound());
  }
}
