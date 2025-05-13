package edu.ucsb.cs156.frontiers.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.models.OrgMember;
import edu.ucsb.cs156.frontiers.services.wiremock.WiremockService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureDataJpa;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.client.MockRestServiceServer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

@RestClientTest(OrganizationMemberService.class)
@AutoConfigureDataJpa
public class OrganizationMemberServiceTests {

    @Autowired
    private OrganizationMemberService organizationMemberService;

    @Autowired
    private MockRestServiceServer mockServer;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private WiremockService wiremockService;

    private Course testCourse;
    private static final String TEST_TOKEN = "test-token";
    private static final String TEST_ORG = "test-org";

    @BeforeEach
    void setUp() throws Exception {
        testCourse = Course.builder()
                .orgName(TEST_ORG)
                .installationId("123")
                .build();

        when(jwtService.getInstallationToken(any(Course.class))).thenReturn(TEST_TOKEN);
    }

    @Test
    void testGetOrganizationMembers_SinglePage() throws Exception {
        // Prepare test data
        List<OrgMember> expectedMembers = List.of(
            OrgMember.builder().githubId(1).githubLogin("user1").build(),
            OrgMember.builder().githubId(2).githubLogin("user2").build()
        );
        String jsonResponse = objectMapper.writeValueAsString(expectedMembers);

        // Setup mock server
        mockServer.expect(requestTo("https://api.github.com/orgs/" + TEST_ORG + "/members"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer " + TEST_TOKEN))
                .andExpect(header("Accept", "application/vnd.github+json"))
                .andExpect(header("X-GitHub-Api-Version", "2022-11-28"))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(jsonResponse));

        // Execute test
        Iterable<OrgMember> result = organizationMemberService.getOrganizationMembers(testCourse);

        // Verify results
        mockServer.verify();
        assertIterableEquals(expectedMembers, result);
    }

    @Test
    void testGetOrganizationMembers_MultiplePages() throws Exception {
        // Prepare test data for two pages
        OrgMember orgMember1 = OrgMember.builder().githubId(1).githubLogin("user1").build();
        OrgMember orgMember2 = OrgMember.builder().githubId(2).githubLogin("user2").build();

        List<OrgMember> firstPageMembers = List.of(
            orgMember1
        );
        List<OrgMember> secondPageMembers = List.of(
            orgMember2
        );

        String firstPageJson = objectMapper.writeValueAsString(firstPageMembers);
        String secondPageJson = objectMapper.writeValueAsString(secondPageMembers);

        List<OrgMember> expectedResults = List.of(orgMember1, orgMember2);
        // Setup headers for pagination
        HttpHeaders firstPageHeaders = new HttpHeaders();
        firstPageHeaders.add("link", 
            "<https://api.github.com/orgs/" + TEST_ORG + "/members?page=2>; rel=\"next\"");

        HttpHeaders secondPageHeaders = new HttpHeaders();
        secondPageHeaders.add("link", "<https://api.github.com/orgs/" + TEST_ORG + "/members?page=1>; rel=\"previous\"");
        // Setup mock server for first page
        mockServer.expect(requestTo("https://api.github.com/orgs/" + TEST_ORG + "/members"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer " + TEST_TOKEN))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .headers(firstPageHeaders)
                        .body(firstPageJson));

        // Setup mock server for second page
        mockServer.expect(requestTo("https://api.github.com/orgs/" + TEST_ORG + "/members?page=2"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer " + TEST_TOKEN))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .headers(secondPageHeaders)
                        .body(secondPageJson));

        // Execute test
        List<OrgMember> result = (List<OrgMember>) organizationMemberService.getOrganizationMembers(testCourse);

        // Verify results
        mockServer.verify();
        assertEquals(expectedResults, result);
    }

    @Test
    void testGetOrganizationMembers_EmptyResponse() throws Exception {
        // Setup mock server with empty response
        mockServer.expect(requestTo("https://api.github.com/orgs/" + TEST_ORG + "/members"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer " + TEST_TOKEN))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("[]"));

        // Execute test
        Iterable<OrgMember> result = organizationMemberService.getOrganizationMembers(testCourse);

        // Verify results
        mockServer.verify();
        assertEquals(result, List.of());
    }

    @Test
    void testAddOrganizationMember() throws Exception {
        // Prepare test data
        int githubId = 12345;

        // Prepare expected request body
        Map<String, Object> expectedRequestBody = new HashMap<>();
        expectedRequestBody.put("invitee_id", githubId);
        expectedRequestBody.put("role", "direct_member");
        String expectedJsonRequest = objectMapper.writeValueAsString(expectedRequestBody);

        // Setup mock server
        mockServer.expect(requestTo("https://api.github.com/orgs/" + TEST_ORG + "/invitations"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer " + TEST_TOKEN))
                .andExpect(header("Accept", "application/vnd.github+json"))
                .andExpect(content().json(expectedJsonRequest))
                .andRespond(withStatus(HttpStatus.CREATED));

        // Execute test
        organizationMemberService.addOrganizationMember(testCourse, githubId);

        // Verify results
        mockServer.verify();
    }

    @Test
    void testRemoveOrganizationMember() throws Exception {
        // Prepare test data
        String githubLogin = "test-user";

        // Setup mock server
        mockServer.expect(requestTo("https://api.github.com/orgs/" + TEST_ORG + "/members/" + githubLogin))
                .andExpect(method(HttpMethod.DELETE))
                .andExpect(header("Authorization", "Bearer " + TEST_TOKEN))
                .andExpect(header("Accept", "application/vnd.github+json"))
                .andRespond(withStatus(HttpStatus.NO_CONTENT));

        // Execute test
        organizationMemberService.removeOrganizationMember(testCourse, githubLogin);

        // Verify results
        mockServer.verify();
    }

    @Test
    void testReplaceOrganizationMember() throws Exception {
        // Prepare test data
        String oldGithubLogin = "old-user";
        int newGithubId = 54321;

        // Prepare expected request body for add
        Map<String, Object> expectedRequestBody = new HashMap<>();
        expectedRequestBody.put("invitee_id", newGithubId);
        expectedRequestBody.put("role", "direct_member");
        String expectedJsonRequest = objectMapper.writeValueAsString(expectedRequestBody);

        // Setup mock server for remove
        mockServer.expect(requestTo("https://api.github.com/orgs/" + TEST_ORG + "/members/" + oldGithubLogin))
                .andExpect(method(HttpMethod.DELETE))
                .andExpect(header("Authorization", "Bearer " + TEST_TOKEN))
                .andExpect(header("Accept", "application/vnd.github+json"))
                .andRespond(withStatus(HttpStatus.NO_CONTENT));

        // Setup mock server for add
        mockServer.expect(requestTo("https://api.github.com/orgs/" + TEST_ORG + "/invitations"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer " + TEST_TOKEN))
                .andExpect(header("Accept", "application/vnd.github+json"))
                .andExpect(content().json(expectedJsonRequest))
                .andRespond(withStatus(HttpStatus.CREATED));

        // Execute test
        organizationMemberService.replaceOrganizationMember(testCourse, oldGithubLogin, newGithubId);

        // Verify results
        mockServer.verify();
    }

}