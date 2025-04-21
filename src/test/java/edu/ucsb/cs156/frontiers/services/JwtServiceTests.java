package edu.ucsb.cs156.frontiers.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import edu.ucsb.cs156.frontiers.services.wiremock.WiremockService;
import edu.ucsb.cs156.frontiers.testconfig.DummyClock;
import edu.ucsb.cs156.frontiers.testconfig.TestConfig;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureDataJpa;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.match.MockRestRequestMatchers;

import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;

@TestPropertySource(locations = "/testproperties.properties")
@RestClientTest(JwtService.class)
@AutoConfigureDataJpa
public class JwtServiceTests {


    @Autowired private MockRestServiceServer mockRestServiceServer;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private WiremockService wiremockService;

    @Value("${app.public.key:no-key-present}")
    private String publicKey;

    private String expectedToken = "ghs_16C7e42F292c6912E7710c838347Ae178B4a";

    @MockitoBean
    private DateTimeProvider dateTimeProvider;

    private Instant setInstant = Instant.parse("2024-05-23T08:00:00.00Z");


    @Test
    public void testGettingJwt() throws NoSuchAlgorithmException, InvalidKeySpecException {
        doReturn(Optional.of(setInstant)).when(dateTimeProvider).getNow();
        String jwt = jwtService.getJwt();
        validateJwt(jwt, setInstant);
    }

    @Test
    public void testObtainingInstallationToken() throws JsonProcessingException, NoSuchAlgorithmException, InvalidKeySpecException {
        String expectedResult = String.format("""
                {
                  "token": "%s",
                  "expires_at": "2016-07-11T22:14:10Z",
                  "permissions": {
                    "issues": "write",
                    "contents": "read"
                  },
                  "repository_selection": "all"
                }
                """, expectedToken);
        String installationId = "03112004";
        String expectedURL = "https://api.github.com/app/installations/"+installationId+"/access_tokens";
        mockRestServiceServer.expect(requestTo(expectedURL))
                .andExpect(header("Accept", "application/vnd.github+json"))
                .andExpect(header("X-GitHub-Api-Version", "2022-11-28"))
                .andExpect(header("Authorization", Matchers.startsWith("Bearer ")))
                .andRespond(withSuccess(expectedResult, MediaType.APPLICATION_JSON));

        doReturn(Optional.of(setInstant)).when(dateTimeProvider).getNow();
        String token = jwtService.getInstallationToken(installationId);
        assertEquals(expectedToken, token);
    }

    public void validateJwt(String compacted, Instant setInstant) throws InvalidKeySpecException, NoSuchAlgorithmException {
        String key = publicKey;
        key = key.replace("-----BEGIN PUBLIC KEY-----", "");
        key = key.replace("-----END PUBLIC KEY-----", "");
        key = key.replaceAll(" ", "");
        key = key.replaceAll(System.lineSeparator(), "");
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        byte[] keyBytes = Base64.getDecoder().decode(key.getBytes());
        X509EncodedKeySpec x509EncodedKeySpec=new X509EncodedKeySpec(keyBytes);
        PublicKey secretKeySpec =  keyFactory.generatePublic(x509EncodedKeySpec);
        JwtParser parser = Jwts.parser()
                .verifyWith(secretKeySpec)
                .clock(new DummyClock())
                .requireIssuer("testing-client-id")
                .requireIssuedAt(Date.from(setInstant.minus(30, ChronoUnit.SECONDS)))
                .requireExpiration(Date.from(setInstant.plus(5, ChronoUnit.MINUTES)))
                .build();
        parser.parse(compacted);
    }



}
