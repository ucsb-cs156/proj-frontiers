package edu.ucsb.cs156.frontiers.web;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import edu.ucsb.cs156.frontiers.WebTestCase;
import edu.ucsb.cs156.frontiers.testconfig.IntegrationConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("integration")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@ResourceLock("port-8080")
@Import(IntegrationConfig.class)
public class OauthWebIT extends WebTestCase {
  @Test
  public void regular_user_can_login_logout() throws Exception {
    setupUser(false);
    assertThat(page.getByText("Log Out")).isVisible();
    assertThat(page.getByText("Welcome, cgaucho@ucsb.edu")).isVisible();
    page.getByText("Log Out").click();

    assertThat(page.getByText("Log In")).isVisible();
    assertThat(page.getByText("Log Out")).not().isVisible();
  }
}
