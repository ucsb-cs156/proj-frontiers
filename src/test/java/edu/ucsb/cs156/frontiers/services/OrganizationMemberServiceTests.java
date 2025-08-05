package edu.ucsb.cs156.frontiers.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.entities.CourseStaff;
import edu.ucsb.cs156.frontiers.entities.RosterStudent;
import edu.ucsb.cs156.frontiers.enums.OrgStatus;
import edu.ucsb.cs156.frontiers.models.OrgMember;
import edu.ucsb.cs156.frontiers.repositories.RosterStudentRepository;
import edu.ucsb.cs156.frontiers.services.wiremock.WiremockService;
import edu.ucsb.cs156.frontiers.testconfig.TestConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureDataJpa;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.context.annotation.Import;
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
@Import(TestConfig.class)
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
        List<OrgMember> expectedMembers = List.of(
            OrgMember.builder().githubId(1).githubLogin("user1").build(),
            OrgMember.builder().githubId(2).githubLogin("user2").build()
        );
        String jsonResponse = objectMapper.writeValueAsString(expectedMembers);

        mockServer.expect(requestTo("https://api.github.com/orgs/" + TEST_ORG + "/members?role=member"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer " + TEST_TOKEN))
                .andExpect(header("Accept", "application/vnd.github+json"))
                .andExpect(header("X-GitHub-Api-Version", "2022-11-28"))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(jsonResponse));

        Iterable<OrgMember> result = organizationMemberService.getOrganizationMembers(testCourse);

        mockServer.verify();
        assertIterableEquals(expectedMembers, result);
    }

    @Test
    void testGetOrganizationMembers_MultiplePages() throws Exception {
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
        HttpHeaders firstPageHeaders = new HttpHeaders();
        firstPageHeaders.add("link", 
            "<https://api.github.com/orgs/" + TEST_ORG + "/members?page=2&role=member>; rel=\"next\"");

        HttpHeaders secondPageHeaders = new HttpHeaders();
        secondPageHeaders.add("link", "<https://api.github.com/orgs/" + TEST_ORG + "/members?page=1>; rel=\"previous\"");
        mockServer.expect(requestTo("https://api.github.com/orgs/" + TEST_ORG + "/members?role=member"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer " + TEST_TOKEN))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .headers(firstPageHeaders)
                        .body(firstPageJson));

        mockServer.expect(requestTo("https://api.github.com/orgs/" + TEST_ORG + "/members?page=2&role=member"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer " + TEST_TOKEN))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .headers(secondPageHeaders)
                        .body(secondPageJson));

        List<OrgMember> result = (List<OrgMember>) organizationMemberService.getOrganizationMembers(testCourse);

        mockServer.verify();
        assertEquals(expectedResults, result);
    }

    @Test
    void testGetOrganizationMembers_EmptyResponse() throws Exception {
        mockServer.expect(requestTo("https://api.github.com/orgs/" + TEST_ORG + "/members?role=member"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer " + TEST_TOKEN))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("[]"));

        Iterable<OrgMember> result = organizationMemberService.getOrganizationMembers(testCourse);

        mockServer.verify();
        assertEquals(result, List.of());
    }



    @Test
    void testGetOrganizationAdmins_SinglePage() throws Exception {
        List<OrgMember> expectedAdmins = List.of(
            OrgMember.builder().githubId(1).githubLogin("admin1").build(),
            OrgMember.builder().githubId(2).githubLogin("admin2").build()
        );
        String jsonResponse = objectMapper.writeValueAsString(expectedAdmins);

        mockServer.expect(requestTo("https://api.github.com/orgs/" + TEST_ORG + "/members?role=admin"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer " + TEST_TOKEN))
                .andExpect(header("Accept", "application/vnd.github+json"))
                .andExpect(header("X-GitHub-Api-Version", "2022-11-28"))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(jsonResponse));

        Iterable<OrgMember> result = organizationMemberService.getOrganizationAdmins(testCourse);

        mockServer.verify();
        assertIterableEquals(expectedAdmins, result);
    }

    @Test
    void testGetOrganizationAdmins_MultiplePages() throws Exception {
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
        HttpHeaders firstPageHeaders = new HttpHeaders();
        firstPageHeaders.add("link",
            "<https://api.github.com/orgs/" + TEST_ORG + "/members?page=2&role=admin>; rel=\"next\"");

        HttpHeaders secondPageHeaders = new HttpHeaders();
        secondPageHeaders.add("link", "<https://api.github.com/orgs/" + TEST_ORG + "/members?page=1>; rel=\"previous\"");
        mockServer.expect(requestTo("https://api.github.com/orgs/" + TEST_ORG + "/members?role=admin"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer " + TEST_TOKEN))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .headers(firstPageHeaders)
                        .body(firstPageJson));

        mockServer.expect(requestTo("https://api.github.com/orgs/" + TEST_ORG + "/members?page=2&role=admin"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer " + TEST_TOKEN))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .headers(secondPageHeaders)
                        .body(secondPageJson));

        List<OrgMember> result = (List<OrgMember>) organizationMemberService.getOrganizationAdmins(testCourse);

        mockServer.verify();
        assertEquals(expectedResults, result);
    }

    @Test
    void testGetOrganizationAdmins_EmptyResponse() throws Exception {
        mockServer.expect(requestTo("https://api.github.com/orgs/" + TEST_ORG + "/members?role=admin"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer " + TEST_TOKEN))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("[]"));

        Iterable<OrgMember> result = organizationMemberService.getOrganizationAdmins(testCourse);

        mockServer.verify();
        assertEquals(result, List.of());
    }


    @Test
    void testInviteOrganizationMember_Success() throws Exception {
        RosterStudent testStudent = RosterStudent.builder()
                .githubId(12345)
                .course(testCourse)
                .build();

        Map<String, Object> expectedRequestBody = new HashMap<>();
        expectedRequestBody.put("invitee_id", 12345);
        expectedRequestBody.put("role", "direct_member");
        String expectedRequestBodyJson = objectMapper.writeValueAsString(expectedRequestBody);

        mockServer.expect(requestTo("https://api.github.com/orgs/" + TEST_ORG + "/invitations"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer " + TEST_TOKEN))
                .andExpect(header("Accept", "application/vnd.github+json"))
                .andExpect(header("X-GitHub-Api-Version", "2022-11-28"))
                .andExpect(content().json(expectedRequestBodyJson))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{}"));

        OrgStatus result = organizationMemberService.inviteOrganizationMember(testStudent);

        mockServer.verify();
        assertEquals(OrgStatus.INVITED, result);
    }

    @Test
    void testInviteOrganizationMember_failure_is_member() throws Exception {
        RosterStudent testStudent = RosterStudent.builder()
                .githubId(12345)
                .githubLogin("banana")
                .course(testCourse)
                .build();

        Map<String, Object> expectedRequestBody = new HashMap<>();
        expectedRequestBody.put("invitee_id", 12345);
        expectedRequestBody.put("role", "direct_member");
        String expectedRequestBodyJson = objectMapper.writeValueAsString(expectedRequestBody);

        mockServer.expect(requestTo("https://api.github.com/orgs/" + TEST_ORG + "/invitations"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer " + TEST_TOKEN))
                .andExpect(header("Accept", "application/vnd.github+json"))
                .andExpect(header("X-GitHub-Api-Version", "2022-11-28"))
                .andExpect(content().json(expectedRequestBodyJson))
                .andRespond(withStatus(HttpStatus.UNPROCESSABLE_ENTITY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"message\": \"Error inviting member\"}"));

        mockServer.expect(requestTo("https://api.github.com/orgs/" + TEST_ORG + "/memberships/" + "banana"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer " + TEST_TOKEN))
                .andExpect(header("Accept", "application/vnd.github+json"))
                .andExpect(header("X-GitHub-Api-Version", "2022-11-28"))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{ \"role\": \"member\" }"));

        OrgStatus result = organizationMemberService.inviteOrganizationMember(testStudent);

        mockServer.verify();
        assertEquals(OrgStatus.MEMBER, result);
    }

    @Test
    void testInviteOrganizationMember_failure_is_owner() throws Exception {
        RosterStudent testStudent = RosterStudent.builder()
                .githubId(12345)
                .githubLogin("banana")
                .course(testCourse)
                .build();

        Map<String, Object> expectedRequestBody = new HashMap<>();
        expectedRequestBody.put("invitee_id", 12345);
        expectedRequestBody.put("role", "direct_member");
        String expectedRequestBodyJson = objectMapper.writeValueAsString(expectedRequestBody);

        mockServer.expect(requestTo("https://api.github.com/orgs/" + TEST_ORG + "/invitations"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer " + TEST_TOKEN))
                .andExpect(header("Accept", "application/vnd.github+json"))
                .andExpect(header("X-GitHub-Api-Version", "2022-11-28"))
                .andExpect(content().json(expectedRequestBodyJson))
                .andRespond(withStatus(HttpStatus.UNPROCESSABLE_ENTITY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"message\": \"Error inviting member\"}"));

        mockServer.expect(requestTo("https://api.github.com/orgs/" + TEST_ORG + "/memberships/" + "banana"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer " + TEST_TOKEN))
                .andExpect(header("Accept", "application/vnd.github+json"))
                .andExpect(header("X-GitHub-Api-Version", "2022-11-28"))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{ \"role\": \"admin\" }"));

        OrgStatus result = organizationMemberService.inviteOrganizationMember(testStudent);

        mockServer.verify();
        assertEquals(OrgStatus.OWNER, result);
    }

    @Test
    void testInviteOrganizationMember_failure_is_unexpected_role() throws Exception {
        RosterStudent testStudent = RosterStudent.builder()
                .githubId(12345)
                .githubLogin("banana")
                .course(testCourse)
                .build();

        Map<String, Object> expectedRequestBody = new HashMap<>();
        expectedRequestBody.put("invitee_id", 12345);
        expectedRequestBody.put("role", "direct_member");
        String expectedRequestBodyJson = objectMapper.writeValueAsString(expectedRequestBody);

        mockServer.expect(requestTo("https://api.github.com/orgs/" + TEST_ORG + "/invitations"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer " + TEST_TOKEN))
                .andExpect(header("Accept", "application/vnd.github+json"))
                .andExpect(header("X-GitHub-Api-Version", "2022-11-28"))
                .andExpect(content().json(expectedRequestBodyJson))
                .andRespond(withStatus(HttpStatus.UNPROCESSABLE_ENTITY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"message\": \"Error inviting member\"}"));

        mockServer.expect(requestTo("https://api.github.com/orgs/" + TEST_ORG + "/memberships/" + "banana"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer " + TEST_TOKEN))
                .andExpect(header("Accept", "application/vnd.github+json"))
                .andExpect(header("X-GitHub-Api-Version", "2022-11-28"))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{ \"role\": \"billing_manager\" }"));

        OrgStatus result = organizationMemberService.inviteOrganizationMember(testStudent);

        mockServer.verify();
        assertEquals(OrgStatus.JOINCOURSE, result);
    }

    @Test
    void testInviteOrganizationMember_failure_is_not_found() throws Exception {
        RosterStudent testStudent = RosterStudent.builder()
                .githubId(12345)
                .githubLogin("banana")
                .course(testCourse)
                .build();

        Map<String, Object> expectedRequestBody = new HashMap<>();
        expectedRequestBody.put("invitee_id", 12345);
        expectedRequestBody.put("role", "direct_member");
        String expectedRequestBodyJson = objectMapper.writeValueAsString(expectedRequestBody);

        mockServer.expect(requestTo("https://api.github.com/orgs/" + TEST_ORG + "/invitations"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer " + TEST_TOKEN))
                .andExpect(header("Accept", "application/vnd.github+json"))
                .andExpect(header("X-GitHub-Api-Version", "2022-11-28"))
                .andExpect(content().json(expectedRequestBodyJson))
                .andRespond(withStatus(HttpStatus.UNPROCESSABLE_ENTITY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"message\": \"Error inviting member\"}"));

        mockServer.expect(requestTo("https://api.github.com/orgs/" + TEST_ORG + "/memberships/" + "banana"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer " + TEST_TOKEN))
                .andExpect(header("Accept", "application/vnd.github+json"))
                .andExpect(header("X-GitHub-Api-Version", "2022-11-28"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        OrgStatus result = organizationMemberService.inviteOrganizationMember(testStudent);

        mockServer.verify();
        assertEquals(OrgStatus.JOINCOURSE, result);
    }

    @Test
    void testInviteOrganizationOwner_Success() throws Exception {
        CourseStaff testStaff = CourseStaff.builder()
                .githubId(12345)
                .course(testCourse)
                .build();

        Map<String, Object> expectedRequestBody = new HashMap<>();
        expectedRequestBody.put("invitee_id", 12345);
        expectedRequestBody.put("role", "admin");
        String expectedRequestBodyJson = objectMapper.writeValueAsString(expectedRequestBody);

        mockServer.expect(requestTo("https://api.github.com/orgs/" + TEST_ORG + "/invitations"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer " + TEST_TOKEN))
                .andExpect(header("Accept", "application/vnd.github+json"))
                .andExpect(header("X-GitHub-Api-Version", "2022-11-28"))
                .andExpect(content().json(expectedRequestBodyJson))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{}"));

        OrgStatus result = organizationMemberService.inviteOrganizationOwner(testStaff);

        mockServer.verify();
        assertEquals(OrgStatus.INVITED, result);
    }

    @Test
    void testGetOrganizationInvitees_SinglePage() throws Exception {
        List<OrgMember> expectedInvitees = List.of(
            OrgMember.builder().githubId(1).githubLogin("invitee1").build(),
            OrgMember.builder().githubId(2).githubLogin("invitee2").build()
        );
        String jsonResponse = objectMapper.writeValueAsString(expectedInvitees);

        mockServer.expect(requestTo("https://api.github.com/orgs/" + TEST_ORG + "/invitations"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer " + TEST_TOKEN))
                .andExpect(header("Accept", "application/vnd.github+json"))
                .andExpect(header("X-GitHub-Api-Version", "2022-11-28"))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(jsonResponse));

        Iterable<OrgMember> result = organizationMemberService.getOrganizationInvitees(testCourse);

        mockServer.verify();
        assertIterableEquals(expectedInvitees, result);
    }

    @Test
    void testGetOrganizationInvitees_MultiplePages() throws Exception {
        OrgMember orgInvitee1 = OrgMember.builder().githubId(1).githubLogin("invitee1").build();
        OrgMember orgInvitee2 = OrgMember.builder().githubId(2).githubLogin("invitee2").build();

        List<OrgMember> firstPageInvitees = List.of(
            orgInvitee1
        );
        List<OrgMember> secondPageInvitees = List.of(
            orgInvitee2
        );

        String firstPageJson = objectMapper.writeValueAsString(firstPageInvitees);
        String secondPageJson = objectMapper.writeValueAsString(secondPageInvitees);

        List<OrgMember> expectedResults = List.of(orgInvitee1, orgInvitee2);
        HttpHeaders firstPageHeaders = new HttpHeaders();
        firstPageHeaders.add("link", 
            "<https://api.github.com/orgs/" + TEST_ORG + "/invitations?page=2>; rel=\"next\"");

        HttpHeaders secondPageHeaders = new HttpHeaders();
        secondPageHeaders.add("link", "<https://api.github.com/orgs/" + TEST_ORG + "/invitations?page=1>; rel=\"previous\"");
        mockServer.expect(requestTo("https://api.github.com/orgs/" + TEST_ORG + "/invitations"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer " + TEST_TOKEN))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .headers(firstPageHeaders)
                        .body(firstPageJson));

        mockServer.expect(requestTo("https://api.github.com/orgs/" + TEST_ORG + "/invitations?page=2"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer " + TEST_TOKEN))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .headers(secondPageHeaders)
                        .body(secondPageJson));

        List<OrgMember> result = (List<OrgMember>) organizationMemberService.getOrganizationInvitees(testCourse);

        mockServer.verify();
        assertEquals(expectedResults, result);
    }

    @Test
    void testGetOrganizationInvitees_EmptyResponse() throws Exception {
        mockServer.expect(requestTo("https://api.github.com/orgs/" + TEST_ORG + "/invitations"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer " + TEST_TOKEN))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("[]"));

        Iterable<OrgMember> result = organizationMemberService.getOrganizationInvitees(testCourse);

        mockServer.verify();
        assertEquals(result, List.of());
    }

    @Test
    void testRemoveOrganizationMember_Success() throws Exception {
        RosterStudent testStudent = RosterStudent.builder()
                .githubId(12345)
                .githubLogin("testuser")
                .course(testCourse)
                .build();

        mockServer.expect(requestTo("https://api.github.com/orgs/" + TEST_ORG + "/members/testuser"))
                .andExpect(method(HttpMethod.DELETE))
                .andExpect(header("Authorization", "Bearer " + TEST_TOKEN))
                .andExpect(header("Accept", "application/vnd.github+json"))
                .andExpect(header("X-GitHub-Api-Version", "2022-11-28"))
                .andRespond(withStatus(HttpStatus.NO_CONTENT));

        // No exception should be thrown
        organizationMemberService.removeOrganizationMember(testStudent);

        mockServer.verify();
        // No assertion needed as we're just verifying no exception is thrown
    }

    @Test
    void testRemoveOrganizationMember_NullGithubLogin() throws Exception {
        RosterStudent testStudent = RosterStudent.builder()
                .githubId(12345)
                .githubLogin(null)
                .course(testCourse)
                .build();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            organizationMemberService.removeOrganizationMember(testStudent);
        });

        assertEquals("Cannot remove student from organization: GitHub login is null", exception.getMessage());
        mockServer.verify();
    }

    @Test
    void testRemoveOrganizationMember_NullOrgName() throws Exception {
        Course courseWithoutOrg = Course.builder()
                .installationId("123")
                .orgName(null)
                .build();

        RosterStudent testStudent = RosterStudent.builder()
                .githubId(12345)
                .githubLogin("testuser")
                .course(courseWithoutOrg)
                .build();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            organizationMemberService.removeOrganizationMember(testStudent);
        });

        assertEquals("Cannot remove student from organization: Course has no linked organization", exception.getMessage());
        mockServer.verify();
    }

    @Test
    void testRemoveOrganizationMember_NullInstallationId() throws Exception {
        Course courseWithoutInstallation = Course.builder()
                .orgName(TEST_ORG)
                .installationId(null)
                .build();

        RosterStudent testStudent = RosterStudent.builder()
                .githubId(12345)
                .githubLogin("testuser")
                .course(courseWithoutInstallation)
                .build();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            organizationMemberService.removeOrganizationMember(testStudent);
        });

        assertEquals("Cannot remove student from organization: Course has no linked organization", exception.getMessage());
        mockServer.verify();
    }

    @Test
    void testRemoveOrganizationMember_ApiError() throws Exception {
        RosterStudent testStudent = RosterStudent.builder()
                .githubId(12345)
                .githubLogin("testuser")
                .course(testCourse)
                .build();

        mockServer.expect(requestTo("https://api.github.com/orgs/" + TEST_ORG + "/members/testuser"))
                .andExpect(method(HttpMethod.DELETE))
                .andExpect(header("Authorization", "Bearer " + TEST_TOKEN))
                .andExpect(header("Accept", "application/vnd.github+json"))
                .andExpect(header("X-GitHub-Api-Version", "2022-11-28"))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"message\": \"Error removing member\"}"));

        Exception exception = assertThrows(Exception.class, () -> {
            organizationMemberService.removeOrganizationMember(testStudent);
        });

        assertTrue(exception.getMessage().contains("Error removing member"));
        mockServer.verify();
    }
}
