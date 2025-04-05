package edu.ucsb.cs156.frontiers.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    public void createStudentRepository(String installationId, String orgName, RosterStudent student, String repoPrefix) throws NoSuchAlgorithmException, InvalidKeySpecException, JsonProcessingException {
        String newRepoName = repoPrefix+"-"+student.getUser().getGithubLogin();
        String token = jwtService.getInstallationToken(installationId);
        String existenceEndpoint = "https://api.github.com/repos/"+orgName+"/"+newRepoName;
        String createEndpoint = "https://api.github.com/orgs/"+orgName+"/repos";
        String provisionEndpoint = "https://api.github.com/"+orgName+"/"+newRepoName+"/collaborators/"+student.getUser().getGithubLogin();

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
