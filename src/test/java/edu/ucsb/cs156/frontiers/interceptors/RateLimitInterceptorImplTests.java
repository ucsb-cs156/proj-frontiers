package edu.ucsb.cs156.frontiers.interceptors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import edu.ucsb.cs156.frontiers.entities.RateLimitDataPoint;
import edu.ucsb.cs156.frontiers.repositories.RateLimitDataPointRepository;
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
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

@RestClientTest({RateLimitInterceptorImpl.class})
public class RateLimitInterceptorImplTests {

  @MockitoBean RateLimitDataPointRepository rateLimitDataPointRepository;

  private MockRestServiceServer mockServer;

  @Autowired private RestTemplateBuilder restTemplateBuilder;

  @Autowired private RateLimitInterceptor interceptor;

  private RestTemplate template;

  @BeforeEach
  public void setup() {
    template = restTemplateBuilder.additionalInterceptors(interceptor).build();
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
    verifyNoInteractions(rateLimitDataPointRepository);
  }

  @Test
  public void properly_saves_data() {
    HttpHeaders responseHeaders = new HttpHeaders();
    responseHeaders.add("X-RateLimit-Remaining", "1");
    HttpEntity<String> entity = new HttpEntity<>(null);

    String url = "https://api.github.com/app/installations/" + "123456" + "/access_tokens";

    RateLimitDataPoint expected =
        RateLimitDataPoint.builder().installationId("123456").remaining(1L).build();

    mockServer.expect(requestTo(url)).andRespond(withSuccess().headers(responseHeaders));

    template.exchange(url, HttpMethod.POST, entity, String.class);
    verify(rateLimitDataPointRepository).save(expected);
  }

  @Test
  public void no_implosion_on_no_header() {
    HttpHeaders responseHeaders = new HttpHeaders();
    HttpEntity<String> entity = new HttpEntity<>(null);

    String url = "https://api.github.com/app/installations/" + "123456" + "/access_tokens";

    mockServer.expect(requestTo(url)).andRespond(withSuccess().headers(responseHeaders));

    ResponseEntity<String> response = template.exchange(url, HttpMethod.POST, entity, String.class);
    verifyNoInteractions(rateLimitDataPointRepository);
    assertEquals(HttpStatus.OK, response.getStatusCode());
  }

  @Test
  public void no_implosion_on_empty_header() {
    HttpHeaders responseHeaders = new HttpHeaders();
    responseHeaders.put("X-RateLimit-Remaining", new ArrayList<String>());
    HttpEntity<String> entity = new HttpEntity<>(null);

    String url = "https://api.github.com/app/installations/" + "123456" + "/access_tokens";

    mockServer.expect(requestTo(url)).andRespond(withSuccess().headers(responseHeaders));

    ResponseEntity<String> response = template.exchange(url, HttpMethod.POST, entity, String.class);
    verifyNoInteractions(rateLimitDataPointRepository);
    assertEquals(HttpStatus.OK, response.getStatusCode());
  }

  @Test
  public void no_implosion_on_not_number() {
    HttpHeaders responseHeaders = new HttpHeaders();
    responseHeaders.add("X-RateLimit-Remaining", "banana");
    HttpEntity<String> entity = new HttpEntity<>(null);

    String url = "https://api.github.com/app/installations/" + "123456" + "/access_tokens";

    mockServer.expect(requestTo(url)).andRespond(withSuccess().headers(responseHeaders));

    ResponseEntity<String> response = template.exchange(url, HttpMethod.POST, entity, String.class);
    verifyNoInteractions(rateLimitDataPointRepository);
    assertEquals(HttpStatus.OK, response.getStatusCode());
  }

  @Test
  public void on_implosion_not_app_installations() {
    HttpHeaders responseHeaders = new HttpHeaders();
    HttpEntity<String> entity = new HttpEntity<>(null);

    String url = "https://api.github.com/api/repos/ucsb-cs156/proj-frontiers";

    mockServer.expect(requestTo(url)).andRespond(withSuccess().headers(responseHeaders));

    ResponseEntity<String> response = template.exchange(url, HttpMethod.POST, entity, String.class);
    verifyNoInteractions(rateLimitDataPointRepository);
    assertEquals(HttpStatus.OK, response.getStatusCode());
  }
}
