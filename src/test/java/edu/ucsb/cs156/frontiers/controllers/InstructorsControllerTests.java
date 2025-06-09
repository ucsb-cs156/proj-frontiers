package edu.ucsb.cs156.frontiers.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.ucsb.cs156.frontiers.entities.Instructor;
import edu.ucsb.cs156.frontiers.errors.EntityNotFoundException;
import edu.ucsb.cs156.frontiers.repositories.InstructorRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureDataJpa;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;

import edu.ucsb.cs156.frontiers.ControllerTestCase;

import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
@WebMvcTest(controllers = InstructorsController.class)
@AutoConfigureDataJpa
public class InstructorsControllerTests extends ControllerTestCase {

    @MockitoBean
    private InstructorRepository instructorRepository;

    @Autowired
    private ObjectMapper mapper;


    private final Instructor instr1 = Instructor.builder()
            .email("jdoe@ucsb.edu")
            .build();

    private final Instructor instr2 = Instructor.builder()
            .email("yuxiangzhang@ucsb.edu")
            .build();

    // POST
    @Test
    @WithMockUser(roles = { "ADMIN" })
    public void testPostInstructor() throws Exception {

        when(instructorRepository.save(any(Instructor.class))).thenReturn(instr1);

        MvcResult response = mockMvc.perform(post("/api/instructors/post")
                        .with(csrf())
                        .param("email", "jdoe@ucsb.edu"))
                .andExpect(status().isOk())
                .andReturn();

        verify(instructorRepository, times(1))
                .save(eq(instr1));

        String expectedJson = mapper.writeValueAsString(instr1);
        assertEquals(expectedJson, response.getResponse().getContentAsString());
    }

    // GET
    @Test
    @WithMockUser(roles = { "ADMIN" })
    public void testAllInstructors() throws Exception {

        when(instructorRepository.findAll())
                .thenReturn(java.util.List.of(instr1, instr2));

        MvcResult response = mockMvc.perform(get("/api/instructors/all"))
                .andExpect(status().isOk())
                .andReturn();

        String expectedJson = mapper.writeValueAsString(java.util.List.of(instr1, instr2));
        assertEquals(expectedJson, response.getResponse().getContentAsString());
    }

    // DELETE
    @Test
    @WithMockUser(roles = { "ADMIN" })
    public void testDeleteInstructor() throws Exception {

        when(instructorRepository.findById(eq("jdoe@ucsb.edu")))
                .thenReturn(Optional.of(instr1));

        MvcResult response = mockMvc.perform(delete("/api/instructors")
                        .with(csrf())
                        .param("email", "jdoe@ucsb.edu"))
                .andExpect(status().isOk())
                .andReturn();

        verify(instructorRepository, times(1))
                .delete(eq(instr1));

        assertEquals("Instructor with email jdoe@ucsb.edu deleted",
                response.getResponse().getContentAsString());
    }

    @Test
    @WithMockUser(roles = { "ADMIN" })
    public void testDeleteInstructor_NotFound() throws Exception {

        when(instructorRepository.findById(eq("missing@ucsb.edu")))
                .thenReturn(Optional.empty());

        MvcResult response = mockMvc.perform(delete("/api/instructors")
                        .with(csrf())
                        .param("email", "missing@ucsb.edu"))
                .andExpect(status().isNotFound())
                .andReturn();

        Map<String,String> expectedMap = Map.of(
                "type", "EntityNotFoundException",
                "message", "Instructor with id missing@ucsb.edu not found");
        String expectedJson = mapper.writeValueAsString(expectedMap);

        assertEquals(expectedJson, response.getResponse().getContentAsString());
    }
}
