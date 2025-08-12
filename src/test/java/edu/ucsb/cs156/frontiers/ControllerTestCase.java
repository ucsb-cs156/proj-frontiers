package edu.ucsb.cs156.frontiers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.ucsb.cs156.frontiers.config.SecurityConfig;
import edu.ucsb.cs156.frontiers.interceptors.RoleUpdateInterceptor;
import edu.ucsb.cs156.frontiers.services.CurrentUserService;
import edu.ucsb.cs156.frontiers.services.GrantedAuthoritiesService;
import edu.ucsb.cs156.frontiers.testconfig.TestConfig;
import edu.ucsb.cs156.frontiers.testconfig.TestCourseSecurity;
import jakarta.annotation.PostConstruct;
import java.io.UnsupportedEncodingException;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * This is a base class for all controller test cases. It provides common functionality such as
 * mocking the current user and setting up the MockMvc object.
 */
@ActiveProfiles("test")
@Import({TestConfig.class, SecurityConfig.class, TestCourseSecurity.class})
public abstract class ControllerTestCase {
  @Autowired public CurrentUserService currentUserService;

  @Autowired public GrantedAuthoritiesService grantedAuthoritiesService;

  @Autowired public MockMvc mockMvc;

  @Autowired public ObjectMapper mapper;

  @MockitoBean RoleUpdateInterceptor roleUpdateInterceptor;

  protected Map<String, Object> responseToJson(MvcResult result)
      throws UnsupportedEncodingException, JsonProcessingException {
    String responseString = result.getResponse().getContentAsString();
    return mapper.readValue(responseString, Map.class);
  }

  /**
   * To prevent interference with @WebMvcTest test slices, ControllerTestCase contains a passthrough
   * RoleUpdateInterceptor MockitoBean so that every ControllerTestCase is not required to add an
   * AdminRepository and InstructorRepository MockitoBean.
   *
   * <p>As a MockitoBean, it does not require any dependencies. Since it is an antipattern to mark
   * RoleUpdateInterceptor as a Service (as it is not one), it must be a Component. Components are
   * consistently loaded into the context. Since it requires the Repositories (even if it remains
   * unused), the context would throw an error. As a MockitoBean, the dependencies are ignored.
   */
  @PostConstruct
  void passthroughInterceptor() throws Exception {
    when(roleUpdateInterceptor.preHandle(any(), any(), any())).thenReturn(true);
  }
}
