package edu.ucsb.cs156.frontiers.startup;

import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePropertySource;

@Order(Ordered.LOWEST_PRECEDENCE)
@Slf4j
public class AzureProfileEnabler implements EnvironmentPostProcessor {

  @Override
  public void postProcessEnvironment(
      ConfigurableEnvironment environment, SpringApplication application) {
    if (System.getenv("MICROSOFT") != null && System.getenv("MICROSOFT").equals("true")) {
      environment.addActiveProfile("microsoft");
      try {
        Resource resource = new ClassPathResource("application-microsoft.properties");
        if (resource.exists()) {
          ResourcePropertySource propertySource =
              new ResourcePropertySource("application-microsoft", resource);
          environment.getPropertySources().addLast(propertySource);
        }
      } catch (IOException e) {
        // Handle exception appropriately
        log.error(
            "Azure Oauth2 data file is missing. Please check that application-microsoft exists.");
      }
    }
  }
}
