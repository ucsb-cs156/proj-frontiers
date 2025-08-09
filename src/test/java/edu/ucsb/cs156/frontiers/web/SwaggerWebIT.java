package edu.ucsb.cs156.frontiers.web;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import edu.ucsb.cs156.frontiers.testconfig.IntegrationConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ResourceLock("port-8080")
@Import(IntegrationConfig.class)
@ActiveProfiles("integration")
public class SwaggerWebIT {
  @Value("${app.playwright.headless:true}")
  private boolean runHeadless;

  @LocalServerPort private int port;

  private Browser browser;
  private Page page;

  @BeforeEach
  public void setup() {
    browser =
        Playwright.create()
            .chromium()
            .launch(new BrowserType.LaunchOptions().setHeadless(runHeadless));

    BrowserContext context = browser.newContext();
    page = context.newPage();

    String url = String.format("http://localhost:%d/swagger-ui/index.html", port);
    page.navigate(url);
  }

  @AfterEach
  public void teardown() {
    browser.close();
  }

  @Test
  public void swagger_page_can_be_loaded() throws Exception {
    assertThat(page.getByText("Swagger: UCSB CMPSC 156 team01")).isVisible();

    assertThat(page.getByText("Home Page")).isVisible();

    assertThat(page.getByText("H2 Console (only on localhost)")).isVisible();
  }
}
