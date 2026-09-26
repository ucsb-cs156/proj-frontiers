package edu.ucsb.cs156.frontiers.interceptors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.ucsb.cs156.frontiers.config.SecurityConfig;
import edu.ucsb.cs156.frontiers.controllers.CustomErrorController;
import edu.ucsb.cs156.frontiers.controllers.DummyController;
import edu.ucsb.cs156.frontiers.testconfig.TestConfig;
import edu.ucsb.cs156.frontiers.testconfig.TestCourseSecurity;
import jakarta.servlet.RequestDispatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/*
 * Tests for ApiKeyInterceptor
 * NOTE: CustomErrorController is needed because adding it ensures that error messages actually propagate forward.
 * Otherwise, it's possible that in production, the interceptor will be called again causing an error. However, that
 * wouldn't appear in these tests.
 */
@WebMvcTest({ApiKeyInterceptor.class, DummyController.class, CustomErrorController.class})
@Import({TestConfig.class, SecurityConfig.class, TestCourseSecurity.class})
@ActiveProfiles("test")
public class ApiKeyInterceptorTests {

  @Autowired MockMvc mockMvc;

  @MockitoBean private RoleUpdateInterceptor disabledInterceptor;

  @BeforeEach
  public void setup() {
    when(disabledInterceptor.preHandle(any(), any(), any())).thenReturn(true);
  }

  @Test
  public void handler_ignores_unauthenticated_request() throws Exception {
    mockMvc.perform(get("/dummycontroller").param("id", "1")).andExpect(status().isOk());
  }

  @Test
  @WithMockUser
  public void handler_ignores_authenticated_non_api_key_request() throws Exception {
    mockMvc.perform(get("/dummycontroller").param("id", "1")).andExpect(status().isOk());
  }

  @Test
  @WithMockUser(roles = "API_KEY")
  public void handler_ignores_static_file() throws Exception {
    mockMvc.perform(get("/static/fake_file")).andExpect(status().isNotFound());
  }

  @Test
  @WithMockUser(roles = "API_KEY")
  public void handler_blocks_api_key_request_to_unannotated_endpoint() throws Exception {
    MvcResult result =
        mockMvc
            .perform(get("/dummycontroller").param("id", "1"))
            .andExpect(status().isForbidden())
            .andExpect(
                jsonPath("$.message")
                    .value(
                        "Attempting to gain access to an API endpoint that does not allow an API key"))
            .andReturn();
    assertFalse(result.getResponse().getContentAsString().contains("String1"));
  }

  @Test
  @WithMockUser(roles = "API_KEY")
  public void handler_allows_api_access_to_annotated_endpoint() throws Exception {
    mockMvc
        .perform(get("/dummycontroller/apikey"))
        .andExpect(status().isOk())
        .andExpect(content().string("OK"));
  }

  /*
   * Directly call error controller to ensure that it is ignored;
   * MockMvc will not call the actual error controller when simulating a normal request.
   */
  @Test
  @WithMockUser(roles = "API_KEY")
  public void handler_ignores_error_controller() throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                get("/error")
                    .requestAttr(RequestDispatcher.ERROR_STATUS_CODE, HttpStatus.NOT_FOUND.value())
                    .requestAttr(RequestDispatcher.ERROR_REQUEST_URI, "/fake-uri"))
            .andExpect(status().isNotFound())
            .andReturn();
    assertFalse(result.getResponse().getContentAsString().contains("String1"));
  }
}
