package edu.ucsb.cs156.frontiers.controllers;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.ucsb.cs156.frontiers.ControllerTestCase;
import edu.ucsb.cs156.frontiers.entities.User;
import edu.ucsb.cs156.frontiers.repositories.UserRepository;
import edu.ucsb.cs156.frontiers.services.CurrentUserService;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;

@WebMvcTest(controllers = GithubController.class)
public class GithubControllerTests extends ControllerTestCase {

  @MockitoBean private UserRepository userRepository;

  @Autowired private CurrentUserService currentUserService;

  @Test
  public void successfulDisconnect() throws Exception {

    ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

    MvcResult response =
        mockMvc
            .perform(
                delete("/api/github/disconnect")
                    .with(csrf())
                    .with(
                        oidcLogin()
                            .authorities(
                                new SimpleGrantedAuthority("ROLE_USER"),
                                new SimpleGrantedAuthority("ROLE_GITHUB"))))
            .andExpect(status().isOk())
            .andExpect(authenticated().withRoles("USER"))
            .andReturn();

    verify(userRepository).save(userCaptor.capture());

    assertNull(userCaptor.getValue().getGithubId());
    assertNull(userCaptor.getValue().getGithubLogin());

    Map<String, Object> json = responseToJson(response);
    assertEquals(
        "Disconnected from GitHub. You may now log in with a different account.",
        json.get("message"));
  }
}
