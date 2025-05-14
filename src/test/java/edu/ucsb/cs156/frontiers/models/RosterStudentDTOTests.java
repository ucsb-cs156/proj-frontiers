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
        rosterStudent.setOrgStatus(OrgStatus.NONE);

        // Act

        RosterStudentDTO dto = RosterStudentDTO.from(rosterStudent);
        // Assert

        assertEquals(3L, dto.getId());
        assertEquals(1L, dto.getCourseId());
        assertEquals("U123456", dto.getStudentId());
        assertEquals("John", dto.getFirstName());
        assertEquals("Doe", dto.getLastName());
        assertEquals("johndoe@example.com", dto.getEmail());
        assertEquals(2L, dto.getUserId());
        assertEquals(12345, dto.getUserGithubId());
        assertEquals("testuser", dto.getUserGithubLogin());
        assertEquals(RosterStatus.ROSTER, dto.getRosterStatus());
        assertEquals(OrgStatus.NONE, dto.getOrgStatus());
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
        rosterStudent.setOrgStatus(OrgStatus.NONE);

        // Act
        RosterStudentDTO dto = RosterStudentDTO.from(rosterStudent);

        // Assert
        assertEquals(3L, dto.getId());
        assertEquals(1L, dto.getCourseId());
        assertEquals("U123456", dto.getStudentId());
        assertEquals("John", dto.getFirstName());
        assertEquals("Doe", dto.getLastName());
        assertEquals("johndoe@example.com", dto.getEmail());
        assertEquals(0, dto.getUserId());
        assertEquals(12345, dto.getUserGithubId());
        assertEquals("testuser", dto.getUserGithubLogin());
        assertEquals(RosterStatus.ROSTER, dto.getRosterStatus());
        assertEquals(OrgStatus.NONE, dto.getOrgStatus());

    }
}