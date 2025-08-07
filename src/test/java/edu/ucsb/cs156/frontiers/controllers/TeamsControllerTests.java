package edu.ucsb.cs156.frontiers.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import edu.ucsb.cs156.frontiers.annotations.WithInstructorCoursePermissions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;

import edu.ucsb.cs156.frontiers.ControllerTestCase;
import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.entities.RosterStudent;
import edu.ucsb.cs156.frontiers.entities.Team;
import edu.ucsb.cs156.frontiers.entities.TeamMember;
import edu.ucsb.cs156.frontiers.repositories.CourseRepository;
import edu.ucsb.cs156.frontiers.repositories.RosterStudentRepository;
import edu.ucsb.cs156.frontiers.repositories.TeamRepository;
import edu.ucsb.cs156.frontiers.repositories.TeamMemberRepository;

@WebMvcTest(controllers = TeamsController.class)
public class TeamsControllerTests extends ControllerTestCase {

    @MockitoBean
    TeamRepository teamRepository;

    @MockitoBean
    TeamMemberRepository teamMemberRepository;

    @MockitoBean
    CourseRepository courseRepository;

    @MockitoBean
    RosterStudentRepository rosterStudentRepository;


    @WithMockUser(roles = {"ADMIN"})
    @Test
    public void testPostTeam_byAdmin() throws Exception {
        // arrange
        Course course = Course.builder()
                .id(1L)
                .courseName("CS156")
                .term("F23")
                .school("UCSB")
                .build();

        Team team = Team.builder()
                .id(1L)
                .name("Team Alpha")
                .course(course)
                .build();

        Team teamToSave = Team.builder()
                .name("Team Alpha")
                .course(course)
                .build();

        when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(course));
        when(teamRepository.save(any(Team.class))).thenReturn(team);

        // act
        MvcResult response = mockMvc.perform(
                        post("/api/teams/post")
                                .param("name", "Team Alpha")
                                .param("courseId", "1")
                                .with(csrf()))
                .andExpect(status().isOk()).andReturn();

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
        Course course = Course.builder()
                .id(1L)
                .courseName("CS156")
                .term("F23")
                .school("UCSB")
                .build();

        Team team = Team.builder()
                .id(1L)
                .name("Team Beta")
                .course(course)
                .build();

        Team teamToSave = Team.builder()
                .name("Team Beta")
                .course(course)
                .build();

        when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(course));
        when(teamRepository.save(any(Team.class))).thenReturn(team);

        // act
        MvcResult response = mockMvc.perform(
                        post("/api/teams/post")
                                .param("name", "Team Beta")
                                .param("courseId", "1")
                                .with(csrf()))
                .andExpect(status().isOk()).andReturn();

        // assert
        verify(courseRepository, times(1)).findById(1L);
        verify(teamRepository, times(1)).save(teamToSave);

        String expectedJson = mapper.writeValueAsString(team);
        String responseString = response.getResponse().getContentAsString();
        assertEquals(expectedJson, responseString);
    }

    @WithMockUser(roles = {"ADMIN"})
    @Test
    public void testPostTeam_courseNotFound() throws Exception {
        // arrange
        when(courseRepository.findById(eq(999L))).thenReturn(Optional.empty());

        // act
        MvcResult response = mockMvc.perform(
                        post("/api/teams/post")
                                .param("name", "Team Gamma")
                                .param("courseId", "999")
                                .with(csrf()))
                .andExpect(status().isNotFound()).andReturn();

        // assert
        verify(courseRepository, times(1)).findById(999L);
        verify(teamRepository, never()).save(any(Team.class));

        String responseString = response.getResponse().getContentAsString();
        Map<String, String> expectedMap = Map.of(
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

        when(teamRepository.findByCourseId(eq(1L))).thenReturn(expectedTeams);

        // act
        MvcResult response = mockMvc.perform(get("/api/teams/all")
                        .param("courseId", "1"))
                .andExpect(status().isOk()).andReturn();

        // assert
        verify(teamRepository, times(1)).findByCourseId(eq(1L));

        String expectedJson = mapper.writeValueAsString(
                expectedTeams);
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

        when(teamRepository.findByCourseId(eq(1L))).thenReturn(expectedTeams);

        // act
        MvcResult response = mockMvc.perform(get("/api/teams/all")
                        .param("courseId", "1"))
                .andExpect(status().isOk()).andReturn();

        // assert
        verify(teamRepository, times(1)).findByCourseId(eq(1L));

        String expectedJson = mapper.writeValueAsString(expectedTeams);
        String responseString = response.getResponse().getContentAsString();
        assertEquals(expectedJson, responseString);
    }

    // Tests for GET /api/teams

    @WithMockUser(roles = {"ADMIN"})
    @Test
    public void testGetTeamById() throws Exception {
        // arrange
        Course course = Course.builder().id(1L).courseName("CS156").build();
        Team team = Team.builder().id(1L).name("Team Alpha").course(course).build();

        when(teamRepository.findById(eq(1L))).thenReturn(Optional.of(team));

        // act
        MvcResult response = mockMvc.perform(get("/api/teams").param("id", "1"))
                .andExpect(status().isOk()).andReturn();

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
        MvcResult response = mockMvc.perform(get("/api/teams").param("id", "999"))
                .andExpect(status().isNotFound()).andReturn();

        // assert
        verify(teamRepository, times(1)).findById(999L);

        String responseString = response.getResponse().getContentAsString();
        Map<String, String> expectedMap = Map.of(
                "message", "Team with id 999 not found",
                "type", "EntityNotFoundException");
        String expectedJson = mapper.writeValueAsString(expectedMap);
        assertEquals(expectedJson, responseString);
    }

    // Tests for DELETE /api/teams

    @WithMockUser(roles = {"ADMIN"})
    @Test
    public void testDeleteTeam_success() throws Exception {
        // arrange
        Course course = Course.builder().id(1L).courseName("CS156").build();
        Team team = Team.builder().id(1L).name("Team Alpha").course(course).build();

        when(teamRepository.findById(eq(1L))).thenReturn(Optional.of(team));

        // act
        MvcResult response = mockMvc.perform(
                        delete("/api/teams").param("id", "1").with(csrf()))
                .andExpect(status().isOk()).andReturn();

        // assert
        verify(teamRepository, times(1)).findById(1L);
        verify(teamRepository, times(1)).delete(team);

        String responseString = response.getResponse().getContentAsString();
        Map<String, String> expectedMap = Map.of(
                "message", "Team with id 1 deleted");
        String expectedJson = mapper.writeValueAsString(expectedMap);
        assertEquals(responseString, expectedJson);
    }

    @WithMockUser(roles = {"ADMIN"})
    @Test
    public void testDeleteTeam_teamDoesNotExist() throws Exception {
        // arrange
        when(teamRepository.findById(eq(999L))).thenReturn(Optional.empty());

        // act
        MvcResult response = mockMvc.perform(
                        delete("/api/teams").param("id", "999").with(csrf()))
                .andExpect(status().isNotFound()).andReturn();

        // assert
        verify(teamRepository, times(1)).findById(999L);
        verify(teamRepository, never()).delete(any(Team.class));

        String responseString = response.getResponse().getContentAsString();
        Map<String, String> expectedMap = Map.of(
                "message", "Team with id 999 not found",
                "type", "EntityNotFoundException");
        String expectedJson = mapper.writeValueAsString(expectedMap);
        assertEquals(expectedJson, responseString);
    }

    // Tests for POST /api/teams/addMember

    @WithMockUser(roles = {"ADMIN"})
    @Test
    public void testAddTeamMember_success() throws Exception {
        // arrange
        Course course = Course.builder().id(1L).courseName("CS156").build();
        Team team = Team.builder().id(1L).name("Team Alpha").course(course).build();
        RosterStudent rosterStudent = RosterStudent.builder().id(1L).email("student@ucsb.edu").build();
        TeamMember teamMember = TeamMember.builder().id(1L).team(team).rosterStudent(rosterStudent).build();

        TeamMember teamMemberToSave = TeamMember.builder()
                .team(team)
                .rosterStudent(rosterStudent)
                .build();

        when(teamRepository.findById(eq(1L))).thenReturn(Optional.of(team));
        when(rosterStudentRepository.findById(eq(1L))).thenReturn(Optional.of(rosterStudent));
        when(teamMemberRepository.save(any(TeamMember.class))).thenReturn(teamMember);

        // act
        MvcResult response = mockMvc.perform(
                        post("/api/teams/addMember")
                                .param("teamId", "1")
                                .param("rosterStudentId", "1")
                                .with(csrf()))
                .andExpect(status().isOk()).andReturn();

        // assert
        verify(teamRepository, times(1)).findById(1L);
        verify(rosterStudentRepository, times(1)).findById(1L);
        verify(teamMemberRepository, times(1)).save(teamMemberToSave);

        String expectedJson = mapper.writeValueAsString(teamMember);
        String responseString = response.getResponse().getContentAsString();
        assertEquals(expectedJson, responseString);
    }

    @WithMockUser(roles = {"ADMIN"})
    @Test
    public void testAddTeamMember_teamNotFound() throws Exception {
        // arrange
        when(teamRepository.findById(eq(999L))).thenReturn(Optional.empty());

        // act
        MvcResult response = mockMvc.perform(
                        post("/api/teams/addMember")
                                .param("teamId", "999")
                                .param("rosterStudentId", "1")
                                .with(csrf()))
                .andExpect(status().isNotFound()).andReturn();

        // assert
        verify(teamRepository, times(1)).findById(999L);
        verify(rosterStudentRepository, never()).findById(any());
        verify(teamMemberRepository, never()).save(any(TeamMember.class));

        String responseString = response.getResponse().getContentAsString();
        Map<String, String> expectedMap = Map.of(
                "message", "Team with id 999 not found",
                "type", "EntityNotFoundException");
        String expectedJson = mapper.writeValueAsString(expectedMap);
        assertEquals(expectedJson, responseString);
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
        MvcResult response = mockMvc.perform(
                        post("/api/teams/addMember")
                                .param("teamId", "1")
                                .param("rosterStudentId", "999")
                                .with(csrf()))
                .andExpect(status().isNotFound()).andReturn();

        // assert
        verify(teamRepository, times(1)).findById(1L);
        verify(rosterStudentRepository, times(1)).findById(999L);
        verify(teamMemberRepository, never()).save(any(TeamMember.class));

        String responseString = response.getResponse().getContentAsString();
        Map<String, String> expectedMap = Map.of(
                "message", "RosterStudent with id 999 not found",
                "type", "EntityNotFoundException");
        String expectedJson = mapper.writeValueAsString(expectedMap);
        assertEquals(expectedJson, responseString);
    }

    // Tests for DELETE /api/teams/removeMember

    @WithMockUser(roles = {"ADMIN"})
    @Test
    public void testRemoveTeamMember_success() throws Exception {
        // arrange
        Course course = Course.builder().id(1L).courseName("CS156").build();
        Team team = Team.builder().id(1L).name("Team Alpha").course(course).build();
        TeamMember teamMember = TeamMember.builder().id(1L).team(team).build();

        when(teamMemberRepository.findById(eq(1L))).thenReturn(Optional.of(teamMember));

        // act
        MvcResult response = mockMvc.perform(
                        delete("/api/teams/removeMember").param("teamMemberId", "1").with(csrf()))
                .andExpect(status().isOk()).andReturn();

        // assert
        verify(teamMemberRepository, times(1)).findById(1L);
        verify(teamMemberRepository, times(1)).delete(teamMember);

        String responseString = response.getResponse().getContentAsString();
        Map<String, String> expectedMap = Map.of(
                "message", "Team member with id 1 deleted");
        String expectedJson = mapper.writeValueAsString(expectedMap);
        assertEquals(responseString, expectedJson);
    }

    @WithMockUser(roles = {"ADMIN"})
    @Test
    public void testRemoveTeamMember_teamMemberDoesNotExist() throws Exception {
        // arrange
        when(teamMemberRepository.findById(eq(999L))).thenReturn(Optional.empty());

        // act
        MvcResult response = mockMvc.perform(
                        delete("/api/teams/removeMember").param("teamMemberId", "999").with(csrf()))
                .andExpect(status().isNotFound()).andReturn();

        // assert
        verify(teamMemberRepository, times(1)).findById(999L);
        verify(teamMemberRepository, never()).delete(any(TeamMember.class));

        String responseString = response.getResponse().getContentAsString();
        Map<String, String> expectedMap = Map.of(
                "message", "TeamMember with id 999 not found",
                "type", "EntityNotFoundException");
        String expectedJson = mapper.writeValueAsString(expectedMap);
        assertEquals(expectedJson, responseString);
    }
}