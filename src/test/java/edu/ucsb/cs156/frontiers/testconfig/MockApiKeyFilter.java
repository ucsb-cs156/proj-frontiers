package edu.ucsb.cs156.frontiers.testconfig;

import edu.ucsb.cs156.frontiers.config.ApiKeyFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

public class MockApiKeyFilter extends ApiKeyFilter {

  public MockApiKeyFilter() {
    super(null);
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    filterChain.doFilter(request, response);
  }
}
