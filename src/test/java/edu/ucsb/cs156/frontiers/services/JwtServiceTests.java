package edu.ucsb.cs156.frontiers.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import edu.ucsb.cs156.frontiers.services.wiremock.WiremockService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.client.MockRestServiceServer;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@TestPropertySource(locations = "/testproperties.yaml")
@RestClientTest(JwtService.class)
public class JwtServiceTests {

    private MockedStatic<Instant> mockedInstant;

    @Autowired private MockRestServiceServer mockRestServiceServer;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private WiremockService wiremockService;

    private String expectedJwt = "eyJhbGciOiJSUzI1NiJ9.eyJpYXQiOjE3MzU2ODk1NzAsImV4cCI6MTczNTY4OTkwMCwiaXNzIjoiJHthcHAuY2xpZW50" +
            "LmlkfSJ9.0WgTqDfUqqVjZODiYdxOXd7QkTKw7ozQd-HLbNK00DIwaqWJJHcO0HQWJfa6MMZHQvs0YtcJXMPAlJ1W-CZrCCyCaUZT_y" +
            "EobBVA9uXJjWINJKW93lYf0XFODidi4WoU3R6TTiM9TxnX3UU4Hzp8Kn6t06Kf6ddmVjz7o_gupjKTuPlipJjBhgCGQog3BcHnmw_u8" +
            "fWaD17jw_xRhjaK-B_Wy6iL6qHXxOVpJCrUpcqPuLit19-1pONErpDvydue1fPr6IlGes1ByKgmvcgl3wsUlYqApWYspEPL_JjBwaJ3Q" +
            "NFuLLxRGwFw55IfxuMopb_EHqFNZ6bTRcmG2--HUw";
    private String expectedToken = "ghs_16C7e42F292c6912E7710c838347Ae178B4a";

    @BeforeEach
    public void setup() {
        mockedInstant = Mockito.mockStatic(Instant.class, Mockito.CALLS_REAL_METHODS);
    }

    @AfterEach
    public void teardown() {
        mockedInstant.close();
    }

    @Test
    public void testGettingJwt() throws NoSuchAlgorithmException, InvalidKeySpecException {
        Instant timeAtTest = Instant.parse("2025-01-01T00:00:00.00Z");
        mockedInstant.when(Instant::now).thenReturn(timeAtTest);
        String result = jwtService.getJwt();
        assertEquals(expectedJwt, result);
    }

    @Test
    public void testObtainingInstallationToken() throws JsonProcessingException, NoSuchAlgorithmException, InvalidKeySpecException {
        Instant timeAtTest = Instant.parse("2025-01-01T00:00:00.00Z");
        mockedInstant.when(Instant::now).thenReturn(timeAtTest);
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
                .andExpect(header("Authorization", "Bearer " + expectedJwt))
                .andExpect(header("Accept", "application/vnd.github+json"))
                .andExpect(header("X-GitHub-Api-Version", "2022-11-28"))
                .andRespond(withSuccess(expectedResult, MediaType.APPLICATION_JSON));

        String token = jwtService.getInstallationToken(installationId);
        assertEquals(expectedToken, token);
    }



}
