package edu.ucsb.cs156.frontiers.models;

import static org.junit.jupiter.api.Assertions.assertEquals;

import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.entities.RosterStudent;
import edu.ucsb.cs156.frontiers.entities.Team;
import edu.ucsb.cs156.frontiers.entities.TeamMember;
import edu.ucsb.cs156.frontiers.entities.User;
import edu.ucsb.cs156.frontiers.enums.OrgStatus;
import edu.ucsb.cs156.frontiers.enums.RosterStatus;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** This is a test class for the RosterStudentDTO class. */
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
    rosterStudent.setSection("Section A");
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
    assertEquals("Section A", dto.section());
    assertEquals(2L, dto.userId());
    assertEquals(12345, dto.githubId());
    assertEquals("testuser", dto.githubLogin());
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
    rosterStudent.setSection("Section B");
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
    assertEquals("Section B", dto.section());
    assertEquals(0L, dto.userId());
    assertEquals(12345, dto.githubId());
    assertEquals("testuser", dto.githubLogin());
    assertEquals(RosterStatus.ROSTER, dto.rosterStatus());
    assertEquals(OrgStatus.PENDING, dto.orgStatus());
  }

  @Test
  public void test_when_student_is_on_team() {
    // Arrange
    Course course = new Course();
    course.setId(1L);

    RosterStudent teammate = new RosterStudent();
    teammate.setId(1L);
    teammate.setCourse(course);
    teammate.setStudentId("U111111");
    teammate.setFirstName("Alice");
    teammate.setLastName("Smith");
    teammate.setEmail("alice.smith@example.com");
    teammate.setSection("Section A");
    teammate.setGithubId(11111);
    teammate.setGithubLogin("alicesmith");
    teammate.setUser(null);
    teammate.setRosterStatus(RosterStatus.ROSTER);
    teammate.setOrgStatus(OrgStatus.PENDING);

    TeamMember teamMember = new TeamMember();
    teamMember.setId(1L);
    teamMember.setRosterStudent(teammate);

    RosterStudent rosterStudent = new RosterStudent();
    rosterStudent.setId(3L);
    rosterStudent.setCourse(course);
    rosterStudent.setStudentId("U123456");
    rosterStudent.setFirstName("John");
    rosterStudent.setLastName("Doe");
    rosterStudent.setEmail("johndoe@example.com");
    rosterStudent.setSection("Section B");
    rosterStudent.setGithubId(12345);
    rosterStudent.setGithubLogin("testuser");
    rosterStudent.setUser(null);
    rosterStudent.setRosterStatus(RosterStatus.ROSTER);
    rosterStudent.setOrgStatus(OrgStatus.PENDING);

    TeamMember teamMember2 = new TeamMember();
    teamMember2.setId(2L);
    teamMember2.setRosterStudent(rosterStudent);

    Team team = new Team();
    team.setId(1L);
    team.setName("Team Rocket");
    team.setCourse(course);
    team.setTeamMembers(java.util.Set.of(teamMember, teamMember2));

    rosterStudent.setTeamMembers(java.util.Set.of(teamMember2));
    teammate.setTeamMembers(java.util.Set.of(teamMember));

    teamMember.setTeam(team);
    teamMember2.setTeam(team);

    // Act
    RosterStudentDTO dto = new RosterStudentDTO(rosterStudent);

    // Assert
    assertEquals(3L, dto.id());
    assertEquals(1L, dto.courseId());
    assertEquals("U123456", dto.studentId());
    assertEquals("John", dto.firstName());
    assertEquals("Doe", dto.lastName());
    assertEquals("johndoe@example.com", dto.email());
    assertEquals("Section B", dto.section());
    assertEquals(0L, dto.userId());
    assertEquals(12345, dto.githubId());
    assertEquals("testuser", dto.githubLogin());
    assertEquals(RosterStatus.ROSTER, dto.rosterStatus());
    assertEquals(rosterStudent.getTeams(), dto.teams());
    assertEquals(Set.of("Team Rocket"), dto.teams());
  }
}
