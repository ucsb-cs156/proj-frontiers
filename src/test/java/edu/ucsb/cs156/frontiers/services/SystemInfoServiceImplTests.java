package edu.ucsb.cs156.frontiers.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.test.context.NestedTestConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import edu.ucsb.cs156.frontiers.models.SystemInfo;
import edu.ucsb.cs156.frontiers.services.SystemInfoService;
import edu.ucsb.cs156.frontiers.services.SystemInfoServiceImpl;

// The unit under test relies on property values
// For hints on testing, see: https://www.baeldung.com/spring-boot-testing-configurationproperties

@ExtendWith(SpringExtension.class)
@EnableConfigurationProperties(value = SystemInfoServiceImpl.class)
@NestedTestConfiguration(NestedTestConfiguration.EnclosingConfiguration.INHERIT)
@TestPropertySource("classpath:application-development.properties")
class SystemInfoServiceImplTests {

  @Autowired
  private SystemInfoService systemInfoService;

  @Test
  void test_getSystemInfo() {
    SystemInfo si = systemInfoService.getSystemInfo();
    assertTrue(si.getSpringH2ConsoleEnabled());
    assertTrue(si.getShowSwaggerUILink());
    assertTrue(si.getGithubUrl().startsWith(si.getSourceRepo()));
    assertTrue(si.getGithubUrl().endsWith(si.getCommitId()));
    assertTrue(si.getGithubUrl().contains("/commit/"));
  }

  @Test
  void test_githubUrl() {
    assertEquals(
        SystemInfoServiceImpl.githubUrl(
            "https://github.com/ucsb-cs156/proj-courses", "abcdef12345"),
        "https://github.com/ucsb-cs156/proj-courses/commit/abcdef12345");
    assertNull(SystemInfoServiceImpl.githubUrl(null, null));
    assertNull(SystemInfoServiceImpl.githubUrl("x", null));
    assertNull(SystemInfoServiceImpl.githubUrl(null, "x"));
  }

  @Nested
  @TestPropertySource(properties = "spring.security.oauth2.client.registration.azure-dev.provider=azure")
  class MicrosoftEnabledInfoServiceTests{

    @Autowired
    private SystemInfoService secondaryLoadedService;

    @Test
    public void displaysMicrosoftProvider() {
      SystemInfo si = secondaryLoadedService.getSystemInfo();
      assertEquals("/oauth2/authorization/azure-dev", si.getActiveDirectoryUrl());
    }
  }
}
