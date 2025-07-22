package edu.ucsb.cs156.frontiers.controllers;

import edu.ucsb.cs156.frontiers.ControllerTestCase;
import edu.ucsb.cs156.frontiers.entities.User;
import edu.ucsb.cs156.frontiers.models.CurrentUser;
import edu.ucsb.cs156.frontiers.repositories.UserRepository;
import edu.ucsb.cs156.frontiers.services.CurrentUserService;
import edu.ucsb.cs156.frontiers.testconfig.MockCurrentUserServiceImpl;
import edu.ucsb.cs156.frontiers.testconfig.TestConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;

import java.util.*;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = GithubController.class)
public class GithubControllerTests extends ControllerTestCase {

    @MockitoBean
    private UserRepository userRepository;

    @Autowired
    private CurrentUserService currentUserService;

    @Test
    public void successfulDisconnect() throws Exception {

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

        MvcResult response = mockMvc.perform(delete("/api/github/disconnect")
                        .with(csrf())
                        .with(oidcLogin().authorities(
                                new SimpleGrantedAuthority("ROLE_USER"),
                                new SimpleGrantedAuthority("ROLE_GITHUB"))))
                .andExpect(status().isOk())
                .andExpect(authenticated().withRoles("USER"))
                .andReturn();

        verify(userRepository).save(userCaptor.capture());

        assertNull(userCaptor.getValue().getGithubId());
        assertNull(userCaptor.getValue().getGithubLogin());

        Map<String, Object> json = responseToJson(response);
        assertEquals("Disconnected from GitHub. You may now log in with a different account.", json.get("message"));
    }
}
