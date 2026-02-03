package edu.ucsb.cs156.frontiers.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.errors.InvalidInstallationTypeException;
import edu.ucsb.cs156.frontiers.models.CourseWarning;
import edu.ucsb.cs156.frontiers.services.wiremock.WiremockService;
import edu.ucsb.cs156.frontiers.testconfig.TestConfig;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.client.MockRestServiceServer;

@RestClientTest(OrganizationLinkerService.class)
@Import({TestConfig.class})
public class OrganizationLinkerServiceTests {
  @Autowired private OrganizationLinkerService organizationLinkerService;

  @MockitoBean private JwtService jwtService;

  @Autowired private MockRestServiceServer mockRestServiceServer;

  @MockitoBean private WiremockService wiremockService;

  @MockitoBean DateTimeProvider provider;

  @Test
  public void testRedirectUrl()
      throws NoSuchAlgorithmException, InvalidKeySpecException, JsonProcessingException {
    String appUrl = "https://github.com/apps/octoapp";
    String apiResponse =
        String.format(
            """
                {
                  "id": 1,
                  "slug": "octoapp",
                  "client_id": "Iv1.ab1112223334445c",
                  "node_id": "MDExOkludGVncmF0aW9uMQ==",
                  "html_url": "%s"
                }
                """,
            appUrl);

    doReturn("definitely.real.jwt").when(jwtService).getJwt();
    mockRestServiceServer
        .expect(requestTo("https://api.github.com/app"))
        .andExpect(header("Authorization", "Bearer definitely.real.jwt"))
        .andExpect(header("Accept", "application/vnd.github+json"))
        .andExpect(header("X-GitHub-Api-Version", "2022-11-28"))
        .andRespond(withSuccess(apiResponse, MediaType.APPLICATION_JSON));

    String actualUrl = organizationLinkerService.getRedirectUrl();
    assertEquals(appUrl, actualUrl);
  }

  @Test
  public void testGetOrgName()
      throws NoSuchAlgorithmException, InvalidKeySpecException, JsonProcessingException {
    String orgName = "ucsb-cs156";
    String apiResponse =
        String.format(
            """
                {
                  "id": 1,
                  "account":{
                    "login" : "%s",
                    "type" : "Organization"
                  }
                }
                """,
            orgName);

    doReturn("definitely.real.jwt").when(jwtService).getJwt();
    mockRestServiceServer
        .expect(requestTo("https://api.github.com/app/installations/123456"))
        .andExpect(header("Authorization", "Bearer definitely.real.jwt"))
        .andExpect(header("Accept", "application/vnd.github+json"))
        .andExpect(header("X-GitHub-Api-Version", "2022-11-28"))
        .andRespond(withSuccess(apiResponse, MediaType.APPLICATION_JSON));

    String actualOrgName = organizationLinkerService.getOrgName("123456");
    assertEquals(orgName, actualOrgName);
  }

  @Test
  public void testNotAnOrganization()
      throws NoSuchAlgorithmException, InvalidKeySpecException, JsonProcessingException {
    String orgName = "githubuser1";
    String apiResponse =
        String.format(
            """
                {
                  "id": 1,
                  "account":{
                    "login" : "%s",
                    "type" : "User"
                  }
                }
                """,
            orgName);

    String expectedMessage =
        "Invalid installation type: User. Frontiers can only be linked to organizations";

    doReturn("definitely.real.jwt").when(jwtService).getJwt();
    mockRestServiceServer
        .expect(requestTo("https://api.github.com/app/installations/123456"))
        .andExpect(header("Authorization", "Bearer definitely.real.jwt"))
        .andExpect(header("Accept", "application/vnd.github+json"))
        .andExpect(header("X-GitHub-Api-Version", "2022-11-28"))
        .andRespond(withSuccess(apiResponse, MediaType.APPLICATION_JSON));

    Exception thrownException =
        assertThrows(
            InvalidInstallationTypeException.class,
            () -> {
              organizationLinkerService.getOrgName("123456");
            });

    assertEquals(expectedMessage, thrownException.getMessage());
  }

  @Test
  public void earlyReturnOnNoInstallationId() throws Exception {
    Course course = Course.builder().orgName("ucsb-cs156").build();
    organizationLinkerService.unenrollOrganization(course);
    mockRestServiceServer.verify();
  }

  @Test
  public void earlyReturnOnNoOrgName() throws Exception {
    Course course = Course.builder().build();
    organizationLinkerService.unenrollOrganization(course);
    mockRestServiceServer.verify();
  }

  @Test
  public void testUnenrollOrganization() throws Exception {
    Course course = Course.builder().orgName("ucsb-cs156").installationId("123456").build();

    doReturn("definitely.real.jwt").when(jwtService).getJwt();
    mockRestServiceServer
        .expect(requestTo("https://api.github.com/app/installations/123456"))
        .andExpect(method(HttpMethod.DELETE))
        .andExpect(header("Authorization", "Bearer definitely.real.jwt"))
        .andExpect(header("Accept", "application/vnd.github+json"))
        .andExpect(header("X-GitHub-Api-Version", "2022-11-28"))
        .andRespond(withNoContent());
    organizationLinkerService.unenrollOrganization(course);
    mockRestServiceServer.verify();
  }

  @Test
  public void testNotInstalled() throws Exception {
    Course course = Course.builder().orgName("ucsb-cs156").installationId("123456").build();

    doReturn("definitely.real.jwt").when(jwtService).getJwt();
    mockRestServiceServer
        .expect(requestTo("https://api.github.com/app/installations/123456"))
        .andExpect(method(HttpMethod.DELETE))
        .andExpect(header("Authorization", "Bearer definitely.real.jwt"))
        .andExpect(header("Accept", "application/vnd.github+json"))
        .andExpect(header("X-GitHub-Api-Version", "2022-11-28"))
        .andRespond(withResourceNotFound());
    organizationLinkerService.unenrollOrganization(course);
    mockRestServiceServer.verify();
  }

  @Test
  public void test_no_warning_when_old() throws Exception {
    Course course = Course.builder().orgName("ucsb-cs156").installationId("12345").build();
    when(provider.getNow())
        .thenReturn(Optional.of(ZonedDateTime.of(2025, 3, 11, 0, 0, 0, 0, ZoneId.of("UTC"))));
    doReturn("definitely.real.jwt").when(jwtService).getInstallationToken(eq(course));
    String apiResponse =
        """
            {
              "created_at": "2024-10-11T04:33:35Z"
            }
            """;
    mockRestServiceServer
        .expect(requestTo("https://api.github.com/orgs/ucsb-cs156"))
        .andExpect(method(HttpMethod.GET))
        .andExpect(header("Authorization", "Bearer definitely.real.jwt"))
        .andExpect(header("Accept", "application/vnd.github+json"))
        .andExpect(header("X-GitHub-Api-Version", "2022-11-28"))
        .andRespond(withSuccess(apiResponse, MediaType.APPLICATION_JSON));

    CourseWarning warning = organizationLinkerService.checkCourseWarnings(course);
    assertFalse(warning.showOrganizationAgeWarning());
  }

  @Test
  public void test_warning_when_new() throws Exception {
    Course course = Course.builder().orgName("ucsb-cs156").installationId("12345").build();
    when(provider.getNow())
        .thenReturn(Optional.of(ZonedDateTime.of(2025, 3, 11, 0, 0, 0, 0, ZoneId.of("UTC"))));
    doReturn("definitely.real.jwt").when(jwtService).getInstallationToken(eq(course));
    String apiResponse =
        """
            {
              "created_at": "2025-03-08T04:33:35Z"
            }
            """;
    mockRestServiceServer
        .expect(requestTo("https://api.github.com/orgs/ucsb-cs156"))
        .andExpect(method(HttpMethod.GET))
        .andExpect(header("Authorization", "Bearer definitely.real.jwt"))
        .andExpect(header("Accept", "application/vnd.github+json"))
        .andExpect(header("X-GitHub-Api-Version", "2022-11-28"))
        .andRespond(withSuccess(apiResponse, MediaType.APPLICATION_JSON));

    CourseWarning warning = organizationLinkerService.checkCourseWarnings(course);
    assertTrue(warning.showOrganizationAgeWarning());
  }

  @Test
  public void no_rest_service_calls_when_not_installed() throws Exception {
    Course course = Course.builder().orgName("ucsb-cs156").build();
    CourseWarning warning = organizationLinkerService.checkCourseWarnings(course);
    assertFalse(warning.showOrganizationAgeWarning());
    mockRestServiceServer.verify();
  }

  @Test
  public void no_rest_service_calls_when_not_installed_blank() throws Exception {
    Course course = Course.builder().build();
    CourseWarning warning = organizationLinkerService.checkCourseWarnings(course);
    assertFalse(warning.showOrganizationAgeWarning());
    mockRestServiceServer.verify();
  }
}
