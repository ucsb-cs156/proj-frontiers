package edu.ucsb.cs156.frontiers.jobs;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.entities.Job;
import edu.ucsb.cs156.frontiers.entities.Team;
import edu.ucsb.cs156.frontiers.repositories.CourseRepository;
import edu.ucsb.cs156.frontiers.repositories.TeamRepository;
import edu.ucsb.cs156.frontiers.services.GithubTeamService;
import edu.ucsb.cs156.frontiers.services.GithubTeamService.GithubTeamInfo;
import edu.ucsb.cs156.frontiers.services.jobs.JobContext;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class PullTeamsFromGithubJobTests {

  @Mock private CourseRepository courseRepository;
  @Mock private TeamRepository teamRepository;
  @Mock private GithubTeamService githubTeamService;

  Job jobStarted = Job.builder().build();
  JobContext ctx = new JobContext(null, jobStarted);

  @Test
  public void testAccept_CourseNotFound() throws Exception {
    Long courseId = 1L;
    when(courseRepository.findById(courseId)).thenReturn(Optional.empty());

    PullTeamsFromGithubJob job =
        PullTeamsFromGithubJob.builder()
            .courseId(courseId)
            .courseRepository(courseRepository)
            .teamRepository(teamRepository)
            .githubTeamService(githubTeamService)
            .build();

    job.accept(ctx);

    verify(courseRepository).findById(courseId);
    verifyNoInteractions(teamRepository, githubTeamService);
  }

  @Test
  public void testAccept_CourseWithoutGithubOrg() throws Exception {
    Long courseId = 1L;
    Course course = Course.builder().id(courseId).courseName("Test Course").build();
    when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));

    PullTeamsFromGithubJob job =
        PullTeamsFromGithubJob.builder()
            .courseId(courseId)
            .courseRepository(courseRepository)
            .teamRepository(teamRepository)
            .githubTeamService(githubTeamService)
            .build();

    job.accept(ctx);

    verify(courseRepository).findById(courseId);
    verifyNoInteractions(teamRepository, githubTeamService);
  }

  @Test
  public void testAccept_GetAllTeamsFailure() throws Exception {
    Long courseId = 1L;
    Course course =
        Course.builder()
            .id(courseId)
            .courseName("Test Course")
            .orgName("test-org")
            .installationId("123")
            .build();

    when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
    when(githubTeamService.getAllTeams(course)).thenThrow(new RuntimeException("GitHub API error"));

    PullTeamsFromGithubJob job =
        PullTeamsFromGithubJob.builder()
            .courseId(courseId)
            .courseRepository(courseRepository)
            .teamRepository(teamRepository)
            .githubTeamService(githubTeamService)
            .build();

    job.accept(ctx);

    verify(courseRepository).findById(courseId);
    verify(githubTeamService).getAllTeams(course);
    verifyNoInteractions(teamRepository);
  }

  @Test
  public void testAccept_UpsertsTeamsByGithubIdAndName() throws Exception {
    Long courseId = 1L;
    Course course =
        Course.builder()
            .id(courseId)
            .courseName("Test Course")
            .orgName("test-org")
            .installationId("123")
            .build();

    Team existingByName =
        Team.builder().name("team-by-name").githubTeamId(null).course(course).build();
    Team existingById = Team.builder().name("old-name").githubTeamId(222).course(course).build();
    Team unchanged = Team.builder().name("same-team").githubTeamId(333).course(course).build();

    List<GithubTeamInfo> githubTeams =
        Arrays.asList(
            new GithubTeamInfo(111, "team-by-name"),
            new GithubTeamInfo(222, "renamed-team"),
            new GithubTeamInfo(333, "same-team"),
            new GithubTeamInfo(444, "new-team"));

    when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
    when(githubTeamService.getAllTeams(course)).thenReturn(githubTeams);
    when(teamRepository.findByCourseId(courseId))
        .thenReturn(Arrays.asList(existingByName, existingById, unchanged));

    PullTeamsFromGithubJob job =
        PullTeamsFromGithubJob.builder()
            .courseId(courseId)
            .courseRepository(courseRepository)
            .teamRepository(teamRepository)
            .githubTeamService(githubTeamService)
            .build();

    job.accept(ctx);

    verify(githubTeamService).getAllTeams(course);
    verify(teamRepository).findByCourseId(courseId);

    verify(teamRepository)
        .save(argThat(t -> t.getName().equals("team-by-name") && t.getGithubTeamId().equals(111)));
    verify(teamRepository)
        .save(argThat(t -> t.getName().equals("renamed-team") && t.getGithubTeamId().equals(222)));
    verify(teamRepository)
        .save(
            argThat(
                t ->
                    t.getName().equals("new-team")
                        && t.getGithubTeamId().equals(444)
                        && t.getCourse().equals(course)));
    verify(teamRepository, times(3)).save(any(Team.class));

    verify(teamRepository, never())
        .save(argThat(t -> t.getName().equals("same-team") && t.getGithubTeamId().equals(333)));
  }
}
