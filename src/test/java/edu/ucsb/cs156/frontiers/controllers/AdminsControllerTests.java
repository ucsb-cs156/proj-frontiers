package edu.ucsb.cs156.frontiers.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import edu.ucsb.cs156.frontiers.ControllerTestCase;
import edu.ucsb.cs156.frontiers.entities.Admin;
import edu.ucsb.cs156.frontiers.repositories.AdminRepository;
import edu.ucsb.cs156.frontiers.repositories.UserRepository;
import edu.ucsb.cs156.frontiers.testconfig.TestConfig;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;

@WebMvcTest(controllers = AdminsController.class)
@Import(TestConfig.class)
@TestPropertySource(
    properties =
        "ADMIN_EMAILS=djensen@ucsb.edu,benjaminconte@ucsb.edu,samuelzhu@ucsb.edu,divyanipunj@ucsb.edu,sangitakunapuli@ucsb.edu,amey@ucsb.edu,phtcon@ucsb.edu,acdamstedt@ucsb.edu,lzhou@ucsb.edu,hannahzhang@ucsb.edu,yilei_yan@ucsb.edu,zhixiuzhu@ucsb.edu,yuchenliu735@ucsb.edu")
public class AdminsControllerTests extends ControllerTestCase {

  @MockitoBean AdminRepository adminRepository;

  @MockitoBean UserRepository userRepository;

  // Authorization tests for post

  @Test
  public void logged_out_users_cannot_post() throws Exception {
    mockMvc.perform(post("/api/admin/post")).andExpect(status().is(403));
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void logged_in_regular_users_cannot_post() throws Exception {
    mockMvc.perform(post("/api/admin/post")).andExpect(status().is(403)); // only admins can post
  }

  // Authorization tests for get all

  @Test
  public void logged_out_users_cannot_get_all() throws Exception {
    mockMvc
        .perform(get("/api/admin/all"))
        .andExpect(status().is(403)); // logged out users can't get all
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void logged_in_users_cannot_get_all() throws Exception {
    mockMvc.perform(get("/api/admin/all")).andExpect(status().is(403)); // user roles can't get all
  }

  @WithMockUser(roles = {"ADMIN"})
  @Test
  public void logged_in_admin_can_get_all() throws Exception {
    mockMvc.perform(get("/api/admin/all")).andExpect(status().is(200));
  }

  // Authorization tests for delete

  @Test
  public void logged_out_users_cannot_delete() throws Exception {
    mockMvc.perform(delete("/api/admin?email=acdamstedt@gmail.com")).andExpect(status().is(403));
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void logged_in_regular_users_cannot_delete() throws Exception {
    mockMvc.perform(delete("/api/admin?email=acdamstedt@gmail.com")).andExpect(status().is(403));
  }

  // Functionality tests

  @WithMockUser(roles = {"ADMIN"})
  @Test
  public void an_admin_user_can_post_a_new_admin() throws Exception {
    Admin admin = Admin.builder().email("acdamstedt@ucsb.edu").build();
    when(adminRepository.save(eq(admin))).thenReturn(admin);
    // act
    MvcResult response =
        mockMvc
            .perform(post("/api/admin/post?email=acdamstedt@ucsb.edu").with(csrf()))
            .andExpect(status().isOk())
            .andReturn();
    // assert
    verify(adminRepository, times(1)).save(admin);
    String expectedJson = mapper.writeValueAsString(admin);
    String responseString = response.getResponse().getContentAsString();
    assertEquals(expectedJson, responseString);
  }

  @WithMockUser(roles = {"ADMIN"})
  @Test
  public void logged_in_admin_can_get_all_admins() throws Exception {
    List<String> adminEmails = Arrays.asList("acdamstedt@ucsb.edu");

    Admin admin1 = Admin.builder().email("acdamstedt@ucsb.edu").build();

    AdminsController.AdminDTO adminDTO1 = new AdminsController.AdminDTO(admin1, adminEmails);

    Admin admin2 = Admin.builder().email("acdamstedt@csil.cs.ucsb.edu").build();

    AdminsController.AdminDTO adminDTO2 = new AdminsController.AdminDTO(admin2, adminEmails);

    ArrayList<Admin> expectedAdmins = new ArrayList<>();
    expectedAdmins.addAll(Arrays.asList(admin1, admin2));

    ArrayList<AdminsController.AdminDTO> expectedAdminDTOs = new ArrayList<>();
    expectedAdminDTOs.addAll(Arrays.asList(adminDTO1, adminDTO2));

    when(adminRepository.findAll()).thenReturn(expectedAdmins);

    // act
    MvcResult response =
        mockMvc.perform(get("/api/admin/all")).andExpect(status().isOk()).andReturn();

    // assert
    verify(adminRepository, times(1)).findAll();

    String expectedJson = mapper.writeValueAsString(expectedAdminDTOs);
    String responseString = response.getResponse().getContentAsString();
    assertEquals(expectedJson, responseString);
  }

  @WithMockUser(roles = {"ADMIN"})
  @Test
  public void admin_can_delete_an_admin() throws Exception {
    // arrange

    Admin admin = Admin.builder().email("acdamstedt@gmail.com").build();

    when(adminRepository.findByEmail("acdamstedt@gmail.com")).thenReturn(Optional.of(admin));

    // act
    MvcResult response =
        mockMvc
            .perform(delete("/api/admin/delete?email=acdamstedt@gmail.com").with(csrf()))
            .andExpect(status().isOk())
            .andReturn();

    // assert
    verify(adminRepository, times(1)).findByEmail("acdamstedt@gmail.com");
    verify(adminRepository, times(1)).delete(any());

    Map<String, Object> json = responseToJson(response);
    assertEquals("Admin with id acdamstedt@gmail.com deleted", json.get("message"));
  }

  @WithMockUser(roles = {"ADMIN"})
  @Test
  public void admin_tries_to_delete_non_existant_admin_and_gets_right_error_message()
      throws Exception {
    // arrange

    when(adminRepository.findByEmail("acdamstedt@gmail.com")).thenReturn(Optional.empty());

    // act
    MvcResult response =
        mockMvc
            .perform(delete("/api/admin/delete?email=acdamstedt@gmail.com").with(csrf()))
            .andExpect(status().isNotFound())
            .andReturn();

    // assert
    verify(adminRepository, times(1)).findByEmail("acdamstedt@gmail.com");
    Map<String, Object> json = responseToJson(response);
    assertEquals("Admin with id acdamstedt@gmail.com not found", json.get("message"));
  }

  @WithMockUser(roles = {"ADMIN"})
  @Test
  public void admin_tries_to_delete_an_ADMIN_EMAIL_and_gets_right_error_message() throws Exception {

    Admin admin = Admin.builder().email("acdamstedt@ucsb.edu").build();

    when(adminRepository.findByEmail("acdamstedt@ucsb.edu")).thenReturn(Optional.of(admin));

    // act
    MvcResult response =
        mockMvc
            .perform(delete("/api/admin/delete?email=acdamstedt@ucsb.edu").with(csrf()))
            .andExpect(status().is(403))
            .andReturn();

    String content = response.getResponse().getContentAsString();
    System.out.println("Response content: " + content);

    // assert
    verify(adminRepository, times(1)).findByEmail("acdamstedt@ucsb.edu");
    Map<String, Object> json = responseToJson(response);
    assertEquals("Forbidden to delete an admin from ADMIN_EMAILS list", json.get("message"));
  }
}
