package edu.ucsb.cs156.frontiers.services;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.entities.RosterStudent;
import edu.ucsb.cs156.frontiers.entities.User;
import edu.ucsb.cs156.frontiers.enums.OrgStatus;
import edu.ucsb.cs156.frontiers.enums.RosterStatus;
import edu.ucsb.cs156.frontiers.models.RosterStudentDTO;
import edu.ucsb.cs156.frontiers.repositories.RosterStudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * This class contains unit tests for the RosterStudentDTOService class.
 */

public class RosterStudentServiceTests {

    @Mock
    private RosterStudentRepository rosterStudentRepository;

    @Mock
    private UpdateUserService updateUserService;


    @InjectMocks
    private RosterStudentService rosterStudentService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void test_getRosterStudentDTOById_success() {
        // Arrange
        Course course = new Course();
        course.setId(1L);

        User user = User.builder().build();
        user.setId(2L);

        RosterStudent rosterStudent = new RosterStudent();
        rosterStudent.setId(3L);
        rosterStudent.setCourse(course);
        rosterStudent.setStudentId("U123456");
        rosterStudent.setFirstName("John");
        rosterStudent.setLastName("Doe");
        rosterStudent.setEmail("johndoe@example.com");
        rosterStudent.setGithubId(12345);
        rosterStudent.setGithubLogin("testuser");
        rosterStudent.setUser(user);
        rosterStudent.setRosterStatus(RosterStatus.ROSTER);
        rosterStudent.setOrgStatus(OrgStatus.PENDING);

        when(rosterStudentRepository.findByCourseId(1L)).thenReturn(List.of(rosterStudent));

        // Act
        List<RosterStudentDTO> dtos = rosterStudentService.getRosterStudentDTOs(1L);
        // Assert
        assertEquals(1, dtos.size());
        RosterStudentDTO dto = dtos.get(0);
        assertEquals(3L, dto.id());
        assertEquals(1L, dto.courseId());
        assertEquals("U123456", dto.studentId());
        assertEquals("John", dto.firstName());
        assertEquals("Doe", dto.lastName());
        assertEquals("johndoe@example.com", dto.email());
        assertEquals(2L, dto.userId());
        assertEquals(12345, dto.userGithubId());
        assertEquals("testuser", dto.userGithubLogin());
        assertEquals(RosterStatus.ROSTER, dto.rosterStatus());
        assertEquals(OrgStatus.PENDING, dto.orgStatus());
    }

    private final String sampleCSVContents = """
            Enrl Cd,Perm #,Grade,Final Units,Student Last,Student First Middle,Quarter,Course ID,Section,Meeting Time(s) / Location(s),Email,ClassLevel,Major1,Major2,Date/Time,Pronoun
            
            08235,A123456,,4.0,GAUCHO,CHRIS FAKE,F23,CMPSC156,0100,T R   2:00- 3:15 SH 1431     W    5:00- 5:50 PHELP 3525  W    6:00- 6:50 PHELP 3525  W    7:00- 7:50 PHELP 3525  ,cgaucho@ucsb.edu,SR,CMPSC,,9/27/2023 9:39:25 AM,
            08250,A987654,,4.0,DEL PLAYA,LAUREN,F23,CMPSC156,0100,T R   2:00- 3:15 SH 1431     W    5:00- 5:50 PHELP 3525  W    6:00- 6:50 PHELP 3525  W    7:00- 7:50 PHELP 3525  ,ldelplaya@umail.ucsb.edu,SR,CMPSC,,9/27/2023 9:39:25 AM,She (She/Her/Hers)
            08243,1234567,,4.0,TARDE,SABADO,F23,CMPSC156,0100,T R   2:00- 3:15 SH 1431     W    5:00- 5:50 PHELP 3525  W    6:00- 6:50 PHELP 3525  W    7:00- 7:50 PHELP 3525  ,sabadotarde@umail.ucsb.edu,SR,CMPSC,,9/27/2023 9:39:25 AM,He (He/Him/His)
            08245,A626,,4.0,ORANGE,APPLE,F23,CMPSC156,0100,T R   2:00- 3:15 SH 1431     W    5:00- 5:50 PHELP 3525  W    6:00- 6:50 PHELP 3525  W    7:00- 7:50 PHELP 3525  ,fakeemail@umail.ucsb.edu,SR,CMPSC,,9/27/2023 9:39:25 AM,He (He/Him/His)
            """;

    @Test
    public void marks_students_appropriately() throws IOException {
        Course c1 = Course.builder().id(1L).build();

        //Returned Objects
        RosterStudent rs2DifferingEmail = RosterStudent.builder().id(2L).studentId("A987654").firstName("LAUREN").lastName("DEL PLAYA").email("differingemail@umail.ucsb.edu").course(c1).orgStatus(OrgStatus.PENDING).build();
        RosterStudent rs3DifferingStudentId = RosterStudent.builder().id(3L).studentId("differingId").firstName("APPLE").lastName("ORANGE").email("sabadotarde@umail.ucsb.edu").course(c1).orgStatus(OrgStatus.PENDING).build();
        RosterStudent rs4PartialStudentId = RosterStudent.builder().id(4L).studentId("A626").firstName("APPLE").lastName("ORANGE").email("wrongemail@umail.ucsb.edu").course(c1).orgStatus(OrgStatus.PENDING).build();
        RosterStudent rs4PartialEmail = RosterStudent.builder().id(5L).studentId("wrongid").firstName("APPLE").lastName("ORANGE").email("fakeemail@umail.ucsb.edu").course(c1).orgStatus(OrgStatus.PENDING).build();

        //Results
        RosterStudent rs4Rejected = RosterStudent.builder().studentId("A626").firstName("APPLE").lastName("ORANGE").email("fakeemail@umail.ucsb.edu").course(c1).build();
        RosterStudent rs1Inserted = RosterStudent.builder().studentId("A123456").firstName("CHRIS FAKE").lastName("GAUCHO").email("cgaucho@ucsb.edu").course(c1).orgStatus(OrgStatus.PENDING).rosterStatus(RosterStatus.ROSTER).build();
        RosterStudent rs2Updated = RosterStudent.builder().id(2L).studentId("A987654").firstName("LAUREN").lastName("DEL PLAYA").email("ldelplaya@ucsb.edu").course(c1).orgStatus(OrgStatus.PENDING).rosterStatus(RosterStatus.ROSTER).build();
        RosterStudent rs3Updated = RosterStudent.builder().id(3L).studentId("1234567").firstName("SABADO").lastName("TARDE").email("sabadotarde@ucsb.edu").course(c1).orgStatus(OrgStatus.PENDING).rosterStatus(RosterStatus.ROSTER).build();

        when(rosterStudentRepository.findByCourseIdAndStudentId(eq(1L), contains("A123456"))).thenReturn(Optional.empty());
        when(rosterStudentRepository.findByCourseIdAndEmail(eq(1L), contains("cgaucho@ucsb.edu"))).thenReturn(Optional.empty());

        when(rosterStudentRepository.findByCourseIdAndStudentId(eq(1L), contains("A987654"))).thenReturn(Optional.of(rs2DifferingEmail));

        when(rosterStudentRepository.findByCourseIdAndEmail(eq(1L), contains("sabadotarde@ucsb.edu"))).thenReturn(Optional.of(rs3DifferingStudentId));

        when(rosterStudentRepository.findByCourseIdAndStudentId(eq(1L), contains("A626"))).thenReturn(Optional.of(rs4PartialStudentId));
        when(rosterStudentRepository.findByCourseIdAndEmail(eq(1L), contains("fakeemail@ucsb.edu"))).thenReturn(Optional.of(rs4PartialEmail));

        when(rosterStudentRepository.save(any(RosterStudent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RosterStudentService.LoadResult expectedResult = new RosterStudentService.LoadResult(1, 2, List.of(rs4Rejected));

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "egrades.csv",
                MediaType.TEXT_PLAIN_VALUE,
                sampleCSVContents.getBytes());

        RosterStudentService.LoadResult actualResult = rosterStudentService.loadCsv(file, c1);

        verify(updateUserService, times(3)).attachUserToRosterStudent(any(RosterStudent.class));
        verify(rosterStudentRepository, atLeastOnce()).save(rs1Inserted);
        verify(rosterStudentRepository, atLeastOnce()).save(rs2Updated);
        verify(rosterStudentRepository, atLeastOnce()).save(rs3Updated);


        assertEquals(expectedResult, actualResult);

    }

    @Test
    public void both_match_join_course() throws IOException {
        Course c1 = Course.builder().id(1L).installationId("fioqehw").orgName("fqbqfopj").build();
        String shortenedCsv = """
            Enrl Cd,Perm #,Grade,Final Units,Student Last,Student First Middle,Quarter,Course ID,Section,Meeting Time(s) / Location(s),Email,ClassLevel,Major1,Major2,Date/Time,Pronoun
            
            08235,A123456,,4.0,GAUCHO,CHRIS FAKE,F23,CMPSC156,0100,T R   2:00- 3:15 SH 1431     W    5:00- 5:50 PHELP 3525  W    6:00- 6:50 PHELP 3525  W    7:00- 7:50 PHELP 3525  ,cgaucho@ucsb.edu,SR,CMPSC,,9/27/2023 9:39:25 AM,
            08250,A987654,,4.0,DEL PLAYA,LAUREN,F23,CMPSC156,0100,T R   2:00- 3:15 SH 1431     W    5:00- 5:50 PHELP 3525  W    6:00- 6:50 PHELP 3525  W    7:00- 7:50 PHELP 3525  ,ldelplaya@umail.ucsb.edu,SR,CMPSC,,9/27/2023 9:39:25 AM,She (She/Her/Hers)
            ,,,,,,,,,,,,,,,,
            """;

        RosterStudent rs2PreChange = RosterStudent.builder().id(2L).studentId("A987654").firstName("APPLE").lastName("ORANGE").email("ldelplaya@ucsb.edu").course(c1).orgStatus(OrgStatus.JOINCOURSE).rosterStatus(RosterStatus.MANUAL).build();

        RosterStudent rs1Inserted = RosterStudent.builder().studentId("A123456").firstName("CHRIS FAKE").lastName("GAUCHO").email("cgaucho@ucsb.edu").course(c1).orgStatus(OrgStatus.JOINCOURSE).rosterStatus(RosterStatus.ROSTER).build();
        RosterStudent rs2Updated = RosterStudent.builder().id(2L).studentId("A987654").firstName("LAUREN").lastName("DEL PLAYA").email("ldelplaya@ucsb.edu").course(c1).orgStatus(OrgStatus.JOINCOURSE).rosterStatus(RosterStatus.ROSTER).build();
        when(rosterStudentRepository.findByCourseIdAndStudentId(eq(1L), contains("A123456"))).thenReturn(Optional.empty());
        when(rosterStudentRepository.findByCourseIdAndEmail(eq(1L), contains("cgaucho@ucsb.edu"))).thenReturn(Optional.empty());

        when(rosterStudentRepository.findByCourseIdAndStudentId(eq(1L), contains("A987654"))).thenReturn(Optional.of(rs2PreChange));
        when(rosterStudentRepository.findByCourseIdAndEmail(eq(1L), contains("ldelplaya@ucsb.edu"))).thenReturn(Optional.of(rs2PreChange));

        when(rosterStudentRepository.save(any(RosterStudent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "egrades.csv",
                MediaType.TEXT_PLAIN_VALUE,
                shortenedCsv.getBytes());

        rosterStudentService.loadCsv(file, c1);
        verify(updateUserService, times(2)).attachUserToRosterStudent(any(RosterStudent.class));
        verify(rosterStudentRepository, atLeastOnce()).save(rs1Inserted);
        verify(rosterStudentRepository, atLeastOnce()).save(rs2Updated);
    }


  
}