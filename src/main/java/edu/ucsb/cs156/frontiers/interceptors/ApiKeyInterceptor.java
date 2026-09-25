package edu.ucsb.cs156.frontiers.interceptors;

import edu.ucsb.cs156.frontiers.config.AllowApiKeyAccess;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
public class ApiKeyInterceptor implements HandlerInterceptor {

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
      throws IOException {

    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth.getAuthorities().stream().noneMatch(a -> a.getAuthority().equals("ROLE_API_KEY"))) {
      return true;
    }
    if (!(handler instanceof HandlerMethod commonHandler)) {
      return true;
    }
    if (AnnotatedElementUtils.findAllMergedAnnotations(
            commonHandler.getMethod(), AllowApiKeyAccess.class)
        .isEmpty()) {
      response.sendError(
          HttpServletResponse.SC_FORBIDDEN,
          "Attempting to gain access to an API endpoint that does not allow an API key");
      return false;
    }
    return true;
  }
}
