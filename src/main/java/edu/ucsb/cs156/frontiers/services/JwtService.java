package edu.ucsb.cs156.frontiers.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.errors.NoLinkedOrganizationException;
import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;

@Service
@Slf4j
public class JwtService {
    @Value("${app.private.key:no-key-present}")
    private String privateKey;

    @Value("${app.client.id:no-client-id}")
    private String clientId;

    private final RestTemplate restTemplate;

    private final ObjectMapper objectMapper;

    private final DateTimeProvider  dateTimeProvider;

    public JwtService(RestTemplateBuilder restTemplateBuilder,  ObjectMapper objectMapper, DateTimeProvider dateTimeProvider) {
        this.restTemplate = restTemplateBuilder.build();
        this.objectMapper = objectMapper;
        this.dateTimeProvider = dateTimeProvider;
    }

    private RSAPrivateKey getPrivateKey() throws NoSuchAlgorithmException, InvalidKeySpecException {
        String key = privateKey;
        key = key.replace("-----BEGIN PRIVATE KEY-----", "");
        key = key.replace("-----END PRIVATE KEY-----", "");
        key = key.replaceAll(" ", "");
        key = key.replaceAll(System.lineSeparator(), "");
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(Base64.getDecoder().decode(key.getBytes()));
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return (RSAPrivateKey) kf.generatePrivate(spec);
    }

    /**
     * Method to retrieve a signed JWT that a service can use to authenticate with GitHub as an app installation without permissions to a specific organization.
     * @return Signed JWT that expires in 5 minutes in the form of a String
     * @throws InvalidKeySpecException if the key is invalid, the exception will be thrown.
     */
    public String getJwt() throws NoSuchAlgorithmException, InvalidKeySpecException {
        Instant currentTime = Instant.from(dateTimeProvider.getNow().get());
        String token = Jwts.builder()
                .issuedAt(Date.from(currentTime.minus(30, ChronoUnit.SECONDS)))
                .expiration(Date.from(currentTime.plus(5, ChronoUnit.MINUTES)))
                .issuer(clientId)
                .signWith(getPrivateKey(), Jwts.SIG.RS256)
                .compact();
        return token;
    }

    /**
     * Method to retrieve a token to act as a particular app installation in a particular organization
     *
     * @param course ID of the particular app installation to act as
     * @return Token accepted by GitHub to act as a particular installation.
     */
    public String getInstallationToken(Course course) throws JsonProcessingException, NoSuchAlgorithmException, InvalidKeySpecException, NoLinkedOrganizationException {
        if(course.getOrgName() == null || course.getInstallationId() == null){
            throw new NoLinkedOrganizationException(course.getCourseName());
        }else {
            String token = getJwt();
            String ENDPOINT = "https://api.github.com/app/installations/" + course.getOrgName() + "/access_tokens";
            HttpHeaders headers = new HttpHeaders();
            headers.add("Authorization", "Bearer " + token);
            headers.add("Accept", "application/vnd.github+json");
            headers.add("X-GitHub-Api-Version", "2022-11-28");
            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(ENDPOINT, HttpMethod.POST, entity, String.class);
            JsonNode responseJson = objectMapper.readTree(response.getBody());
            String installationToken = responseJson.get("token").asText();
            return installationToken;
        }
    }
}
