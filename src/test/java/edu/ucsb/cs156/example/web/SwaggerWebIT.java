package edu.ucsb.cs156.example.web;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.AriaRole;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("integration")
public class SwaggerWebIT {
    @Value("${app.playwright.headless:true}")
    private boolean runHeadless;

    @LocalServerPort
    private int port;

    private Browser browser;
    private Page page;

    @BeforeEach
    public void setup() {
        browser = Playwright.create().chromium().launch(new BrowserType.LaunchOptions().setHeadless(runHeadless));

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
        assertThat(page.getByText("Swagger: UCSB CMPSC 156 team01"))
                .isVisible();

        assertThat(page.getByText("Home Page"))
                .isVisible();

        assertThat(page.getByText("H2 Console (only on localhost)"))
                .isVisible();
    }

    /**
     * This test checks that the Swagger page has a few of then endpoints for the
     * UCSBDiningCommons API.
     * It is probably not necessary to test all controllers or endpoints; we are
     * mainly checking that
     * swagger is appropriate configured and operational. Presumably, if one
     * controller's
     * endpoints are present, then all controllers' endpoints are present. Think of
     * it as
     * a kind of "smoke test" to make sure that Swagger is working, rather than
     * a comprehensive test of the Swagger page.
     */
    @Test
    public void swagger_page_has_endpoints_for_UCSBDiningCommons() throws Exception {


        assertThat(
                page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("UCSBDiningCommons").setExact(true)))
                .isVisible();

        assertThat(page.getByText("Get a single commons"))
                .isVisible();

        assertThat(page.getByText("Update a single commons"))
                .isVisible();
    }

}