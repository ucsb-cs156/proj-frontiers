package edu.ucsb.cs156.frontiers.controllers;

import edu.ucsb.cs156.frontiers.repositories.UserRepository;
import edu.ucsb.cs156.frontiers.testconfig.TestConfig;
import edu.ucsb.cs156.frontiers.ControllerTestCase;
import edu.ucsb.cs156.frontiers.entities.Admin;
import edu.ucsb.cs156.frontiers.repositories.AdminRepository;

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
import org.springframework.test.context.TestPropertySource;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.ArgumentCaptor;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;


@WebMvcTest(controllers = AdminsController.class)
@Import(TestConfig.class)
@TestPropertySource(properties = 
"ADMIN_EMAILS=djensen@ucsb.edu,benjaminconte@ucsb.edu,samuelzhu@ucsb.edu,divyanipunj@ucsb.edu,sangitakunapuli@ucsb.edu,amey@ucsb.edu,phtcon@ucsb.edu,saul_diaz@ucsb.edu,jonahso@ucsb.edu,luismendoza@ucsb.edu,shuang_li@ucsb.edu,t_rocha@ucsb.edu,wsong@ucsb.edu")
public class AdminsControllerTests extends ControllerTestCase {

    @MockBean
    AdminRepository adminRepository;
    @MockBean
    UserRepository userRepository;

    // Authorization tests for /api/ucsborganizations/post

    @Test
    public void logged_out_users_cannot_post() throws Exception {
        mockMvc.perform(post("/api/admins/post"))
                .andExpect(status().is(403)); 
    }

    @WithMockUser(roles = { "USER" })
    @Test
    public void logged_in_users_cannot_post() throws Exception {
        mockMvc.perform(post("/api/admins/post"))
                .andExpect(status().is(403)); // only admins can post
    }

    @WithMockUser(roles = { "ADMIN" })
    @Test
    public void logged_in_admins_can_post() throws Exception {
        // arrange
        Admin expectedAdmin = new Admin("test@ucsb.edu");
        when(adminRepository.save(any(Admin.class))).thenReturn(expectedAdmin);

        // act
        MvcResult response = mockMvc.perform(post("/api/admins/post?email=test@ucsb.edu").with(csrf()))
                .andExpect(status().isOk())
                .andReturn();

        // assert
        String expectedJson = mapper.writeValueAsString(expectedAdmin);
        String actualResponse = response.getResponse().getContentAsString();
        assertEquals(expectedJson, actualResponse);
    }

    // Authorization tests for /api/admins/admin/all
    @Test
    public void logged_out_users_cannot_get() throws Exception {
        mockMvc.perform(get("/api/admins/all"))
                .andExpect(status().is(403)); // logged out users cannot get
    }

    @WithMockUser(roles = { "USER" })
    @Test
    public void logged_in_users_cannot_get() throws Exception {
        mockMvc.perform(get("/api/admins/all"))
                .andExpect(status().is(403)); // logged in users cannot get
    }


    @WithMockUser(roles = { "ADMIN" })
    @Test
    public void logged_in_admins_can_get() throws Exception {
        // arrage
        Admin admin = Admin.builder()
                .email("test@ucsb.edu")
                .build();

        ArrayList<Admin> expectedAdmins = new ArrayList<>(Arrays.asList(admin));
        when(adminRepository.findAll()).thenReturn(expectedAdmins);
        // act
        MvcResult response = mockMvc.perform(get("/api/admins/all"))
                .andExpect(status().isOk())
                .andReturn();

        // assert
        verify(adminRepository, times(1)).findAll();
        String expectedJson = mapper.writeValueAsString(expectedAdmins);
        String responseString = response.getResponse().getContentAsString();
        assertEquals(expectedJson, responseString);
    }

    @WithMockUser(roles = { "ADMIN" })
    @Test
    public void admin_can_delete_an_admin() throws Exception {
            // arrange

            Admin admin = Admin.builder()
                .email("test01@ucsb.edu")
                .build();


            when(adminRepository.findById(eq("test01@ucsb.edu"))).thenReturn(Optional.of(admin));

            // act
            MvcResult response = mockMvc.perform(
                    delete("/api/admins")
                        .param("email", "test01@ucsb.edu")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andReturn();

            // assert
            verify(adminRepository, times(1)).findById("test01@ucsb.edu");
            verify(adminRepository, times(1)).delete(any());

            Map<String, Object> json = responseToJson(response);
            assertEquals("Admin with id test01@ucsb.edu deleted", json.get("message"));
    }

    @WithMockUser(roles = { "ADMIN" })
    @Test
    public void admin_tries_to_delete_non_existant_admin_and_gets_right_error_message()
                    throws Exception {
            // arrange

            when(adminRepository.findById(eq("test@ucsb.edu"))).thenReturn(Optional.empty());

            // act
            MvcResult response = mockMvc.perform(
                            delete("/api/admins")
                                .param("email", "test@ucsb.edu")
                                .with(csrf()))
                            .andExpect(status().isNotFound()).andReturn();

            // assert
            verify(adminRepository, times(1)).findById("test@ucsb.edu");
            Map<String, Object> json = responseToJson(response);
            assertEquals("Admin with id test@ucsb.edu not found", json.get("message"));
    }

    @Test
    public void logged_out_users_cannot_delete() throws Exception {
        mockMvc.perform(delete("/api/admins")
                .param("email", "test01@ucsb.edu")
                .with(csrf()))
            .andExpect(status().is(403));
    }

    @WithMockUser(roles = { "ADMIN" })
    @Test
    public void admin_tries_to_delete_an_ADMIN_EMAIL_and_gets_right_error_message()
                    throws Exception {

            Admin admin = Admin.builder()
                    .email("saul_diaz@ucsb.edu")
                    .build();

            when(adminRepository.findById("saul_diaz@ucsb.edu")).thenReturn(Optional.of(admin));

            // act
            MvcResult response = mockMvc.perform(
                            delete("/api/admins")
                                    .param("email", "saul_diaz@ucsb.edu")
                                    .with(csrf()))
                            .andExpect(status().is(403)).andReturn();

            String content = response.getResponse().getContentAsString();
            System.out.println("Response content: " + content);

            // assert
            verify(adminRepository, times(1)).findById("saul_diaz@ucsb.edu");
            Map<String, Object> json = responseToJson(response);
            assertEquals("Can not delete an admin from ADMIN_EMAILS list", json.get("message"));
    }

}