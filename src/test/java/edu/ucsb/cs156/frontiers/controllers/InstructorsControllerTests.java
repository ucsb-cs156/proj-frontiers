package edu.ucsb.cs156.frontiers.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MvcResult;

import edu.ucsb.cs156.frontiers.ControllerTestCase;
import edu.ucsb.cs156.frontiers.entities.Instructor;
import edu.ucsb.cs156.frontiers.repositories.InstructorRepository;
import edu.ucsb.cs156.frontiers.repositories.UserRepository;
import edu.ucsb.cs156.frontiers.testconfig.TestConfig;

@WebMvcTest(controllers = InstructorsController.class)
@Import(TestConfig.class)
public class InstructorsControllerTests extends ControllerTestCase {

        @MockBean
        InstructorRepository instructorRepository;
        @MockBean
        UserRepository userRepository;

        // Tests for the POST endpoint
        @Test
        public void logged_out_users_cannot_post() throws Exception {
                mockMvc.perform(post("/api/admin/instructors/post"))
                                .andExpect(status().is(403)); // logged out users cannot post
        }

        @WithMockUser(roles = { "USER" })
        @Test
        public void logged_in_users_cannot_post() throws Exception {
                mockMvc.perform(post("/api/admin/instructors/post"))
                                .andExpect(status().is(403)); // logged in users cannot post
        }

        @WithMockUser(roles = { "ADMIN" })
        @Test
        public void logged_in_admins_can_post() throws Exception {
                // arrage
                Instructor instructor = Instructor.builder()
                                .email("ins@ucsb.edu")
                                .build();
                when(instructorRepository.findAll()).thenReturn(new ArrayList<>(Arrays.asList(instructor)));

                // act
                MvcResult response = mockMvc
                                .perform(post("/api/admin/instructors/post?email=ins@ucsb.edu").with(csrf()))
                                .andExpect(status().isOk())
                                .andReturn();

                // assert
                verify(instructorRepository, times(1)).save(eq(instructor));
                String expectedJson = mapper.writeValueAsString(instructor);
                String responseString = response.getResponse().getContentAsString();
                assertEquals(expectedJson, responseString);
        }

        // Tests for the GET endpoint
        @Test
        public void logged_out_users_cannot_get() throws Exception {
                mockMvc.perform(get("/api/admin/instructors/get"))
                                .andExpect(status().is(403)); // logged out users cannot get
        }

        @WithMockUser(roles = { "USER" })
        @Test
        public void logged_in_users_cannot_get() throws Exception {
                mockMvc.perform(get("/api/admin/instructors/get"))
                                .andExpect(status().is(403)); // logged in users cannot get
        }

        @WithMockUser(roles = { "ADMIN" })
        @Test
        public void logged_in_admins_can_get() throws Exception {
                // arrage
                Instructor instructor = Instructor.builder()
                                .email("ins@ucsb.edu")
                                .build();
                ArrayList<Instructor> expectedInstructors = new ArrayList<>();
                expectedInstructors.addAll(Arrays.asList(instructor));
                when(instructorRepository.findAll()).thenReturn(new ArrayList<>(Arrays.asList(instructor)));

                // act
                MvcResult response = mockMvc.perform(get("/api/admin/instructors/get"))
                                .andExpect(status().isOk())
                                .andReturn();

                // assert
                verify(instructorRepository, times(1)).findAll();
                String expectedJson = mapper.writeValueAsString(expectedInstructors);
                String responseString = response.getResponse().getContentAsString();
                assertEquals(expectedJson, responseString);
        }

        // Tests for the DELETE endpoint
        @Test
        public void logged_out_users_cannot_delete() throws Exception {
                mockMvc.perform(delete("/api/admin/instructors/delete"))
                                .andExpect(status().is(403)); // logged out users cannot delete
        }

        @WithMockUser(roles = { "USER" })
        @Test
        public void logged_in_users_cannot_delete() throws Exception {
                mockMvc.perform(delete("/api/admin/instructors/delete"))
                                .andExpect(status().is(403)); // logged in users cannot delete
        }

        @WithMockUser(roles = { "ADMIN" })
        @Test
        public void logged_in_admins_can_delete() throws Exception {
                // Arrange
                Instructor instructor = Instructor.builder()
                                .email("ins@ucsb.edu")
                                .build();
                when(instructorRepository.findById(eq("ins@ucsb.edu"))).thenReturn(Optional.of(instructor));

                // Act
                MvcResult response = mockMvc.perform(delete("/api/admin/instructors/delete")
                                .param("email", "ins@ucsb.edu")
                                .with(csrf()))
                                .andExpect(status().isOk())
                                .andReturn();

                // Assert
                verify(instructorRepository, times(1)).findById("ins@ucsb.edu");
                verify(instructorRepository, times(1)).delete(instructor); 
                String expectedMessage = String.format("Instructor with email %s deleted.", instructor.getEmail());
                String responseString = response.getResponse().getContentAsString();
                assertEquals(expectedMessage, responseString);
        }

        @WithMockUser(roles = { "ADMIN" })
        @Test
        public void admin_try_to_delete_a_instructor_not_found() throws Exception {
                // Arrange
                String email = "nonexistent@ucsb.edu";
                when(instructorRepository.findById(eq(email))).thenReturn(Optional.empty());

                // Act
                MvcResult response = mockMvc.perform(delete("/api/admin/instructors/delete")
                                .param("email", email)
                                .with(csrf()))
                                .andExpect(status().isNotFound())
                                .andReturn();

                // Assert
                verify(instructorRepository, times(1)).findById(email);
                verify(instructorRepository, times(0)).delete(any());
                String expectedMessage = String.format("Instructor with email %s not found.", email);
                String responseString = response.getResponse().getContentAsString();
                assertEquals(expectedMessage, responseString);
        }

}
