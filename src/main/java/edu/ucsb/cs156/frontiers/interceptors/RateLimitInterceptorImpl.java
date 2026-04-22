package edu.ucsb.cs156.frontiers.interceptors;

import edu.ucsb.cs156.frontiers.entities.RateLimitDataPoint;
import edu.ucsb.cs156.frontiers.repositories.RateLimitDataPointRepository;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

@Component
public class RateLimitInterceptorImpl implements RateLimitInterceptor {

  private final RateLimitDataPointRepository rateLimitDataPointRepository;

  public RateLimitInterceptorImpl(RateLimitDataPointRepository rateLimitDataPointRepository) {
    this.rateLimitDataPointRepository = rateLimitDataPointRepository;
  }

  @Override
  public ClientHttpResponse intercept(
      HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {

    if (!request.getURI().getHost().equals("api.github.com")) {
      return execution.execute(request, body);
    }

    if (!request.getURI().getPath().contains("/app/installations/")) {
      return execution.execute(request, body);
    }

    String installationId =
        request.getURI().getPath().replace("/app/installations/", "").replace("/access_tokens", "");

    ClientHttpResponse response = execution.execute(request, body);
    RateLimitDataPoint newPoint =
        RateLimitDataPoint.builder().installationId(installationId).build();

    List<String> headers = response.getHeaders().get("X-RateLimit-Remaining");

    if (headers == null || headers.isEmpty()) {
      return response;
    }
    try {
      long remaining = Long.parseLong(headers.getFirst());
      newPoint.setRemaining(remaining);
      rateLimitDataPointRepository.save(newPoint);
    } catch (NumberFormatException e) {
      return response;
    }
    return response;
  }
}
