package edu.ucsb.cs156.frontiers.models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.entities.RosterStudent;
import edu.ucsb.cs156.frontiers.entities.User;
import edu.ucsb.cs156.frontiers.enums.OrgStatus;
import edu.ucsb.cs156.frontiers.enums.RosterStatus;
import org.junit.jupiter.api.Test;

/**
 * This is a test class for the RosterStudentDTO class.
 */

public class RosterStudentDTOTests {

    @Test
    public void test_from() {
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
        rosterStudent.setGithubId(12345);
        rosterStudent.setGithubLogin("testuser");
        rosterStudent.setEmail("johndoe@example.com");
        rosterStudent.setUser(user);
        rosterStudent.setRosterStatus(RosterStatus.ROSTER);
        rosterStudent.setOrgStatus(OrgStatus.PENDING);

        // Act

        RosterStudentDTO dto = new RosterStudentDTO(rosterStudent);
        // Assert

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

    @Test
    public void test_from_when_user_is_null() {
        // Arrange
        Course course = new Course();
        course.setId(1L);

        RosterStudent rosterStudent = new RosterStudent();
        rosterStudent.setId(3L);
        rosterStudent.setCourse(course);
        rosterStudent.setStudentId("U123456");
        rosterStudent.setFirstName("John");
        rosterStudent.setLastName("Doe");
        rosterStudent.setEmail("johndoe@example.com");
        rosterStudent.setGithubId(12345);
        rosterStudent.setGithubLogin("testuser");
        rosterStudent.setUser(null);
        rosterStudent.setRosterStatus(RosterStatus.ROSTER);
        rosterStudent.setOrgStatus(OrgStatus.PENDING);

        // Act
        RosterStudentDTO dto = new RosterStudentDTO(rosterStudent);

        // Assert
        assertEquals(3L, dto.id());
        assertEquals(1L, dto.courseId());
        assertEquals("U123456", dto.studentId());
        assertEquals("John", dto.firstName());
        assertEquals("Doe", dto.lastName());
        assertEquals("johndoe@example.com", dto.email());
        assertEquals(0L, dto.userId());
        assertEquals(12345, dto.userGithubId());
        assertEquals("testuser", dto.userGithubLogin());
        assertEquals(RosterStatus.ROSTER, dto.rosterStatus());
        assertEquals(OrgStatus.PENDING, dto.orgStatus());

    }
}