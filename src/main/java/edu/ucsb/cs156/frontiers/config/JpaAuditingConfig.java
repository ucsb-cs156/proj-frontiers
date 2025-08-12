package edu.ucsb.cs156.frontiers.config;

import java.time.ZonedDateTime;
import java.util.Optional;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing(dateTimeProviderRef = "utcDateTimeProvider")
public class JpaAuditingConfig {
  /**
   * This method provides a DateTimeProvider that always returns the current UTC time. This is used
   * to ensure that all timestamps in the database are in UTC.
   *
   * @return a DateTimeProvider that always returns the current UTC time
   */
  @Bean
  public DateTimeProvider utcDateTimeProvider() {
    return () -> {
      ZonedDateTime now = ZonedDateTime.now();
      return Optional.of(now);
    };
  }
}
