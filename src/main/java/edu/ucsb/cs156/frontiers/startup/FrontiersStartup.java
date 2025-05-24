package edu.ucsb.cs156.frontiers.startup;

import edu.ucsb.cs156.frontiers.entities.Admin;
import edu.ucsb.cs156.frontiers.repositories.AdminRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.List;

/** This class contains a `run` method that is called once at application startup time. */
@Slf4j
@Component
public class FrontiersStartup {

    private final AdminRepository adminRepository;

    @Value("${app.admin.emails}")
    private List<String> adminEmails;

    public FrontiersStartup(AdminRepository adminRepository) {
        this.adminRepository = adminRepository;
    }

    /**
    * Called once at application startup time . Put code here if you want it to run once each time
    * the Spring Boot application starts up in all environments.
    */
    public void alwaysRunOnStartup() {
        log.info("Running FrontiersStartup.alwaysRunOnStartup");

        for (String email : adminEmails) {
            if (!adminRepository.existsByEmail(email)) {
                Admin admin = Admin.builder().email(email).build();
                adminRepository.save(admin);
                log.info("Added admin: {}", email);
            } else {
                log.info("Admin already exists: {}", email);
            }
        }
    }
}