package edu.ucsb.cs156.frontiers.startup;

import edu.ucsb.cs156.frontiers.entities.Admin;
import edu.ucsb.cs156.frontiers.repositories.AdminRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
@Component
public class FrontiersStartup {

    @Autowired
    private AdminRepository adminRepository;

    @Value("${admin.emails:}")
    private String adminEmails;

    public void alwaysRunOnStartup() {
        log.info("FrontiersStartup.alwaysRunOnStartup called");

        String[] emails = adminEmails.split(",");
        for (String email : emails) {
            email = email.trim();
            if (!email.isEmpty()) {
                if (adminRepository.findById(email).isEmpty()) {
                    adminRepository.save(Admin.builder().email(email).build());
                    log.info("Added admin: " + email);
                } else {
                    log.info("Admin already exists: " + email);
                }
            }
        }
    }
}
