package edu.ucsb.cs156.frontiers.controllers;

import edu.ucsb.cs156.frontiers.repositories.UserRepository;
import edu.ucsb.cs156.frontiers.testconfig.TestConfig;
import edu.ucsb.cs156.frontiers.ControllerTestCase;
import edu.ucsb.cs156.frontiers.entities.Admin;
import edu.ucsb.cs156.frontiers.repositories.AdminRepository;

import java.util.ArrayList;
import java.util.Arrays;
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


@WebMvcTest(controllers = AdminsController.class)
@Import(TestConfig.class)
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
}