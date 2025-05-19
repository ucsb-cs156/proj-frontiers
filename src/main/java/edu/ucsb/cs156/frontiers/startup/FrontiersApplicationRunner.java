package edu.ucsb.cs156.frontiers.startup;

import edu.ucsb.cs156.frontiers.entities.Admin;
import edu.ucsb.cs156.frontiers.repositories.AdminRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.ApplicationArguments;
import org.springframework.stereotype.Component;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;

/** This class contains a `run` method that is called once at application startup time. */
@Slf4j
@Component
public class FrontiersApplicationRunner implements ApplicationRunner {

    private final AdminRepository adminRepository;

    @Value("${admin.emails:}")
    private String adminEmailsCsv;

    public FrontiersApplicationRunner(AdminRepository adminRepository) {
        this.adminRepository = adminRepository;
    }

    /** Called once at application startup time, development only */
    @Override
    public void run(ApplicationArguments args) {
        log.info("FrontiersApplicationRunner.run() called");
        
        if (adminEmailsCsv == null || adminEmailsCsv.trim().isEmpty()) {
            log.info("No admin emails configured, skipping admin initialization");
            return;
        }

        Set<String> existingEmails = adminRepository.findAll().stream()
            .map(Admin::getEmail)
            .collect(Collectors.toSet());

        Arrays.stream(adminEmailsCsv.split(","))
            .map(String::trim)
            .filter(email -> !email.isEmpty())
            .filter(email -> !existingEmails.contains(email))
            .forEach(email -> {
                log.info("Adding admin: {}", email);
                adminRepository.save(new Admin(email));
            });
    }
}