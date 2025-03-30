package edu.ucsb.cs156.frontiers.services;


import edu.ucsb.cs156.frontiers.entities.RosterStudent;
import edu.ucsb.cs156.frontiers.entities.User;
import edu.ucsb.cs156.frontiers.repositories.RosterStudentRepository;
import edu.ucsb.cs156.frontiers.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

public class UpdateUserServiceTests {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RosterStudentRepository rosterStudentRepository;

    @InjectMocks
    private UpdateUserService updateUserService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testAttachRosterStudents() {
        // Arrange
        User user = User.builder().email("test@example.com").build();

        RosterStudent student1 = new RosterStudent();
        RosterStudent student2 = new RosterStudent();

        List<RosterStudent> matchedStudents = Arrays.asList(student1, student2);

        when(rosterStudentRepository.findAllByEmail("test@example.com")).thenReturn(matchedStudents);

        // Act
        updateUserService.attachRosterStudents(user);

        // Assert
        verify(rosterStudentRepository, times(1)).findAllByEmail("test@example.com");
        verify(rosterStudentRepository, times(1)).saveAll(matchedStudents);

        assertEquals(user, student1.getUser());
        assertEquals(user, student2.getUser());
    }

    @Test
    public void testAttachRosterStudentsAllUsers() {
        // Arrange
        User user1 =  User.builder().build();
        user1.setEmail("user1@example.com");

        User user2 = User.builder().build();
        user2.setEmail("user2@example.com");

        Iterable<User> allUsers = Arrays.asList(user1, user2);

        RosterStudent student1 = new RosterStudent();
        RosterStudent student2 = new RosterStudent();

        when(userRepository.findAll()).thenReturn(allUsers);
        when(rosterStudentRepository.findAllByEmail("user1@example.com")).thenReturn(Collections.singletonList(student1));
        when(rosterStudentRepository.findAllByEmail("user2@example.com")).thenReturn(Collections.singletonList(student2));

        // Act
        updateUserService.attachRosterStudentsAllUsers();

        // Assert
        verify(userRepository, times(1)).findAll();
        verify(rosterStudentRepository, times(1)).findAllByEmail("user1@example.com");
        verify(rosterStudentRepository, times(1)).findAllByEmail("user2@example.com");
        verify(rosterStudentRepository, times(2)).saveAll(anyList());

        assertEquals(user1, student1.getUser());
        assertEquals(user2, student2.getUser());
    }
}