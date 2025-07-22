package edu.ucsb.cs156.frontiers.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.entities.CourseStaff;
import edu.ucsb.cs156.frontiers.entities.RosterStudent;
import edu.ucsb.cs156.frontiers.enums.OrgStatus;
import edu.ucsb.cs156.frontiers.models.OrgMember;
import edu.ucsb.cs156.frontiers.repositories.RosterStudentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class OrganizationMemberService {

    private final JwtService jwtService;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;
    private final RosterStudentRepository rosterStudentRepository;

    public OrganizationMemberService(JwtService jwtService, ObjectMapper objectMapper, RestTemplateBuilder builder, RosterStudentRepository rosterStudentRepository) {
        this.jwtService = jwtService;
        this.objectMapper = objectMapper;
        this.rosterStudentRepository = rosterStudentRepository;
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.restTemplate = builder.build();
    }


    /**
    * This endpoint returns the list of **members**, not admins for the organization. This is so that the roles are known for the return values.
    */
    public Iterable<OrgMember> getOrganizationMembers(Course course) throws NoSuchAlgorithmException, InvalidKeySpecException, JsonProcessingException {
        String ENDPOINT = "https://api.github.com/orgs/" + course.getOrgName() + "/members?role=member";
        return getOrganizationMembersWithRole(course, ENDPOINT);
    }

    /**
    * This endpoint returns the list of **admins** for the organization. This is so that the roles are known for the return values.
    */
    public Iterable<OrgMember> getOrganizationAdmins(Course course) throws NoSuchAlgorithmException, InvalidKeySpecException, JsonProcessingException {
        String ENDPOINT = "https://api.github.com/orgs/" + course.getOrgName() + "/members?role=admin";
        return getOrganizationMembersWithRole(course, ENDPOINT);
    }

    /**
    * This endpoint returns the list of users who have been **invited** to the organization but have not yet accepted.
    */
    public Iterable<OrgMember> getOrganizationInvitees(Course course) throws NoSuchAlgorithmException, InvalidKeySpecException, JsonProcessingException {
        String ENDPOINT = "https://api.github.com/orgs/" + course.getOrgName() + "/invitations";
        return getOrganizationMembersWithRole(course, ENDPOINT);
    }

    private Iterable<OrgMember> getOrganizationMembersWithRole(Course course, String ENDPOINT) throws NoSuchAlgorithmException, InvalidKeySpecException, JsonProcessingException {
        //happily stolen directly from GitHub: https://docs.github.com/en/rest/using-the-rest-api/using-pagination-in-the-rest-api?apiVersion=2022-11-28
        Pattern pattern = Pattern.compile("(?<=<)([\\S]*)(?=>; rel=\"next\")");
        String token = jwtService.getInstallationToken(course);
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + token);
        headers.add("Accept", "application/vnd.github+json");
        headers.add("X-GitHub-Api-Version", "2022-11-28");
        HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange(ENDPOINT, HttpMethod.GET, entity, String.class);
        List<String> responseLinks = response.getHeaders().getOrEmpty("link");
        List<OrgMember> orgMembers = new ArrayList<>();
        while (!responseLinks.isEmpty()&&responseLinks.getFirst().contains("next")) {
            orgMembers.addAll(objectMapper.convertValue(objectMapper.readTree(response.getBody()), new TypeReference<List<OrgMember>>() {
            }));
            Matcher matcher = pattern.matcher(responseLinks.getFirst());
            matcher.find();
            response = restTemplate.exchange(matcher.group(0), HttpMethod.GET, entity, String.class);
            responseLinks = response.getHeaders().getOrEmpty("link");
        }
        orgMembers.addAll(objectMapper.convertValue(objectMapper.readTree(response.getBody()), new TypeReference<List<OrgMember>>() {
        }));
        return orgMembers;
    }

    public OrgStatus inviteOrganizationMember(RosterStudent student) throws NoSuchAlgorithmException, InvalidKeySpecException, JsonProcessingException {
        Course course = student.getCourse();
        return inviteMember(student.getGithubId(), course, "direct_member", student.getGithubLogin());
    }

    public OrgStatus inviteOrganizationOwner(CourseStaff staff) throws NoSuchAlgorithmException, InvalidKeySpecException, JsonProcessingException {
        Course course = staff.getCourse();
        return inviteMember(staff.getGithubId(), course, "admin", staff.getGithubLogin());
    }

    private OrgStatus inviteMember(int githubId, Course course, String role, String githubLogin) throws JsonProcessingException, NoSuchAlgorithmException, InvalidKeySpecException {
        String ENDPOINT = "https://api.github.com/orgs/" + course.getOrgName() + "/invitations";
        HttpHeaders headers = new HttpHeaders();
        String token = jwtService.getInstallationToken(course);
        headers.add("Authorization", "Bearer " + token);
        headers.add("Accept", "application/vnd.github+json");
        headers.add("X-GitHub-Api-Version", "2022-11-28");
        Map<String, Object> body = new HashMap<>();
        body.put("invitee_id", githubId);
        body.put("role", role);
        String bodyAsJson = objectMapper.writeValueAsString(body);
        HttpEntity<String> entity = new HttpEntity<>(bodyAsJson, headers);
        try{
            restTemplate.exchange(ENDPOINT, HttpMethod.POST, entity, String.class);
        } catch (HttpClientErrorException e) {
            return getMemberStatus(githubLogin, course);
        }
        return OrgStatus.INVITED;
    }

    private OrgStatus getMemberStatus(String githubLogin, Course course) throws JsonProcessingException, NoSuchAlgorithmException, InvalidKeySpecException {
        String ENDPOINT = "https://api.github.com/orgs/" + course.getOrgName() + "/memberships/" + githubLogin;
        HttpHeaders headers = new HttpHeaders();
        String token = jwtService.getInstallationToken(course);
        headers.add("Authorization", "Bearer " + token);
        headers.add("Accept", "application/vnd.github+json");
        headers.add("X-GitHub-Api-Version", "2022-11-28");
        HttpEntity<String> entity = new HttpEntity<>(headers);
        try{
            ResponseEntity<String> response = restTemplate.exchange(ENDPOINT, HttpMethod.GET, entity, String.class);
            JsonNode responseJson = objectMapper.readTree(response.getBody());
            if(responseJson.get("role").asText().equalsIgnoreCase("admin")){
                return OrgStatus.OWNER;
            }else if (responseJson.get("role").asText().equalsIgnoreCase("member")){
                return OrgStatus.MEMBER;
            }else{
                log.warn("Unexpected role {} used in course {}", responseJson.get("role").asText(), course.getCourseName());
                return OrgStatus.JOINCOURSE;
            }
        }catch (HttpClientErrorException e){
            log.warn("Error while trying to get member status: {}", e.getMessage());
            return OrgStatus.JOINCOURSE;
        }

    }
}
