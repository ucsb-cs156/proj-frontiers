package edu.ucsb.cs156.frontiers.interceptors;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withTooManyRequests;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withUnauthorizedRequest;

import java.io.IOException;
import java.util.ArrayList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.mock.http.client.MockClientHttpResponse;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

@RestClientTest({RateLimitInterceptorImpl.class})
public class RateLimitInterceptorImplTests {

  private MockRestServiceServer mockServer;

  @Autowired private RestTemplateBuilder restTemplateBuilder;

  @Autowired private RateLimitInterceptor interceptor;

  private RestTemplate template;

  @BeforeEach
  public void setup() {
    // prevent Spring from throwing a RestClientException on Too Many Requests or Unauthorized
    template =
        restTemplateBuilder
            .additionalInterceptors(interceptor)
            .errorHandler(error -> false)
            .build();
    mockServer = MockRestServiceServer.bindTo(template).build();
  }

  @Test
  public void does_not_interfere() {
    HttpEntity<String> entity = new HttpEntity<>(null);

    mockServer
        .expect(requestTo("http://localhost:8080/dummycontroller/test"))
        .andRespond(withSuccess());

    template.exchange(
        "http://localhost:8080/dummycontroller/test", HttpMethod.GET, entity, String.class);
  }

  @Test
  public void properly_stops() throws IOException {
    HttpHeaders responseHeaders = new HttpHeaders();
    responseHeaders.add("X-RateLimit-Remaining", "0");
    HttpEntity<String> entity = new HttpEntity<>(null);

    String url = "https://api.github.com/app/installations/" + "123456" + "/access_tokens";

    ClientHttpResponse spy = spy(MockClientHttpResponse.class);
    when(spy.getStatusCode()).thenReturn(HttpStatus.TOO_MANY_REQUESTS);
    when(spy.getHeaders()).thenReturn(responseHeaders);

    mockServer.expect(requestTo(url)).andRespond(request -> spy);

    assertThrows(
        RuntimeException.class,
        () -> template.exchange(url, HttpMethod.POST, entity, String.class));

    verify(spy, times(1)).close();
  }

  @Test
  public void no_implosion_on_no_header() {
    HttpHeaders responseHeaders = new HttpHeaders();
    HttpEntity<String> entity = new HttpEntity<>(null);

    String url = "https://api.github.com/app/installations/" + "123456" + "/access_tokens";

    mockServer.expect(requestTo(url)).andRespond(withTooManyRequests().headers(responseHeaders));

    assertDoesNotThrow(() -> template.exchange(url, HttpMethod.POST, entity, String.class));
  }

  @Test
  public void no_implosion_on_empty_header() {
    HttpHeaders responseHeaders = new HttpHeaders();
    responseHeaders.put("X-RateLimit-Remaining", new ArrayList<String>());
    HttpEntity<String> entity = new HttpEntity<>(null);

    String url = "https://api.github.com/app/installations/" + "123456" + "/access_tokens";

    mockServer
        .expect(requestTo(url))
        .andRespond(withUnauthorizedRequest().headers(responseHeaders));

    assertDoesNotThrow(() -> template.exchange(url, HttpMethod.POST, entity, String.class));
  }

  @Test
  public void no_implosion_on_not_number() {
    HttpHeaders responseHeaders = new HttpHeaders();
    responseHeaders.add("X-RateLimit-Remaining", "banana");
    HttpEntity<String> entity = new HttpEntity<>(null);

    String url = "https://api.github.com/app/installations/" + "123456" + "/access_tokens";

    mockServer
        .expect(requestTo(url))
        .andRespond(withUnauthorizedRequest().headers(responseHeaders));

    assertDoesNotThrow(() -> template.exchange(url, HttpMethod.POST, entity, String.class));
  }

  @Test
  public void no_implosion_on_remaining_rate_limit() {
    HttpHeaders responseHeaders = new HttpHeaders();
    responseHeaders.add("X-RateLimit-Remaining", "20");
    HttpEntity<String> entity = new HttpEntity<>(null);

    String url = "https://api.github.com/app/installations/" + "123456" + "/access_tokens";

    mockServer
        .expect(requestTo(url))
        .andRespond(withUnauthorizedRequest().headers(responseHeaders));

    assertDoesNotThrow(() -> template.exchange(url, HttpMethod.POST, entity, String.class));
  }

  @Test
  public void no_implosion_on_success() {
    HttpHeaders responseHeaders = new HttpHeaders();
    responseHeaders.add("X-RateLimit-Remaining", "0");
    HttpEntity<String> entity = new HttpEntity<>(null);

    String url = "https://api.github.com/app/installations/" + "123456" + "/access_tokens";

    mockServer.expect(requestTo(url)).andRespond(withSuccess().headers(responseHeaders));

    assertDoesNotThrow(() -> template.exchange(url, HttpMethod.POST, entity, String.class));
  }
}
