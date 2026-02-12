package edu.ucsb.cs156.frontiers.web;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import com.microsoft.playwright.Page;
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
public class CourseCreationWebIT extends WebTestCase {

  @Test
  public void can_create_course() {
    setupUser(true, true);
    page.getByText("Create Course").click();
    page.getByLabel("Course Name").fill("test course");
    page.getByLabel("Term").fill("Fall 2025");
    page.getByLabel("School").fill("UCSB");
    page.getByText("Create", new Page.GetByTextOptions().setExact(true)).click();
    assertThat(page.getByTestId("CoursesTable-cell-row-0-col-courseName-link"))
        .containsText("test course");
  }
}
