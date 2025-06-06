package edu.ucsb.cs156.frontiers.models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
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

    @Test
    public void test_noArgsConstructor_defaults() {
        // exercise Lombok @NoArgsConstructor
        RosterStudentDTO dto = new RosterStudentDTO();

        // all object‐types should be null
        assertNull(dto.getId());
        assertNull(dto.getCourseId());
        assertNull(dto.getStudentId());
        assertNull(dto.getFirstName());
        assertNull(dto.getLastName());
        assertNull(dto.getEmail());
        assertNull(dto.getUserGithubId());
        assertNull(dto.getUserGithubLogin());
        assertNull(dto.getRosterStatus());
        assertNull(dto.getOrgStatus());

        // primitive default
        assertEquals(0L, dto.getUserId());
    }

    @Test
    public void test_data_annotation_getters_setters_equals_hashCode_toString() {
        // set up two identical DTOs via setters
        RosterStudentDTO dto1 = new RosterStudentDTO();
        dto1.setId(1L);
        dto1.setCourseId(2L);
        dto1.setStudentId("U0001");
        dto1.setFirstName("Alice");
        dto1.setLastName("Smith");
        dto1.setEmail("alice@example.com");
        dto1.setUserId(42L);
        dto1.setUserGithubId(1001);
        dto1.setUserGithubLogin("aliceGH");
        dto1.setRosterStatus(RosterStatus.ROSTER);
        dto1.setOrgStatus(OrgStatus.MEMBER);

        RosterStudentDTO dto2 = new RosterStudentDTO();
        dto2.setId(1L);
        dto2.setCourseId(2L);
        dto2.setStudentId("U0001");
        dto2.setFirstName("Alice");
        dto2.setLastName("Smith");
        dto2.setEmail("alice@example.com");
        dto2.setUserId(42L);
        dto2.setUserGithubId(1001);
        dto2.setUserGithubLogin("aliceGH");
        dto2.setRosterStatus(RosterStatus.ROSTER);
        dto2.setOrgStatus(OrgStatus.MEMBER);

        // equals and hashCode come from @Data
        assertEquals(dto1, dto2);
        assertEquals(dto1.hashCode(), dto2.hashCode());

        // toString comes from @Data
        String ts = dto1.toString();
        assertTrue(ts.contains("id=1"));
        assertTrue(ts.contains("courseId=2"));
        assertTrue(ts.contains("studentId=U0001"));
    }

}