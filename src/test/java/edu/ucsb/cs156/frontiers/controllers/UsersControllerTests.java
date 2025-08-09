package edu.ucsb.cs156.frontiers.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import edu.ucsb.cs156.frontiers.ControllerTestCase;
import edu.ucsb.cs156.frontiers.entities.Admin;
import edu.ucsb.cs156.frontiers.entities.Instructor;
import edu.ucsb.cs156.frontiers.entities.User;
import edu.ucsb.cs156.frontiers.models.UserDataDTO;
import edu.ucsb.cs156.frontiers.services.UserDataDTOService;
import edu.ucsb.cs156.frontiers.testconfig.TestConfig;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;

@WebMvcTest(controllers = UsersController.class)
@Import(TestConfig.class)
public class UsersControllerTests extends ControllerTestCase {

  @MockitoBean private UserDataDTOService mockUserDataDTOService;

  @Test
  public void users__logged_out() throws Exception {
    mockMvc.perform(get("/api/admin/users")).andExpect(status().is(403));
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void users__user_logged_in() throws Exception {
    mockMvc.perform(get("/api/admin/users")).andExpect(status().is(403));
  }

  @WithMockUser(roles = {"ADMIN", "USER"})
  @Test
  public void users__admin_logged_in() throws Exception {

    // arrange

    User u1 = User.builder().id(1L).email("djensen@ucsb.edu").build();
    User u2 = User.builder().id(2L).email("cgaucho@ucsb.edu").build();
    User u = currentUserService.getCurrentUser().getUser();

    Admin admin = Admin.builder().email(u1.getEmail()).build();
    Instructor instructor = Instructor.builder().email(u2.getEmail()).build();

    ArrayList<User> expectedUsers = new ArrayList<>();
    expectedUsers.addAll(Arrays.asList(u1, u2, u));

    ArrayList<Admin> expectedAdmins = new ArrayList<>();
    expectedAdmins.add(admin);

    ArrayList<Instructor> expectedInstructors = new ArrayList<>();
    expectedInstructors.add(instructor);

    List<UserDataDTO> userDTOS = new ArrayList<>();
    userDTOS.add(UserDataDTO.from(u1, true, false));
    userDTOS.add(UserDataDTO.from(u2, false, true));
    userDTOS.add(UserDataDTO.from(u, false, false));

    Page<UserDataDTO> page = new PageImpl<>(userDTOS);

    String expectedJson = mapper.writeValueAsString(page);

    when(mockUserDataDTOService.getUserDataDTOs(any(Pageable.class))).thenReturn(page);

    // act

    MvcResult response =
        mockMvc.perform(get("/api/admin/users")).andExpect(status().isOk()).andReturn();

    // assert

    String responseString = response.getResponse().getContentAsString();
    assertEquals(expectedJson, responseString);
  }
}
