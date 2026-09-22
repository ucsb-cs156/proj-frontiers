package edu.ucsb.cs156.frontiers.jobs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.entities.RosterStudent;
import edu.ucsb.cs156.frontiers.entities.Team;
import edu.ucsb.cs156.frontiers.entities.TeamMember;
import edu.ucsb.cs156.frontiers.enums.RosterStatus;
import edu.ucsb.cs156.frontiers.models.CATMETeamAssignment;
import edu.ucsb.cs156.frontiers.repositories.CourseRepository;
import edu.ucsb.cs156.frontiers.repositories.RosterStudentRepository;
import edu.ucsb.cs156.frontiers.repositories.TeamMemberRepository;
import edu.ucsb.cs156.frontiers.repositories.TeamRepository;
import edu.ucsb.cs156.jobs.entities.Job;
import edu.ucsb.cs156.jobs.services.JobContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CreateTeamsFromCATMEJobTests {

  @Mock private CourseRepository courseRepository;
  @Mock private RosterStudentRepository rosterStudentRepository;
  @Mock private TeamRepository teamRepository;
  @Mock private TeamMemberRepository teamMemberRepository;

  Job jobStarted = Job.builder().build();
  JobContext ctx = new JobContext(null, jobStarted);

  private final Course course = Course.builder().id(1L).courseName("CS156").build();

  private CreateTeamsFromCATMEJob job(List<CATMETeamAssignment> assignments) {
    return CreateTeamsFromCATMEJob.builder()
        .course(Course.builder().id(1L).build())
        .assignments(assignments)
        .courseRepository(courseRepository)
        .rosterStudentRepository(rosterStudentRepository)
        .teamRepository(teamRepository)
        .teamMemberRepository(teamMemberRepository)
        .build();
  }

  private static RosterStudent student(String studentId, RosterStatus status) {
    return RosterStudent.builder()
        .id(Long.parseLong(studentId))
        .studentId(studentId)
        .firstName("First" + studentId)
        .lastName("Last")
        .rosterStatus(status)
        .build();
  }

  private static String log(String... lines) {
    return String.join("\n", lines);
  }

  @Test
  public void scope_is_the_course_and_assignments_are_exposed() {
    List<CATMETeamAssignment> assignments = List.of(new CATMETeamAssignment("1", "A", "t"));
    CreateTeamsFromCATMEJob job =
        CreateTeamsFromCATMEJob.builder()
            .course(Course.builder().id(42L).build())
            .assignments(assignments)
            .build();
    assertEquals("course", job.getScopeType());
    assertEquals(42L, job.getScopeId());
    assertEquals(assignments, job.getAssignments());
  }

  @Test
  public void creates_teams_and_adds_students_without_pushing_to_github() throws Exception {
    when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
    RosterStudent alice = student("1111111", RosterStatus.ROSTER);
    RosterStudent bob = student("2222222", RosterStatus.MANUAL);
    RosterStudent carol = student("3333333", RosterStatus.ROSTER);
    RosterStudent dropped = student("4444444", RosterStatus.DROPPED);
    RosterStudent noId = RosterStudent.builder().id(5L).rosterStatus(RosterStatus.ROSTER).build();
    when(rosterStudentRepository.findByCourseId(1L))
        .thenReturn(List.of(alice, bob, carol, dropped, noId));

    Team existing =
        Team.builder()
            .id(10L)
            .name("team-02")
            .course(course)
            .teamMembers(new ArrayList<>())
            .build();
    when(teamRepository.findByCourseIdAndName(1L, "team-01")).thenReturn(Optional.empty());
    when(teamRepository.findByCourseIdAndName(1L, "team-02")).thenReturn(Optional.of(existing));
    when(teamRepository.save(any(Team.class)))
        .thenAnswer(
            inv -> {
              Team t = inv.getArgument(0);
              t.setId(20L);
              return t;
            });
    when(teamMemberRepository.findByTeamAndRosterStudent(any(), any()))
        .thenAnswer(
            inv ->
                inv.getArgument(1) == carol
                    ? Optional.of(TeamMember.builder().build())
                    : Optional.empty());

    job(List.of(
            new CATMETeamAssignment("1111111", "Last, First1111111", "team-01"),
            new CATMETeamAssignment("2222222", "Last, First2222222", " team-01 "),
            new CATMETeamAssignment("3333333", "Last, First3333333", "team-02"),
            new CATMETeamAssignment("4444444", "Last, First4444444", "team-02"),
            new CATMETeamAssignment("9999999", "Gone, Someone", "team-02"),
            new CATMETeamAssignment("8888888", "No, Team", ""),
            new CATMETeamAssignment("7777777", "Null, Team", null)))
        .accept(ctx);

    assertEquals(
        log(
            "Creating teams from CATME file (teams are NOT pushed to GitHub by this job)",
            "2 student(s) in the file have no team name; skipped",
            "Created team team-01",
            "Added Last, First1111111 (1111111) to team team-01",
            "Added Last, First2222222 (2222222) to team team-01",
            "Team team-02 already exists",
            "Last, First3333333 (3333333) is already on team team-02",
            "Student Last, First4444444 (4444444) is not an enrolled student of this course; skipped",
            "Student Gone, Someone (9999999) is not an enrolled student of this course; skipped",
            "Done: 1 team(s) created, 2 student(s) added to teams. Teams have NOT been pushed to GitHub; use Push Teams to GitHub on the Teams tab when ready."),
        jobStarted.getLog());

    ArgumentCaptor<Team> teamCaptor = ArgumentCaptor.forClass(Team.class);
    verify(teamRepository).save(teamCaptor.capture());
    Team created = teamCaptor.getValue();
    assertEquals("team-01", created.getName());
    assertEquals(course, created.getCourse());
    assertEquals(List.of(), created.getTeamMembers());
    assertEquals(null, created.getGithubTeamId());
    assertEquals(null, created.getGithubTeamSlug());

    ArgumentCaptor<TeamMember> memberCaptor = ArgumentCaptor.forClass(TeamMember.class);
    verify(teamMemberRepository, org.mockito.Mockito.times(2)).save(memberCaptor.capture());
    List<TeamMember> members = memberCaptor.getAllValues();
    assertEquals(alice, members.get(0).getRosterStudent());
    assertEquals(created, members.get(0).getTeam());
    assertEquals(bob, members.get(1).getRosterStudent());
    assertEquals(created, members.get(1).getTeam());
    verify(teamRepository, never()).save(existing);
  }

  @Test
  public void no_assignments_creates_nothing() throws Exception {
    when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
    when(rosterStudentRepository.findByCourseId(1L)).thenReturn(List.of());

    job(List.of()).accept(ctx);

    assertEquals(
        log(
            "Creating teams from CATME file (teams are NOT pushed to GitHub by this job)",
            "Done: 0 team(s) created, 0 student(s) added to teams. Teams have NOT been pushed to GitHub; use Push Teams to GitHub on the Teams tab when ready."),
        jobStarted.getLog());
    verify(teamRepository, never()).save(any());
    verify(teamMemberRepository, never()).save(any());
  }
}
