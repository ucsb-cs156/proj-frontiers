package edu.ucsb.cs156.frontiers.controllers;

import edu.ucsb.cs156.frontiers.repositories.UserRepository;
import edu.ucsb.cs156.frontiers.testconfig.TestConfig;
import edu.ucsb.cs156.frontiers.ControllerTestCase;
import edu.ucsb.cs156.frontiers.entities.Instructor;
import edu.ucsb.cs156.frontiers.repositories.InstructorRepository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.ArgumentCaptor;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;


@WebMvcTest(controllers = InstructorsController.class)
@Import(TestConfig.class)
public class InstructorsControllerTests extends ControllerTestCase {

    @MockBean
    InstructorRepository instructorRepository;
    @MockBean
    UserRepository userRepository;

    // Authorization tests for /api/ucsborganizations/post

    @Test
    public void logged_out_users_cannot_post() throws Exception {
        mockMvc.perform(post("/api/instructors/post"))
                .andExpect(status().is(403)); 
    }

    @WithMockUser(roles = { "USER" })
    @Test
    public void logged_in_users_cannot_post() throws Exception {
        mockMvc.perform(post("/api/instructors/post"))
                .andExpect(status().is(403)); // only admins can post
    }

    @WithMockUser(roles = { "ADMIN" })
    @Test
    public void logged_in_admins_can_post() throws Exception {
        // arrange
        Instructor expectedInstructor = new Instructor("test@ucsb.edu");
        when(instructorRepository.save(any(Instructor.class))).thenReturn(expectedInstructor);

        // act
        MvcResult response = mockMvc.perform(post("/api/instructors/post?email=test@ucsb.edu").with(csrf()))
                .andExpect(status().isOk())
                .andReturn();

        // assert
        String expectedJson = mapper.writeValueAsString(expectedInstructor);
        String actualResponse = response.getResponse().getContentAsString();
        assertEquals(expectedJson, actualResponse);
    }

    // Authorization tests for /api/instructors/admin/all
    @Test
    public void logged_out_users_cannot_get() throws Exception {
        mockMvc.perform(get("/api/instructors/all"))
                .andExpect(status().is(403)); // logged out users cannot get
    }

    @WithMockUser(roles = { "USER" })
    @Test
    public void logged_in_users_cannot_get() throws Exception {
        mockMvc.perform(get("/api/instructors/all"))
                .andExpect(status().is(403)); // logged in users cannot get
    }


    @WithMockUser(roles = { "ADMIN" })
    @Test
    public void logged_in_admins_can_get() throws Exception {
        // arrage
        Instructor instructor = Instructor.builder()
                .email("test@ucsb.edu")
                .build();

        ArrayList<Instructor> expectedInstructors = new ArrayList<>(Arrays.asList(instructor));
        when(instructorRepository.findAll()).thenReturn(expectedInstructors);
        // act
        MvcResult response = mockMvc.perform(get("/api/instructors/all"))
                .andExpect(status().isOk())
                .andReturn();

        // assert
        verify(instructorRepository, times(1)).findAll();
        String expectedJson = mapper.writeValueAsString(expectedInstructors);
        String responseString = response.getResponse().getContentAsString();
        assertEquals(expectedJson, responseString);
    }

    @WithMockUser(roles = { "ADMIN" })
    @Test
    public void admin_can_delete_an_insrtuctor() throws Exception {
            // arrange

            Instructor instructor = Instructor.builder()
                .email("testing@ucsb.edu")
                .build();


            when(instructorRepository.findById(eq("testing@ucsb.edu"))).thenReturn(Optional.of(instructor));

            // act
            MvcResult response = mockMvc.perform(
                    delete("/api/instructors")
                        .param("email", "testing@ucsb.edu")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andReturn();

            // assert
            verify(instructorRepository, times(1)).findById("testing@ucsb.edu");
            verify(instructorRepository, times(1)).delete(any());

            Map<String, Object> json = responseToJson(response);
            assertEquals("Instructor with id testing@ucsb.edu deleted", json.get("message"));
    }

    @WithMockUser(roles = { "ADMIN" })
    @Test
    public void admin_tries_to_delete_non_existant_instructor_and_gets_right_error_message()
                    throws Exception {
            // arrange

            when(instructorRepository.findById(eq("testing@ucsb.edu"))).thenReturn(Optional.empty());

            // act
            MvcResult response = mockMvc.perform(
                            delete("/api/instructors")
                                .param("email", "testing@ucsb.edu")
                                .with(csrf()))
                            .andExpect(status().isNotFound()).andReturn();

            // assert
            verify(instructorRepository, times(1)).findById("testing@ucsb.edu");
            Map<String, Object> json = responseToJson(response);
            assertEquals("Instructor with id testing@ucsb.edu not found", json.get("message"));
    }

    @Test
    public void logged_out_users_cannot_delete() throws Exception {
        mockMvc.perform(delete("/api/instructors")
                .param("email", "testing@ucsb.edu")
                .with(csrf()))
            .andExpect(status().is(403));
    }
}