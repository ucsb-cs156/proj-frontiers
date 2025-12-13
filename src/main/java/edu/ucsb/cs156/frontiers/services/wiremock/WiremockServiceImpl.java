package edu.ucsb.cs156.frontiers.services.wiremock;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.temporaryRedirect;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.common.ConsoleNotifier;
import com.github.tomakehurst.wiremock.junit.Stubbing;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.wiremock.extension.jwt.JwtExtensionFactory;

/**
 * This is a service for mocking authentication using wiremock
 *
 * <p>This class relies on property values. For hints on testing, see: <a href=
 * "https://www.baeldung.com/spring-boot-testing-configurationproperties">https://www.baeldung.com/spring-boot-testing-configurationproperties</a>
 */
@Slf4j
@Service("wiremockService")
@Profile("wiremock")
@ConfigurationProperties
public class WiremockServiceImpl extends WiremockService {

  WireMockServer wireMockServer;

  /**
   * This method returns the wiremockServer
   *
   * @return the wiremockServer
   */
  public WireMockServer getWiremockServer() {
    return wireMockServer;
  }

  /**
   * This method sets up the necessary mocks for authentication
   *
   * @param s in an instance of a WireMockServer or WireMockExtension
   */
  public static void setupOauthMocks(Stubbing s, boolean isAdmin) {

    s.stubFor(
        get(urlPathMatching("/oauth/authorize.*"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "text/html")
                    .withHeader("Set-Cookie", "nonce={{request.query.nonce}};path=/")
                    .withBodyFile("login.html")));

    s.stubFor(
        post(urlPathEqualTo("/login"))
            .willReturn(
                temporaryRedirect(
                    "{{formData request.body 'form' urlDecode=true}}{{{form.redirectUri}}}?code={{{request.cookies.nonce}}}&state={{{form.state}}}")));

    String emailAddress = "cgaucho@ucsb.edu";
    if (isAdmin) {
      emailAddress = "admingaucho@ucsb.edu";
    }

    if (isAdmin) {
      s.stubFor(
          post(urlPathEqualTo("/oauth/token"))
              .willReturn(
                  okJson(
                      """
                          {{#trim}}
                          {{formData request.body 'form'}}
                          {{#assign 'emailAddress'}}admingaucho@ucsb.edu{{/assign}}
                          {{#assign 'subject'}}{{{base64 emailAddress padding=false}}}{{/assign}}
                          {{#assign 'accessToken'}}{{{base64 (stringFormat 'access..%s' emailAddress) padding=false}}}{{/assign}}
                          {{#assign 'submittedNonce'}}{{form.code}}{{/assign}}
                          {{#assign 'idToken'}}{{#trim}}
                          {{{jwt alg='RS256' email=emailAddress iss=request.baseUrl aud='integrationtest' nonce=submittedNonce sub=subject}}}
                          {{/trim}}{{/assign}}

                          {
                          "access_token":"{{{accessToken}}}",
                          "token_type": "Bearer",
                          "id_token": "{{{idToken}}}"
                          }
  {{/trim}}""")));
    } else {
      s.stubFor(
          post(urlPathEqualTo("/oauth/token"))
              .willReturn(
                  okJson(
                      """
                          {{#trim}}
                          {{formData request.body 'form'}}
                          {{#assign 'emailAddress'}}cgaucho@ucsb.edu{{/assign}}
                          {{#assign 'subject'}}{{{base64 emailAddress padding=false}}}{{/assign}}
                          {{#assign 'accessToken'}}{{{base64 (stringFormat 'access..%s' emailAddress) padding=false}}}{{/assign}}
                          {{#assign 'submittedNonce'}}{{form.code}}{{/assign}}
                          {{#assign 'idToken'}}{{#trim}}
                          {{{jwt alg='RS256' email=emailAddress iss=request.baseUrl aud='integrationtest' nonce=submittedNonce sub=subject}}}
                          {{/trim}}{{/assign}}

                          {
                          "access_token":"{{{accessToken}}}",
                          "token_type": "Bearer",
                          "id_token": "{{{idToken}}}"
                          }
  {{/trim}}""")));
    }
    if (isAdmin) {
      s.stubFor(
          get(urlPathMatching("/userinfo"))
              .willReturn(
                  aResponse()
                      .withStatus(200)
                      .withHeader("Content-Type", "application/json")
                      .withBody(
                          """
                              {{#trim}}
                                   {{#assign 'accessToken'}}{{{regexExtract request.headers.Authorization.0 '[^\\s]*$'}}}{{/assign}}
                                   {{regexExtract (base64 accessToken decode=true) '(.+?)\\.\\.(.+?)$' 'parts'}}
                                   {{#assign 'email'}}admingaucho@ucsb.edu{{/assign}}
                                   {{#assign 'sub'}}{{{base64 email padding=false}}}{{/assign}}
                                   {
                                   "email": "{{{email}}}",
                                   "sub": "{{{sub}}}"
                                   }
                               {{/trim}}
                      """)));
    } else {
      s.stubFor(
          get(urlPathMatching("/userinfo"))
              .willReturn(
                  aResponse()
                      .withStatus(200)
                      .withHeader("Content-Type", "application/json")
                      .withBody(
                          """
                              {{#trim}}
                                   {{#assign 'accessToken'}}{{{regexExtract request.headers.Authorization.0 '[^\\s]*$'}}}{{/assign}}
                                   {{regexExtract (base64 accessToken decode=true) '(.+?)\\.\\.(.+?)$' 'parts'}}
                                   {{#assign 'email'}}cgaucho@ucsb.edu{{/assign}}
                                   {{#assign 'sub'}}{{{base64 email padding=false}}}{{/assign}}
                                   {
                                   "email": "{{{email}}}",
                                   "sub": "{{{sub}}}"
                                   }
                               {{/trim}}
                      """)));
    }

    s.stubFor(
        get(urlPathMatching("/.well-known/jwks\\.json"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{{{jwks}}}")));
  }

  /** This method initializes the WireMockServer */
  public void init() {
    log.info("WiremockServiceImpl.init() called");

    WireMockServer wireMockServer =
        new WireMockServer(
            wireMockConfig()
                .port(8090)
                .globalTemplating(true)
                .extensions(new JwtExtensionFactory())
                .notifier(new ConsoleNotifier(true)));
    setupOauthMocks(wireMockServer, true);

    wireMockServer.start();

    log.info("WiremockServiceImpl.init() completed");
  }
}
