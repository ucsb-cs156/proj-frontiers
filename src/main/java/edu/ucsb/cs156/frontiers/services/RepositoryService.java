package edu.ucsb.cs156.frontiers.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.entities.RosterStudent;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.HashMap;
import java.util.Map;

@Service
public class RepositoryService {
    private final JwtService  jwtService;
    private final RestTemplate restTemplate;
    private final ObjectMapper mapper;

    public RepositoryService(JwtService jwtService, RestTemplateBuilder restTemplateBuilder, ObjectMapper mapper) {
        this.jwtService = jwtService;
        this.restTemplate = restTemplateBuilder.build();
        this.mapper = mapper;
    }

    /**
     * Creates a single student repository if it doesn't already exist, and provisions access to the repository by that student
     * @param course The Course in question
     * @param student RosterStudent of the student the repository should be created for
     * @param repoPrefix Name of the project or assignment. Used to title the repository, in the format repoPrefix-githubLogin
     * @param isPrivate Whether the repository is private or not
     */
    public void createStudentRepository(Course course, RosterStudent student, String repoPrefix, Boolean isPrivate) throws NoSuchAlgorithmException, InvalidKeySpecException, JsonProcessingException {
        String newRepoName = repoPrefix+"-"+student.getGithubLogin();
        String token = jwtService.getInstallationToken(course);
        String existenceEndpoint = "https://api.github.com/repos/"+course.getOrgName()+"/"+newRepoName;
        String createEndpoint = "https://api.github.com/orgs/"+course.getOrgName()+"/repos";
        String provisionEndpoint = "https://api.github.com/repos/"+course.getOrgName()+"/"+newRepoName+"/collaborators/"+student.getGithubLogin();
        HttpHeaders existenceHeaders = new HttpHeaders();
        existenceHeaders.add("Authorization", "Bearer " + token);
        existenceHeaders.add("Accept", "application/vnd.github+json");
        existenceHeaders.add("X-GitHub-Api-Version", "2022-11-28");

        HttpEntity<String> existenceEntity = new HttpEntity<>(existenceHeaders);

        try {
            restTemplate.exchange(existenceEndpoint, HttpMethod.GET, existenceEntity, String.class);
        } catch(HttpClientErrorException e){
            if(e.getStatusCode().equals(HttpStatus.NOT_FOUND)){
                HttpHeaders createHeaders = new HttpHeaders();
                createHeaders.add("Authorization", "Bearer " + token);
                createHeaders.add("Accept", "application/vnd.github+json");
                createHeaders.add("X-GitHub-Api-Version", "2022-11-28");

                Map<String, Object> body  = new HashMap<>();
                body.put("name", newRepoName);
                body.put("private",  isPrivate);
                String bodyAsJson =  mapper.writeValueAsString(body);

                HttpEntity<String> createEntity = new HttpEntity<>(bodyAsJson, createHeaders);

                restTemplate.exchange(createEndpoint, HttpMethod.POST, createEntity, String.class);
                Map<String, Object> provisionBody  = new HashMap<>();
                provisionBody.put("permission", "admin");
                String provisionAsJson =  mapper.writeValueAsString(provisionBody);

                HttpEntity<String> provisionEntity = new HttpEntity<>(provisionAsJson, createHeaders);
                restTemplate.exchange(provisionEndpoint, HttpMethod.PUT, provisionEntity, String.class);
            }
        }

    }
}
