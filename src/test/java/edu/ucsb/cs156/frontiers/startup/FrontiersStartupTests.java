package edu.ucsb.cs156.frontiers.startup;

import edu.ucsb.cs156.frontiers.entities.Admin;
import edu.ucsb.cs156.frontiers.repositories.AdminRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestPropertySource(properties = {
        "admin.emails=test1@ucsb.edu,test2@ucsb.edu"
})
class FrontiersStartupTests {

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private FrontiersStartup frontiersStartup;   // real bean

    @BeforeEach
    void clearRepo() {
        adminRepository.deleteAll();
    }

    /** happy‑path: both emails from ADMIN_EMAILS inserted */
    @Test
    void testAdminsInsertedOnStartup() {
        frontiersStartup.alwaysRunOnStartup();
        assertThat(adminRepository.findById("test1@ucsb.edu")).isPresent();
        assertThat(adminRepository.findById("test2@ucsb.edu")).isPresent();
    }

    /** branch: entry already exists, so no duplicate row */
    @Test
    void testSkipsExistingAdmin() {
        adminRepository.save(Admin.builder().email("test1@ucsb.edu").build());
        frontiersStartup.alwaysRunOnStartup();
        assertThat(adminRepository.findAll())
                .filteredOn(a -> a.getEmail().equals("test1@ucsb.edu"))
                .hasSize(1);
    }

    /** trims surrounding whitespace */
    @Test
    void testTrimsWhitespace() {
        FrontiersStartup trimmed = new FrontiersStartup();
        ReflectionTestUtils.setField(trimmed, "adminRepository", adminRepository);
        ReflectionTestUtils.setField(trimmed, "adminEmails", "  spaced@ucsb.edu  ");

        trimmed.alwaysRunOnStartup();
        assertThat(adminRepository.findById("spaced@ucsb.edu")).isPresent();
    }

    /** handles empty / blank entries gracefully */
    @Test
    void testSkipsEmptyEntries() {
        FrontiersStartup blank = new FrontiersStartup();
        ReflectionTestUtils.setField(blank, "adminRepository", adminRepository);
        ReflectionTestUtils.setField(blank, "adminEmails", ", , ,");

        blank.alwaysRunOnStartup();
        assertThat(adminRepository.findAll()).isEmpty();
    }
}
