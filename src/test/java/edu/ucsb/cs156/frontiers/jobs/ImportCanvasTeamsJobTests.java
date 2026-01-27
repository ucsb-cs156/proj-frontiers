package edu.ucsb.cs156.frontiers.jobs;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.entities.Job;
import edu.ucsb.cs156.frontiers.entities.RosterStudent;
import edu.ucsb.cs156.frontiers.entities.Team;
import edu.ucsb.cs156.frontiers.entities.TeamMember;
import edu.ucsb.cs156.frontiers.enums.TeamStatus;
import edu.ucsb.cs156.frontiers.repositories.CourseRepository;
import edu.ucsb.cs156.frontiers.repositories.TeamRepository;
import edu.ucsb.cs156.frontiers.services.CanvasService;
import edu.ucsb.cs156.frontiers.services.jobs.JobContext;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ImportCanvasTeamsJobTests {

  @Mock private CourseRepository courseRepository;
  @Mock private TeamRepository teamRepository;
  @Mock private CanvasService canvasService;

  private ObjectMapper objectMapper = new ObjectMapper();

  Job jobStarted = Job.builder().build();
  JobContext ctx = new JobContext(null, jobStarted);

  @BeforeEach
  public void setup() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  public void testAccept_CourseNotFound() throws Exception {
    // Arrange
    Long courseId = 1L;
    when(courseRepository.findById(courseId)).thenReturn(Optional.empty());

    ImportCanvasTeamsJob job =
        ImportCanvasTeamsJob.builder()
            .courseId(courseId)
            .courseRepository(courseRepository)
            .teamRepository(teamRepository)
            .canvasService(canvasService)
            .build();

    // Act
    job.accept(ctx);

    // Assert
    verify(courseRepository).findById(courseId);
    verifyNoInteractions(teamRepository, canvasService);
  }

  @Test
  public void testAccept_CourseWithNoCanvasApiToken() throws Exception {
    // Arrange
    Long courseId = 1L;
    Course course =
        Course.builder()
            .id(courseId)
            .courseName("Test Course")
            .canvasCourseId("12345")
            .canvasApiToken(null)
            .build();
    when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));

    ImportCanvasTeamsJob job =
        ImportCanvasTeamsJob.builder()
            .courseId(courseId)
            .courseRepository(courseRepository)
            .teamRepository(teamRepository)
            .canvasService(canvasService)
            .build();

    // Act
    job.accept(ctx);

    // Assert
    verify(courseRepository).findById(courseId);
    verifyNoInteractions(teamRepository, canvasService);
  }

  @Test
  public void testAccept_CourseWithNoCanvasCourseId() throws Exception {
    // Arrange
    Long courseId = 1L;
    Course course =
        Course.builder()
            .id(courseId)
            .courseName("Test Course")
            .canvasCourseId(null)
            .canvasApiToken("test-token")
            .build();
    when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));

    ImportCanvasTeamsJob job =
        ImportCanvasTeamsJob.builder()
            .courseId(courseId)
            .courseRepository(courseRepository)
            .teamRepository(teamRepository)
            .canvasService(canvasService)
            .build();

    // Act
    job.accept(ctx);

    // Assert
    verify(courseRepository).findById(courseId);
    verifyNoInteractions(teamRepository, canvasService);
  }

  @Test
  public void testAccept_CanvasFetchFails() throws Exception {
    // Arrange
    Long courseId = 1L;
    Course course =
        Course.builder()
            .id(courseId)
            .courseName("Test Course")
            .canvasCourseId("12345")
            .canvasApiToken("test-token")
            .rosterStudents(new ArrayList<>())
            .teams(new ArrayList<>())
            .build();
    when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
    when(canvasService.fetchCanvasTeamGroups(course))
        .thenThrow(new RuntimeException("Canvas API error"));

    ImportCanvasTeamsJob job =
        ImportCanvasTeamsJob.builder()
            .courseId(courseId)
            .courseRepository(courseRepository)
            .teamRepository(teamRepository)
            .canvasService(canvasService)
            .build();

    // Act
    job.accept(ctx);

    // Assert
    verify(courseRepository).findById(courseId);
    verify(canvasService).fetchCanvasTeamGroups(course);
    verifyNoInteractions(teamRepository);
  }

  @Test
  public void testAccept_SuccessfulImportWithNewTeam() throws Exception {
    // Arrange
    Long courseId = 1L;

    RosterStudent student1 =
        RosterStudent.builder()
            .id(1L)
            .email("student1@ucsb.edu")
            .firstName("Alice")
            .lastName("Smith")
            .teamMembers(new ArrayList<>())
            .build();

    Course course =
        Course.builder()
            .id(courseId)
            .courseName("Test Course")
            .canvasCourseId("12345")
            .canvasApiToken("test-token")
            .rosterStudents(new ArrayList<>(List.of(student1)))
            .teams(new ArrayList<>())
            .build();

    // Create Canvas group response
    String groupJson =
        """
        {
          "name": "Team Alpha",
          "_id": 123,
          "membersConnection": {
            "edges": [
              {"node": {"user": {"email": "student1@ucsb.edu"}}}
            ]
          }
        }
        """;
    JsonNode group = objectMapper.readTree(groupJson);
    List<JsonNode> groups = List.of(group);

    when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
    when(canvasService.fetchCanvasTeamGroups(course)).thenReturn(groups);

    ImportCanvasTeamsJob job =
        ImportCanvasTeamsJob.builder()
            .courseId(courseId)
            .courseRepository(courseRepository)
            .teamRepository(teamRepository)
            .canvasService(canvasService)
            .build();

    // Act
    job.accept(ctx);

    // Assert
    verify(courseRepository).findById(courseId);
    verify(canvasService).fetchCanvasTeamGroups(course);
    verify(teamRepository)
        .saveAll(
            argThat(
                teams -> {
                  List<Team> teamList = new ArrayList<>();
                  teams.forEach(teamList::add);
                  return teamList.size() == 1
                      && teamList.get(0).getName().equals("Team Alpha")
                      && teamList.get(0).getCanvasId().equals(123)
                      && teamList.get(0).getTeamMembers().size() == 1
                      && teamList.get(0).getTeamMembers().get(0).getRosterStudent().equals(student1)
                      && teamList
                          .get(0)
                          .getTeamMembers()
                          .get(0)
                          .getTeamStatus()
                          .equals(TeamStatus.NO_GITHUB_ID);
                }));
  }

  @Test
  public void testAccept_SuccessfulImportWithExistingTeam() throws Exception {
    // Arrange
    Long courseId = 1L;

    RosterStudent student1 =
        RosterStudent.builder()
            .id(1L)
            .email("student1@ucsb.edu")
            .firstName("Alice")
            .lastName("Smith")
            .teamMembers(new ArrayList<>())
            .build();

    Team existingTeam =
        Team.builder()
            .id(10L)
            .name("Team Alpha")
            .canvasId(100)
            .teamMembers(new ArrayList<>())
            .build();

    Course course =
        Course.builder()
            .id(courseId)
            .courseName("Test Course")
            .canvasCourseId("12345")
            .canvasApiToken("test-token")
            .rosterStudents(new ArrayList<>(List.of(student1)))
            .teams(new ArrayList<>(List.of(existingTeam)))
            .build();

    // Create Canvas group response with updated canvasId
    String groupJson =
        """
        {
          "name": "Team Alpha",
          "_id": 123,
          "membersConnection": {
            "edges": [
              {"node": {"user": {"email": "student1@ucsb.edu"}}}
            ]
          }
        }
        """;
    JsonNode group = objectMapper.readTree(groupJson);
    List<JsonNode> groups = List.of(group);

    when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
    when(canvasService.fetchCanvasTeamGroups(course)).thenReturn(groups);

    ImportCanvasTeamsJob job =
        ImportCanvasTeamsJob.builder()
            .courseId(courseId)
            .courseRepository(courseRepository)
            .teamRepository(teamRepository)
            .canvasService(canvasService)
            .build();

    // Act
    job.accept(ctx);

    // Assert
    verify(courseRepository).findById(courseId);
    verify(canvasService).fetchCanvasTeamGroups(course);
    verify(teamRepository)
        .saveAll(
            argThat(
                teams -> {
                  List<Team> teamList = new ArrayList<>();
                  teams.forEach(teamList::add);
                  return teamList.size() == 1
                      && teamList.get(0).getId().equals(10L)
                      && teamList.get(0).getName().equals("Team Alpha")
                      && teamList.get(0).getCanvasId().equals(123)
                      && teamList.get(0).getTeamMembers().size() == 1;
                }));
  }

  @Test
  public void testAccept_StudentNotFoundInRoster() throws Exception {
    // Arrange
    Long courseId = 1L;

    Course course =
        Course.builder()
            .id(courseId)
            .courseName("Test Course")
            .canvasCourseId("12345")
            .canvasApiToken("test-token")
            .rosterStudents(new ArrayList<>())
            .teams(new ArrayList<>())
            .build();

    // Create Canvas group with member not in roster
    String groupJson =
        """
        {
          "name": "Team Alpha",
          "_id": 123,
          "membersConnection": {
            "edges": [
              {"node": {"user": {"email": "unknown@ucsb.edu"}}}
            ]
          }
        }
        """;
    JsonNode group = objectMapper.readTree(groupJson);
    List<JsonNode> groups = List.of(group);

    when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
    when(canvasService.fetchCanvasTeamGroups(course)).thenReturn(groups);

    ImportCanvasTeamsJob job =
        ImportCanvasTeamsJob.builder()
            .courseId(courseId)
            .courseRepository(courseRepository)
            .teamRepository(teamRepository)
            .canvasService(canvasService)
            .build();

    // Act
    job.accept(ctx);

    // Assert
    verify(teamRepository)
        .saveAll(
            argThat(
                teams -> {
                  List<Team> teamList = new ArrayList<>();
                  teams.forEach(teamList::add);
                  // Team is created but with no members
                  return teamList.size() == 1
                      && teamList.get(0).getName().equals("Team Alpha")
                      && teamList.get(0).getTeamMembers().isEmpty();
                }));
  }

  @Test
  public void testAccept_StudentAlreadyInTeam() throws Exception {
    // Arrange
    Long courseId = 1L;

    Team existingTeam =
        Team.builder()
            .id(10L)
            .name("Team Alpha")
            .canvasId(100)
            .teamMembers(new ArrayList<>())
            .build();

    RosterStudent student1 =
        RosterStudent.builder()
            .id(1L)
            .email("student1@ucsb.edu")
            .firstName("Alice")
            .lastName("Smith")
            .build();

    // Student is already in the team
    TeamMember existingMember =
        TeamMember.builder()
            .id(100L)
            .team(existingTeam)
            .rosterStudent(student1)
            .teamStatus(TeamStatus.TEAM_MEMBER)
            .build();

    existingTeam.getTeamMembers().add(existingMember);
    student1.setTeamMembers(new ArrayList<>(List.of(existingMember)));

    Course course =
        Course.builder()
            .id(courseId)
            .courseName("Test Course")
            .canvasCourseId("12345")
            .canvasApiToken("test-token")
            .rosterStudents(new ArrayList<>(List.of(student1)))
            .teams(new ArrayList<>(List.of(existingTeam)))
            .build();

    // Create Canvas group with the same member
    String groupJson =
        """
        {
          "name": "Team Alpha",
          "_id": 123,
          "membersConnection": {
            "edges": [
              {"node": {"user": {"email": "student1@ucsb.edu"}}}
            ]
          }
        }
        """;
    JsonNode group = objectMapper.readTree(groupJson);
    List<JsonNode> groups = List.of(group);

    when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
    when(canvasService.fetchCanvasTeamGroups(course)).thenReturn(groups);

    ImportCanvasTeamsJob job =
        ImportCanvasTeamsJob.builder()
            .courseId(courseId)
            .courseRepository(courseRepository)
            .teamRepository(teamRepository)
            .canvasService(canvasService)
            .build();

    // Act
    job.accept(ctx);

    // Assert
    verify(teamRepository)
        .saveAll(
            argThat(
                teams -> {
                  List<Team> teamList = new ArrayList<>();
                  teams.forEach(teamList::add);
                  // Team should still have only 1 member (no duplicate)
                  return teamList.size() == 1
                      && teamList.get(0).getTeamMembers().size() == 1
                      && teamList
                          .get(0)
                          .getTeamMembers()
                          .get(0)
                          .getTeamStatus()
                          .equals(TeamStatus.TEAM_MEMBER);
                }));
  }

  @Test
  public void testAccept_MultipleTeamsAndMembers() throws Exception {
    // Arrange
    Long courseId = 1L;

    RosterStudent student1 =
        RosterStudent.builder()
            .id(1L)
            .email("student1@ucsb.edu")
            .firstName("Alice")
            .lastName("Smith")
            .teamMembers(new ArrayList<>())
            .build();

    RosterStudent student2 =
        RosterStudent.builder()
            .id(2L)
            .email("student2@ucsb.edu")
            .firstName("Bob")
            .lastName("Jones")
            .teamMembers(new ArrayList<>())
            .build();

    Course course =
        Course.builder()
            .id(courseId)
            .courseName("Test Course")
            .canvasCourseId("12345")
            .canvasApiToken("test-token")
            .rosterStudents(new ArrayList<>(Arrays.asList(student1, student2)))
            .teams(new ArrayList<>())
            .build();

    // Create Canvas groups response with two teams
    String group1Json =
        """
        {
          "name": "Team Alpha",
          "_id": 123,
          "membersConnection": {
            "edges": [
              {"node": {"user": {"email": "student1@ucsb.edu"}}}
            ]
          }
        }
        """;
    String group2Json =
        """
        {
          "name": "Team Beta",
          "_id": 456,
          "membersConnection": {
            "edges": [
              {"node": {"user": {"email": "student2@ucsb.edu"}}}
            ]
          }
        }
        """;
    JsonNode group1 = objectMapper.readTree(group1Json);
    JsonNode group2 = objectMapper.readTree(group2Json);
    List<JsonNode> groups = Arrays.asList(group1, group2);

    when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
    when(canvasService.fetchCanvasTeamGroups(course)).thenReturn(groups);

    ImportCanvasTeamsJob job =
        ImportCanvasTeamsJob.builder()
            .courseId(courseId)
            .courseRepository(courseRepository)
            .teamRepository(teamRepository)
            .canvasService(canvasService)
            .build();

    // Act
    job.accept(ctx);

    // Assert
    verify(teamRepository)
        .saveAll(
            argThat(
                teams -> {
                  List<Team> teamList = new ArrayList<>();
                  teams.forEach(teamList::add);
                  return teamList.size() == 2;
                }));
  }

  @Test
  public void testAccept_EmailNormalization() throws Exception {
    // Test that umail.ucsb.edu emails are normalized to ucsb.edu
    Long courseId = 1L;

    RosterStudent student1 =
        RosterStudent.builder()
            .id(1L)
            .email("student1@ucsb.edu")
            .firstName("Alice")
            .lastName("Smith")
            .teamMembers(new ArrayList<>())
            .build();

    Course course =
        Course.builder()
            .id(courseId)
            .courseName("Test Course")
            .canvasCourseId("12345")
            .canvasApiToken("test-token")
            .rosterStudents(new ArrayList<>(List.of(student1)))
            .teams(new ArrayList<>())
            .build();

    // Create Canvas group with umail.ucsb.edu email
    String groupJson =
        """
        {
          "name": "Team Alpha",
          "_id": 123,
          "membersConnection": {
            "edges": [
              {"node": {"user": {"email": "student1@umail.ucsb.edu"}}}
            ]
          }
        }
        """;
    JsonNode group = objectMapper.readTree(groupJson);
    List<JsonNode> groups = List.of(group);

    when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
    when(canvasService.fetchCanvasTeamGroups(course)).thenReturn(groups);

    ImportCanvasTeamsJob job =
        ImportCanvasTeamsJob.builder()
            .courseId(courseId)
            .courseRepository(courseRepository)
            .teamRepository(teamRepository)
            .canvasService(canvasService)
            .build();

    // Act
    job.accept(ctx);

    // Assert
    verify(teamRepository)
        .saveAll(
            argThat(
                teams -> {
                  List<Team> teamList = new ArrayList<>();
                  teams.forEach(teamList::add);
                  // Should match student1 even though Canvas had umail.ucsb.edu
                  return teamList.size() == 1
                      && teamList.get(0).getTeamMembers().size() == 1
                      && teamList
                          .get(0)
                          .getTeamMembers()
                          .get(0)
                          .getRosterStudent()
                          .equals(student1);
                }));
  }

  @Test
  public void testAccept_EmptyGroups() throws Exception {
    // Arrange
    Long courseId = 1L;

    Course course =
        Course.builder()
            .id(courseId)
            .courseName("Test Course")
            .canvasCourseId("12345")
            .canvasApiToken("test-token")
            .rosterStudents(new ArrayList<>())
            .teams(new ArrayList<>())
            .build();

    List<JsonNode> groups = new ArrayList<>();

    when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
    when(canvasService.fetchCanvasTeamGroups(course)).thenReturn(groups);

    ImportCanvasTeamsJob job =
        ImportCanvasTeamsJob.builder()
            .courseId(courseId)
            .courseRepository(courseRepository)
            .teamRepository(teamRepository)
            .canvasService(canvasService)
            .build();

    // Act
    job.accept(ctx);

    // Assert
    verify(teamRepository)
        .saveAll(
            argThat(
                teams -> {
                  List<Team> teamList = new ArrayList<>();
                  teams.forEach(teamList::add);
                  return teamList.isEmpty();
                }));
  }
}
