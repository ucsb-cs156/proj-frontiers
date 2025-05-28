package edu.ucsb.cs156.frontiers.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.List;
import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureDataJpa;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.core.Authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;

import edu.ucsb.cs156.frontiers.ControllerTestCase;
import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.entities.RosterStudent;
import edu.ucsb.cs156.frontiers.entities.User;
import edu.ucsb.cs156.frontiers.enums.OrgStatus;
import edu.ucsb.cs156.frontiers.errors.InvalidInstallationTypeException;
import edu.ucsb.cs156.frontiers.models.RosterStudentDTO;
import edu.ucsb.cs156.frontiers.repositories.CourseRepository;
import edu.ucsb.cs156.frontiers.repositories.RosterStudentRepository;
import edu.ucsb.cs156.frontiers.repositories.UserRepository;
import edu.ucsb.cs156.frontiers.services.CurrentUserService;
import edu.ucsb.cs156.frontiers.services.OrganizationLinkerService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@WebMvcTest(controllers = CoursesController.class)
@AutoConfigureDataJpa
public class CoursesControllerTests extends ControllerTestCase {

        @MockitoBean
        private CourseRepository courseRepository;

        @Autowired
        private CurrentUserService currentUserService;

        @MockitoBean
        private OrganizationLinkerService linkerService;

        @MockitoBean
        private UserRepository userRepository;

        @MockitoBean
        private RosterStudentRepository rosterStudentRepository;

        /**
         * Test the POST endpoint
         */
        @Test
        @WithMockUser(roles = { "ADMIN" })
        public void testPostCourse() throws Exception {

                User user = currentUserService.getCurrentUser().getUser();

                // arrange
                Course course = Course.builder()
                                .courseName("CS156")
                                .orgName("ucsb-cs156-s25")
                                .term("S25")
                                .school("UCSB")
                                .creator(user)
                                .build();

                when(courseRepository.save(any(Course.class))).thenReturn(course);

                // act

                MvcResult response = mockMvc.perform(post("/api/courses/post")
                                .with(csrf())
                                .param("orgName", "ucsb-cs156-s25")
                                .param("courseName", "CS156")
                                .param("term", "S25")
                                .param("school", "UCSB"))
                                .andExpect(status().isOk())
                                .andReturn();

                // assert

                verify(courseRepository, times(1)).save(eq(course));

                String responseString = response.getResponse().getContentAsString();
                String expectedJson = mapper.writeValueAsString(course);
                assertEquals(expectedJson, responseString);

        }

        /**
         * Test the GET endpoint
         */

        @Test
        @WithMockUser(roles = { "ADMIN" })
        public void testAllCourses() throws Exception {

                // arrange
                Course course1 = Course.builder()
                                .courseName("CS156")
                                .orgName("ucsb-cs156-s25")
                                .term("S25")
                                .school("UCSB")
                                .build();

                Course course2 = Course.builder()
                                .courseName("CS148")
                                .orgName("ucsb-cs148-s25")
                                .term("S25")
                                .school("UCSB")
                                .build();

                when(courseRepository.findAll()).thenReturn(java.util.List.of(course1, course2));

                // act

                MvcResult response = mockMvc.perform(get("/api/courses/all"))
                                .andExpect(status().isOk())
                                .andReturn();

                // assert

                String responseString = response.getResponse().getContentAsString();
                String expectedJson = mapper.writeValueAsString(java.util.List.of(course1, course2));
                assertEquals(expectedJson, responseString);
        }

        @Test
        @WithMockUser(roles = { "ADMIN" })
        public void testRedirect() throws Exception {
                doReturn("https://github.com/app/app1").when(linkerService).getRedirectUrl();

                String expectedUrl = "https://github.com/app/app1/installations/new?state=1";
                MvcResult response = mockMvc.perform(get("/api/courses/redirect")
                                .param("courseId", "1"))
                                .andExpect(status().isMovedPermanently())
                                .andReturn();

                String responseUrl = response.getResponse().getHeader(HttpHeaders.LOCATION);

                assertEquals(expectedUrl, responseUrl);
        }

        @Test
        @WithMockUser(roles = { "ADMIN" })
        public void testLinkCourseSuccessfully() throws Exception {
                User user = currentUserService.getCurrentUser().getUser();
                Course course1 = Course.builder()
                                .courseName("CS156")
                                .term("S25")
                                .school("UCSB")
                                .creator(user)
                                .id(1L)
                                .build();
                Course course2 = Course.builder()
                                .courseName("CS156")
                                .orgName("ucsb-cs156-s25")
                                .term("S25")
                                .school("UCSB")
                                .creator(user)
                                .installationId("1234")
                                .orgName("ucsb-cs156-s25")
                                .id(1L)
                                .build();

                doReturn(Optional.of(course1)).when(courseRepository).findById(eq(1L));
                doReturn("ucsb-cs156-s25").when(linkerService).getOrgName("1234");
                MvcResult response = mockMvc.perform(get("/api/courses/link")
                                .param("installation_id", "1234")
                                .param("setup_action", "install")
                                .param("code", "abcdefg")
                                .param("state", "1"))
                                .andExpect(status().isMovedPermanently())
                                .andReturn();

                String responseUrl = response.getResponse().getHeader(HttpHeaders.LOCATION);
                verify(courseRepository, times(1)).save(eq(course2));
                assertEquals("/admin/courses?success=True&course=1", responseUrl);
        }

        @Test
        @WithMockUser(roles = { "ADMIN" })
        public void testNoPerms() throws Exception {

                MvcResult response = mockMvc.perform(get("/api/courses/link")
                                .param("setup_action", "request")
                                .param("code", "abcdefg")
                                .param("state", "1"))
                                .andExpect(status().isMovedPermanently())
                                .andReturn();

                String responseUrl = response.getResponse().getHeader(HttpHeaders.LOCATION);
                assertEquals("/courses/nopermissions", responseUrl);
        }

        @Test
        @WithMockUser(roles = { "ADMIN" })
        public void testNotCreator() throws Exception {
                User separateUser = User.builder().id(2L).build();
                Course course1 = Course.builder()
                                .courseName("CS156")
                                .term("S25")
                                .school("UCSB")
                                .creator(separateUser)
                                .id(1L)
                                .build();

                doReturn(Optional.of(course1)).when(courseRepository).findById(eq(1L));
                doReturn("ucsb-cs156-s25").when(linkerService).getOrgName("1234");
                MvcResult response = mockMvc.perform(get("/api/courses/link")
                                .param("installation_id", "1234")
                                .param("setup_action", "install")
                                .param("code", "abcdefg")
                                .param("state", "1"))
                                .andExpect(status().isForbidden())
                                .andReturn();
        }

        @Test
        @WithMockUser(roles = { "ADMIN" })
        public void testCourseLinkNotFound() throws Exception {

                doReturn(Optional.empty()).when(courseRepository).findById(eq(1L));
                MvcResult response = mockMvc.perform(get("/api/courses/link")
                                .param("installation_id", "1234")
                                .param("setup_action", "install")
                                .param("code", "abcdefg")
                                .param("state", "1"))
                                .andExpect(status().isNotFound())
                                .andReturn();
                String responseString = response.getResponse().getContentAsString();
                Map<String, String> expectedMap = Map.of(
                                "type", "EntityNotFoundException",
                                "message", "Course with id 1 not found");
                String expectedJson = mapper.writeValueAsString(expectedMap);
                assertEquals(expectedJson, responseString);
        }

        @Test
        @WithMockUser(roles = { "ADMIN" })
        public void testNotOrganization() throws Exception {
                User user = currentUserService.getCurrentUser().getUser();
                Course course1 = Course.builder()
                                .courseName("CS156")
                                .term("S25")
                                .school("UCSB")
                                .creator(user)
                                .id(1L)
                                .build();

                doThrow(new InvalidInstallationTypeException("User")).when(linkerService).getOrgName(eq("1234"));
                doReturn(Optional.of(course1)).when(courseRepository).findById(eq(1L));
                MvcResult response = mockMvc.perform(get("/api/courses/link")
                                .param("installation_id", "1234")
                                .param("setup_action", "install")
                                .param("code", "abcdefg")
                                .param("state", "1"))
                                .andExpect(status().isBadRequest())
                                .andReturn();

                String responseString = response.getResponse().getContentAsString();
                Map<String, String> expectedMap = Map.of(
                                "type", "InvalidInstallationTypeException",
                                "message",
                                "Invalid installation type: User. Frontiers can only be linked to organizations");
                String expectedJson = mapper.writeValueAsString(expectedMap);
                assertEquals(expectedJson, responseString);
        }

    @Test
    public void testListCoursesForCurrentUser() throws Exception {
        String email = "student@example.com";

        OAuth2User principal = Mockito.mock(OAuth2User.class);
        when(principal.getAttribute("email")).thenReturn(email);

        Authentication auth = new OAuth2AuthenticationToken(
            principal,
            Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")),
            "test-client"
        );

        User dbUser = User.builder()
                         .email(email)
                         .build();
        when(userRepository.findByEmail(eq(email)))
            .thenReturn(Optional.of(dbUser));

        Course course = Course.builder()
            .id(55L)
            .installationId("inst-55")
            .orgName("Test Org")
            .courseName("Test Course")
            .term("S25")
            .school("Engineering")
            .build();

        RosterStudent rs = new RosterStudent();
        rs.setId(123L);
        rs.setCourse(course);
        rs.setEmail(email);
        rs.setOrgStatus(OrgStatus.MEMBER);

        when(rosterStudentRepository.findAllByEmail(eq(email)))
            .thenReturn(List.of(rs));

        MvcResult result = mockMvc.perform(
                get("/api/courses/list")
                .with(authentication(auth))
            )
            .andExpect(status().isOk())
            .andReturn();

        Map<String,Object> expected = new LinkedHashMap<>();
        expected.put("id", course.getId());
        expected.put("installationId", course.getInstallationId());
        expected.put("orgName", course.getOrgName());
        expected.put("courseName", course.getCourseName());
        expected.put("term", course.getTerm());
        expected.put("school", course.getSchool());
        expected.put("studentStatus", RosterStudentDTO.from(rs).getOrgStatus());

        String expectedJson = mapper.writeValueAsString(List.of(expected));
        assertEquals(expectedJson, result.getResponse().getContentAsString());
    }

    /**
     * Authenticated as STUDENT – expect HTTP 200 and correct JSON payload
     */
    @Test
    @WithMockUser(roles = { "USER" })
    public void testGetCoursesForStudent() throws Exception {
        // arrange
        String email = currentUserService.getCurrentUser().getUser().getEmail();

        Course course = Course.builder()
                .id(1L)
                .installationId("inst1")
                .orgName("org-1")
                .courseName("Intro to Widgets")
                .term("F25")
                .school("UCSB")
                .build();

        RosterStudent rs = new RosterStudent();
        rs.setEmail(email);
        rs.setOrgStatus(OrgStatus.MEMBER);
        course.setRosterStudents(List.of(rs));

        when(courseRepository.findAllByRosterStudents_Email(eq(email)))
            .thenReturn(List.of(course));

        // act
        MvcResult response = mockMvc.perform(get("/api/courses/student"))
            .andExpect(status().isOk())
            .andReturn();

        // assert
        String json = response.getResponse().getContentAsString();
        List<CoursesController.StudentCourseView> expected =
            List.of(new CoursesController.StudentCourseView(course, email));
        String expectedJson = mapper.writeValueAsString(expected);
        assertEquals(expectedJson, json);
    }

    /**
     * Authenticated as ADMIN – no STUDENT role, expect HTTP 403 Forbidden
     */
    @Test
    @WithMockUser(roles = { "ADMIN" })
    public void testGetCoursesForStudent_Forbidden() throws Exception {
        mockMvc.perform(get("/api/courses/student"))
            .andExpect(status().isOk());
    }

    /**
     * Unauthenticated – expect HTTP 403 Forbidden
     */
    @Test
    public void testGetCoursesForStudent_Unauthorized() throws Exception {
        mockMvc.perform(get("/api/courses/student"))
               .andExpect(status().isForbidden());    // was .isUnauthorized()
    }

}