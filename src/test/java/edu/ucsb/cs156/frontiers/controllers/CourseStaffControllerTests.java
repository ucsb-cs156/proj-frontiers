package edu.ucsb.cs156.frontiers.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.ucsb.cs156.frontiers.ControllerTestCase;
import edu.ucsb.cs156.frontiers.annotations.WithInstructorCoursePermissions;
import edu.ucsb.cs156.frontiers.entities.*;
import edu.ucsb.cs156.frontiers.enums.OrgStatus;
import edu.ucsb.cs156.frontiers.repositories.CourseRepository;
import edu.ucsb.cs156.frontiers.repositories.CourseStaffRepository;
import edu.ucsb.cs156.frontiers.services.CurrentUserService;
import edu.ucsb.cs156.frontiers.services.OrganizationMemberService;
import edu.ucsb.cs156.frontiers.services.UpdateUserService;
import edu.ucsb.cs156.frontiers.services.jobs.JobService;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;

@Slf4j
@WebMvcTest(controllers = CourseStaffController.class)
public class CourseStaffControllerTests extends ControllerTestCase {

  @MockitoBean private CourseRepository courseRepository;

  @MockitoBean private CourseStaffRepository courseStaffRepository;

  @Autowired private CurrentUserService currentUserService;

  @MockitoBean private UpdateUserService updateUserService;

  @MockitoBean private OrganizationMemberService organizationMemberService;

  @MockitoBean private JobService service;

  @Autowired private ObjectMapper objectMapper;

  Course course1 =
      Course.builder()
          .id(1L)
          .courseName("CS156")
          .orgName("ucsb-cs156-s25")
          .term("S25")
          .school("UCSB")
          .build();

  CourseStaff cs1 =
      CourseStaff.builder()
          .firstName("Chris")
          .lastName("Gaucho")
          .email("cgaucho@example.org")
          .course(course1)
          .build();

  CourseStaff cs2 =
      CourseStaff.builder()
          .id(2L)
          .firstName("Lauren")
          .lastName("Del Playa")
          .email("ldelplaya@ucsb.edu")
          .course(course1)
          .build();

  /** Test the POST endpoint */
  @Test
  @WithMockUser(roles = {"ADMIN"})
  public void testPostCourseStaff() throws Exception {

    Course course2 =
        Course.builder()
            .id(1L)
            .courseName("CS156")
            .orgName("ucsb-cs156-s25")
            .term("S25")
            .school("UCSB")
            .build();

    CourseStaff cs2 =
        CourseStaff.builder()
            .firstName("Chris")
            .lastName("Gaucho")
            .email("cgaucho@example.org")
            .course(course2)
            .orgStatus(OrgStatus.PENDING)
            .build();

    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(course2));
    when(courseStaffRepository.save(any(CourseStaff.class))).thenReturn(cs2);

    // act

    MvcResult response =
        mockMvc
            .perform(
                post("/api/coursestaff/post")
                    .with(csrf())
                    .param("firstName", "Chris")
                    .param("lastName", "Gaucho")
                    .param("email", "cgaucho@example.org")
                    .param("courseId", "1"))
            .andExpect(status().isOk())
            .andReturn();

    // assert

    verify(courseRepository, times(1)).findById(eq(1L));
    verify(courseStaffRepository, times(1)).save(eq(cs2));

    verify(updateUserService).attachUserToCourseStaff(any(CourseStaff.class));

    String responseString = response.getResponse().getContentAsString();
    String expectedJson = mapper.writeValueAsString(cs2);
    assertEquals(expectedJson, responseString);
  }

  /** Test the POST endpoint */
  @Test
  @WithMockUser(roles = {"ADMIN"})
  public void test_post_course_staff_join_course_status() throws Exception {

    Course course2 =
        Course.builder()
            .id(1L)
            .courseName("CS156")
            .orgName("ucsb-cs156-s25")
            .installationId("12345")
            .term("S25")
            .school("UCSB")
            .build();

    CourseStaff cs2 =
        CourseStaff.builder()
            .firstName("Chris")
            .lastName("Gaucho")
            .email("cgaucho@example.org")
            .course(course2)
            .orgStatus(OrgStatus.JOINCOURSE)
            .build();

    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(course2));
    when(courseStaffRepository.save(any(CourseStaff.class))).thenReturn(cs2);

    // act

    MvcResult response =
        mockMvc
            .perform(
                post("/api/coursestaff/post")
                    .with(csrf())
                    .param("firstName", "Chris")
                    .param("lastName", "Gaucho")
                    .param("email", "cgaucho@example.org")
                    .param("courseId", "1"))
            .andExpect(status().isOk())
            .andReturn();

    // assert

    verify(courseRepository, times(1)).findById(eq(1L));
    verify(courseStaffRepository, times(1)).save(eq(cs2));

    String responseString = response.getResponse().getContentAsString();
    String expectedJson = mapper.writeValueAsString(cs2);
    assertEquals(expectedJson, responseString);
  }

  /**
   * Test that you cannot post a single roster student for a course that does not exist
   *
   * @throws Exception
   */
  @Test
  @WithMockUser(roles = {"ADMIN"})
  public void test_AdminCannotPostCourseStaffForCourseThatDoesNotExist() throws Exception {
    when(courseRepository.findById(eq(1L))).thenReturn(Optional.empty());

    // act

    MvcResult response =
        mockMvc
            .perform(
                post("/api/coursestaff/post")
                    .with(csrf())
                    .param("firstName", "Chris")
                    .param("lastName", "Gaucho")
                    .param("email", "cgaucho@example.org")
                    .param("courseId", "1"))
            .andExpect(status().isNotFound())
            .andReturn();

    // assert

    String responseString = response.getResponse().getContentAsString();
    Map<String, String> expectedMap =
        Map.of(
            "type", "EntityNotFoundException",
            "message", "Course with id 1 not found");
    String expectedJson = mapper.writeValueAsString(expectedMap);
    assertEquals(expectedJson, responseString);
  }

  /** Test the GET endpoint */
  @Test
  @WithMockUser(roles = {"ADMIN"})
  public void testCourseStaffByCourse() throws Exception {

    // arrange

    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(course1));
    when(courseStaffRepository.findByCourseId(eq(1L))).thenReturn(java.util.List.of(cs1, cs2));

    // act

    MvcResult response =
        mockMvc
            .perform(get("/api/coursestaff/course").param("courseId", "1"))
            .andExpect(status().isOk())
            .andReturn();

    // assert

    String responseString = response.getResponse().getContentAsString();
    String expectedJson = mapper.writeValueAsString(java.util.List.of(cs1, cs2));
    assertEquals(expectedJson, responseString);
  }

  /** Test whether admin can get course staff for a non existing course */
  @WithMockUser(roles = {"ADMIN"})
  @Test
  public void admin_can_get_course_staff_for_a_non_existing_course() throws Exception {

    // arrange

    when(courseRepository.findById(eq(1L))).thenReturn(Optional.empty());

    // act

    MvcResult response =
        mockMvc
            .perform(get("/api/coursestaff/course").param("courseId", "1"))
            .andExpect(status().isNotFound())
            .andReturn();

    // assert

    verify(courseRepository, atLeastOnce()).findById(eq(1L));
    String responseString = response.getResponse().getContentAsString();
    Map<String, String> expectedMap =
        Map.of(
            "type", "EntityNotFoundException",
            "message", "Course with id 1 not found");
    String expectedJson = mapper.writeValueAsString(expectedMap);
    assertEquals(expectedJson, responseString);
  }

  /** Tests for the joinCourseOnGitHub endpoint */
  @Test
  @WithMockUser(roles = {"USER", "GITHUB"})
  public void testLinkGitHub_notFound() throws Exception {
    // Arrange
    when(courseStaffRepository.findById(eq(99L))).thenReturn(Optional.empty());

    // Act
    MvcResult response =
        mockMvc
            .perform(put("/api/coursestaff/joinCourse").with(csrf()).param("courseStaffId", "99"))
            .andExpect(status().isNotFound())
            .andReturn();

    // Assert
    verify(courseStaffRepository).findById(eq(99L));

    // Verify correct error response
    String responseString = response.getResponse().getContentAsString();
    Map<String, String> expectedMap =
        Map.of(
            "type", "EntityNotFoundException",
            "message", "CourseStaff with id 99 not found");
    String expectedJson = mapper.writeValueAsString(expectedMap);
    assertEquals(expectedJson, responseString);
  }

  @Test
  @WithMockUser(roles = {"USER", "GITHUB"})
  public void testJoinCourseOnGitHub_unauthorized() throws Exception {
    User currentUser = currentUserService.getUser();

    User differentUser = User.builder().id(24L).build();

    CourseStaff courseStaff =
        CourseStaff.builder()
            .id(4L)
            .firstName("Other")
            .lastName("Student")
            .email("otherstudent@ucsb.edu")
            .course(course1)
            .orgStatus(OrgStatus.PENDING)
            .user(differentUser)
            .build();

    when(courseStaffRepository.findById(eq(4L))).thenReturn(Optional.of(courseStaff));

    MvcResult response =
        mockMvc
            .perform(put("/api/coursestaff/joinCourse").with(csrf()).param("courseStaffId", "4"))
            .andExpect(status().isBadRequest())
            .andReturn();

    String responseString = response.getResponse().getContentAsString();
    Map<String, String> expectedMap =
        Map.of(
            "type",
            "IllegalArgumentException",
            "message",
            "This operation is restricted to the user associated with staff member with id 4");
    String expectedJson = mapper.writeValueAsString(expectedMap);
    assertEquals(expectedJson, responseString);
    verify(courseStaffRepository, never()).save(any(CourseStaff.class));
  }

  @Test
  @WithMockUser(roles = {"USER", "GITHUB"})
  public void test_null_user_on_join() throws Exception {

    CourseStaff courseStaff =
        CourseStaff.builder()
            .id(4L)
            .firstName("Other")
            .lastName("Student")
            .email("otherstudent@ucsb.edu")
            .course(course1)
            .orgStatus(OrgStatus.PENDING)
            .user(null)
            .build();

    when(courseStaffRepository.findById(eq(4L))).thenReturn(Optional.of(courseStaff));

    MvcResult response =
        mockMvc
            .perform(put("/api/coursestaff/joinCourse").with(csrf()).param("courseStaffId", "4"))
            .andExpect(status().isBadRequest())
            .andReturn();

    String responseString = response.getResponse().getContentAsString();
    Map<String, String> expectedMap =
        Map.of(
            "type",
            "IllegalArgumentException",
            "message",
            "This operation is restricted to the user associated with staff member with id 4");
    String expectedJson = mapper.writeValueAsString(expectedMap);
    assertEquals(expectedJson, responseString);
    verify(courseStaffRepository, never()).save(any(CourseStaff.class));
  }

  @Test
  @WithMockUser(roles = {"USER", "GITHUB"})
  public void testJoinCourseOnGitHub_alreadyJoined() throws Exception {
    User currentUser = currentUserService.getUser();

    CourseStaff courseStaff =
        CourseStaff.builder()
            .id(5L)
            .firstName("Already")
            .lastName("Linked")
            .email("alreadylinked@ucsb.edu")
            .course(course1)
            .orgStatus(OrgStatus.MEMBER)
            .githubId(98765)
            .githubLogin("existinguser")
            .user(currentUser)
            .build();

    when(courseStaffRepository.findById(eq(5L))).thenReturn(Optional.of(courseStaff));

    // Act
    MvcResult response =
        mockMvc
            .perform(put("/api/coursestaff/joinCourse").with(csrf()).param("courseStaffId", "5"))
            .andExpect(status().isBadRequest())
            .andReturn();

    // Assert
    verify(courseStaffRepository).findById(eq(5L));
    verify(courseStaffRepository, never()).save(any(CourseStaff.class));
    verify(organizationMemberService, times(0)).inviteOrganizationOwner(any(CourseStaff.class));

    assertEquals(
        "You have already linked a Github account to this course.",
        response.getResponse().getContentAsString());
  }

  @Test
  @WithMockUser(roles = {"USER", "GITHUB"})
  public void testJoinCourseOnGitHub_alreadyJoined_Owner() throws Exception {
    User currentUser = currentUserService.getUser();

    CourseStaff courseStaff =
        CourseStaff.builder()
            .id(5L)
            .firstName("Already")
            .lastName("Linked")
            .email("alreadylinked@ucsb.edu")
            .course(course1)
            .orgStatus(OrgStatus.OWNER)
            .githubId(98765)
            .githubLogin("existinguser")
            .user(currentUser)
            .build();

    when(courseStaffRepository.findById(eq(5L))).thenReturn(Optional.of(courseStaff));

    // Act
    MvcResult response =
        mockMvc
            .perform(put("/api/coursestaff/joinCourse").with(csrf()).param("courseStaffId", "5"))
            .andExpect(status().isBadRequest())
            .andReturn();

    // Assert
    verify(courseStaffRepository).findById(eq(5L));
    verify(courseStaffRepository, never()).save(any(CourseStaff.class));
    verify(organizationMemberService, times(0)).inviteOrganizationOwner(any(CourseStaff.class));

    assertEquals(
        "You have already linked a Github account to this course.",
        response.getResponse().getContentAsString());
  }

  @Test
  @WithMockUser(roles = {"USER", "GITHUB"})
  public void no_fire_on_no_org_name() throws Exception {
    User currentUser = currentUserService.getUser();

    Course course2 =
        Course.builder()
            .id(2L)
            .installationId("1234")
            .courseName("course")
            .creator(currentUser)
            .build();

    CourseStaff courseStaff =
        CourseStaff.builder()
            .id(3L)
            .firstName("Test")
            .lastName("User")
            .email("testuser@ucsb.edu")
            .course(course2)
            .orgStatus(OrgStatus.PENDING)
            .githubId(0)
            .githubLogin("login")
            .user(currentUser)
            .build();

    CourseStaff courseStaffUpdated =
        CourseStaff.builder()
            .id(3L)
            .firstName("Test")
            .lastName("User")
            .email("testuser@ucsb.edu")
            .course(course2)
            .orgStatus(OrgStatus.PENDING)
            .githubId(currentUser.getGithubId())
            .githubLogin(currentUser.getGithubLogin())
            .user(currentUser)
            .build();

    when(courseStaffRepository.findById(eq(3L))).thenReturn(Optional.of(courseStaff));

    MvcResult response =
        mockMvc
            .perform(put("/api/coursestaff/joinCourse").with(csrf()).param("courseStaffId", "3"))
            .andExpect(status().isBadRequest())
            .andReturn();

    verify(courseStaffRepository).findById(eq(3L));

    verify(courseStaffRepository, times(0)).save(eq(courseStaffUpdated));
    verify(organizationMemberService, times(0)).inviteOrganizationOwner(any(CourseStaff.class));

    assertEquals(
        "Course has not been set up. Please ask your instructor for help.",
        response.getResponse().getContentAsString());
  }

  @Test
  @WithMockUser(roles = {"USER", "GITHUB"})
  public void no_fire_on_no_installation_id() throws Exception {
    User currentUser = currentUserService.getUser();

    CourseStaff courseStaff =
        CourseStaff.builder()
            .id(3L)
            .firstName("Test")
            .lastName("User")
            .email("testuser@ucsb.edu")
            .course(course1)
            .orgStatus(OrgStatus.PENDING)
            .githubId(123456789)
            .githubLogin(null)
            .user(currentUser)
            .build();

    CourseStaff courseStaffUpdated =
        CourseStaff.builder()
            .id(3L)
            .firstName("Test")
            .lastName("User")
            .email("testuser@ucsb.edu")
            .course(course1)
            .orgStatus(OrgStatus.PENDING)
            .githubId(currentUser.getGithubId())
            .githubLogin(currentUser.getGithubLogin())
            .user(currentUser)
            .build();

    when(courseStaffRepository.findById(eq(3L))).thenReturn(Optional.of(courseStaff));

    // Act
    MvcResult response =
        mockMvc
            .perform(put("/api/coursestaff/joinCourse").with(csrf()).param("courseStaffId", "3"))
            .andExpect(status().isBadRequest())
            .andReturn();

    verify(courseStaffRepository).findById(eq(3L));

    verifyNoMoreInteractions(courseStaffRepository, organizationMemberService);

    assertEquals(
        "Course has not been set up. Please ask your instructor for help.",
        response.getResponse().getContentAsString());
  }

  @Test
  @WithMockUser(roles = {"USER", "GITHUB"})
  public void test_fires_invite() throws Exception {
    User currentUser = currentUserService.getUser();

    Course course2 =
        Course.builder()
            .id(2L)
            .installationId("1234")
            .orgName("ucsb-cs156")
            .courseName("course")
            .creator(currentUser)
            .build();

    CourseStaff courseStaff =
        CourseStaff.builder()
            .id(3L)
            .firstName("Test")
            .lastName("User")
            .email("testuser@ucsb.edu")
            .course(course2)
            .orgStatus(OrgStatus.JOINCOURSE)
            .githubId(null)
            .githubLogin(null)
            .user(currentUser)
            .build();

    CourseStaff courseStaffUpdated =
        CourseStaff.builder()
            .id(3L)
            .firstName("Test")
            .lastName("User")
            .email("testuser@ucsb.edu")
            .course(course2)
            .orgStatus(OrgStatus.INVITED)
            .githubId(currentUser.getGithubId())
            .githubLogin(currentUser.getGithubLogin())
            .user(currentUser)
            .build();

    when(courseStaffRepository.findById(eq(3L))).thenReturn(Optional.of(courseStaff));
    when(courseStaffRepository.save(eq(courseStaffUpdated))).thenReturn(courseStaffUpdated);
    when(organizationMemberService.inviteOrganizationOwner(any(CourseStaff.class)))
        .thenReturn(OrgStatus.INVITED);

    MvcResult response =
        mockMvc
            .perform(put("/api/coursestaff/joinCourse").with(csrf()).param("courseStaffId", "3"))
            .andExpect(status().isAccepted())
            .andReturn();

    verify(courseStaffRepository).findById(eq(3L));

    verify(courseStaffRepository, times(1)).save(eq(courseStaffUpdated));
    assertEquals(
        "Successfully invited staff member to Organization",
        response.getResponse().getContentAsString());
  }

  @Test
  @WithMockUser(roles = {"USER", "GITHUB"})
  public void test_already_part_is_member() throws Exception {
    User currentUser = currentUserService.getUser();

    Course course2 =
        Course.builder()
            .id(2L)
            .installationId("1234")
            .orgName("ucsb-cs156")
            .courseName("course")
            .creator(currentUser)
            .build();

    CourseStaff courseStaff =
        CourseStaff.builder()
            .id(3L)
            .firstName("Test")
            .lastName("User")
            .email("testuser@ucsb.edu")
            .course(course2)
            .orgStatus(OrgStatus.JOINCOURSE)
            .githubId(null)
            .githubLogin(null)
            .user(currentUser)
            .build();

    CourseStaff courseStaffUpdated =
        CourseStaff.builder()
            .id(3L)
            .firstName("Test")
            .lastName("User")
            .email("testuser@ucsb.edu")
            .course(course2)
            .orgStatus(OrgStatus.MEMBER)
            .githubId(currentUser.getGithubId())
            .githubLogin(currentUser.getGithubLogin())
            .user(currentUser)
            .build();

    when(courseStaffRepository.findById(eq(3L))).thenReturn(Optional.of(courseStaff));
    when(courseStaffRepository.save(eq(courseStaffUpdated))).thenReturn(courseStaffUpdated);
    when(organizationMemberService.inviteOrganizationOwner(any(CourseStaff.class)))
        .thenReturn(OrgStatus.MEMBER);

    MvcResult response =
        mockMvc
            .perform(put("/api/coursestaff/joinCourse").with(csrf()).param("courseStaffId", "3"))
            .andExpect(status().isAccepted())
            .andReturn();

    verify(courseStaffRepository).findById(eq(3L));

    verify(courseStaffRepository, times(1)).save(eq(courseStaffUpdated));
    assertEquals(
        "Already in organization - set status to MEMBER",
        response.getResponse().getContentAsString());
  }

  @Test
  @WithMockUser(roles = {"USER", "GITHUB"})
  public void test_already_part_is_owner() throws Exception {
    User currentUser = currentUserService.getUser();

    Course course2 =
        Course.builder()
            .id(2L)
            .installationId("1234")
            .orgName("ucsb-cs156")
            .courseName("course")
            .creator(currentUser)
            .build();

    CourseStaff courseStaff =
        CourseStaff.builder()
            .id(3L)
            .firstName("Test")
            .lastName("User")
            .email("testuser@ucsb.edu")
            .course(course2)
            .orgStatus(OrgStatus.JOINCOURSE)
            .githubId(null)
            .githubLogin(null)
            .user(currentUser)
            .build();

    CourseStaff courseStaffUpdated =
        CourseStaff.builder()
            .id(3L)
            .firstName("Test")
            .lastName("User")
            .email("testuser@ucsb.edu")
            .course(course2)
            .orgStatus(OrgStatus.OWNER)
            .githubId(currentUser.getGithubId())
            .githubLogin(currentUser.getGithubLogin())
            .user(currentUser)
            .build();

    when(courseStaffRepository.findById(eq(3L))).thenReturn(Optional.of(courseStaff));
    when(courseStaffRepository.save(eq(courseStaffUpdated))).thenReturn(courseStaffUpdated);
    when(organizationMemberService.inviteOrganizationOwner(any(CourseStaff.class)))
        .thenReturn(OrgStatus.OWNER);

    MvcResult response =
        mockMvc
            .perform(put("/api/coursestaff/joinCourse").with(csrf()).param("courseStaffId", "3"))
            .andExpect(status().isAccepted())
            .andReturn();

    verify(courseStaffRepository).findById(eq(3L));

    verify(courseStaffRepository, times(1)).save(eq(courseStaffUpdated));
    assertEquals(
        "Already in organization - set status to OWNER",
        response.getResponse().getContentAsString());
  }

  @Test
  @WithMockUser(roles = {"USER", "GITHUB"})
  public void cant_invite() throws Exception {
    User currentUser = currentUserService.getUser();

    Course course2 =
        Course.builder()
            .id(2L)
            .installationId("1234")
            .orgName("ucsb-cs156")
            .courseName("course")
            .creator(currentUser)
            .build();

    CourseStaff courseStaff =
        CourseStaff.builder()
            .id(3L)
            .firstName("Test")
            .lastName("User")
            .email("testuser@ucsb.edu")
            .course(course2)
            .orgStatus(OrgStatus.PENDING)
            .githubId(123456789)
            .githubLogin(null)
            .user(currentUser)
            .build();

    CourseStaff courseStaffUpdated =
        CourseStaff.builder()
            .id(3L)
            .firstName("Test")
            .lastName("User")
            .email("testuser@ucsb.edu")
            .course(course2)
            .orgStatus(OrgStatus.PENDING)
            .githubId(currentUser.getGithubId())
            .githubLogin(currentUser.getGithubLogin())
            .user(currentUser)
            .build();

    when(courseStaffRepository.findById(eq(3L))).thenReturn(Optional.of(courseStaff));
    when(courseStaffRepository.save(eq(courseStaffUpdated))).thenReturn(courseStaffUpdated);
    when(organizationMemberService.inviteOrganizationOwner(any(CourseStaff.class)))
        .thenReturn(OrgStatus.PENDING);

    // Act
    MvcResult response =
        mockMvc
            .perform(put("/api/coursestaff/joinCourse").with(csrf()).param("courseStaffId", "3"))
            .andExpect(status().isInternalServerError())
            .andReturn();

    // Assert
    verify(courseStaffRepository).findById(eq(3L));

    // Verify the GitHub ID and login were set
    verify(courseStaffRepository, times(1)).save(eq(courseStaffUpdated));
    assertEquals(
        "Could not invite staff member to Organization",
        response.getResponse().getContentAsString());
  }

  @Test
  @WithInstructorCoursePermissions
  public void testUpdateRosterStudent_success() throws Exception {
    CourseStaff existingStaffMember =
        CourseStaff.builder()
            .id(1L)
            .firstName("Old")
            .lastName("OldName")
            .email("old@ucsb.edu")
            .course(course1)
            .orgStatus(OrgStatus.PENDING)
            .build();

    CourseStaff updatedStaffMember =
        CourseStaff.builder()
            .id(1L)
            .firstName("New")
            .lastName("NewName")
            .email("old@ucsb.edu")
            .course(course1)
            .orgStatus(OrgStatus.PENDING)
            .build();

    when(courseStaffRepository.findById(eq(1L))).thenReturn(Optional.of(existingStaffMember));
    when(courseStaffRepository.save(any(CourseStaff.class))).thenReturn(updatedStaffMember);

    MvcResult response =
        mockMvc
            .perform(
                put("/api/coursestaff")
                    .with(csrf())
                    .param("courseId", "1")
                    .param("id", "1")
                    .param("firstName", "   New   ")
                    .param("lastName", "   NewName   "))
            .andExpect(status().isOk())
            .andReturn();

    ArgumentCaptor<CourseStaff> captor = ArgumentCaptor.forClass(CourseStaff.class);
    verify(courseStaffRepository).save(captor.capture());
    CourseStaff saved = captor.getValue();
    assertEquals("New", saved.getFirstName());
    assertEquals("NewName", saved.getLastName());

    String responseString = response.getResponse().getContentAsString();
    String expectedJson = mapper.writeValueAsString(updatedStaffMember);
    assertEquals(expectedJson, responseString);
  }

  @Test
  @WithInstructorCoursePermissions
  public void testUpdateRosterStudent_course_not_found() throws Exception {

    when(courseRepository.findById(eq(42L))).thenReturn(Optional.empty());

    MvcResult response =
        mockMvc
            .perform(
                put("/api/coursestaff")
                    .with(csrf())
                    .param("courseId", "42")
                    .param("id", "1")
                    .param("firstName", "   New   ")
                    .param("lastName", "   NewName   "))
            .andExpect(status().isNotFound())
            .andReturn();
  }

  @Test
  @WithInstructorCoursePermissions
  public void testDeleteCourseStaff_success() throws Exception {
    CourseStaff staffMember =
        CourseStaff.builder()
            .id(1L)
            .firstName("Test")
            .lastName("Staff")
            .email("teststaff@ucsb.edu")
            .course(course1)
            .orgStatus(OrgStatus.PENDING)
            .build();

    List<CourseStaff> staffList = new ArrayList<>();
    staffList.add(staffMember);
    course1.setCourseStaff(staffList);

    List<CourseStaff> staffListSpy = Mockito.spy(staffList);
    course1.setCourseStaff(staffListSpy);

    when(courseStaffRepository.findById(eq(1L))).thenReturn(Optional.of(staffMember));
    when(courseRepository.save(any(Course.class))).thenReturn(course1);

    MvcResult response =
        mockMvc
            .perform(
                delete("/api/coursestaff/delete")
                    .with(csrf())
                    .param("id", "1")
                    .param("courseId", "7"))
            .andExpect(status().isOk())
            .andReturn();

    verify(courseStaffRepository).findById(eq(1L));
    verify(courseRepository).save(any(Course.class));
    verify(courseStaffRepository).delete(eq(staffMember));
    verify(staffListSpy).remove(eq(staffMember));
    // Since the staff member doesn't have a GitHub login, removeOrganizationMember
    // should not be called
    verify(organizationMemberService, never()).removeOrganizationMember(any(CourseStaff.class));

    assertEquals(
        "Successfully deleted staff member and removed them from the staff roster.",
        response.getResponse().getContentAsString());
  }

  @Test
  @WithInstructorCoursePermissions
  public void testDeleteCourseStaff_withGithubLogin_success() throws Exception {
    course1.setOrgName("test-org");
    course1.setInstallationId("12345");

    CourseStaff staffMember =
        CourseStaff.builder()
            .id(1L)
            .firstName("Test")
            .lastName("Staff")
            .email("teststaff@ucsb.edu")
            .course(course1)
            .orgStatus(OrgStatus.MEMBER)
            .githubId(67890)
            .githubLogin("teststaff")
            .build();

    List<CourseStaff> staffList = new ArrayList<>();
    staffList.add(staffMember);
    course1.setCourseStaff(staffList);

    List<CourseStaff> staffListSpy = Mockito.spy(staffList);
    course1.setCourseStaff(staffListSpy);

    when(courseStaffRepository.findById(eq(1L))).thenReturn(Optional.of(staffMember));
    when(courseRepository.save(any(Course.class))).thenReturn(course1);
    doNothing().when(organizationMemberService).removeOrganizationMember(any(CourseStaff.class));

    MvcResult response =
        mockMvc
            .perform(
                delete("/api/coursestaff/delete")
                    .with(csrf())
                    .param("id", "1")
                    .param("courseId", "7"))
            .andExpect(status().isOk())
            .andReturn();

    verify(courseStaffRepository).findById(eq(1L));
    verify(courseRepository).save(any(Course.class));
    verify(courseStaffRepository).delete(eq(staffMember));
    verify(staffListSpy).remove(eq(staffMember));
    verify(organizationMemberService).removeOrganizationMember(eq(staffMember));

    assertEquals(
        "Successfully deleted staff member and removed them from the staff roster and organization.",
        response.getResponse().getContentAsString());
  }

  @Test
  @WithInstructorCoursePermissions
  public void testDeleteCourseStaff_withGithubLogin_noOrgName_success() throws Exception {
    course1.setOrgName(null);
    course1.setInstallationId("12345");

    CourseStaff staffMember =
        CourseStaff.builder()
            .id(1L)
            .firstName("Test")
            .lastName("Staff")
            .email("teststaff@ucsb.edu")
            .course(course1)
            .orgStatus(OrgStatus.MEMBER)
            .githubId(67890)
            .githubLogin("teststaff")
            .build();

    List<CourseStaff> staffList = new ArrayList<>();
    staffList.add(staffMember);
    course1.setCourseStaff(staffList);

    List<CourseStaff> staffListSpy = Mockito.spy(staffList);
    course1.setCourseStaff(staffListSpy);

    when(courseStaffRepository.findById(eq(1L))).thenReturn(Optional.of(staffMember));
    when(courseRepository.save(any(Course.class))).thenReturn(course1);

    MvcResult response =
        mockMvc
            .perform(
                delete("/api/coursestaff/delete")
                    .with(csrf())
                    .param("id", "1")
                    .param("courseId", "7"))
            .andExpect(status().isOk())
            .andReturn();

    verify(courseStaffRepository).findById(eq(1L));
    verify(courseRepository).save(any(Course.class));
    verify(courseStaffRepository).delete(eq(staffMember));
    verify(staffListSpy).remove(eq(staffMember));
    verify(organizationMemberService, never()).removeOrganizationMember(any(CourseStaff.class));

    assertEquals(
        "Successfully deleted staff member and removed them from the staff roster.",
        response.getResponse().getContentAsString());
  }

  @Test
  @WithInstructorCoursePermissions
  public void testDeleteCourseStaff_withGithubLogin_noInstallationId_success() throws Exception {
    course1.setOrgName("test-org");
    course1.setInstallationId(null);

    CourseStaff staffMember =
        CourseStaff.builder()
            .id(1L)
            .firstName("Test")
            .lastName("Staff")
            .email("teststaff@ucsb.edu")
            .course(course1)
            .orgStatus(OrgStatus.MEMBER)
            .githubId(67890)
            .githubLogin("teststaff")
            .build();

    List<CourseStaff> staffList = new ArrayList<>();
    staffList.add(staffMember);
    course1.setCourseStaff(staffList);

    List<CourseStaff> staffListSpy = Mockito.spy(staffList);
    course1.setCourseStaff(staffListSpy);

    when(courseStaffRepository.findById(eq(1L))).thenReturn(Optional.of(staffMember));
    when(courseRepository.save(any(Course.class))).thenReturn(course1);

    MvcResult response =
        mockMvc
            .perform(
                delete("/api/coursestaff/delete")
                    .with(csrf())
                    .param("id", "1")
                    .param("courseId", "1"))
            .andExpect(status().isOk())
            .andReturn();

    verify(courseStaffRepository).findById(eq(1L));
    verify(courseRepository).save(any(Course.class));
    verify(courseStaffRepository).delete(eq(staffMember));
    verify(staffListSpy).remove(eq(staffMember));
    verify(organizationMemberService, never()).removeOrganizationMember(any(CourseStaff.class));

    assertEquals(
        "Successfully deleted staff member and removed them from the staff roster.",
        response.getResponse().getContentAsString());
  }

  @Test
  @WithInstructorCoursePermissions
  public void testDeleteCourseStaff_withGithubLogin_orgRemovalFails() throws Exception {
    course1.setOrgName("test-org");
    course1.setInstallationId("12345");

    CourseStaff staffMember =
        CourseStaff.builder()
            .id(1L)
            .firstName("Test")
            .lastName("Staff")
            .email("teststaff@ucsb.edu")
            .course(course1)
            .orgStatus(OrgStatus.MEMBER)
            .githubId(67890)
            .githubLogin("teststaff")
            .build();

    List<CourseStaff> staffList = new ArrayList<>();
    staffList.add(staffMember);
    course1.setCourseStaff(staffList);

    List<CourseStaff> staffListSpy = Mockito.spy(staffList);
    course1.setCourseStaff(staffListSpy);

    when(courseStaffRepository.findById(eq(1L))).thenReturn(Optional.of(staffMember));
    when(courseRepository.save(any(Course.class))).thenReturn(course1);

    String errorMessage = "API rate limit exceeded";
    doThrow(new RuntimeException(errorMessage))
        .when(organizationMemberService)
        .removeOrganizationMember(any(CourseStaff.class));

    MvcResult response =
        mockMvc
            .perform(
                delete("/api/coursestaff/delete")
                    .with(csrf())
                    .param("id", "1")
                    .param("courseId", "1"))
            .andExpect(status().isOk())
            .andReturn();

    verify(courseStaffRepository).findById(eq(1L));
    verify(courseRepository).save(any(Course.class));
    verify(courseStaffRepository).delete(eq(staffMember));
    verify(staffListSpy).remove(eq(staffMember));
    verify(organizationMemberService).removeOrganizationMember(eq(staffMember));

    assertEquals(
        "Successfully deleted staff member but there was an error removing them from the course organization: "
            + errorMessage,
        response.getResponse().getContentAsString());
  }

  @Test
  @WithInstructorCoursePermissions
  public void testDeleteCourseStaff_notFound() throws Exception {
    when(courseStaffRepository.findById(eq(99L))).thenReturn(Optional.empty());
    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(course1));
    MvcResult response =
        mockMvc
            .perform(
                delete("/api/coursestaff/delete")
                    .with(csrf())
                    .param("id", "99")
                    .param("courseId", "1"))
            .andExpect(status().isNotFound())
            .andReturn();

    verify(courseStaffRepository).findById(eq(99L));
    verify(courseStaffRepository, never()).delete(any(CourseStaff.class));
    verify(courseRepository, never()).save(any(Course.class));

    String responseString = response.getResponse().getContentAsString();
    Map<String, String> expectedMap =
        Map.of(
            "type", "EntityNotFoundException",
            "message", "CourseStaff with id 99 not found");
    String expectedJson = mapper.writeValueAsString(expectedMap);
    assertEquals(expectedJson, responseString);
  }

  @Test
  @WithMockUser(roles = {"USER"})
  public void testDeleteCourseStaff_unauthorized() throws Exception {
    MvcResult response =
        mockMvc
            .perform(
                delete("/api/coursestaff/delete")
                    .with(csrf())
                    .param("id", "1")
                    .param("courseId", "7"))
            .andExpect(status().isForbidden())
            .andReturn();

    verify(courseStaffRepository, never()).findById(any());
    verify(courseStaffRepository, never()).delete(any(CourseStaff.class));
    verify(courseRepository, never()).save(any(Course.class));
  }
}
