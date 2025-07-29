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

import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import edu.ucsb.cs156.frontiers.services.wiremock.WiremockService;
import lombok.extern.slf4j.Slf4j;

import static org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO;

/**
 * The FrontiersMain class is the main entry point for the application.
 */
@SpringBootApplication
@Slf4j
@EnableAsync // for @Async annotation for JobsService
@EnableScheduling // for @Scheduled annotation for JobsService
// enables automatic population of @CreatedDate and @LastModifiedDate
@EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO) //Replaces PageImpl instances with a permanent type PagedModel that is not subject to change
//See https://docs.spring.io/spring-data/commons/reference/repositories/core-extensions.html#core.web.page
public class FrontiersMain {

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