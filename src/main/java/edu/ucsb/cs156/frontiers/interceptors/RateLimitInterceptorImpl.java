package edu.ucsb.cs156.frontiers.interceptors;

import java.io.IOException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

/*
 * Rate limit interceptor for GitHub API requests.
 * This serves as the backstop against repeatedly exceeding the rate limit.
 * If we get a response from GitHub indicating that we've exceeded the rate limit,
 * we throw a RuntimeException to kill the offending job or endpoint.
 */

@Component
@Slf4j
public class RateLimitInterceptorImpl implements RateLimitInterceptor {

  @Override
  public ClientHttpResponse intercept(
      HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {

    if (!request.getURI().getHost().equals("api.github.com")) {
      return execution.execute(request, body);
    }

    try (ClientHttpResponse response = execution.execute(request, body)) {
      List<String> headers = response.getHeaders().get("X-RateLimit-Remaining");
      if (response.getStatusCode() != HttpStatus.TOO_MANY_REQUESTS
          && response.getStatusCode() != HttpStatus.UNAUTHORIZED) {
        return response;
      }
      if (headers == null || headers.isEmpty()) {
        return response;
      }
      try {
        int remaining = Integer.parseInt(headers.getFirst());
        if (remaining == 0) {
          log.error("ERROR: RATE LIMIT EXCEEDED");
          log.error("URI: {}", request.getURI());
          log.error("HEADERS: {}", response.getHeaders());
          log.error("BODY: {}", response.getBody());
          throw new RuntimeException("Rate limit exceeded");
        }
      } catch (NumberFormatException e) {
        return response;
      }
      return response;
    }
  }
}
