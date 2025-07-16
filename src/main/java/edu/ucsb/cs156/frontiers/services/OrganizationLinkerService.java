package edu.ucsb.cs156.frontiers.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.errors.InvalidInstallationTypeException;
import org.springframework.beans.factory.annotation.Autowired;
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

@Service
public class OrganizationLinkerService {
    private RestTemplate restTemplate;

    @Autowired
    JwtService jwtService;

    @Autowired
    ObjectMapper objectMapper;

    public OrganizationLinkerService(RestTemplateBuilder restTemplateBuilder) {
        restTemplate = restTemplateBuilder.build();
    }

    /**
     * Returns the URL for a redirect to install Frontiers
     * @return URL to install Frontiers to an organization
     */
    public String getRedirectUrl() throws NoSuchAlgorithmException, InvalidKeySpecException, JsonProcessingException {
        String token = jwtService.getJwt();
        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.add("Authorization", "Bearer " + token);
        requestHeaders.add("Accept", "application/vnd.github+json");
        requestHeaders.add("X-GitHub-Api-Version", "2022-11-28");
        String ENDPOINT = "https://api.github.com/app";
        HttpEntity<String> newEntity = new HttpEntity<>(requestHeaders);
        ResponseEntity<String> response = restTemplate.exchange(ENDPOINT, HttpMethod.GET,  newEntity, String.class);

        JsonNode responseJson = objectMapper.readTree(response.getBody());

        String newUrl = responseJson.get("html_url").toString().replaceAll("\"", "");
        return newUrl;
    }

    /**
     * Provides the name of the organization attached to a particular installation ID
     * @param installation_id ID of the app installation
     * @return name of the organization attached to the installation
     */
    public String getOrgName(String installation_id) throws NoSuchAlgorithmException, InvalidKeySpecException, JsonProcessingException {
        String token = jwtService.getJwt();
        String ENDPOINT = "https://api.github.com/app/installations/" + installation_id;

        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + token);
        headers.add("Accept", "application/vnd.github+json");
        headers.add("X-GitHub-Api-Version", "2022-11-28");
        HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange(ENDPOINT, HttpMethod.GET, entity, String.class);
        JsonNode responseJson = objectMapper.readTree(response.getBody());
        String type = responseJson.get("account").get("type").asText();
        if(!type.equals("Organization")){
            throw new InvalidInstallationTypeException(type);
        }
        String orgName = responseJson.get("account").get("login").asText();
        return orgName;
    }


    /**
     * Removes the Frontiers installation from the linked GitHub org
     *
     * @param course The entity for the course about to be deleted
     */
    public void unenrollOrganization(Course course) throws NoSuchAlgorithmException, InvalidKeySpecException {
        if(course.getOrgName() == null || course.getInstallationId() == null){
            return;
        }
        String token = jwtService.getJwt();
        String ENDPOINT = "https://api.github.com/app/installations/" + course.getInstallationId();
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + token);
        headers.add("Accept", "application/vnd.github+json");
        headers.add("X-GitHub-Api-Version", "2022-11-28");
        HttpEntity<String> entity = new HttpEntity<>(headers);
        try {
            ResponseEntity<String> response = restTemplate.exchange(ENDPOINT, HttpMethod.DELETE, entity, String.class);
        }catch(HttpClientErrorException ignored){

        }
    }
}
