package edu.ucsb.cs156.frontiers.startup;

import edu.ucsb.cs156.frontiers.entities.Admin;
import edu.ucsb.cs156.frontiers.repositories.AdminRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FrontiersStartupTests {

    @Mock
    private AdminRepository adminRepository;

    @InjectMocks
    private FrontiersStartup startup;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void whenAdminEmailsIsNull_thenNoRepoInteraction() {
        ReflectionTestUtils.setField(startup, "adminEmails", null);
        startup.alwaysRunOnStartup();
        verifyNoInteractions(adminRepository);
    }

    @Test
    void whenAdminEmailsIsEmpty_thenNoRepoInteraction() {
        ReflectionTestUtils.setField(startup, "adminEmails", "");
        startup.alwaysRunOnStartup();
        verifyNoInteractions(adminRepository);
    }

    @Test
    void whenSingleNewEmail_thenSaveCalledOnce() {
        ReflectionTestUtils.setField(startup, "adminEmails", "new@example.com");

        when(adminRepository.findByEmail("new@example.com"))
            .thenReturn(Optional.empty());

        startup.alwaysRunOnStartup();

        // verify find + save
        verify(adminRepository).findByEmail("new@example.com");
        ArgumentCaptor<Admin> captor = ArgumentCaptor.forClass(Admin.class);
        verify(adminRepository).save(captor.capture());

        assertEquals("new@example.com",
                     captor.getValue().getEmail());
    }

    @Test
    void whenSingleExistingEmail_thenSaveNeverCalled() {
        ReflectionTestUtils.setField(startup, "adminEmails", "exists@example.com");

        when(adminRepository.findByEmail("exists@example.com"))
            .thenReturn(Optional.of(new Admin()));

        startup.alwaysRunOnStartup();

        verify(adminRepository).findByEmail("exists@example.com");
        verify(adminRepository, never()).save(any());
    }

    @Test
    void whenMultipleEmails_mixedNewAndExisting() {
        ReflectionTestUtils.setField(
            startup,
            "adminEmails",
            " first@foo.com  ,second@bar.com,third@baz.com "
        );

        // first and third are new, second already exists
        when(adminRepository.findByEmail("first@foo.com"))
            .thenReturn(Optional.empty());
        when(adminRepository.findByEmail("second@bar.com"))
            .thenReturn(Optional.of(new Admin()));
        when(adminRepository.findByEmail("third@baz.com"))
            .thenReturn(Optional.empty());

        startup.alwaysRunOnStartup();

        // findByEmail called for each trimmed address
        verify(adminRepository).findByEmail("first@foo.com");
        verify(adminRepository).findByEmail("second@bar.com");
        verify(adminRepository).findByEmail("third@baz.com");

        // save called only for first and third
        ArgumentCaptor<Admin> captor = ArgumentCaptor.forClass(Admin.class);
        verify(adminRepository, times(2)).save(captor.capture());

        assertTrue(
            captor.getAllValues()
                   .stream()
                   .map(Admin::getEmail)
                   .toList()
                   .containsAll(
                       List.of("first@foo.com", "third@baz.com")
                   )
        );
    }
}
