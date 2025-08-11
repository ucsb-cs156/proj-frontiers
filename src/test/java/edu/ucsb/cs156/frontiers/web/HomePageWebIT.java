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
public class HomePageWebIT {
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
  }

  @AfterEach
  public void teardown() {
    browser.close();
  }

  @Test
  public void home_page_shows_greeting() throws Exception {
    String url = String.format("http://localhost:%d/", port);
    page.navigate(url);

    assertThat(
            page.getByText(
                "This is the MVP for the Frontiers Project.",
                new Page.GetByTextOptions().setExact(true)))
        .isVisible();

    assertThat(page.getByText("Github")).isVisible();
  }
}
