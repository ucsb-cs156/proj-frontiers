package edu.ucsb.cs156.frontiers.startup;

import edu.ucsb.cs156.frontiers.entities.Admin;
import edu.ucsb.cs156.frontiers.repositories.AdminRepository;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** This class contains a `run` method that is called once at application startup time. */
@Slf4j
@Component
public class FrontiersStartup {

  @Value("#{'${app.admin.emails}'.split(',')}")
  List<String> adminEmails;

  @Value("${app.webhook.secret}")
  String webhookSecret;

  @Autowired AdminRepository adminRepository;

  /**
   * Called once at application startup time . Put code here if you want it to run once each time
   * the Spring Boot application starts up in all environments.
   */
  public void alwaysRunOnStartup() {
    log.info("alwaysRunOnStartup called");

    validateWebhookSecret();

    try {
      adminEmails.forEach(
          (email) -> {
            Admin admin = new Admin(email);
            adminRepository.save(admin);
          });
    } catch (Exception e) {
      log.error("Error in loading all ADMIN_EMAILS:", e);
    }
  }

  private void validateWebhookSecret() {
    if (webhookSecret == null || webhookSecret.length() < 10) {
      String error =
          String.format(
              "WEBHOOK_SECRET must be at least 10 characters long. Current length: %d",
              webhookSecret == null ? 0 : webhookSecret.length());
      log.error(error);
      throw new IllegalStateException(error);
    }
    log.info("Webhook secret validation passed");
  }
}
