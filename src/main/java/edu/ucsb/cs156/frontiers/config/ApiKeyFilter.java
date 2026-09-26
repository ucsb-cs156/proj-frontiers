package edu.ucsb.cs156.frontiers.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.ucsb.cs156.frontiers.services.ApiKeyService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
public class ApiKeyFilter extends OncePerRequestFilter {

  ApiKeyService apiKeyService;
  ObjectMapper mapper;

  public ApiKeyFilter(ApiKeyService apiKeyService, ObjectMapper mapper) {
    this.apiKeyService = apiKeyService;
    this.mapper = mapper;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String apiKey = request.getHeader("X-API-KEY");
    if (apiKey == null || apiKey.isEmpty()) {
      filterChain.doFilter(request, response);
      return;
    }
    try {
      ApiKeyToken token = apiKeyService.authenticateKey(apiKey);
      SecurityContext context = SecurityContextHolder.createEmptyContext();
      context.setAuthentication(token);
      SecurityContextHolder.setContext(context);
      filterChain.doFilter(request, response);
    } catch (AccessDeniedException e) {
      Map<String, String> responseMessage = Map.of("message", e.getMessage());
      response.setStatus(HttpServletResponse.SC_FORBIDDEN);
      response.getWriter().write(mapper.writeValueAsString(responseMessage));
    }
  }
}
