package edu.ucsb.cs156.frontiers.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.ucsb.cs156.frontiers.entities.Admin;
import edu.ucsb.cs156.frontiers.errors.EntityNotFoundException;
import edu.ucsb.cs156.frontiers.repositories.AdminRepository;
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

import org.springframework.test.context.TestPropertySource;

@Slf4j
@WebMvcTest(controllers = AdminsController.class)
@AutoConfigureDataJpa
@TestPropertySource(properties = "admin.emails=admin1@ucsb.edu")
public class AdminsControllerTests extends ControllerTestCase {

    @MockitoBean
    private AdminRepository adminRepository;

    @Autowired
    private ObjectMapper mapper;


    private final Admin admin1 = Admin.builder()
            .email("admin1@ucsb.edu")
            .build();

    private final Admin admin2 = Admin.builder()
            .email("admin2@ucsb.edu")
            .build();

    // POST
    @Test
    @WithMockUser(roles = { "ADMIN" })
    public void testPostAdmin() throws Exception {

        when(adminRepository.save(any(Admin.class))).thenReturn(admin1);

        MvcResult response = mockMvc.perform(post("/api/admins/post")
                        .with(csrf())
                        .param("email", "admin1@ucsb.edu"))
                .andExpect(status().isOk())
                .andReturn();

        verify(adminRepository, times(1))
                .save(eq(admin1));

        String expectedJson = mapper.writeValueAsString(admin1);
        assertEquals(expectedJson, response.getResponse().getContentAsString());
    }

    // GET
    @Test
    @WithMockUser(roles = { "ADMIN" })
    public void testAllAdmins() throws Exception {
        when(adminRepository.findAll()).thenReturn(java.util.List.of(admin1, admin2));

        MvcResult response = mockMvc.perform(get("/api/admins/all"))
                .andExpect(status().isOk())
                .andReturn();

        String expectedJson = mapper.writeValueAsString(java.util.List.of(admin1, admin2));
        assertEquals(expectedJson, response.getResponse().getContentAsString());
    }


    // DELETE
    @Test
    @WithMockUser(roles = { "ADMIN" })
    public void testDeleteAdmin() throws Exception {
        when(adminRepository.findById(eq("admin2@ucsb.edu"))).thenReturn(Optional.of(admin2));

        MvcResult response = mockMvc.perform(delete("/api/admins")
                        .with(csrf())
                        .param("email", "admin2@ucsb.edu"))
                .andExpect(status().isOk())
                .andReturn();

        verify(adminRepository, times(1)).delete(eq(admin2));
        assertEquals("Admin with email admin2@ucsb.edu deleted", response.getResponse().getContentAsString());
    }

    @Test
    @WithMockUser(roles = { "ADMIN" })
    public void testDeleteAdmin_NotFound() throws Exception {
        when(adminRepository.findById(eq("missing@ucsb.edu"))).thenReturn(Optional.empty());

        MvcResult response = mockMvc.perform(delete("/api/admins")
                        .with(csrf())
                        .param("email", "missing@ucsb.edu"))
                .andExpect(status().isNotFound())
                .andReturn();

        Map<String,String> expectedMap = Map.of(
                "type", "EntityNotFoundException",
                "message", "Admin with id missing@ucsb.edu not found");
        String expectedJson = mapper.writeValueAsString(expectedMap);

        assertEquals(expectedJson, response.getResponse().getContentAsString());
    }

    @Test
    @WithMockUser(roles = { "ADMIN" })
    public void testDeleteAdmin_PermanentAdmin_Forbidden() throws Exception {
        when(adminRepository.findById(eq("admin1@ucsb.edu")))
                .thenReturn(Optional.of(admin1));

        MvcResult result = mockMvc.perform(delete("/api/admins")
                            .with(csrf())
                            .param("email", "admin1@ucsb.edu"))
                .andExpect(status().isForbidden())
                .andReturn();

        Map<String, String> expected = Map.of(
            "type", "UnsupportedOperationException",
            "message", "Cannot delete permanent admin: admin1@ucsb.edu"
        );

        String expectedJson = mapper.writeValueAsString(expected);
        String actualJson = result.getResponse().getContentAsString();

        assertEquals(expectedJson, actualJson);
    }



}