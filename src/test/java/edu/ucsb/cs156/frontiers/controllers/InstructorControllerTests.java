package edu.ucsb.cs156.frontiers.controllers;

import edu.ucsb.cs156.frontiers.repositories.UserRepository;
import edu.ucsb.cs156.frontiers.testconfig.TestConfig;
import edu.ucsb.cs156.frontiers.ControllerTestCase;
import edu.ucsb.cs156.frontiers.entities.Instructor;
import edu.ucsb.cs156.frontiers.repositories.InstructorRepository;
import edu.ucsb.cs156.frontiers.controllers.InstructorController;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import org.springframework.test.context.TestPropertySource;


import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@TestPropertySource(properties = {
    "app.admin.emails=testadmin@ucsb.edu"
})
@WebMvcTest(controllers = InstructorController.class)
@Import(TestConfig.class)
public class InstructorControllerTests extends ControllerTestCase{
    @MockBean
    InstructorRepository instructorRepository;

    @MockBean
    UserRepository userRepository;

    @Autowired
    private InstructorController instructorController;

    @Test
    public void logged_out_users_cannot_get_all() throws Exception {
            mockMvc.perform(get("/api/instructor/all"))
                            .andExpect(status().is(403)); 
    }

    @WithMockUser(roles = { "USER" })
    @Test
    public void logged_in_users_canot_get_all() throws Exception {
            mockMvc.perform(get("/api/instructor/all"))
                            .andExpect(status().is(403)); 
    }

    @WithMockUser(roles = { "ADMIN" })
    @Test
    public void admin_users_can_get_all() throws Exception {
            mockMvc.perform(get("/api/instructor/all"))
                            .andExpect(status().is(200)); 
    }


        @Test
        public void logged_out_users_cannot_post() throws Exception {
                mockMvc.perform(post("/api/instructor/post"))
                                .andExpect(status().is(403));
        }

    

        @WithMockUser(roles = { "USER" })
        @Test
        public void logged_in_regular_users_cannot_post() throws Exception {
                mockMvc.perform(post("/api/instructor/post"))
                                .andExpect(status().is(403));
        }



     @WithMockUser(roles = { "ADMIN"})
        @Test
        public void admin_user_can_get_all_properly() throws Exception {

                Instructor instructor = Instructor.builder()
                    .email("jasonz2005@gmail.com")
                    .build();


                ArrayList<Instructor> expectedInstructor = new ArrayList<>();
                expectedInstructor.addAll(Arrays.asList(instructor));

                when(instructorRepository.findAll()).thenReturn(expectedInstructor);

                MvcResult response = mockMvc.perform(get("/api/instructor/all"))
                                .andExpect(status().isOk()).andReturn();


                verify(instructorRepository, times(1)).findAll();
                String expectedJson = mapper.writeValueAsString(expectedInstructor);
                String responseString = response.getResponse().getContentAsString();
                assertEquals(expectedJson, responseString);
        }

        @WithMockUser(roles = { "ADMIN" })
        @Test
        public void an_admin_user_can_post_a_new_admin() throws Exception {
            
              Instructor instructor = Instructor.builder()
                    .email("jasonz2005@gmail.com")
                    .build();

                when(instructorRepository.save(eq(instructor))).thenReturn(instructor);

                MvcResult response = mockMvc.perform(
                                post("/api/instructor/post?email=jasonz2005@gmail.com")
                                                .with(csrf()))
                                .andExpect(status().isOk()).andReturn();

                verify(instructorRepository, times(1)).save(instructor);
                String expectedJson = mapper.writeValueAsString(instructor);
                String responseString = response.getResponse().getContentAsString();
                assertEquals(expectedJson, responseString);
        }
     

        @WithMockUser(roles = { "ADMIN" })
        @Test
        public void admin_can_delete_a_admin() throws Exception {

                Instructor instructor = Instructor.builder()
                    .email("jasonz2005@gmail.com")
                    .build();

                when(instructorRepository.findById(eq("jasonz2005@gmail.com"))).thenReturn(Optional.of(instructor));


                MvcResult response = mockMvc.perform(
                                delete("/api/instructor?email=jasonz2005@gmail.com")
                                                .with(csrf()))
                                .andExpect(status().isOk()).andReturn();

                verify(instructorRepository, times(1)).findById("jasonz2005@gmail.com");
                verify(instructorRepository, times(1)).delete(any());

                Map<String, Object> json = responseToJson(response);
                assertEquals("Instructor with valid email is deleted", json.get("message"));
        }
        

        @WithMockUser(roles = { "ADMIN" })
        @Test
        public void admin_tries_to_delete_non_existant_admin_and_gets_right_error_message() throws Exception {

                when(instructorRepository.findById(eq("jasonz2005@gmail.com"))).thenReturn(Optional.empty());

                MvcResult response = mockMvc.perform(
                                delete("/api/instructor?email=jasonz2005@gmail.com")
                                                .with(csrf()))
                                .andExpect(status().isNotFound()).andReturn();

                verify(instructorRepository, times(1)).findById("jasonz2005@gmail.com");
                Map<String, Object> json = responseToJson(response);
                assertEquals("Instructor with id jasonz2005@gmail.com not found", json.get("message"));
        }

}