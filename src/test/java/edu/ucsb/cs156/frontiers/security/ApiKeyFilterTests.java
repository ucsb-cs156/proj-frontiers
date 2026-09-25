package edu.ucsb.cs156.frontiers.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.ucsb.cs156.frontiers.ControllerTestCase;
import edu.ucsb.cs156.frontiers.config.ApiKeyFilter;
import edu.ucsb.cs156.frontiers.config.ApiKeyToken;
import edu.ucsb.cs156.frontiers.controllers.DummyController;
import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.entities.CourseApiKey;
import edu.ucsb.cs156.frontiers.entities.User;
import edu.ucsb.cs156.frontiers.services.ApiKeyService;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@WebMvcTest(DummyController.class)
public class ApiKeyFilterTests extends ControllerTestCase {

  @MockitoBean private ApiKeyService apiKeyService;

  @TestConfiguration
  public static class ConfigClass {
    @Bean
    @Primary
    public ApiKeyFilter under_test_api_key_filter(ApiKeyService apiKeyService) {
      return new ApiKeyFilter(apiKeyService);
    }

    @Bean
    public FilterRegistrationBean<ApiKeyFilter> disableFilterRegistration(ApiKeyFilter filter) {
      FilterRegistrationBean<ApiKeyFilter> registrationBean = new FilterRegistrationBean<>(filter);
      registrationBean.setEnabled(false);
      return registrationBean;
    }
  }

  @Test
  public void csrf_disabled_with_correct_api_key() throws Exception {
    Course course = Course.builder().id(1L).build();
    CourseApiKey key = CourseApiKey.builder().course(course).build();
    User user = User.builder().build();
    ApiKeyToken token =
        new ApiKeyToken(key, user, Set.of(new SimpleGrantedAuthority("ROLE_API_KEY")));
    when(apiKeyService.authenticateKey(eq("12345"))).thenReturn(token);

    mockMvc
        .perform(post("/dummycontroller/apikey/post").header("X-API-KEY", "12345"))
        .andExpect(status().isOk())
        .andExpect(content().string("OK"))
        .andExpect(authenticated().withRoles("API_KEY"));
  }

  @Test
  public void csrf_enforced_with_non_api_key() throws Exception {
    mockMvc.perform(post("/dummycontroller/apikey/post")).andExpect(status().isForbidden());
  }

  @Test
  public void filter_ignores_no_api_key_header_requests() throws Exception {
    mockMvc
        .perform(post("/dummycontroller/apikey/post").with(csrf()).header("X-API-KEY", ""))
        .andExpect(status().isOk())
        .andExpect(content().string("OK"))
        .andExpect(unauthenticated());

    mockMvc
        .perform(post("/dummycontroller/apikey/post").with(csrf()))
        .andExpect(status().isOk())
        .andExpect(content().string("OK"))
        .andExpect(unauthenticated());
    verify(apiKeyService, never()).authenticateKey(any());
  }

  @Test
  public void communicates_access_denied_reason() throws Exception {

    when(apiKeyService.authenticateKey(any()))
        .thenThrow(new AccessDeniedException("Access denied"));

    String errorMessage =
        mockMvc
            .perform(post("/dummycontroller/apikey/post").header("X-API-KEY", "12345"))
            .andExpect(status().isForbidden())
            .andReturn()
            .getResponse()
            .getErrorMessage();

    assertEquals("Access denied", errorMessage);
  }
}
