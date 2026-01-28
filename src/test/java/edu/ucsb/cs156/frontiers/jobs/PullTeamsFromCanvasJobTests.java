package edu.ucsb.cs156.frontiers.jobs;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.entities.Job;
import edu.ucsb.cs156.frontiers.entities.RosterStudent;
import edu.ucsb.cs156.frontiers.entities.Team;
import edu.ucsb.cs156.frontiers.entities.TeamMember;
import edu.ucsb.cs156.frontiers.errors.DuplicateGroupException;
import edu.ucsb.cs156.frontiers.models.CanvasGroup;
import edu.ucsb.cs156.frontiers.repositories.TeamRepository;
import edu.ucsb.cs156.frontiers.services.CanvasService;
import edu.ucsb.cs156.frontiers.services.jobs.JobContext;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class PullTeamsFromCanvasJobTests {

  @Mock private CanvasService canvasService;
  @Mock private TeamRepository teamRepository;

  Job jobStarted = Job.builder().build();
  JobContext ctx = new JobContext(null, jobStarted);

  @BeforeEach
  public void setup() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  public void testAccept_CreatesNewTeams() throws Exception {
    // Arrange
    RosterStudent student1 =
        RosterStudent.builder().email("alice@ucsb.edu").teamMembers(new ArrayList<>()).build();

    Course course =
        Course.builder()
            .id(1L)
            .courseName("CS156")
            .rosterStudents(List.of(student1))
            .teams(new ArrayList<>())
            .build();

    CanvasGroup group =
        CanvasGroup.builder().name("Team Alpha").id(101).members(List.of("alice@ucsb.edu")).build();

    when(canvasService.getCanvasGroups(course, "groupset123")).thenReturn(List.of(group));

    PullTeamsFromCanvasJob job =
        PullTeamsFromCanvasJob.builder()
            .course(course)
            .groupsetId("groupset123")
            .canvasService(canvasService)
            .teamRepository(teamRepository)
            .build();

    // Act
    job.accept(ctx);

    // Assert
    verify(canvasService).getCanvasGroups(course, "groupset123");

    ArgumentCaptor<List<Team>> teamsCaptor = ArgumentCaptor.forClass(List.class);
    verify(teamRepository).saveAll(teamsCaptor.capture());

    List<Team> savedTeams = teamsCaptor.getValue();
    assertEquals(1, savedTeams.size());

    Team savedTeam = savedTeams.get(0);
    assertEquals("Team Alpha", savedTeam.getName());
    assertEquals(101, savedTeam.getCanvasId());
    assertEquals(course, savedTeam.getCourse());
    assertEquals(1, savedTeam.getTeamMembers().size());
    assertEquals(student1, savedTeam.getTeamMembers().get(0).getRosterStudent());
  }

  @Test
  public void testAccept_LinksExistingTeamByCanvasId() throws Exception {
    // Arrange
    RosterStudent student1 =
        RosterStudent.builder().email("alice@ucsb.edu").teamMembers(new ArrayList<>()).build();

    Team existingTeam =
        Team.builder().name("Team Alpha").canvasId(101).teamMembers(new ArrayList<>()).build();

    Course course =
        Course.builder()
            .id(1L)
            .courseName("CS156")
            .rosterStudents(List.of(student1))
            .teams(new ArrayList<>(List.of(existingTeam)))
            .build();

    CanvasGroup group =
        CanvasGroup.builder()
            .name("Team Alpha Renamed")
            .id(101) // Same Canvas ID
            .members(List.of("alice@ucsb.edu"))
            .build();

    when(canvasService.getCanvasGroups(course, "groupset123")).thenReturn(List.of(group));

    PullTeamsFromCanvasJob job =
        PullTeamsFromCanvasJob.builder()
            .course(course)
            .groupsetId("groupset123")
            .canvasService(canvasService)
            .teamRepository(teamRepository)
            .build();

    // Act
    job.accept(ctx);

    // Assert
    verify(teamRepository).saveAll(anyList());
  }

  @Test
  public void testAccept_LinksExistingTeamByName() throws Exception {
    // Arrange
    RosterStudent student1 =
        RosterStudent.builder().email("alice@ucsb.edu").teamMembers(new ArrayList<>()).build();

    Team existingTeam =
        Team.builder()
            .name("Team Alpha")
            .canvasId(null) // No Canvas ID yet
            .teamMembers(new ArrayList<>())
            .build();

    Course course =
        Course.builder()
            .id(1L)
            .courseName("CS156")
            .rosterStudents(List.of(student1))
            .teams(new ArrayList<>(List.of(existingTeam)))
            .build();

    CanvasGroup group =
        CanvasGroup.builder()
            .name("Team Alpha") // Same name
            .id(101)
            .members(List.of("alice@ucsb.edu"))
            .build();

    when(canvasService.getCanvasGroups(course, "groupset123")).thenReturn(List.of(group));

    PullTeamsFromCanvasJob job =
        PullTeamsFromCanvasJob.builder()
            .course(course)
            .groupsetId("groupset123")
            .canvasService(canvasService)
            .teamRepository(teamRepository)
            .build();

    // Act
    job.accept(ctx);

    // Assert
    ArgumentCaptor<List<Team>> teamsCaptor = ArgumentCaptor.forClass(List.class);
    verify(teamRepository).saveAll(teamsCaptor.capture());

    List<Team> savedTeams = teamsCaptor.getValue();
    assertEquals(1, savedTeams.size());

    // The existing team should be linked with the Canvas ID
    Team savedTeam = savedTeams.get(0);
    assertEquals("Team Alpha", savedTeam.getName());
    assertEquals(101, savedTeam.getCanvasId());
  }

  @Test
  public void testAccept_ThrowsDuplicateGroupException() throws Exception {
    // Arrange
    Team existingTeam =
        Team.builder()
            .name("Team Alpha")
            .canvasId(999) // Already linked to a different Canvas ID
            .teamMembers(new ArrayList<>())
            .build();

    Course course =
        Course.builder()
            .id(1L)
            .courseName("CS156")
            .rosterStudents(new ArrayList<>())
            .teams(new ArrayList<>(List.of(existingTeam)))
            .build();

    CanvasGroup group =
        CanvasGroup.builder()
            .name("Team Alpha") // Same name but different Canvas ID
            .id(101)
            .members(new ArrayList<>())
            .build();

    when(canvasService.getCanvasGroups(course, "groupset123")).thenReturn(List.of(group));

    PullTeamsFromCanvasJob job =
        PullTeamsFromCanvasJob.builder()
            .course(course)
            .groupsetId("groupset123")
            .canvasService(canvasService)
            .teamRepository(teamRepository)
            .build();

    // Act & Assert
    assertThrows(DuplicateGroupException.class, () -> job.accept(ctx));
    verify(teamRepository, never()).saveAll(anyList());
  }

  @Test
  public void testAccept_SkipsMembersNotOnRoster() throws Exception {
    // Arrange
    RosterStudent student1 =
        RosterStudent.builder().email("alice@ucsb.edu").teamMembers(new ArrayList<>()).build();

    Course course =
        Course.builder()
            .id(1L)
            .courseName("CS156")
            .rosterStudents(List.of(student1))
            .teams(new ArrayList<>())
            .build();

    // Group has a member not on roster
    CanvasGroup group =
        CanvasGroup.builder()
            .name("Team Alpha")
            .id(101)
            .members(List.of("alice@ucsb.edu", "unknown@ucsb.edu"))
            .build();

    when(canvasService.getCanvasGroups(course, "groupset123")).thenReturn(List.of(group));

    PullTeamsFromCanvasJob job =
        PullTeamsFromCanvasJob.builder()
            .course(course)
            .groupsetId("groupset123")
            .canvasService(canvasService)
            .teamRepository(teamRepository)
            .build();

    // Act
    job.accept(ctx);

    // Assert
    ArgumentCaptor<List<Team>> teamsCaptor = ArgumentCaptor.forClass(List.class);
    verify(teamRepository).saveAll(teamsCaptor.capture());

    List<Team> savedTeams = teamsCaptor.getValue();
    assertEquals(1, savedTeams.size());

    // Only the student on roster should be added
    Team savedTeam = savedTeams.get(0);
    assertEquals(1, savedTeam.getTeamMembers().size());
    assertEquals("alice@ucsb.edu", savedTeam.getTeamMembers().get(0).getRosterStudent().getEmail());
  }

  @Test
  public void testAccept_SkipsExistingTeamMembers() throws Exception {
    // Arrange
    Team existingTeam =
        Team.builder().name("Team Alpha").canvasId(101).teamMembers(new ArrayList<>()).build();

    RosterStudent student1 =
        RosterStudent.builder().email("alice@ucsb.edu").teamMembers(new ArrayList<>()).build();

    // Student is already a member of this team
    TeamMember existingMember =
        TeamMember.builder().team(existingTeam).rosterStudent(student1).build();
    student1.getTeamMembers().add(existingMember);
    existingTeam.getTeamMembers().add(existingMember);

    Course course =
        Course.builder()
            .id(1L)
            .courseName("CS156")
            .rosterStudents(List.of(student1))
            .teams(new ArrayList<>(List.of(existingTeam)))
            .build();

    CanvasGroup group =
        CanvasGroup.builder().name("Team Alpha").id(101).members(List.of("alice@ucsb.edu")).build();

    when(canvasService.getCanvasGroups(course, "groupset123")).thenReturn(List.of(group));

    PullTeamsFromCanvasJob job =
        PullTeamsFromCanvasJob.builder()
            .course(course)
            .groupsetId("groupset123")
            .canvasService(canvasService)
            .teamRepository(teamRepository)
            .build();

    // Act
    job.accept(ctx);

    // Assert
    ArgumentCaptor<List<Team>> teamsCaptor = ArgumentCaptor.forClass(List.class);
    verify(teamRepository).saveAll(teamsCaptor.capture());

    List<Team> savedTeams = teamsCaptor.getValue();
    assertEquals(1, savedTeams.size());

    // Should not add duplicate member
    Team savedTeam = savedTeams.get(0);
    assertEquals(1, savedTeam.getTeamMembers().size());
  }

  @Test
  public void testAccept_AddsNewMemberToExistingTeam() throws Exception {
    // This test verifies that new members ARE added when they are not already team members.
    // This catches the mutation where anyMatch() always returns true.
    // Arrange
    Team existingTeam =
        Team.builder().name("Team Alpha").canvasId(101).teamMembers(new ArrayList<>()).build();

    // Another team that a student can already be a member of
    Team otherTeam =
        Team.builder().name("Other Team").canvasId(999).teamMembers(new ArrayList<>()).build();

    RosterStudent existingStudent =
        RosterStudent.builder().email("alice@ucsb.edu").teamMembers(new ArrayList<>()).build();

    RosterStudent newStudent =
        RosterStudent.builder().email("bob@ucsb.edu").teamMembers(new ArrayList<>()).build();

    // Alice is already a member of the linked team (existingTeam)
    TeamMember existingMember =
        TeamMember.builder().team(existingTeam).rosterStudent(existingStudent).build();
    existingStudent.getTeamMembers().add(existingMember);
    existingTeam.getTeamMembers().add(existingMember);

    // Bob is a member of OTHER team, but NOT of the linked team
    // This is critical: with the mutation, anyMatch will return true for Bob
    // because he has at least one team membership, and the mutated predicate returns true
    TeamMember otherMember = TeamMember.builder().team(otherTeam).rosterStudent(newStudent).build();
    newStudent.getTeamMembers().add(otherMember);

    Course course =
        Course.builder()
            .id(1L)
            .courseName("CS156")
            .rosterStudents(List.of(existingStudent, newStudent))
            .teams(new ArrayList<>(List.of(existingTeam, otherTeam)))
            .build();

    // Canvas group has both alice (existing) and bob (member of other team)
    CanvasGroup group =
        CanvasGroup.builder()
            .name("Team Alpha")
            .id(101)
            .members(List.of("alice@ucsb.edu", "bob@ucsb.edu"))
            .build();

    when(canvasService.getCanvasGroups(course, "groupset123")).thenReturn(List.of(group));

    PullTeamsFromCanvasJob job =
        PullTeamsFromCanvasJob.builder()
            .course(course)
            .groupsetId("groupset123")
            .canvasService(canvasService)
            .teamRepository(teamRepository)
            .build();

    // Act
    job.accept(ctx);

    // Assert
    ArgumentCaptor<List<Team>> teamsCaptor = ArgumentCaptor.forClass(List.class);
    verify(teamRepository).saveAll(teamsCaptor.capture());

    List<Team> savedTeams = teamsCaptor.getValue();
    Team savedTeam =
        savedTeams.stream().filter(t -> t.getName().equals("Team Alpha")).findFirst().orElse(null);
    assertNotNull(savedTeam);

    // Should have 2 members: Alice (existing) and Bob (new)
    // With the mutation, Bob wouldn't be added because anyMatch would return true
    assertEquals(2, savedTeam.getTeamMembers().size());

    // Verify Bob was added
    boolean bobAdded =
        savedTeam.getTeamMembers().stream()
            .anyMatch(tm -> tm.getRosterStudent().getEmail().equals("bob@ucsb.edu"));
    assertTrue(bobAdded, "Bob should have been added as a new team member");
  }

  @Test
  public void testAccept_TeamsWithNullCanvasIdNotInCanvasIdMap() throws Exception {
    // This test verifies that teams with null Canvas ID are NOT looked up by Canvas ID.
    // This catches the mutation where the null check is negated.
    // Arrange
    Team teamWithNullCanvasId =
        Team.builder().name("Team Alpha").canvasId(null).teamMembers(new ArrayList<>()).build();

    // CRITICAL: This team has a canvas ID but a DIFFERENT name than the group
    // When mutation is active, this team won't be in the Canvas ID map
    // so the name lookup will fail, and a NEW team will be created
    Team teamWithCanvasId =
        Team.builder().name("Old Team Name").canvasId(200).teamMembers(new ArrayList<>()).build();

    Course course =
        Course.builder()
            .id(1L)
            .courseName("CS156")
            .rosterStudents(new ArrayList<>())
            .teams(new ArrayList<>(List.of(teamWithNullCanvasId, teamWithCanvasId)))
            .build();

    // Canvas group with ID 200 but different name - should match by canvas ID
    // If mutation is active, this will create a NEW team instead of linking
    CanvasGroup group =
        CanvasGroup.builder().name("New Team Name").id(200).members(new ArrayList<>()).build();

    when(canvasService.getCanvasGroups(course, "groupset123")).thenReturn(List.of(group));

    PullTeamsFromCanvasJob job =
        PullTeamsFromCanvasJob.builder()
            .course(course)
            .groupsetId("groupset123")
            .canvasService(canvasService)
            .teamRepository(teamRepository)
            .build();

    // Act
    job.accept(ctx);

    // Assert
    ArgumentCaptor<List<Team>> teamsCaptor = ArgumentCaptor.forClass(List.class);
    verify(teamRepository).saveAll(teamsCaptor.capture());

    List<Team> savedTeams = teamsCaptor.getValue();
    assertEquals(1, savedTeams.size());

    // The existing team should be found by canvas ID (NOT by name since names don't match)
    // If mutation is active, a NEW team called "New Team Name" would be created
    Team savedTeam = savedTeams.get(0);
    assertEquals(teamWithCanvasId, savedTeam); // Should be the same object
    assertEquals("Old Team Name", savedTeam.getName()); // Original name should be preserved
    assertEquals(200, savedTeam.getCanvasId());
  }

  @Test
  public void testAccept_HandlesMultipleGroups() throws Exception {
    // Arrange
    RosterStudent student1 =
        RosterStudent.builder().email("alice@ucsb.edu").teamMembers(new ArrayList<>()).build();
    RosterStudent student2 =
        RosterStudent.builder().email("bob@ucsb.edu").teamMembers(new ArrayList<>()).build();

    Course course =
        Course.builder()
            .id(1L)
            .courseName("CS156")
            .rosterStudents(List.of(student1, student2))
            .teams(new ArrayList<>())
            .build();

    CanvasGroup group1 =
        CanvasGroup.builder().name("Team Alpha").id(101).members(List.of("alice@ucsb.edu")).build();
    CanvasGroup group2 =
        CanvasGroup.builder().name("Team Beta").id(102).members(List.of("bob@ucsb.edu")).build();

    when(canvasService.getCanvasGroups(course, "groupset123")).thenReturn(List.of(group1, group2));

    PullTeamsFromCanvasJob job =
        PullTeamsFromCanvasJob.builder()
            .course(course)
            .groupsetId("groupset123")
            .canvasService(canvasService)
            .teamRepository(teamRepository)
            .build();

    // Act
    job.accept(ctx);

    // Assert
    ArgumentCaptor<List<Team>> teamsCaptor = ArgumentCaptor.forClass(List.class);
    verify(teamRepository).saveAll(teamsCaptor.capture());

    List<Team> savedTeams = teamsCaptor.getValue();
    assertEquals(2, savedTeams.size());
  }

  @Test
  public void testAccept_HandlesEmailConversion() throws Exception {
    // Arrange
    RosterStudent student1 =
        RosterStudent.builder()
            .email("alice@ucsb.edu") // Stored in canonical form
            .teamMembers(new ArrayList<>())
            .build();

    Course course =
        Course.builder()
            .id(1L)
            .courseName("CS156")
            .rosterStudents(List.of(student1))
            .teams(new ArrayList<>())
            .build();

    // Canvas returns email in non-canonical form (note: the entire email is lowercased
    // and @umail.ucsb.edu is converted to @ucsb.edu by CanonicalFormConverter)
    CanvasGroup group =
        CanvasGroup.builder()
            .name("Team Alpha")
            .id(101)
            .members(List.of("ALICE@umail.ucsb.edu"))
            .build();

    when(canvasService.getCanvasGroups(course, "groupset123")).thenReturn(List.of(group));

    PullTeamsFromCanvasJob job =
        PullTeamsFromCanvasJob.builder()
            .course(course)
            .groupsetId("groupset123")
            .canvasService(canvasService)
            .teamRepository(teamRepository)
            .build();

    // Act
    job.accept(ctx);

    // Assert
    ArgumentCaptor<List<Team>> teamsCaptor = ArgumentCaptor.forClass(List.class);
    verify(teamRepository).saveAll(teamsCaptor.capture());

    List<Team> savedTeams = teamsCaptor.getValue();
    Team savedTeam = savedTeams.get(0);
    assertEquals(1, savedTeam.getTeamMembers().size());
    assertEquals(student1, savedTeam.getTeamMembers().get(0).getRosterStudent());
  }

  @Test
  public void testAccept_HandlesGroupWithEmptyMembers() throws Exception {
    // Arrange
    Course course =
        Course.builder()
            .id(1L)
            .courseName("CS156")
            .rosterStudents(new ArrayList<>())
            .teams(new ArrayList<>())
            .build();

    CanvasGroup group =
        CanvasGroup.builder().name("Empty Team").id(101).members(new ArrayList<>()).build();

    when(canvasService.getCanvasGroups(course, "groupset123")).thenReturn(List.of(group));

    PullTeamsFromCanvasJob job =
        PullTeamsFromCanvasJob.builder()
            .course(course)
            .groupsetId("groupset123")
            .canvasService(canvasService)
            .teamRepository(teamRepository)
            .build();

    // Act
    job.accept(ctx);

    // Assert
    ArgumentCaptor<List<Team>> teamsCaptor = ArgumentCaptor.forClass(List.class);
    verify(teamRepository).saveAll(teamsCaptor.capture());

    List<Team> savedTeams = teamsCaptor.getValue();
    assertEquals(1, savedTeams.size());

    Team savedTeam = savedTeams.get(0);
    assertEquals("Empty Team", savedTeam.getName());
    assertTrue(savedTeam.getTeamMembers().isEmpty());
  }

  @Test
  public void testAccept_HandlesNoGroups() throws Exception {
    // Arrange
    Course course =
        Course.builder()
            .id(1L)
            .courseName("CS156")
            .rosterStudents(new ArrayList<>())
            .teams(new ArrayList<>())
            .build();

    when(canvasService.getCanvasGroups(course, "groupset123")).thenReturn(new ArrayList<>());

    PullTeamsFromCanvasJob job =
        PullTeamsFromCanvasJob.builder()
            .course(course)
            .groupsetId("groupset123")
            .canvasService(canvasService)
            .teamRepository(teamRepository)
            .build();

    // Act
    job.accept(ctx);

    // Assert
    ArgumentCaptor<List<Team>> teamsCaptor = ArgumentCaptor.forClass(List.class);
    verify(teamRepository).saveAll(teamsCaptor.capture());

    List<Team> savedTeams = teamsCaptor.getValue();
    assertTrue(savedTeams.isEmpty());
  }

  @Test
  public void testAccept_LinksTeamByNameWithTrimmedWhitespace() throws Exception {
    // Arrange
    Team existingTeam =
        Team.builder().name("Team Alpha").canvasId(null).teamMembers(new ArrayList<>()).build();

    Course course =
        Course.builder()
            .id(1L)
            .courseName("CS156")
            .rosterStudents(new ArrayList<>())
            .teams(new ArrayList<>(List.of(existingTeam)))
            .build();

    // Canvas group name has whitespace
    CanvasGroup group =
        CanvasGroup.builder()
            .name("Team Alpha   ") // Extra whitespace
            .id(101)
            .members(new ArrayList<>())
            .build();

    when(canvasService.getCanvasGroups(course, "groupset123")).thenReturn(List.of(group));

    PullTeamsFromCanvasJob job =
        PullTeamsFromCanvasJob.builder()
            .course(course)
            .groupsetId("groupset123")
            .canvasService(canvasService)
            .teamRepository(teamRepository)
            .build();

    // Act
    job.accept(ctx);

    // Assert
    ArgumentCaptor<List<Team>> teamsCaptor = ArgumentCaptor.forClass(List.class);
    verify(teamRepository).saveAll(teamsCaptor.capture());

    List<Team> savedTeams = teamsCaptor.getValue();
    assertEquals(1, savedTeams.size());

    // The existing team should be linked
    Team savedTeam = savedTeams.get(0);
    assertEquals(existingTeam, savedTeam);
    assertEquals(101, savedTeam.getCanvasId());
  }

  @Test
  public void testAccept_LogsProcessingMessage() throws Exception {
    // Arrange
    Course course =
        Course.builder()
            .id(1L)
            .courseName("CS156")
            .rosterStudents(new ArrayList<>())
            .teams(new ArrayList<>())
            .build();

    when(canvasService.getCanvasGroups(course, "groupset123")).thenReturn(new ArrayList<>());

    PullTeamsFromCanvasJob job =
        PullTeamsFromCanvasJob.builder()
            .course(course)
            .groupsetId("groupset123")
            .canvasService(canvasService)
            .teamRepository(teamRepository)
            .build();

    // Act
    job.accept(ctx);

    // Assert
    String log = jobStarted.getLog();
    assertTrue(log.contains("Processing..."));
  }
}
