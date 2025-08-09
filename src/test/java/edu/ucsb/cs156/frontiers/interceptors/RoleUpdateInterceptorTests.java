package edu.ucsb.cs156.frontiers.interceptors;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.ucsb.cs156.frontiers.config.SecurityConfig;
import edu.ucsb.cs156.frontiers.controllers.DummyController;
import edu.ucsb.cs156.frontiers.repositories.AdminRepository;
import edu.ucsb.cs156.frontiers.repositories.InstructorRepository;
import edu.ucsb.cs156.frontiers.testconfig.TestConfig;
import edu.ucsb.cs156.frontiers.testconfig.TestCourseSecurity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest({RoleUpdateInterceptor.class, DummyController.class})
@Import({TestConfig.class, SecurityConfig.class, TestCourseSecurity.class})
public class RoleUpdateInterceptorTests {

  @MockitoBean AdminRepository adminRepository;

  @MockitoBean InstructorRepository instructorRepository;

  @Autowired MockMvc mockMvc;

  @Test
  public void user_not_admin_or_instructor_and_no_role_update_by_interceptor() throws Exception {
    when(adminRepository.existsByEmail("testuser@ucsb.edu")).thenReturn(false);
    when(instructorRepository.existsByEmail("testuser@ucsb.edu")).thenReturn(false);

    mockMvc
        .perform(
            get("/dummycontroller/interceptorTest")
                .with(
                    oidcLogin()
                        .userInfoToken(token -> token.email("testuser@ucsb.edu"))
                        .authorities(new SimpleGrantedAuthority("ROLE_USER"))))
        .andExpect(authenticated().withRoles("USER"));
  }

  @Test
  public void user_is_admin_role_update_by_interceptor() throws Exception {
    when(adminRepository.existsByEmail("testuser@ucsb.edu")).thenReturn(true);
    when(instructorRepository.existsByEmail("testuser@ucsb.edu")).thenReturn(false);

    mockMvc
        .perform(
            get("/dummycontroller/interceptorTest")
                .with(
                    oidcLogin()
                        .userInfoToken(token -> token.email("testuser@ucsb.edu"))
                        .authorities(new SimpleGrantedAuthority("ROLE_USER"))))
        .andExpect(authenticated().withRoles("USER", "ADMIN"));
  }

  @Test
  public void user_is_instructor_role_update_by_interceptor() throws Exception {
    when(adminRepository.existsByEmail("testuser@ucsb.edu")).thenReturn(false);
    when(instructorRepository.existsByEmail("testuser@ucsb.edu")).thenReturn(true);

    mockMvc
        .perform(
            get("/dummycontroller/interceptorTest")
                .with(
                    oidcLogin()
                        .userInfoToken(token -> token.email("testuser@ucsb.edu"))
                        .authorities(new SimpleGrantedAuthority("ROLE_USER"))))
        .andExpect(authenticated().withRoles("USER", "INSTRUCTOR"));
  }

  @Test
  public void admin_role_removed_when_user_loses_admin_status() throws Exception {
    when(adminRepository.existsByEmail("testuser@ucsb.edu")).thenReturn(false);
    when(instructorRepository.existsByEmail("testuser@ucsb.edu")).thenReturn(false);

    mockMvc
        .perform(
            get("/dummycontroller/interceptorTest")
                .with(
                    oidcLogin()
                        .userInfoToken(token -> token.email("testuser@ucsb.edu"))
                        .authorities(
                            new SimpleGrantedAuthority("ROLE_USER"),
                            new SimpleGrantedAuthority("ROLE_ADMIN"))))
        .andExpect(authenticated().withRoles("USER"));
  }

  @Test
  public void instructor_role_removed_when_user_loses_instructor_status() throws Exception {
    when(adminRepository.existsByEmail("testuser@ucsb.edu")).thenReturn(false);
    when(instructorRepository.existsByEmail("testuser@ucsb.edu")).thenReturn(false);

    mockMvc
        .perform(
            get("/dummycontroller/interceptorTest")
                .with(
                    oidcLogin()
                        .userInfoToken(token -> token.email("testuser@ucsb.edu"))
                        .authorities(
                            new SimpleGrantedAuthority("ROLE_USER"),
                            new SimpleGrantedAuthority("ROLE_INSTRUCTOR"))))
        .andExpect(authenticated().withRoles("USER"));
  }

  @Test
  public void other_authorities_preserved_while_updating_roles() throws Exception {
    when(adminRepository.existsByEmail("testuser@ucsb.edu")).thenReturn(true);
    when(instructorRepository.existsByEmail("testuser@ucsb.edu")).thenReturn(false);

    mockMvc
        .perform(
            get("/dummycontroller/interceptorTest")
                .with(
                    oidcLogin()
                        .userInfoToken(token -> token.email("testuser@ucsb.edu"))
                        .authorities(
                            new SimpleGrantedAuthority("ROLE_USER"),
                            new SimpleGrantedAuthority("ROLE_CUSTOM"))))
        .andExpect(authenticated().withRoles("USER", "ADMIN", "CUSTOM"))
        .andExpect(status().isOk())
        .andExpect(content().string("OK"));
  }

  @Test
  @WithMockUser
  public void non_oauth_authentication_handled() throws Exception {
    mockMvc
        .perform(get("/dummycontroller/interceptorTest"))
        .andExpect(authenticated().withRoles("USER"));
  }

  @Test
  public void skip_oauth2() throws Exception {
    mockMvc
        .perform(
            get("/dummycontroller/interceptorTest")
                .with(oauth2Login().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
        .andExpect(authenticated().withRoles("USER"));
  }
}
