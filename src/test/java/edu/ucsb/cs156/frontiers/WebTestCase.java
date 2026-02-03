package edu.ucsb.cs156.frontiers;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import edu.ucsb.cs156.frontiers.entities.Admin;
import edu.ucsb.cs156.frontiers.entities.User;
import edu.ucsb.cs156.frontiers.repositories.AdminRepository;
import edu.ucsb.cs156.frontiers.repositories.UserRepository;
import edu.ucsb.cs156.frontiers.services.wiremock.WiremockServiceImpl;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.wiremock.extension.jwt.JwtExtensionFactory;

@ActiveProfiles("integration")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public abstract class WebTestCase {
  @Autowired UserRepository userRepository;
  @Autowired AdminRepository adminRepository;

  @LocalServerPort private int port;

  @Value("${app.playwright.headless:true}")
  private boolean runHeadless;

  private static WireMockServer wireMockServer;

  protected Browser browser;
  protected Page page;

  @BeforeAll
  public static void setupWireMock() {
    wireMockServer =
        new WireMockServer(
            options().port(8090).globalTemplating(true).extensions(new JwtExtensionFactory()));

    WiremockServiceImpl.setupOauthMocks(wireMockServer, false);

    wireMockServer.start();
  }

  @AfterAll
  public static void teardownWiremock() {
    wireMockServer.stop();
  }

  @AfterEach
  public void teardown() {
    browser.close();
  }

  public void setupUser(boolean isAdmin, boolean linkedGitHub) {
    WiremockServiceImpl.setupOauthMocks(wireMockServer, isAdmin);

    User user;
    if (isAdmin) {
      user =
          User.builder()
              .email("admingaucho@ucsb.edu")
              .familyName("Gaucho")
              .givenName("Chris")
              .fullName("Chris Gaucho")
              .googleSub("123456789")
              .pictureUrl("")
              .build();
    } else {
      user =
          User.builder()
              .email("cgaucho@ucsb.edu")
              .familyName("Gaucho")
              .givenName("Chris")
              .fullName("Chris Gaucho")
              .googleSub("123456789")
              .pictureUrl("")
              .build();
    }

    user.setGithubId(linkedGitHub ? 123456789 : null);
    user.setGithubLogin(linkedGitHub ? "teststudent" : null);

    userRepository.save(user);
    adminRepository.save(Admin.builder().email("admingaucho@ucsb.edu").build());

    browser =
        Playwright.create()
            .chromium()
            .launch(new BrowserType.LaunchOptions().setHeadless(runHeadless));

    BrowserContext context = browser.newContext();
    page = context.newPage();

    String url = String.format("http://localhost:%d/oauth2/authorization/my-oauth-provider", port);
    page.navigate(url);

    if (isAdmin) {
      page.locator("#username").fill("admingaucho@ucsb.edu");
    } else {
      page.locator("#username").fill("cgaucho@ucsb.edu");
    }

    page.locator("#password").fill("password");
    page.locator("#submit").click();
  }
}
