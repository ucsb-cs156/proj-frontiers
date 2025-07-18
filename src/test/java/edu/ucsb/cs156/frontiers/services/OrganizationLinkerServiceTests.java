package edu.ucsb.cs156.frontiers.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.errors.InvalidInstallationTypeException;
import edu.ucsb.cs156.frontiers.services.wiremock.WiremockService;
import edu.ucsb.cs156.frontiers.testconfig.TestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureDataJpa;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.client.MockRestServiceServer;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

@RestClientTest(OrganizationLinkerService.class)
@Import({TestConfig.class})
public class OrganizationLinkerServiceTests {
    @Autowired
    private OrganizationLinkerService organizationLinkerService;

    @MockitoBean
    private JwtService jwtService;

    @Autowired
    private MockRestServiceServer mockRestServiceServer;

    @MockitoBean
    private WiremockService wiremockService;

    @Test
    public void testRedirectUrl() throws NoSuchAlgorithmException, InvalidKeySpecException, JsonProcessingException {
        String appUrl = "https://github.com/apps/octoapp";
        String apiResponse = String.format("""
                {
                  "id": 1,
                  "slug": "octoapp",
                  "client_id": "Iv1.ab1112223334445c",
                  "node_id": "MDExOkludGVncmF0aW9uMQ==",
                  "html_url": "%s"
                }
                """, appUrl);

        doReturn("definitely.real.jwt").when(jwtService).getJwt();
        mockRestServiceServer.expect(requestTo("https://api.github.com/app"))
                .andExpect(header("Authorization", "Bearer definitely.real.jwt" ))
                .andExpect(header("Accept", "application/vnd.github+json"))
                .andExpect(header("X-GitHub-Api-Version", "2022-11-28"))
                .andRespond(withSuccess(apiResponse, MediaType.APPLICATION_JSON));

        String actualUrl = organizationLinkerService.getRedirectUrl();
        assertEquals(appUrl, actualUrl);
    }

    @Test
    public void testGetOrgName() throws NoSuchAlgorithmException, InvalidKeySpecException, JsonProcessingException {
        String orgName = "ucsb-cs156";
        String apiResponse = String.format("""
                {
                  "id": 1,
                  "account":{
                    "login" : "%s",
                    "type" : "Organization"
                  }
                }
                """, orgName);

        doReturn("definitely.real.jwt").when(jwtService).getJwt();
        mockRestServiceServer.expect(requestTo("https://api.github.com/app/installations/123456"))
                .andExpect(header("Authorization", "Bearer definitely.real.jwt" ))
                .andExpect(header("Accept", "application/vnd.github+json"))
                .andExpect(header("X-GitHub-Api-Version", "2022-11-28"))
                .andRespond(withSuccess(apiResponse, MediaType.APPLICATION_JSON));

        String actualOrgName = organizationLinkerService.getOrgName("123456");
        assertEquals(orgName, actualOrgName);
    }

    @Test
    public void testNotAnOrganization() throws NoSuchAlgorithmException, InvalidKeySpecException, JsonProcessingException {
        String orgName = "githubuser1";
        String apiResponse = String.format("""
                {
                  "id": 1,
                  "account":{
                    "login" : "%s",
                    "type" : "User"
                  }
                }
                """, orgName);

        String expectedMessage = "Invalid installation type: User. Frontiers can only be linked to organizations";

        doReturn("definitely.real.jwt").when(jwtService).getJwt();
        mockRestServiceServer.expect(requestTo("https://api.github.com/app/installations/123456"))
                .andExpect(header("Authorization", "Bearer definitely.real.jwt" ))
                .andExpect(header("Accept", "application/vnd.github+json"))
                .andExpect(header("X-GitHub-Api-Version", "2022-11-28"))
                .andRespond(withSuccess(apiResponse, MediaType.APPLICATION_JSON));

        Exception thrownException = assertThrows(InvalidInstallationTypeException.class, () -> {
            organizationLinkerService.getOrgName("123456");
        });

        assertEquals(expectedMessage, thrownException.getMessage());
    }

    @Test
    public void earlyReturnOnNoInstallationId() throws Exception{
        Course course = Course.builder().orgName("ucsb-cs156").build();
        organizationLinkerService.unenrollOrganization(course);
        mockRestServiceServer.verify();
    }

    @Test
    public void earlyReturnOnNoOrgName() throws Exception{
        Course course = Course.builder().build();
        organizationLinkerService.unenrollOrganization(course);
        mockRestServiceServer.verify();
    }

    @Test
    public void testUnenrollOrganization() throws Exception {
        Course course = Course.builder().orgName("ucsb-cs156").installationId("123456").build();

        doReturn("definitely.real.jwt").when(jwtService).getJwt();
        mockRestServiceServer.expect(requestTo("https://api.github.com/app/installations/123456"))
                .andExpect(method(HttpMethod.DELETE))
                .andExpect(header("Authorization", "Bearer definitely.real.jwt" ))
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
        mockRestServiceServer.expect(requestTo("https://api.github.com/app/installations/123456"))
                .andExpect(method(HttpMethod.DELETE))
                .andExpect(header("Authorization", "Bearer definitely.real.jwt" ))
                .andExpect(header("Accept", "application/vnd.github+json"))
                .andExpect(header("X-GitHub-Api-Version", "2022-11-28"))
                .andRespond(withResourceNotFound());
        organizationLinkerService.unenrollOrganization(course);
        mockRestServiceServer.verify();
    }

}
