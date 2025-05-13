package edu.ucsb.cs156.frontiers.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.models.OrgMember;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class OrganizationMemberService {

    private final JwtService jwtService;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    public OrganizationMemberService(JwtService jwtService, ObjectMapper objectMapper, RestTemplateBuilder builder) {
        this.jwtService = jwtService;
        this.objectMapper = objectMapper;
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.restTemplate = builder.build();
    }

    public Iterable<OrgMember> getOrganizationMembers(Course course) throws NoSuchAlgorithmException, InvalidKeySpecException, JsonProcessingException {
        String ENDPOINT = "https://api.github.com/orgs/" + course.getOrgName() + "/members";
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
}
