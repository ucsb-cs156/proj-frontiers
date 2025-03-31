package edu.ucsb.cs156.frontiers;

import java.time.ZonedDateTime;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.io.ClassPathResource;

import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import edu.ucsb.cs156.frontiers.services.wiremock.WiremockService;
import lombok.extern.slf4j.Slf4j;

/**
 * The FrontiersMain class is the main entry point for the application.
 */
@SpringBootApplication
@Slf4j
@EnableAsync // for @Async annotation for JobsService
@EnableScheduling // for @Scheduled annotation for JobsService
@EnableJpaAuditing(dateTimeProviderRef = "utcDateTimeProvider")
// enables automatic population of @CreatedDate and @LastModifiedDate
public class FrontiersMain {

  @Autowired
  WiremockService wiremockService;

  /**
   * When using the wiremock profile, this method will call the code needed to set
   * up the wiremock services
   */
  @Profile("wiremock")
  @Bean
  public ApplicationRunner wiremockApplicationRunner() {
    return arg -> {
      log.info("wiremock mode");
      wiremockService.init();
      log.info("wiremockApplicationRunner completed");
    };
  }

  /**
   * Hook that can be used to set up any services needed for development
   */
  @Profile("development")
  @Bean
  public ApplicationRunner developmentApplicationRunner() {
    return arg -> {
      log.info("development mode");
      log.info("developmentApplicationRunner completed");
    };
  }

  /**
   * The main method is the entry point for the application.
   * 
   * @param args command line arguments, typically unused for Spring Boot
   *             applications
   */
  public static void main(String[] args) {
    SpringApplication.run(FrontiersMain.class, args);
  }

  /**
   * This method provides a DateTimeProvider that always returns the current
   * UTC time.
   * This is used to ensure that all timestamps in the database are in UTC.
   * @return a DateTimeProvider that always returns the current UTC time
   */
  @Bean
  public DateTimeProvider utcDateTimeProvider() {
    return () -> {
      ZonedDateTime now = ZonedDateTime.now();
      return Optional.of(now);
    };
  }
  /** 
   *  See: https://www.baeldung.com/spring-git-information
   *  @return a propertySourcePlaceholderConfigurer for git.properties
   */
  @Bean
  public static PropertySourcesPlaceholderConfigurer placeholderConfigurer() {
    PropertySourcesPlaceholderConfigurer propsConfig = new PropertySourcesPlaceholderConfigurer();
    propsConfig.setLocation(new ClassPathResource("git.properties"));
    propsConfig.setIgnoreResourceNotFound(true);
    propsConfig.setIgnoreUnresolvablePlaceholders(true);
    return propsConfig;
  }
}