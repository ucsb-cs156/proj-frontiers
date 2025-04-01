package edu.ucsb.cs156.frontiers.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.enums.OrgStatus;
import edu.ucsb.cs156.frontiers.enums.RosterStatus;
import edu.ucsb.cs156.frontiers.models.RosterStudentDTO;
import edu.ucsb.cs156.frontiers.repositories.CourseRepository;
import edu.ucsb.cs156.frontiers.repositories.RosterStudentRepository;
import edu.ucsb.cs156.frontiers.services.RosterStudentDTOService;

public class CSVDownloadsControllerTests {

    @Mock
    private RosterStudentDTOService rosterStudentDTOService = mock(RosterStudentDTOService.class);

    @Mock
    private CourseRepository courseRepository = mock(CourseRepository.class);

    @InjectMocks
    private CSVDownloadsController csvDownloadsController;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testRosterStudentsCSV_failure() throws Exception {
        when(courseRepository.findById(1L)).thenReturn(java.util.Optional.empty());

        ResponseEntity<StreamingResponseBody> response = csvDownloadsController.csvForQuarter(1L, "");
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    public void testRosterStudentsCSV_success() throws Exception {

        // Arrange
        Course course = Course.builder()
                .id(1L)
                .courseName("ucsb-cs156-s25")
                .term("S25")
                .school("UCSB")
                .build();

        RosterStudentDTO rosterStudentDTO = RosterStudentDTO.builder()
                .id(42L)
                .courseId(course.getId())
                .studentId("12345")
                .firstName("Chris")
                .lastName("Gaucho")
                .email("cgaucho@ucsb.edu")
                .userId(102L)
                .userGithubId(12345)
                .userGithubLogin("cgaucho")
                .rosterStatus(RosterStatus.ROSTER)
                .orgStatus(OrgStatus.NONE)
                .build();

        when(courseRepository.findById(1L)).thenReturn(java.util.Optional.of(course));
        when(rosterStudentDTOService.getRosterStudentDTOs(eq(1L))).thenReturn(List.of(rosterStudentDTO));

        ResponseEntity<StreamingResponseBody> response = csvDownloadsController.csvForQuarter(1L, "");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("text/csv;charset=UTF-8", response.getHeaders().getContentType().toString());
        String expectedFilename = "ucsb-cs156-s25_roster.csv";
        String expectedContentDisposition = "attachment;filename=" + expectedFilename;
        String actualContentDisposition = response.getHeaders().get(HttpHeaders.CONTENT_DISPOSITION).get(0);
        assertEquals(expectedContentDisposition, actualContentDisposition);

        StreamingResponseBody body = response.getBody();
        assertNotNull(body);

        OutputStream outputStream = new ByteArrayOutputStream();
        body.writeTo(outputStream);
        String csvOutput = outputStream.toString();

        String expectedCSVOutput = """
                "COURSEID","EMAIL","FIRSTNAME","ID","LASTNAME","ORGSTATUS","ROSTERSTATUS","STUDENTID","USERGITHUBID","USERGITHUBLOGIN","USERID"
                "1","cgaucho@ucsb.edu","Chris","42","Gaucho","NONE","ROSTER","12345","12345","cgaucho","102"
                """;

        assertEquals(expectedCSVOutput, csvOutput);
    }
}
