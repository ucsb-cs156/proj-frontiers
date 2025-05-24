package edu.ucsb.cs156.frontiers.startup;

import edu.ucsb.cs156.frontiers.entities.Admin;
import edu.ucsb.cs156.frontiers.repositories.AdminRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

public class FrontiersStartupTests {

    private AdminRepository adminRepository;
    private FrontiersStartup frontiersStartup;

    @BeforeEach
    public void setUp() {
        adminRepository = mock(AdminRepository.class);
        frontiersStartup = new FrontiersStartup(adminRepository);
    }

    @Test
    public void testAlwaysRunOnStartup_addsAllEmails() {
        // Arrange

        List<String> testEmails = List.of("admin1@example.com", "admin2@example.com");
        ReflectionTestUtils.setField(frontiersStartup, "adminEmails", testEmails);

        when(adminRepository.existsByEmail(anyString())).thenReturn(false);

        // Act 

        frontiersStartup.alwaysRunOnStartup();

        // Assert

        ArgumentCaptor<Admin> captor = ArgumentCaptor.forClass(Admin.class);
        verify(adminRepository, times(2)).save(captor.capture());

        List<Admin> savedAdmins = captor.getAllValues();
        assertEquals(2, savedAdmins.size());
        assertTrue(savedAdmins.stream().anyMatch(a -> a.getEmail().equals("admin1@example.com")));
        assertTrue(savedAdmins.stream().anyMatch(a -> a.getEmail().equals("admin2@example.com")));
    }

    @Test
    public void test_alwaysRunOnStartup_skips_existing_admins() {
        // Arrange

        List<String> testEmails = List.of("existing@example.com", "new@example.com");
        ReflectionTestUtils.setField(frontiersStartup, "adminEmails", testEmails);

        when(adminRepository.existsByEmail("existing@example.com")).thenReturn(true);
        when(adminRepository.existsByEmail("new@example.com")).thenReturn(false);

        // Act

        frontiersStartup.alwaysRunOnStartup();

        // Assert 

        ArgumentCaptor<Admin> adminCaptor = ArgumentCaptor.forClass(Admin.class);
        verify(adminRepository, times(1)).save(adminCaptor.capture());
        Admin savedAdmin = adminCaptor.getValue();
        assertEquals("new@example.com", savedAdmin.getEmail());

        verify(adminRepository, times(1)).existsByEmail("existing@example.com");
        verify(adminRepository, times(1)).existsByEmail("new@example.com");
    }
}