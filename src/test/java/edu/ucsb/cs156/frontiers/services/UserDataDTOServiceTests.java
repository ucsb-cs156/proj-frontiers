package edu.ucsb.cs156.frontiers.services;

import edu.ucsb.cs156.frontiers.entities.Admin;
import edu.ucsb.cs156.frontiers.entities.Instructor;
import edu.ucsb.cs156.frontiers.entities.User;
import edu.ucsb.cs156.frontiers.models.UserDataDTO;
import edu.ucsb.cs156.frontiers.repositories.AdminRepository;
import edu.ucsb.cs156.frontiers.repositories.InstructorRepository;
import edu.ucsb.cs156.frontiers.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

public class UserDataDTOServiceTests {

    @Mock
    UserRepository userRepository;

    @Mock
    AdminRepository adminRepository;

    @Mock
    InstructorRepository instructorRepository;

    @InjectMocks
    UserDataDTOService userDataService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void service_properly_translates_user_data_to_dto() {
        User u1 = User.builder().id(1L).email("djensen@ucsb.edu").build();
        User u2 = User.builder().id(2L).email("cgaucho@ucsb.edu").build();
        User u3 = User.builder().id(3L).email("jgaucho@ucsb.edu").build();

        Admin admin = Admin.builder().email(u1.getEmail()).build();
        Instructor instructor = Instructor.builder().email(u2.getEmail()).build();

        ArrayList<User> expectedUsers = new ArrayList<>(Arrays.asList(u1, u2, u3));

        ArrayList<Admin> expectedAdmins = new ArrayList<>();
        expectedAdmins.add(admin);

        ArrayList<Instructor> expectedInstructors = new ArrayList<>();
        expectedInstructors.add(instructor);

        List<UserDataDTO> userDTOS = new ArrayList<>();
        userDTOS.add(UserDataDTO.from(u1, true, false));
        userDTOS.add(UserDataDTO.from(u2, false, true));
        userDTOS.add(UserDataDTO.from(u3, false, false));

        PageImpl<User> page = new PageImpl<>(expectedUsers);

        Pageable pageable = Pageable.unpaged();

        when(userRepository.findAll(eq(pageable))).thenReturn(page);
        when(adminRepository.findAll()).thenReturn(expectedAdmins);
        when(instructorRepository.findAll()).thenReturn(expectedInstructors);

        assertEquals(userDTOS, userDataService.getUserDataDTOs(pageable).getContent());

    }
}
