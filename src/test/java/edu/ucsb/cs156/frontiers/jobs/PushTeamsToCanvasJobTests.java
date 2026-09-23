package edu.ucsb.cs156.frontiers.jobs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.entities.RosterStudent;
import edu.ucsb.cs156.frontiers.entities.Team;
import edu.ucsb.cs156.frontiers.entities.TeamMember;
import edu.ucsb.cs156.frontiers.models.CanvasGroupDetail;
import edu.ucsb.cs156.frontiers.models.CanvasGroupSetDetail;
import edu.ucsb.cs156.frontiers.repositories.CourseRepository;
import edu.ucsb.cs156.frontiers.repositories.TeamRepository;
import edu.ucsb.cs156.frontiers.services.CanvasService;
import edu.ucsb.cs156.jobs.entities.Job;
import edu.ucsb.cs156.jobs.services.JobContext;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class PushTeamsToCanvasJobTests {

  @Mock private CanvasService canvasService;
  @Mock private CourseRepository courseRepository;
  @Mock private TeamRepository teamRepository;

  Job jobStarted = Job.builder().build();
  JobContext ctx = new JobContext(null, jobStarted);

  private static final String GROUP_SET_ID = "R3JvdXBTZXQtMTAx";

  private static RosterStudent student(String email) {
    return RosterStudent.builder().email(email).teamMembers(new ArrayList<>()).build();
  }

  private static Team team(String name, Integer canvasId, RosterStudent... students) {
    Team team = Team.builder().name(name).canvasId(canvasId).teamMembers(new ArrayList<>()).build();
    for (RosterStudent student : students) {
      team.getTeamMembers().add(TeamMember.builder().team(team).rosterStudent(student).build());
    }
    return team;
  }

  private static CanvasGroupDetail group(Integer id, String name, Map<String, Integer> members) {
    return CanvasGroupDetail.builder()
        .id(id)
        .name(name)
        .memberUserIdsByEmail(new LinkedHashMap<>(members))
        .build();
  }

  private static CanvasGroupSetDetail groupSet(CanvasGroupDetail... groups) {
    return CanvasGroupSetDetail.builder()
        .id(101)
        .name("Project Teams")
        .groups(new ArrayList<>(List.of(groups)))
        .build();
  }

  private Course course(Team... teams) {
    Course course =
        Course.builder().id(1L).courseName("CS156").teams(new ArrayList<>(List.of(teams))).build();
    for (Team team : teams) {
      team.setCourse(course);
    }
    when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
    return course;
  }

  private PushTeamsToCanvasJob job(Course course) {
    return PushTeamsToCanvasJob.builder()
        .course(course)
        .groupSetId(GROUP_SET_ID)
        .canvasService(canvasService)
        .courseRepository(courseRepository)
        .teamRepository(teamRepository)
        .build();
  }

  private String log() {
    return jobStarted.getLog();
  }

  @Test
  public void getScope_isTheCourse() {
    Course course = Course.builder().id(42L).build();
    PushTeamsToCanvasJob job = PushTeamsToCanvasJob.builder().course(course).build();
    assertEquals("course", job.getScopeType());
    assertEquals(42L, job.getScopeId());
  }

  @Test
  public void createsGroupsForTeamsWithNoGroup_andAddsMembers() throws Exception {
    RosterStudent alice = student("alice@ucsb.edu");
    RosterStudent bob = student("bob@ucsb.edu");
    Course course = course(team("Team Beta", null, bob), team("Team Alpha", null, alice));
    when(canvasService.getCanvasGroupSetDetail(course, GROUP_SET_ID)).thenReturn(groupSet());
    when(canvasService.getCanvasUserIdsByEmail(course))
        .thenReturn(Map.of("alice@ucsb.edu", 11, "bob@ucsb.edu", 12));
    when(canvasService.createCanvasGroup(course, 101, "Team Alpha")).thenReturn(201);
    when(canvasService.createCanvasGroup(course, 101, "Team Beta")).thenReturn(202);

    job(course).accept(ctx);

    // Teams are processed in name order.
    InOrder inOrder = Mockito.inOrder(canvasService);
    inOrder.verify(canvasService).createCanvasGroup(course, 101, "Team Alpha");
    inOrder.verify(canvasService).addCanvasGroupMember(course, 201, 11);
    inOrder.verify(canvasService).createCanvasGroup(course, 101, "Team Beta");
    inOrder.verify(canvasService).addCanvasGroupMember(course, 202, 12);
    verify(canvasService, never()).deleteCanvasGroup(any(), anyInt());
    verify(canvasService, never()).removeCanvasGroupMember(any(), anyInt(), anyInt());

    assertEquals(201, course.getTeams().get(1).getCanvasId());
    assertEquals(202, course.getTeams().get(0).getCanvasId());
    verify(teamRepository).save(course.getTeams().get(0));
    verify(teamRepository).save(course.getTeams().get(1));

    assertTrue(
        log()
            .contains(
                "Pushing 2 teams to Canvas group set Project Teams (id 101), which has 0 groups"));
    assertTrue(log().contains("Created Canvas group Team Alpha (id 201)"));
    assertTrue(log().contains("Added alice@ucsb.edu to Canvas group Team Alpha"));
    assertTrue(log().contains("Created Canvas group Team Beta (id 202)"));
    assertTrue(log().contains("Added bob@ucsb.edu to Canvas group Team Beta"));
    assertTrue(log().contains("Done pushing teams to Canvas"));
    assertFalse(log().contains("WARNING"));
  }

  @Test
  public void matchesGroupByCanvasId_evenWhenNameDiffers_andLeavesMatchingMembersAlone()
      throws Exception {
    RosterStudent alice = student("alice@ucsb.edu");
    Team team = team("Team Alpha", 201, alice);
    Course course = course(team);
    when(canvasService.getCanvasGroupSetDetail(course, GROUP_SET_ID))
        .thenReturn(groupSet(group(201, "Old Alpha Name", Map.of("alice@ucsb.edu", 11))));
    when(canvasService.getCanvasUserIdsByEmail(course)).thenReturn(Map.of("alice@ucsb.edu", 11));

    job(course).accept(ctx);

    verify(canvasService).getCanvasGroupSetDetail(course, GROUP_SET_ID);
    verify(canvasService).getCanvasUserIdsByEmail(course);
    verifyNoMoreInteractions(canvasService);
    verify(teamRepository, never()).save(any());
    assertEquals(201, team.getCanvasId());
    assertTrue(log().contains("Team Team Alpha matches Canvas group Old Alpha Name (id 201)"));
    assertFalse(log().contains("WARNING"));
  }

  @Test
  public void matchesGroupByTrimmedName_whenTeamHasNoCanvasId_andStoresTheId() throws Exception {
    RosterStudent alice = student("alice@ucsb.edu");
    Team team = team(" Team Alpha ", null, alice);
    Course course = course(team);
    when(canvasService.getCanvasGroupSetDetail(course, GROUP_SET_ID))
        .thenReturn(groupSet(group(201, "Team Alpha ", Map.of("alice@ucsb.edu", 11))));
    when(canvasService.getCanvasUserIdsByEmail(course)).thenReturn(Map.of("alice@ucsb.edu", 11));

    job(course).accept(ctx);

    assertEquals(201, team.getCanvasId());
    verify(teamRepository).save(team);
    verify(canvasService, never()).createCanvasGroup(any(), anyInt(), anyString());
    assertTrue(log().contains("Team  Team Alpha  matches Canvas group Team Alpha  (id 201)"));
  }

  @Test
  public void matchesByName_whenStoredCanvasIdIsNotInTheGroupSet_andUpdatesTheId()
      throws Exception {
    Team team = team("Team Alpha", 999);
    Course course = course(team);
    when(canvasService.getCanvasGroupSetDetail(course, GROUP_SET_ID))
        .thenReturn(groupSet(group(201, "Team Alpha", Map.of())));
    when(canvasService.getCanvasUserIdsByEmail(course)).thenReturn(Map.of());

    job(course).accept(ctx);

    assertEquals(201, team.getCanvasId());
    verify(teamRepository).save(team);
    verify(canvasService, never()).createCanvasGroup(any(), anyInt(), anyString());
    verify(canvasService, never()).deleteCanvasGroup(any(), anyInt());
  }

  @Test
  public void addsAndRemovesMembersToMatchTheTeam() throws Exception {
    RosterStudent alice = student("alice@ucsb.edu");
    RosterStudent bob = student("Bob@umail.ucsb.edu");
    Team team = team("Team Alpha", 201, alice, bob);
    Course course = course(team);
    when(canvasService.getCanvasGroupSetDetail(course, GROUP_SET_ID))
        .thenReturn(
            groupSet(group(201, "Team Alpha", Map.of("alice@ucsb.edu", 11, "carol@ucsb.edu", 13))));
    when(canvasService.getCanvasUserIdsByEmail(course))
        .thenReturn(Map.of("alice@ucsb.edu", 11, "bob@ucsb.edu", 12, "carol@ucsb.edu", 13));

    job(course).accept(ctx);

    verify(canvasService).addCanvasGroupMember(course, 201, 12);
    verify(canvasService).removeCanvasGroupMember(course, 201, 13);
    verify(canvasService, never()).addCanvasGroupMember(course, 201, 11);
    verify(canvasService, never()).removeCanvasGroupMember(course, 201, 11);
    assertTrue(log().contains("Added bob@ucsb.edu to Canvas group Team Alpha"));
    assertTrue(log().contains("Removed carol@ucsb.edu from Canvas group Team Alpha"));
    assertFalse(log().contains("WARNING"));
  }

  @Test
  public void deletesGroupsWithNoMatchingTeam() throws Exception {
    Course course = course(team("Team Alpha", 201));
    when(canvasService.getCanvasGroupSetDetail(course, GROUP_SET_ID))
        .thenReturn(
            groupSet(
                group(201, "Team Alpha", Map.of()),
                group(202, "Stale Group", Map.of("zed@ucsb.edu", 26))));
    when(canvasService.getCanvasUserIdsByEmail(course)).thenReturn(Map.of("zed@ucsb.edu", 26));

    job(course).accept(ctx);

    verify(canvasService).deleteCanvasGroup(course, 202);
    verify(canvasService, never()).deleteCanvasGroup(course, 201);
    verify(canvasService, never()).removeCanvasGroupMember(any(), anyInt(), anyInt());
    assertTrue(
        log()
            .contains("Deleted Canvas group Stale Group (id 202), which has no team in Frontiers"));
  }

  @Test
  public void warnsAndSkipsStudentsNotEnrolledInCanvas() throws Exception {
    RosterStudent alice = student("alice@ucsb.edu");
    RosterStudent ghost = student("ghost@ucsb.edu");
    Course course = course(team("Team Alpha", 201, alice, ghost));
    when(canvasService.getCanvasGroupSetDetail(course, GROUP_SET_ID))
        .thenReturn(groupSet(group(201, "Team Alpha", Map.of())));
    when(canvasService.getCanvasUserIdsByEmail(course)).thenReturn(Map.of("alice@ucsb.edu", 11));

    job(course).accept(ctx);

    verify(canvasService).addCanvasGroupMember(course, 201, 11);
    verify(canvasService, never()).addCanvasGroupMember(eq(course), eq(201), eq(null));
    assertTrue(
        log()
            .contains(
                "WARNING: Student ghost@ucsb.edu on team Team Alpha is not enrolled in the Canvas"
                    + " course; skipping"));
    assertTrue(log().contains("Done pushing teams to Canvas"));
  }

  @Test
  public void warnsAndContinues_whenCreatingAGroupFails() throws Exception {
    RosterStudent alice = student("alice@ucsb.edu");
    RosterStudent bob = student("bob@ucsb.edu");
    Course course = course(team("Team Alpha", null, alice), team("Team Beta", null, bob));
    when(canvasService.getCanvasGroupSetDetail(course, GROUP_SET_ID))
        .thenReturn(groupSet(group(299, "Stale Group", Map.of())));
    when(canvasService.getCanvasUserIdsByEmail(course))
        .thenReturn(Map.of("alice@ucsb.edu", 11, "bob@ucsb.edu", 12));
    when(canvasService.createCanvasGroup(course, 101, "Team Alpha"))
        .thenThrow(new RuntimeException("boom"));
    when(canvasService.createCanvasGroup(course, 101, "Team Beta")).thenReturn(202);

    job(course).accept(ctx);

    verify(canvasService, never()).addCanvasGroupMember(course, 201, 11);
    verify(canvasService).addCanvasGroupMember(course, 202, 12);
    verify(canvasService).deleteCanvasGroup(course, 299);
    verify(teamRepository, never()).save(course.getTeams().get(0));
    verify(teamRepository).save(course.getTeams().get(1));
    assertTrue(log().contains("WARNING: Could not create Canvas group Team Alpha: boom"));
    assertTrue(log().contains("Done pushing teams to Canvas"));
  }

  @Test
  public void warnsAndContinues_whenMembershipCallsFail() throws Exception {
    RosterStudent alice = student("alice@ucsb.edu");
    RosterStudent bob = student("bob@ucsb.edu");
    Team team = team("Team Alpha", 201, alice, bob);
    Course course = course(team);
    when(canvasService.getCanvasGroupSetDetail(course, GROUP_SET_ID))
        .thenReturn(groupSet(group(201, "Team Alpha", Map.of("carol@ucsb.edu", 13))));
    when(canvasService.getCanvasUserIdsByEmail(course))
        .thenReturn(Map.of("alice@ucsb.edu", 11, "bob@ucsb.edu", 12, "carol@ucsb.edu", 13));
    doThrow(new RuntimeException("add failed"))
        .when(canvasService)
        .addCanvasGroupMember(course, 201, 11);
    doThrow(new RuntimeException("remove failed"))
        .when(canvasService)
        .removeCanvasGroupMember(course, 201, 13);

    job(course).accept(ctx);

    verify(canvasService).addCanvasGroupMember(course, 201, 11);
    verify(canvasService).addCanvasGroupMember(course, 201, 12);
    verify(canvasService).removeCanvasGroupMember(course, 201, 13);
    assertTrue(
        log()
            .contains(
                "WARNING: Could not add alice@ucsb.edu to Canvas group Team Alpha: add failed"));
    assertTrue(log().contains("Added bob@ucsb.edu to Canvas group Team Alpha"));
    assertTrue(
        log()
            .contains(
                "WARNING: Could not remove carol@ucsb.edu from Canvas group Team Alpha: remove"
                    + " failed"));
    assertTrue(log().contains("Done pushing teams to Canvas"));
  }

  @Test
  public void warnsAndContinues_whenDeletingAGroupFails() throws Exception {
    Course course = course();
    when(canvasService.getCanvasGroupSetDetail(course, GROUP_SET_ID))
        .thenReturn(
            groupSet(group(202, "Stale Group", Map.of()), group(203, "Other Stale", Map.of())));
    when(canvasService.getCanvasUserIdsByEmail(course)).thenReturn(Map.of());
    doThrow(new RuntimeException("delete failed"))
        .when(canvasService)
        .deleteCanvasGroup(course, 202);

    job(course).accept(ctx);

    verify(canvasService).deleteCanvasGroup(course, 202);
    verify(canvasService).deleteCanvasGroup(course, 203);
    assertTrue(
        log()
            .contains(
                "WARNING: Could not delete Canvas group Stale Group (id 202): delete failed"));
    assertTrue(
        log()
            .contains("Deleted Canvas group Other Stale (id 203), which has no team in Frontiers"));
    assertTrue(log().contains("Done pushing teams to Canvas"));
  }

  @Test
  public void failsTheJob_whenTheGroupSetCannotBeRead() throws Exception {
    Course course = course(team("Team Alpha", null));
    when(canvasService.getCanvasGroupSetDetail(course, GROUP_SET_ID))
        .thenThrow(new RuntimeException("no such group set"));

    RuntimeException e = assertThrows(RuntimeException.class, () -> job(course).accept(ctx));

    assertEquals("no such group set", e.getMessage());
    verify(canvasService, never()).createCanvasGroup(any(), anyInt(), anyString());
    assertNull(log());
  }

  @Test
  public void usesTheFreshCourseFromTheRepository() throws Exception {
    Course stale = Course.builder().id(1L).teams(new ArrayList<>()).build();
    Team team = team("Team Alpha", 201);
    Course fresh = course(team);
    when(canvasService.getCanvasGroupSetDetail(fresh, GROUP_SET_ID))
        .thenReturn(groupSet(group(201, "Team Alpha", Map.of())));
    when(canvasService.getCanvasUserIdsByEmail(fresh)).thenReturn(Map.of());

    job(stale).accept(ctx);

    verify(courseRepository).findById(1L);
    verify(canvasService).getCanvasGroupSetDetail(fresh, GROUP_SET_ID);
    assertTrue(log().contains("Pushing 1 teams to Canvas group set Project Teams (id 101)"));
  }
}
