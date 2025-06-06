package edu.ucsb.cs156.frontiers.startup;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import edu.ucsb.cs156.frontiers.entities.Admin;
import edu.ucsb.cs156.frontiers.repositories.AdminRepository;

@Slf4j
@Component
public class FrontiersStartup {

  // Repository for admins
  @Autowired
  private AdminRepository adminRepository;

  @Value("${app.admin.emails}")
  private String adminEmails;

  public void alwaysRunOnStartup() {
    log.info("AdminStartup.alwaysRunOnStartup called");
    // Split the comma-separated emails and add them to the database if they don't exist
    if (adminEmails != null && !adminEmails.isEmpty()) {
        String[] emails = adminEmails.split(",");
        for (String email : emails) {
            email = email.trim();
            log.info("Adding admin: {}", email);
            
            // Check if admin already exists
            if (!adminRepository.findByEmail(email).isPresent()) {
                Admin admin = new Admin();
                admin.setEmail(email);
                adminRepository.save(admin);
                log.info("Admin added: {}", email);
            } else {
                log.info("Admin already exists: {}", email);
            }
        }
    }
  }
}
