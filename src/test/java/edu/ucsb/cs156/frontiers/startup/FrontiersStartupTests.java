package edu.ucsb.cs156.frontiers.startup;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import edu.ucsb.cs156.frontiers.entities.Admin;
import edu.ucsb.cs156.frontiers.repositories.AdminRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

class FrontiersStartupTests {

  @Mock private AdminRepository adminRepository;

  private FrontiersStartup frontiersStartup;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    frontiersStartup = new FrontiersStartup();
    frontiersStartup.adminRepository = adminRepository;
    frontiersStartup.adminEmails = List.of("acdamstedt@ucsb.edu", "phtcon@ucsb.edu");
  }

  @Test
  void test_AlwaysRunOnStartup_saves_admins() {
    frontiersStartup.webhookSecret = "valid_secret_123";
    frontiersStartup.alwaysRunOnStartup();

    verify(adminRepository).save(new Admin("acdamstedt@ucsb.edu"));
    verify(adminRepository).save(new Admin("phtcon@ucsb.edu"));
    verifyNoMoreInteractions(adminRepository);
  }

  @Test
  void test_AlwaysRunOnStartup_handles_exception() {
    frontiersStartup.webhookSecret = "valid_secret_123";
    doThrow(new RuntimeException("Simulated error")).when(adminRepository).save(any(Admin.class));

    frontiersStartup.alwaysRunOnStartup();

    verify(adminRepository, times(1)).save(any(Admin.class));
  }

  @Test
  void test_AlwaysRunOnStartup_fails_with_short_webhook_secret() {
    frontiersStartup.webhookSecret = "short";

    IllegalStateException exception =
        assertThrows(IllegalStateException.class, () -> frontiersStartup.alwaysRunOnStartup());

    assertTrue(
        exception.getMessage().contains("WEBHOOK_SECRET must be at least 10 characters long"));
    assertTrue(exception.getMessage().contains("Current length: 5"));
    verifyNoInteractions(adminRepository);
  }

  @Test
  void test_AlwaysRunOnStartup_fails_with_null_webhook_secret() {
    frontiersStartup.webhookSecret = null;

    IllegalStateException exception =
        assertThrows(IllegalStateException.class, () -> frontiersStartup.alwaysRunOnStartup());

    assertTrue(
        exception.getMessage().contains("WEBHOOK_SECRET must be at least 10 characters long"));
    assertTrue(exception.getMessage().contains("Current length: 0"));
    verifyNoInteractions(adminRepository);
  }

  @Test
  void test_AlwaysRunOnStartup_succeeds_with_minimum_length_webhook_secret() {
    frontiersStartup.webhookSecret = "1234567890"; // exactly 10 characters

    frontiersStartup.alwaysRunOnStartup();

    verify(adminRepository).save(new Admin("acdamstedt@ucsb.edu"));
    verify(adminRepository).save(new Admin("phtcon@ucsb.edu"));
    verifyNoMoreInteractions(adminRepository);
  }
}
