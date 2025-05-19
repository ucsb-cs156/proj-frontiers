package edu.ucsb.cs156.frontiers.startup;

import java.util.Arrays;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import edu.ucsb.cs156.frontiers.entities.Admin;
import edu.ucsb.cs156.frontiers.repositories.AdminRepository;

/** Code that must run once on every startup (all profiles). */
@Slf4j
@Component
public class FrontiersStartup {

    private final AdminRepository adminRepo;

    @Value("${admin.emails:}")
    private String adminEmailsCsv;     // filled from env var

    public FrontiersStartup(AdminRepository adminRepo) {
        this.adminRepo = adminRepo;
    }

    /** Runs once per app launch. */
    public void alwaysRunOnStartup() {
        log.info("FrontiersStartup.alwaysRunOnStartup called");

        Arrays.stream(adminEmailsCsv.split(","))
              .map(String::trim)
              .filter(e -> !e.isEmpty())
              .forEach(this::ensureAdminRow);
    }

    private void ensureAdminRow(String email) {
        adminRepo.findByEmail(email)
                 .orElseGet(() -> {
                     log.info("Inserting admin {}", email);
                     return adminRepo.save(new Admin(email));
                 });
    }
}
