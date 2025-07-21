package edu.ucsb.cs156.frontiers.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.ucsb.cs156.frontiers.entities.*;
import edu.ucsb.cs156.frontiers.jobs.UpdateOrgMembershipJob;
import edu.ucsb.cs156.frontiers.services.OrganizationMemberService;
import edu.ucsb.cs156.frontiers.services.jobs.JobService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureDataJpa;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;

import edu.ucsb.cs156.frontiers.ControllerTestCase;
import edu.ucsb.cs156.frontiers.enums.OrgStatus;
import edu.ucsb.cs156.frontiers.enums.RosterStatus;
import edu.ucsb.cs156.frontiers.repositories.CourseRepository;
import edu.ucsb.cs156.frontiers.repositories.CourseStaffRepository;
import edu.ucsb.cs156.frontiers.services.CurrentUserService;
import edu.ucsb.cs156.frontiers.services.UpdateUserService;
import lombok.extern.slf4j.Slf4j;

import static org.mockito.Mockito.*;
import static org.mockito.Mockito.doReturn;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

import org.springframework.http.MediaType;

@Slf4j
@WebMvcTest(controllers = CourseStaffController.class)
public class CourseStaffControllerTests extends ControllerTestCase {

        @MockitoBean
        private CourseRepository courseRepository;

        @MockitoBean
        private CourseStaffRepository courseStaffRepository;

        @Autowired
        private CurrentUserService currentUserService;

        @MockitoBean
        private UpdateUserService updateUserService;

        @MockitoBean
        private OrganizationMemberService organizationMemberService;

        @MockitoBean
        private JobService service;

        @Autowired
        private ObjectMapper objectMapper;

        Course course1 = Course.builder()
                        .id(1L)
                        .courseName("CS156")
                        .orgName("ucsb-cs156-s25")
                        .term("S25")
                        .school("UCSB")
                        .build();

        CourseStaff cs1 = CourseStaff.builder()
                        .firstName("Chris")
                        .lastName("Gaucho")
                        .email("cgaucho@example.org")
                        .course(course1)
                        .build();

        CourseStaff cs2 = CourseStaff.builder()
                        .id(2L)
                        .firstName("Lauren")
                        .lastName("Del Playa")
                        .email("ldelplaya@ucsb.edu")
                        .course(course1)
                        .build();

        /**
         * Test the POST endpoint
         */
        @Test
        @WithMockUser(roles = { "ADMIN" })
        public void testPostCourseStaff() throws Exception {

                when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(course1));
                when(courseStaffRepository.save(any(CourseStaff.class))).thenReturn(cs1);

                // act

                MvcResult response = mockMvc.perform(post("/api/coursestaff/post")
                                .with(csrf())
                                .param("firstName", "Chris")
                                .param("lastName", "Gaucho")
                                .param("email", "cgaucho@example.org")
                                .param("courseId", "1"))
                                .andExpect(status().isOk())
                                .andReturn();

                // assert

                verify(courseRepository, times(1)).findById(eq(1L));
                verify(courseStaffRepository, times(1)).save(eq(cs1));

                verify(updateUserService).attachUserToCourseStaff(any(CourseStaff.class));

                String responseString = response.getResponse().getContentAsString();
                String expectedJson = mapper.writeValueAsString(cs1);
                assertEquals(expectedJson, responseString);

        }

        /**
         * Test that you cannot post a single roster student for a course that does not
         * exist
         * 
         * @throws Exception
         */

        @Test
        @WithMockUser(roles = { "ADMIN" })
        public void test_AdminCannotPostCourseStaffForCourseThatDoesNotExist() throws Exception {
                when(courseRepository.findById(eq(1L))).thenReturn(Optional.empty());

                // act

                MvcResult response = mockMvc.perform(post("/api/coursestaff/post")
                                .with(csrf())
                                .param("firstName", "Chris")
                                .param("lastName", "Gaucho")
                                .param("email", "cgaucho@example.org")
                                .param("courseId", "1"))
                                .andExpect(status().isNotFound())
                                .andReturn();

                // assert

                String responseString = response.getResponse().getContentAsString();
                Map<String, String> expectedMap = Map.of(
                                "type", "EntityNotFoundException",
                                "message", "Course with id 1 not found");
                String expectedJson = mapper.writeValueAsString(expectedMap);
                assertEquals(expectedJson, responseString);

        }

        /**
         * Test the GET endpoint
         */

        @Test
        @WithMockUser(roles = { "ADMIN" })
        public void testCourseStaffByCourse() throws Exception {

                // arrange

                when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(course1));
                when(courseStaffRepository.findByCourseId(eq(1L))).thenReturn(java.util.List.of(cs1, cs2));

                // act

                MvcResult response = mockMvc.perform(get("/api/coursestaff/course")
                                .param("courseId", "1"))
                                .andExpect(status().isOk())
                                .andReturn();

                // assert

                String responseString = response.getResponse().getContentAsString();
                String expectedJson = mapper.writeValueAsString(java.util.List.of(cs1, cs2));
                assertEquals(expectedJson, responseString);
        }

        /** Test whether admin can get course staff for a non existing course */

        @WithMockUser(roles = { "ADMIN" })
        @Test
        public void admin_can_get_course_staff_for_a_non_existing_course() throws Exception {

                // arrange

                when(courseRepository.findById(eq(1L))).thenReturn(Optional.empty());

                // act

                MvcResult response = mockMvc.perform(get("/api/coursestaff/course")
                                .param("courseId", "1"))
                                .andExpect(status().isNotFound())
                                .andReturn();

                // assert

                verify(courseRepository, atLeastOnce()).findById(eq(1L));
                String responseString = response.getResponse().getContentAsString();
                Map<String, String> expectedMap = Map.of(
                                "type", "EntityNotFoundException",
                                "message", "Course with id 1 not found");
                String expectedJson = mapper.writeValueAsString(expectedMap);
                assertEquals(expectedJson, responseString);

        }

        /**
         * Tests for the joinCourseOnGitHub endpoint
         */
        @Test
        @WithMockUser(roles = { "USER", "GITHUB"})
        public void testLinkGitHub_notFound() throws Exception {
                // Arrange
                when(courseStaffRepository.findById(eq(99L))).thenReturn(Optional.empty());

                // Act
                MvcResult response = mockMvc.perform(put("/api/coursestaff/joinCourse")
                                .with(csrf())
                                .param("courseStaffId", "99"))
                        .andExpect(status().isNotFound())
                        .andReturn();

                // Assert
                verify(courseStaffRepository).findById(eq(99L));

                // Verify correct error response
                String responseString = response.getResponse().getContentAsString();
                Map<String, String> expectedMap = Map.of(
                        "type", "EntityNotFoundException",
                        "message", "CourseStaff with id 99 not found");
                String expectedJson = mapper.writeValueAsString(expectedMap);
                assertEquals(expectedJson, responseString);
        }

        @Test
        @WithMockUser(roles = { "USER", "GITHUB"})
        public void testJoinCourseOnGitHub_unauthorized() throws Exception {
                User currentUser = currentUserService.getUser();

                User differentUser = User.builder()
                        .id(24L)
                        .build();

                CourseStaff courseStaff = CourseStaff.builder()
                        .id(4L)
                        .firstName("Other")
                        .lastName("Student")
                        .email("otherstudent@ucsb.edu")
                        .course(course1)
                        .orgStatus(OrgStatus.PENDING)
                        .user(differentUser)
                        .build();

                when(courseStaffRepository.findById(eq(4L))).thenReturn(Optional.of(courseStaff));

                MvcResult response = mockMvc.perform(put("/api/coursestaff/joinCourse")
                                .with(csrf())
                                .param("courseStaffId", "4"))
                        .andExpect(status().isBadRequest())
                        .andReturn();

                String responseString = response.getResponse().getContentAsString();
                Map<String, String> expectedMap = Map.of(
                        "type", "IllegalArgumentException",
                        "message", "This operation is restricted to the user associated with staff member with id 4");
                String expectedJson = mapper.writeValueAsString(expectedMap);
                assertEquals(expectedJson, responseString);
                verify(courseStaffRepository, never()).save(any(CourseStaff.class));
        }

        @Test
        @WithMockUser(roles = { "USER", "GITHUB"})
        public void test_null_user_on_join() throws Exception {

                CourseStaff courseStaff = CourseStaff.builder()
                        .id(4L)
                        .firstName("Other")
                        .lastName("Student")
                        .email("otherstudent@ucsb.edu")
                        .course(course1)
                        .orgStatus(OrgStatus.PENDING)
                        .user(null)
                        .build();

                when(courseStaffRepository.findById(eq(4L))).thenReturn(Optional.of(courseStaff));

                MvcResult response = mockMvc.perform(put("/api/coursestaff/joinCourse")
                                .with(csrf())
                                .param("courseStaffId", "4"))
                        .andExpect(status().isBadRequest())
                        .andReturn();


                String responseString = response.getResponse().getContentAsString();
                Map<String, String> expectedMap = Map.of(
                        "type", "IllegalArgumentException",
                        "message", "This operation is restricted to the user associated with staff member with id 4");
                String expectedJson = mapper.writeValueAsString(expectedMap);
                assertEquals(expectedJson, responseString);
                verify(courseStaffRepository, never()).save(any(CourseStaff.class));
        }

        @Test
        @WithMockUser(roles = { "USER", "GITHUB"})
        public void testJoinCourseOnGitHub_alreadyJoined() throws Exception {
                User currentUser = currentUserService.getUser();

                CourseStaff courseStaff = CourseStaff.builder()
                        .id(5L)
                        .firstName("Already")
                        .lastName("Linked")
                        .email("alreadylinked@ucsb.edu")
                        .course(course1)
                        .orgStatus(OrgStatus.PENDING)
                        .githubId(98765)
                        .githubLogin("existinguser")
                        .user(currentUser)
                        .build();

                when(courseStaffRepository.findById(eq(5L))).thenReturn(Optional.of(courseStaff));

                // Act
                MvcResult response = mockMvc.perform(put("/api/coursestaff/joinCourse")
                                .with(csrf())
                                .param("courseStaffId", "5"))
                        .andExpect(status().isBadRequest())
                        .andReturn();

                // Assert
                verify(courseStaffRepository).findById(eq(5L));
                verify(courseStaffRepository, never()).save(any(CourseStaff.class));
                verify(organizationMemberService, times(0)).inviteOrganizationOwner(any(CourseStaff.class));

                assertEquals("This course staff has already joined the course with a GitHub account.", response.getResponse().getContentAsString());
        }



        @Test
        @WithMockUser(roles = { "USER", "GITHUB"})
        public void no_fire_on_no_org_name() throws Exception {
                User currentUser = currentUserService.getUser();

                Course course2 = Course.builder().id(2L).installationId("1234").courseName("course").creator(currentUser).build();

                CourseStaff courseStaff = CourseStaff.builder()
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

                CourseStaff courseStaffUpdated = CourseStaff.builder()
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

                MvcResult response = mockMvc.perform(put("/api/coursestaff/joinCourse")
                                .with(csrf())
                                .param("courseStaffId", "3"))
                        .andExpect(status().isBadRequest())
                        .andReturn();

                verify(courseStaffRepository).findById(eq(3L));

                verify(courseStaffRepository, times(0)).save(eq(courseStaffUpdated));
                verify(organizationMemberService, times(0)).inviteOrganizationOwner(any(CourseStaff.class));

                assertEquals("Course has not been set up. Please ask your instructor for help.", response.getResponse().getContentAsString());
        }

        @Test
        @WithMockUser(roles = { "USER", "GITHUB"})
        public void no_fire_on_no_installation_id() throws Exception {
                User currentUser = currentUserService.getUser();

                CourseStaff courseStaff = CourseStaff.builder()
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

                CourseStaff courseStaffUpdated = CourseStaff.builder()
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
                MvcResult response = mockMvc.perform(put("/api/coursestaff/joinCourse")
                                .with(csrf())
                                .param("courseStaffId", "3"))
                        .andExpect(status().isBadRequest())
                        .andReturn();

                verify(courseStaffRepository).findById(eq(3L));

                verifyNoMoreInteractions(courseStaffRepository, organizationMemberService);

                assertEquals("Course has not been set up. Please ask your instructor for help.", response.getResponse().getContentAsString());
        }

        @Test
        @WithMockUser(roles = { "USER", "GITHUB"})
        public void test_fires_invite() throws Exception {
                User currentUser = currentUserService.getUser();

                Course course2 = Course.builder().id(2L).installationId("1234").orgName("ucsb-cs156").courseName("course").creator(currentUser).build();

                CourseStaff courseStaff = CourseStaff.builder()
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

                CourseStaff courseStaffUpdated = CourseStaff.builder()
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
                when(organizationMemberService.inviteOrganizationOwner(any(CourseStaff.class))).thenReturn(OrgStatus.INVITED);

                MvcResult response = mockMvc.perform(put("/api/coursestaff/joinCourse")
                                .with(csrf())
                                .param("courseStaffId", "3"))
                        .andExpect(status().isAccepted())
                        .andReturn();


                verify(courseStaffRepository).findById(eq(3L));

                verify(courseStaffRepository, times(1)).save(eq(courseStaffUpdated));
                assertEquals("Successfully invited staff member to Organization", response.getResponse().getContentAsString());
        }

        @Test
        @WithMockUser(roles = { "USER", "GITHUB"})
        public void cant_invite() throws Exception {
                User currentUser = currentUserService.getUser();

                Course course2 = Course.builder().id(2L).installationId("1234").orgName("ucsb-cs156").courseName("course").creator(currentUser).build();

                CourseStaff courseStaff = CourseStaff.builder()
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

                CourseStaff courseStaffUpdated = CourseStaff.builder()
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
                when(organizationMemberService.inviteOrganizationOwner(any(CourseStaff.class))).thenReturn(OrgStatus.PENDING);

                // Act
                MvcResult response = mockMvc.perform(put("/api/coursestaff/joinCourse")
                                .with(csrf())
                                .param("courseStaffId", "3"))
                        .andExpect(status().isInternalServerError())
                        .andReturn();


                // Assert
                verify(courseStaffRepository).findById(eq(3L));

                // Verify the GitHub ID and login were set
                verify(courseStaffRepository, times(1)).save(eq(courseStaffUpdated));
                assertEquals("Could not invite staff member to Organization", response.getResponse().getContentAsString());
        }
}
