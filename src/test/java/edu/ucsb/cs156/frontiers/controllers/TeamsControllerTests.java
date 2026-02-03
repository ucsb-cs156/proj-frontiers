package edu.ucsb.cs156.frontiers.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.ucsb.cs156.frontiers.ControllerTestCase;
import edu.ucsb.cs156.frontiers.annotations.WithInstructorCoursePermissions;
import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.entities.RosterStudent;
import edu.ucsb.cs156.frontiers.entities.Team;
import edu.ucsb.cs156.frontiers.entities.TeamMember;
import edu.ucsb.cs156.frontiers.repositories.CourseRepository;
import edu.ucsb.cs156.frontiers.repositories.RosterStudentRepository;
import edu.ucsb.cs156.frontiers.repositories.TeamMemberRepository;
import edu.ucsb.cs156.frontiers.repositories.TeamRepository;
import edu.ucsb.cs156.frontiers.services.GithubTeamService;
import edu.ucsb.cs156.frontiers.services.jobs.JobService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;

@WebMvcTest(controllers = TeamsController.class)
public class TeamsControllerTests extends ControllerTestCase {

  @MockitoBean TeamRepository teamRepository;

  @MockitoBean TeamMemberRepository teamMemberRepository;

  @MockitoBean CourseRepository courseRepository;

  @MockitoBean RosterStudentRepository rosterStudentRepository;

  @MockitoBean JobService jobService;

  @MockitoBean GithubTeamService githubTeamService;

  @WithMockUser(roles = {"ADMIN"})
  @Test
  public void testPostTeam_byAdmin() throws Exception {
    // arrange
    Course course = Course.builder().id(1L).courseName("CS156").term("F23").school("UCSB").build();

    Team team = Team.builder().id(1L).name("Team Alpha").course(course).build();

    Team teamToSave = Team.builder().name("Team Alpha").course(course).build();

    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(course));
    when(teamRepository.save(any(Team.class))).thenReturn(team);

    // act
    MvcResult response =
        mockMvc
            .perform(
                post("/api/teams/post")
                    .param("name", "Team Alpha")
                    .param("courseId", "1")
                    .with(csrf()))
            .andExpect(status().isOk())
            .andReturn();

    // assert
    verify(courseRepository, times(1)).findById(1L);
    verify(teamRepository, times(1)).save(teamToSave);

    String expectedJson = mapper.writeValueAsString(team);
    String responseString = response.getResponse().getContentAsString();
    assertEquals(expectedJson, responseString);
  }

  @Test
  @WithInstructorCoursePermissions
  public void testPostTeam_byInstructor() throws Exception {
    // arrange
    Course course = Course.builder().id(1L).courseName("CS156").term("F23").school("UCSB").build();

    Team team = Team.builder().id(1L).name("Team Beta").course(course).build();

    Team teamToSave = Team.builder().name("Team Beta").course(course).build();

    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(course));
    when(teamRepository.save(any(Team.class))).thenReturn(team);

    // act
    MvcResult response =
        mockMvc
            .perform(
                post("/api/teams/post")
                    .param("name", "Team Beta")
                    .param("courseId", "1")
                    .with(csrf()))
            .andExpect(status().isOk())
            .andReturn();

    // assert
    verify(courseRepository, times(1)).findById(1L);
    verify(teamRepository, times(1)).save(teamToSave);

    String expectedJson = mapper.writeValueAsString(team);
    String responseString = response.getResponse().getContentAsString();
    assertEquals(expectedJson, responseString);
  }

  @Test
  @WithInstructorCoursePermissions
  public void testPostTeam_not_unique() throws Exception {
    // arrange
    Course course = Course.builder().id(1L).courseName("CS156").term("F23").school("UCSB").build();

    Team team = Team.builder().id(1L).name("Team Beta").course(course).build();

    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(course));
    when(teamRepository.findByCourseIdAndName(eq(1L), eq("Team Beta")))
        .thenReturn(Optional.of(team));

    // act
    MvcResult response =
        mockMvc
            .perform(
                post("/api/teams/post")
                    .param("name", "Team Beta")
                    .param("courseId", "1")
                    .with(csrf()))
            .andExpect(status().isConflict())
            .andReturn();

    // assert
    verify(courseRepository, times(1)).findById(1L);
    verify(teamRepository, never()).save(any(Team.class));

    assertEquals(
        "Team with name Team Beta already exists", response.getResponse().getErrorMessage());
  }

  @WithMockUser(roles = {"ADMIN"})
  @Test
  public void testPostTeam_courseNotFound() throws Exception {
    // arrange
    when(courseRepository.findById(eq(999L))).thenReturn(Optional.empty());

    // act
    MvcResult response =
        mockMvc
            .perform(
                post("/api/teams/post")
                    .param("name", "Team Gamma")
                    .param("courseId", "999")
                    .with(csrf()))
            .andExpect(status().isNotFound())
            .andReturn();

    // assert
    verify(courseRepository, times(1)).findById(999L);
    verify(teamRepository, never()).save(any(Team.class));

    String responseString = response.getResponse().getContentAsString();
    Map<String, String> expectedMap =
        Map.of(
            "message", "Course with id 999 not found",
            "type", "EntityNotFoundException");
    String expectedJson = mapper.writeValueAsString(expectedMap);
    assertEquals(expectedJson, responseString);
  }

  // Tests for GET /api/teams/all

  @WithMockUser(roles = {"ADMIN"})
  @Test
  public void testAllTeams_ROLE_ADMIN() throws Exception {
    // arrange
    Course course1 = Course.builder().id(1L).courseName("CS156").build();

    Team team1 = Team.builder().id(1L).name("Team Alpha").course(course1).build();
    Team team2 = Team.builder().id(2L).name("Team Beta").course(course1).build();

    List<Team> expectedTeams = Arrays.asList(team1, team2);

    when(teamRepository.findByCourseIdOrderByNameAsc(eq(1L))).thenReturn(expectedTeams);

    // act
    MvcResult response =
        mockMvc
            .perform(get("/api/teams/all").param("courseId", "1"))
            .andExpect(status().isOk())
            .andReturn();

    // assert
    verify(teamRepository, times(1)).findByCourseIdOrderByNameAsc(eq(1L));

    String expectedJson = mapper.writeValueAsString(expectedTeams);
    String responseString = response.getResponse().getContentAsString();
    assertEquals(expectedJson, responseString);
  }

  @WithInstructorCoursePermissions
  @Test
  public void testAllTeams_ROLE_INSTRUCTOR() throws Exception {
    // arrange
    Course course = Course.builder().id(1L).courseName("CS156").build();
    Team team = Team.builder().id(1L).name("Team Alpha").course(course).build();

    List<Team> expectedTeams = Arrays.asList(team);

    when(teamRepository.findByCourseIdOrderByNameAsc(eq(1L))).thenReturn(expectedTeams);

    // act
    MvcResult response =
        mockMvc
            .perform(get("/api/teams/all").param("courseId", "1"))
            .andExpect(status().isOk())
            .andReturn();

    // assert
    verify(teamRepository, times(1)).findByCourseIdOrderByNameAsc(eq(1L));

    String expectedJson = mapper.writeValueAsString(expectedTeams);
    String responseString = response.getResponse().getContentAsString();
    assertEquals(expectedJson, responseString);
  }

  // Tests for GET /api/teams

  @Test
  @WithInstructorCoursePermissions
  public void testGetTeamById() throws Exception {
    // arrange
    Course course = Course.builder().id(1L).courseName("CS156").build();
    Team team = Team.builder().id(1L).name("Team Alpha").course(course).build();

    when(teamRepository.findById(eq(1L))).thenReturn(Optional.of(team));

    // act
    MvcResult response =
        mockMvc
            .perform(get("/api/teams").param("id", "1").param("courseId", "1"))
            .andExpect(status().isOk())
            .andReturn();

    // assert
    verify(teamRepository, times(1)).findById(1L);

    String expectedJson = mapper.writeValueAsString(team);
    String responseString = response.getResponse().getContentAsString();
    assertEquals(expectedJson, responseString);
  }

  @WithMockUser(roles = {"ADMIN"})
  @Test
  public void testGetTeamById_teamDoesNotExist() throws Exception {
    // arrange
    when(teamRepository.findById(eq(999L))).thenReturn(Optional.empty());

    // act
    MvcResult response =
        mockMvc
            .perform(get("/api/teams").param("id", "999").param("courseId", "1"))
            .andExpect(status().isNotFound())
            .andReturn();

    // assert
    verify(teamRepository, times(1)).findById(999L);

    String responseString = response.getResponse().getContentAsString();
    Map<String, String> expectedMap =
        Map.of(
            "message", "Team with id 999 not found",
            "type", "EntityNotFoundException");
    String expectedJson = mapper.writeValueAsString(expectedMap);
    assertEquals(expectedJson, responseString);
  }

  // Tests for DELETE /api/teams

  @WithInstructorCoursePermissions
  @Test
  public void testDeleteTeam_success() throws Exception {
    // arrange
    Course course = Course.builder().id(1L).courseName("CS156").build();
    RosterStudent rs1 = RosterStudent.builder().id(1L).course(course).build();
    RosterStudent rs2 = RosterStudent.builder().id(2L).course(course).build();
    Team team = Team.builder().id(1L).name("Team Alpha").course(course).build();

    TeamMember teamMember1 = TeamMember.builder().id(1L).team(team).rosterStudent(rs1).build();
    TeamMember teamMember2 = TeamMember.builder().id(2L).team(team).rosterStudent(rs2).build();

    team.setTeamMembers(new ArrayList<>(List.of(teamMember1, teamMember2)));
    rs1.setTeamMembers(new ArrayList<>(List.of(teamMember1)));
    rs2.setTeamMembers(new ArrayList<>(List.of(teamMember2)));
    course.setTeams(new ArrayList<>(List.of(team)));

    Team teamUpdated = Team.builder().id(1L).name("Team Alpha").course(course).build();
    teamUpdated.setTeamMembers(List.of(teamMember1, teamMember2));

    when(teamRepository.findById(eq(1L))).thenReturn(Optional.of(team));

    // act
    MvcResult response =
        mockMvc
            .perform(delete("/api/teams").param("id", "1").param("courseId", "1").with(csrf()))
            .andExpect(status().isOk())
            .andReturn();

    // assert
    verify(teamRepository, times(1)).findById(1L);
    verify(teamRepository, times(1)).delete(team);
    assertEquals(course.getTeams(), List.of());
    assertEquals(rs1.getTeamMembers(), List.of());
    assertEquals(rs2.getTeamMembers(), List.of());
    assertNull(teamMember1.getRosterStudent());
    assertNull(teamMember2.getRosterStudent());

    String responseString = response.getResponse().getContentAsString();
    Map<String, String> expectedMap = Map.of("message", "Team with id 1 deleted");
    String expectedJson = mapper.writeValueAsString(expectedMap);
    assertEquals(responseString, expectedJson);
  }

  @WithMockUser(roles = {"ADMIN"})
  @Test
  public void testDeleteTeam_teamDoesNotExist() throws Exception {
    // arrange
    when(teamRepository.findById(eq(999L))).thenReturn(Optional.empty());

    // act
    MvcResult response =
        mockMvc
            .perform(delete("/api/teams").param("id", "999").param("courseId", "1").with(csrf()))
            .andExpect(status().isNotFound())
            .andReturn();

    // assert
    verify(teamRepository, times(1)).findById(999L);
    verify(teamRepository, never()).delete(any(Team.class));

    String responseString = response.getResponse().getContentAsString();
    Map<String, String> expectedMap =
        Map.of(
            "message", "Team with id 999 not found",
            "type", "EntityNotFoundException");
    String expectedJson = mapper.writeValueAsString(expectedMap);
    assertEquals(expectedJson, responseString);
  }

  @WithMockUser(roles = {"ADMIN"})
  @Test
  public void testDeleteTeam_withEmptyTeamMembers() throws Exception {
    // arrange
    Course course = Course.builder().id(1L).courseName("CS156").build();
    Team team = Team.builder().id(1L).name("Team Alpha").course(course).build();

    // Initialize with empty lists to match Hibernate behavior
    team.setTeamMembers(List.of());
    course.setTeams(new ArrayList<>(List.of(team)));

    Team teamUpdated = Team.builder().id(1L).name("Team Alpha").course(null).build();
    teamUpdated.setTeamMembers(List.of());

    when(teamRepository.findById(eq(1L))).thenReturn(Optional.of(team));

    // act
    MvcResult response =
        mockMvc
            .perform(delete("/api/teams").param("id", "1").param("courseId", "1").with(csrf()))
            .andExpect(status().isOk())
            .andReturn();

    // assert
    verify(teamRepository, times(1)).findById(1L);
    verify(teamRepository, times(1)).delete(teamUpdated);
    assertEquals(course.getTeams(), List.of());

    String responseString = response.getResponse().getContentAsString();
    Map<String, String> expectedMap = Map.of("message", "Team with id 1 deleted");
    String expectedJson = mapper.writeValueAsString(expectedMap);
    assertEquals(responseString, expectedJson);
  }

  // Tests for POST /api/teams/addMember

  @WithInstructorCoursePermissions
  @Test
  public void testAddTeamMember_success() throws Exception {
    // arrange
    Course course = Course.builder().id(1L).courseName("CS156").build();
    Team team = Team.builder().id(1L).name("Team Alpha").course(course).build();
    RosterStudent rosterStudent =
        RosterStudent.builder().id(1L).email("student@ucsb.edu").course(course).build();
    TeamMember teamMember =
        TeamMember.builder().id(1L).team(team).rosterStudent(rosterStudent).build();

    TeamMember teamMemberToSave =
        TeamMember.builder().team(team).rosterStudent(rosterStudent).build();

    when(teamRepository.findById(eq(1L))).thenReturn(Optional.of(team));
    when(rosterStudentRepository.findById(eq(1L))).thenReturn(Optional.of(rosterStudent));
    when(teamMemberRepository.save(any(TeamMember.class))).thenReturn(teamMember);

    // act
    MvcResult response =
        mockMvc
            .perform(
                post("/api/teams/addMember")
                    .param("teamId", "1")
                    .param("rosterStudentId", "1")
                    .param("courseId", "1")
                    .with(csrf()))
            .andExpect(status().isOk())
            .andReturn();

    // assert
    verify(teamRepository, times(1)).findById(1L);
    verify(rosterStudentRepository, times(1)).findById(1L);
    verify(teamMemberRepository, times(1)).save(teamMemberToSave);

    String expectedJson = mapper.writeValueAsString(teamMember);
    String responseString = response.getResponse().getContentAsString();
    assertEquals(expectedJson, responseString);
  }

  @WithInstructorCoursePermissions
  @Test
  public void testAddTeamMember_differing_course_team() throws Exception {
    // arrange
    Course course = Course.builder().id(1L).courseName("CS156").build();
    Course course2 = Course.builder().id(2L).courseName("CS126").build();
    Team team = Team.builder().id(1L).name("Team Alpha").course(course2).build();
    RosterStudent rosterStudent =
        RosterStudent.builder().id(1L).email("student@ucsb.edu").course(course).build();
    when(teamRepository.findById(eq(1L))).thenReturn(Optional.of(team));
    when(rosterStudentRepository.findById(eq(1L))).thenReturn(Optional.of(rosterStudent));

    MvcResult response =
        mockMvc
            .perform(
                post("/api/teams/addMember")
                    .param("teamId", "1")
                    .param("rosterStudentId", "1")
                    .param("courseId", "1")
                    .with(csrf()))
            .andExpect(status().isBadRequest())
            .andReturn();

    assertEquals("Team is not from course 1", response.getResponse().getErrorMessage());
    verifyNoInteractions(teamMemberRepository);
  }

  @WithInstructorCoursePermissions
  @Test
  public void testAddTeamMember_differing_course_roster_student() throws Exception {
    // arrange
    Course course = Course.builder().id(1L).courseName("CS156").build();
    Course course2 = Course.builder().id(2L).courseName("CS126").build();
    Team team = Team.builder().id(1L).name("Team Alpha").course(course).build();
    RosterStudent rosterStudent =
        RosterStudent.builder().id(1L).email("student@ucsb.edu").course(course2).build();
    when(teamRepository.findById(eq(1L))).thenReturn(Optional.of(team));
    when(rosterStudentRepository.findById(eq(1L))).thenReturn(Optional.of(rosterStudent));

    MvcResult response =
        mockMvc
            .perform(
                post("/api/teams/addMember")
                    .param("teamId", "1")
                    .param("rosterStudentId", "1")
                    .param("courseId", "1")
                    .with(csrf()))
            .andExpect(status().isBadRequest())
            .andReturn();

    assertEquals("Roster student is not from course 1", response.getResponse().getErrorMessage());
    verifyNoInteractions(teamMemberRepository);
  }

  @WithMockUser(roles = {"ADMIN"})
  @Test
  public void testAddTeamMember_teamNotFound() throws Exception {
    // arrange
    when(teamRepository.findById(eq(999L))).thenReturn(Optional.empty());

    // act
    MvcResult response =
        mockMvc
            .perform(
                post("/api/teams/addMember")
                    .param("teamId", "999")
                    .param("rosterStudentId", "1")
                    .param("courseId", "1")
                    .with(csrf()))
            .andExpect(status().isNotFound())
            .andReturn();

    // assert
    verify(teamRepository, times(1)).findById(999L);
    verify(rosterStudentRepository, never()).findById(any());
    verify(teamMemberRepository, never()).save(any(TeamMember.class));

    String responseString = response.getResponse().getContentAsString();
    Map<String, String> expectedMap =
        Map.of(
            "message", "Team with id 999 not found",
            "type", "EntityNotFoundException");
    String expectedJson = mapper.writeValueAsString(expectedMap);
    assertEquals(expectedJson, responseString);
  }

  @WithInstructorCoursePermissions
  @Test
  public void testAddTeamMember_alreadyexists() throws Exception {
    // arrange
    Course course = Course.builder().id(1L).courseName("CS156").build();
    Team team = Team.builder().id(1L).name("Team Alpha").course(course).build();
    RosterStudent rosterStudent =
        RosterStudent.builder().id(1L).email("student@ucsb.edu").course(course).build();
    TeamMember teamMember =
        TeamMember.builder().id(1L).team(team).rosterStudent(rosterStudent).build();

    when(teamRepository.findById(eq(1L))).thenReturn(Optional.of(team));
    when(rosterStudentRepository.findById(eq(1L))).thenReturn(Optional.of(rosterStudent));
    when(teamMemberRepository.findByTeamAndRosterStudent(eq(team), eq(rosterStudent)))
        .thenReturn(Optional.of(teamMember));

    // act
    MvcResult response =
        mockMvc
            .perform(
                post("/api/teams/addMember")
                    .param("teamId", "1")
                    .param("rosterStudentId", "1")
                    .param("courseId", "1")
                    .with(csrf()))
            .andExpect(status().isConflict())
            .andReturn();

    // assert
    verify(teamMemberRepository, never()).save(any(TeamMember.class));
    assertEquals(
        "Team member already exists for team Team Alpha and roster student student@ucsb.edu",
        response.getResponse().getErrorMessage());
  }

  @WithMockUser(roles = {"ADMIN"})
  @Test
  public void testAddTeamMember_rosterStudentNotFound() throws Exception {
    // arrange
    Course course = Course.builder().id(1L).courseName("CS156").build();
    Team team = Team.builder().id(1L).name("Team Alpha").course(course).build();

    when(teamRepository.findById(eq(1L))).thenReturn(Optional.of(team));
    when(rosterStudentRepository.findById(eq(999L))).thenReturn(Optional.empty());

    // act
    MvcResult response =
        mockMvc
            .perform(
                post("/api/teams/addMember")
                    .param("teamId", "1")
                    .param("rosterStudentId", "999")
                    .param("courseId", "1")
                    .with(csrf()))
            .andExpect(status().isNotFound())
            .andReturn();

    // assert
    verify(teamRepository, times(1)).findById(1L);
    verify(rosterStudentRepository, times(1)).findById(999L);
    verify(teamMemberRepository, never()).save(any(TeamMember.class));

    String responseString = response.getResponse().getContentAsString();
    Map<String, String> expectedMap =
        Map.of(
            "message", "RosterStudent with id 999 not found",
            "type", "EntityNotFoundException");
    String expectedJson = mapper.writeValueAsString(expectedMap);
    assertEquals(expectedJson, responseString);
  }

  // Tests for DELETE /api/teams/removeMember

  @WithInstructorCoursePermissions
  @Test
  public void testRemoveTeamMember_success() throws Exception {
    // arrange
    Course course = Course.builder().id(1L).courseName("CS156").build();
    RosterStudent rs = RosterStudent.builder().id(2L).course(course).build();
    Team team = Team.builder().id(1L).name("Team Alpha").course(course).build();
    TeamMember teamMember = TeamMember.builder().id(1L).team(team).rosterStudent(rs).build();
    team.setTeamMembers(new ArrayList<>(List.of(teamMember)));
    rs.setTeamMembers(new ArrayList<>(List.of(teamMember)));

    Team updatedTeam = Team.builder().id(1L).name("Team Alpha").course(course).build();
    updatedTeam.setTeamMembers(List.of());
    RosterStudent rsUpdated = RosterStudent.builder().id(2L).course(course).build();
    rsUpdated.setTeamMembers(List.of());

    TeamMember teamMemberUpdated = teamMember;

    when(teamMemberRepository.findById(eq(1L))).thenReturn(Optional.of(teamMember));
    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(course));

    // act
    MvcResult response =
        mockMvc
            .perform(
                delete("/api/teams/removeMember")
                    .param("teamMemberId", "1")
                    .param("courseId", "1")
                    .with(csrf()))
            .andExpect(status().isOk())
            .andReturn();

    // assert
    verify(teamMemberRepository, times(1)).findById(1L);
    verify(teamMemberRepository, times(1)).delete(teamMemberUpdated);
    verify(teamRepository, times(1)).save(team);
    verify(rosterStudentRepository, times(1)).save(rs);
    assertEquals(team.getTeamMembers(), List.of());
    assertEquals(rs.getTeamMembers(), List.of());

    String responseString = response.getResponse().getContentAsString();
    Map<String, String> expectedMap = Map.of("message", "Team member with id 1 deleted");
    String expectedJson = mapper.writeValueAsString(expectedMap);
    assertEquals(responseString, expectedJson);
  }

  @WithMockUser(roles = {"ADMIN"})
  @Test
  public void testRemoveTeamMember_teamMemberDoesNotExist() throws Exception {
    // arrange
    when(teamMemberRepository.findById(eq(999L))).thenReturn(Optional.empty());

    // act
    MvcResult response =
        mockMvc
            .perform(
                delete("/api/teams/removeMember")
                    .param("teamMemberId", "999")
                    .param("courseId", "1")
                    .with(csrf()))
            .andExpect(status().isNotFound())
            .andReturn();

    // assert
    verify(teamMemberRepository, times(1)).findById(999L);
    verify(teamMemberRepository, never()).delete(any(TeamMember.class));

    String responseString = response.getResponse().getContentAsString();
    Map<String, String> expectedMap =
        Map.of(
            "message", "TeamMember with id 999 not found",
            "type", "EntityNotFoundException");
    String expectedJson = mapper.writeValueAsString(expectedMap);
    assertEquals(expectedJson, responseString);
  }

  // Tests for GET /api/teams/mapping

  @Test
  @WithInstructorCoursePermissions
  public void testTeamMemberMapping_success() throws Exception {
    // arrange
    Course course = Course.builder().id(1L).courseName("CS156").build();
    Team team = Team.builder().id(1L).name("s25-01").course(course).build();
    RosterStudent student =
        RosterStudent.builder()
            .id(1L)
            .email("cgaucho@ucsb.edu")
            .firstName("Chris")
            .lastName("Gaucho")
            .githubLogin("cgaucho")
            .build();
    TeamMember teamMember = TeamMember.builder().id(1L).team(team).rosterStudent(student).build();
    RosterStudent student2 =
        RosterStudent.builder()
            .id(2L)
            .email("ldelplaya@ucsb.edu")
            .firstName("Lauren")
            .lastName("DelPlaya")
            .githubLogin(null)
            .build();
    TeamMember teamMember2 = TeamMember.builder().id(2L).team(team).rosterStudent(student2).build();
    team.setTeamMembers(List.of(teamMember, teamMember2));
    course.setTeams(List.of(team));

    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(course));

    // act
    MvcResult response =
        mockMvc
            .perform(get("/api/teams/mapping").param("courseId", "1"))
            .andExpect(status().isOk())
            .andReturn();

    // assert
    List<TeamsController.TeamMemberMapping> expectedMappings =
        List.of(
            new TeamsController.TeamMemberMapping(
                1L, "s25-01", 1L, "cgaucho@ucsb.edu", "Chris", "Gaucho", "cgaucho"),
            new TeamsController.TeamMemberMapping(
                1L, "s25-01", 2L, "ldelplaya@ucsb.edu", "Lauren", "DelPlaya", null));

    String expectedJson = mapper.writeValueAsString(expectedMappings);
    String responseString = response.getResponse().getContentAsString();
    assertEquals(expectedJson, responseString);
  }

  @Test
  @WithInstructorCoursePermissions
  public void testTeamMemberMapping_noTeams() throws Exception {
    // arrange
    Course course = Course.builder().id(1L).courseName("CS156").teams(List.of()).build();
    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(course));

    // act
    MvcResult response =
        mockMvc
            .perform(get("/api/teams/mapping").param("courseId", "1"))
            .andExpect(status().isOk())
            .andReturn();

    // assert
    String expectedJson = mapper.writeValueAsString(List.of());
    String responseString = response.getResponse().getContentAsString();
    assertEquals(expectedJson, responseString);
  }

  @Test
  @WithInstructorCoursePermissions
  public void testTeamMemberMapping_courseNotFound() throws Exception {
    // arrange
    when(courseRepository.findById(eq(999L))).thenReturn(Optional.empty());

    // act
    MvcResult response =
        mockMvc
            .perform(get("/api/teams/mapping").param("courseId", "999"))
            .andExpect(status().isNotFound())
            .andReturn();

    // assert
    String responseString = response.getResponse().getContentAsString();
    Map<String, String> expectedMap =
        Map.of(
            "message", "Course with id 999 not found",
            "type", "EntityNotFoundException");
    String expectedJson = mapper.writeValueAsString(expectedMap);
    assertEquals(expectedJson, responseString);
  }

  public static String sampleCSVContents =
      """
          team,email
          team1,fakestudent1@ucsb.edu
          team2,fakestudent2@ucsb.edu
          team1,existingstudent@ucsb.edu
          team1,nors@ucsb.edu
          """;

  @WithMockUser(roles = {"ADMIN"})
  @Test
  public void csv_rejected_returns_conflict() throws Exception {
    // arrange
    Course course = Course.builder().id(1L).courseName("CS156").build();
    RosterStudent student1 =
        RosterStudent.builder().email("fakestudent1@ucsb.edu").course(course).build();
    RosterStudent student2 =
        RosterStudent.builder().email("fakestudent2@ucsb.edu").course(course).build();
    RosterStudent student3 =
        RosterStudent.builder().email("existingstudent@ucsb.edu").course(course).build();
    Team team1 = Team.builder().id(1L).name("team1").course(course).build();
    TeamMember teamMemberFor3 = TeamMember.builder().team(team1).rosterStudent(student3).build();

    MockMultipartFile file =
        new MockMultipartFile(
            "file", "egrades.csv", MediaType.TEXT_PLAIN_VALUE, sampleCSVContents.getBytes());

    TeamMember teamMemberCreated1 =
        TeamMember.builder().team(team1).rosterStudent(student1).build();
    TeamMember teamMemberCreated2 = TeamMember.builder().rosterStudent(student2).build();
    Team team2Created = Team.builder().name("team2").course(course).build();
    teamMemberCreated2.setTeam(team2Created);

    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(course));
    when(rosterStudentRepository.findByCourseIdAndEmail(eq(1L), eq("fakestudent1@ucsb.edu")))
        .thenReturn(Optional.of(student1));
    when(rosterStudentRepository.findByCourseIdAndEmail(eq(1L), eq("fakestudent2@ucsb.edu")))
        .thenReturn(Optional.of(student2));
    when(rosterStudentRepository.findByCourseIdAndEmail(eq(1L), eq("existingstudent@ucsb.edu")))
        .thenReturn(Optional.of(student3));
    when(teamRepository.findByCourseIdAndName(eq(1L), eq("team1"))).thenReturn(Optional.of(team1));
    when(teamMemberRepository.findByTeamAndRosterStudent(eq(team1), eq(student3)))
        .thenReturn(Optional.of(teamMemberFor3));

    // act
    MvcResult response =
        mockMvc
            .perform(
                multipart("/api/teams/upload/csv")
                    .file("file", file.getBytes())
                    .param("courseId", "1")
                    .with(csrf()))
            .andExpect(status().isConflict())
            .andReturn();

    // assert
    verify(teamMemberRepository, atLeastOnce()).save(eq(teamMemberCreated1));
    verify(teamRepository).save(eq(team2Created));
    verify(teamMemberRepository, atLeastOnce()).save(eq(teamMemberCreated2));
    verify(teamMemberRepository, never()).save(eq(teamMemberFor3));

    TeamsController.TeamCreationResponse expectedResponse =
        new TeamsController.TeamCreationResponse(
            TeamsController.TeamSourceType.SIMPLE, 2, 1, List.of("nors@ucsb.edu"));
    String expectedJson = mapper.writeValueAsString(expectedResponse);
    String responseString = response.getResponse().getContentAsString();
    assertEquals(expectedJson, responseString);
  }

  public static String successfulCSVContents =
      """
          team,email
          team1,fakestudent1@ucsb.edu
          team2,fakestudent2@ucsb.edu
          team1,existingstudent@ucsb.edu
          """;

  @WithMockUser(roles = {"ADMIN"})
  @Test
  public void testUploadTeamsCsv_success_noRejectedStudents() throws Exception {
    // arrange
    Course course = Course.builder().id(1L).courseName("CS156").build();
    RosterStudent student1 =
        RosterStudent.builder().email("fakestudent1@ucsb.edu").course(course).build();
    RosterStudent student2 =
        RosterStudent.builder().email("fakestudent2@ucsb.edu").course(course).build();
    RosterStudent student3 =
        RosterStudent.builder().email("existingstudent@ucsb.edu").course(course).build();
    Team team1 = Team.builder().id(1L).name("team1").course(course).build();
    TeamMember teamMemberFor3 = TeamMember.builder().team(team1).rosterStudent(student3).build();

    MockMultipartFile file =
        new MockMultipartFile(
            "file", "egrades.csv", MediaType.TEXT_PLAIN_VALUE, successfulCSVContents.getBytes());

    TeamMember teamMemberCreated1 =
        TeamMember.builder().team(team1).rosterStudent(student1).build();
    TeamMember teamMemberCreated2 = TeamMember.builder().rosterStudent(student2).build();
    Team team2Created = Team.builder().name("team2").course(course).build();
    teamMemberCreated2.setTeam(team2Created);

    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(course));
    when(rosterStudentRepository.findByCourseIdAndEmail(eq(1L), eq("fakestudent1@ucsb.edu")))
        .thenReturn(Optional.of(student1));
    when(rosterStudentRepository.findByCourseIdAndEmail(eq(1L), eq("fakestudent2@ucsb.edu")))
        .thenReturn(Optional.of(student2));
    when(rosterStudentRepository.findByCourseIdAndEmail(eq(1L), eq("existingstudent@ucsb.edu")))
        .thenReturn(Optional.of(student3));
    when(teamRepository.findByCourseIdAndName(eq(1L), eq("team1"))).thenReturn(Optional.of(team1));
    when(teamMemberRepository.findByTeamAndRosterStudent(eq(team1), eq(student3)))
        .thenReturn(Optional.of(teamMemberFor3));

    // act
    MvcResult response =
        mockMvc
            .perform(
                multipart("/api/teams/upload/csv")
                    .file("file", file.getBytes())
                    .param("courseId", "1")
                    .with(csrf()))
            .andExpect(status().isOk())
            .andReturn();

    // assert
    verify(teamMemberRepository, atLeastOnce()).save(eq(teamMemberCreated1));
    verify(teamRepository).save(eq(team2Created));
    verify(teamMemberRepository, atLeastOnce()).save(eq(teamMemberCreated2));
    verify(teamMemberRepository, never()).save(eq(teamMemberFor3));

    TeamsController.TeamCreationResponse expectedResponse =
        new TeamsController.TeamCreationResponse(
            TeamsController.TeamSourceType.SIMPLE, 2, 1, List.of());
    String expectedJson = mapper.writeValueAsString(expectedResponse);
    String responseString = response.getResponse().getContentAsString();
    assertEquals(expectedJson, responseString);
  }

  @Test
  @WithInstructorCoursePermissions
  public void testUploadTeamsCsv_courseNotFound() throws Exception {
    // arrange
    when(courseRepository.findById(eq(999L))).thenReturn(Optional.empty());

    MockMultipartFile file =
        new MockMultipartFile(
            "file", "egrades.csv", MediaType.TEXT_PLAIN_VALUE, sampleCSVContents.getBytes());

    // act
    MvcResult response =
        mockMvc
            .perform(
                multipart("/api/teams/upload/csv")
                    .file("file", file.getBytes())
                    .param("courseId", "999")
                    .with(csrf()))
            .andExpect(status().isNotFound())
            .andReturn();

    // assert
    verify(courseRepository, times(1)).findById(999L);

    String responseString = response.getResponse().getContentAsString();
    Map<String, String> expectedMap =
        Map.of(
            "type", "EntityNotFoundException",
            "message", "Course with id 999 not found");
    String expectedJson = mapper.writeValueAsString(expectedMap);
    assertEquals(expectedJson, responseString);
  }

  @WithMockUser(roles = {"ADMIN"})
  @Test
  public void testUploadTeamsCsv_unknownSourceType() throws Exception {
    // arrange
    Course course = Course.builder().id(1L).courseName("CS156").build();
    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(course));

    String badCsvContents = "team\nunknown_data";

    MockMultipartFile file =
        new MockMultipartFile(
            "file", "egrades.csv", MediaType.TEXT_PLAIN_VALUE, badCsvContents.getBytes());

    // act
    MvcResult response =
        mockMvc
            .perform(
                multipart("/api/teams/upload/csv")
                    .file("file", file.getBytes())
                    .param("courseId", "1")
                    .with(csrf()))
            .andExpect(status().isBadRequest())
            .andReturn();

    // assert
    verify(courseRepository, times(1)).findById(1L);
    assertEquals("Unknown Roster Source Type", response.getResponse().getErrorMessage());
  }

  @WithMockUser(roles = {"ADMIN"})
  @Test
  public void testUploadTeamsCsv_unknownSourceType_loopfill() throws Exception {
    // arrange
    Course course = Course.builder().id(1L).courseName("CS156").build();
    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(course));

    String badCsvContents = "team,unknown_row\nunknown_data";

    MockMultipartFile file =
        new MockMultipartFile(
            "file", "egrades.csv", MediaType.TEXT_PLAIN_VALUE, badCsvContents.getBytes());

    // act
    MvcResult response =
        mockMvc
            .perform(
                multipart("/api/teams/upload/csv")
                    .file("file", file.getBytes())
                    .param("courseId", "1")
                    .with(csrf()))
            .andExpect(status().isBadRequest())
            .andReturn();

    // assert
    verify(courseRepository, times(1)).findById(1L);
    assertEquals("Unknown Roster Source Type", response.getResponse().getErrorMessage());
  }
}
