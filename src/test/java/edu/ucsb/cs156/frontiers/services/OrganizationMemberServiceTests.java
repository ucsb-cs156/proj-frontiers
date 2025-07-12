package edu.ucsb.cs156.frontiers.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.entities.RosterStudent;
import edu.ucsb.cs156.frontiers.enums.OrgStatus;
import edu.ucsb.cs156.frontiers.models.OrgMember;
import edu.ucsb.cs156.frontiers.repositories.RosterStudentRepository;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
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

    @MockitoBean
    private RosterStudentRepository rosterStudentRepository;

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
        mockServer.expect(requestTo("https://api.github.com/orgs/" + TEST_ORG + "/members?role=member"))
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
            "<https://api.github.com/orgs/" + TEST_ORG + "/members?page=2&role=member>; rel=\"next\"");

        HttpHeaders secondPageHeaders = new HttpHeaders();
        secondPageHeaders.add("link", "<https://api.github.com/orgs/" + TEST_ORG + "/members?page=1>; rel=\"previous\"");
        // Setup mock server for first page
        mockServer.expect(requestTo("https://api.github.com/orgs/" + TEST_ORG + "/members?role=member"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer " + TEST_TOKEN))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .headers(firstPageHeaders)
                        .body(firstPageJson));

        // Setup mock server for second page
        mockServer.expect(requestTo("https://api.github.com/orgs/" + TEST_ORG + "/members?page=2&role=member"))
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
        mockServer.expect(requestTo("https://api.github.com/orgs/" + TEST_ORG + "/members?role=member"))
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
    void testGetOrganizationAdmins_SinglePage() throws Exception {
        // Prepare test data
        List<OrgMember> expectedAdmins = List.of(
            OrgMember.builder().githubId(1).githubLogin("admin1").build(),
            OrgMember.builder().githubId(2).githubLogin("admin2").build()
        );
        String jsonResponse = objectMapper.writeValueAsString(expectedAdmins);

        // Setup mock server
        mockServer.expect(requestTo("https://api.github.com/orgs/" + TEST_ORG + "/members?role=admin"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer " + TEST_TOKEN))
                .andExpect(header("Accept", "application/vnd.github+json"))
                .andExpect(header("X-GitHub-Api-Version", "2022-11-28"))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(jsonResponse));

        // Execute test
        Iterable<OrgMember> result = organizationMemberService.getOrganizationAdmins(testCourse);

        // Verify results
        mockServer.verify();
        assertIterableEquals(expectedAdmins, result);
    }

    @Test
    void testGetOrganizationAdmins_MultiplePages() throws Exception {
        // Prepare test data for two pages
        OrgMember orgAdmin1 = OrgMember.builder().githubId(1).githubLogin("admin1").build();
        OrgMember orgAdmin2 = OrgMember.builder().githubId(2).githubLogin("admin2").build();

        List<OrgMember> firstPageAdmins = List.of(
            orgAdmin1
        );
        List<OrgMember> secondPageAdmins = List.of(
            orgAdmin2
        );

        String firstPageJson = objectMapper.writeValueAsString(firstPageAdmins);
        String secondPageJson = objectMapper.writeValueAsString(secondPageAdmins);

        List<OrgMember> expectedResults = List.of(orgAdmin1, orgAdmin2);
        // Setup headers for pagination
        HttpHeaders firstPageHeaders = new HttpHeaders();
        firstPageHeaders.add("link",
            "<https://api.github.com/orgs/" + TEST_ORG + "/members?page=2&role=admin>; rel=\"next\"");

        HttpHeaders secondPageHeaders = new HttpHeaders();
        secondPageHeaders.add("link", "<https://api.github.com/orgs/" + TEST_ORG + "/members?page=1>; rel=\"previous\"");
        // Setup mock server for first page
        mockServer.expect(requestTo("https://api.github.com/orgs/" + TEST_ORG + "/members?role=admin"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer " + TEST_TOKEN))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .headers(firstPageHeaders)
                        .body(firstPageJson));

        // Setup mock server for second page
        mockServer.expect(requestTo("https://api.github.com/orgs/" + TEST_ORG + "/members?page=2&role=admin"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer " + TEST_TOKEN))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .headers(secondPageHeaders)
                        .body(secondPageJson));

        // Execute test
        List<OrgMember> result = (List<OrgMember>) organizationMemberService.getOrganizationAdmins(testCourse);

        // Verify results
        mockServer.verify();
        assertEquals(expectedResults, result);
    }

    @Test
    void testGetOrganizationAdmins_EmptyResponse() throws Exception {
        // Setup mock server with empty response
        mockServer.expect(requestTo("https://api.github.com/orgs/" + TEST_ORG + "/members?role=admin"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer " + TEST_TOKEN))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("[]"));

        // Execute test
        Iterable<OrgMember> result = organizationMemberService.getOrganizationAdmins(testCourse);

        // Verify results
        mockServer.verify();
        assertEquals(result, List.of());
    }


    @Test
    void testInviteOrganizationMember_Success() throws Exception {
        // Create test roster student
        RosterStudent testStudent = RosterStudent.builder()
                .githubId(12345)
                .course(testCourse)
                .build();

        // Expected request body
        Map<String, Object> expectedRequestBody = new HashMap<>();
        expectedRequestBody.put("invitee_id", 12345);
        expectedRequestBody.put("role", "direct_member");
        String expectedRequestBodyJson = objectMapper.writeValueAsString(expectedRequestBody);

        // Setup mock server for successful response
        mockServer.expect(requestTo("https://api.github.com/orgs/" + TEST_ORG + "/invitations"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer " + TEST_TOKEN))
                .andExpect(header("Accept", "application/vnd.github+json"))
                .andExpect(header("X-GitHub-Api-Version", "2022-11-28"))
                .andExpect(content().json(expectedRequestBodyJson))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{}"));

        // Execute test
        OrgStatus result = organizationMemberService.inviteOrganizationMember(testStudent);

        // Verify results
        mockServer.verify();
        assertEquals(OrgStatus.INVITED, result);
    }

    @Test
    void testInviteOrganizationMember_Failure() throws Exception {
        // Create test roster student
        RosterStudent testStudent = RosterStudent.builder()
                .githubId(12345)
                .course(testCourse)
                .build();

        // Expected request body
        Map<String, Object> expectedRequestBody = new HashMap<>();
        expectedRequestBody.put("invitee_id", 12345);
        expectedRequestBody.put("role", "direct_member");
        String expectedRequestBodyJson = objectMapper.writeValueAsString(expectedRequestBody);

        // Setup mock server for failed response
        mockServer.expect(requestTo("https://api.github.com/orgs/" + TEST_ORG + "/invitations"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer " + TEST_TOKEN))
                .andExpect(header("Accept", "application/vnd.github+json"))
                .andExpect(header("X-GitHub-Api-Version", "2022-11-28"))
                .andExpect(content().json(expectedRequestBodyJson))
                .andRespond(withStatus(HttpStatus.UNPROCESSABLE_ENTITY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"message\": \"Error inviting member\"}"));

        // Execute test
        OrgStatus result = organizationMemberService.inviteOrganizationMember(testStudent);

        // Verify results
        mockServer.verify();
        assertEquals(OrgStatus.JOINCOURSE, result);
    }
}
