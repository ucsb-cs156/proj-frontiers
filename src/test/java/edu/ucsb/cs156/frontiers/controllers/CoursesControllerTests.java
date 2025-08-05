package edu.ucsb.cs156.frontiers.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import edu.ucsb.cs156.frontiers.annotations.WithInstructorCoursePermissions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureDataJpa;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.core.type.TypeReference;

import edu.ucsb.cs156.frontiers.ControllerTestCase;
import edu.ucsb.cs156.frontiers.controllers.CoursesController.InstructorCourseView;
import edu.ucsb.cs156.frontiers.controllers.CoursesController.StaffCoursesDTO;
import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.entities.CourseStaff;
import edu.ucsb.cs156.frontiers.entities.RosterStudent;
import edu.ucsb.cs156.frontiers.entities.User;
import edu.ucsb.cs156.frontiers.enums.OrgStatus;
import edu.ucsb.cs156.frontiers.errors.InvalidInstallationTypeException;
import edu.ucsb.cs156.frontiers.models.RosterStudentDTO;
import edu.ucsb.cs156.frontiers.repositories.CourseRepository;
import edu.ucsb.cs156.frontiers.repositories.CourseStaffRepository;
import edu.ucsb.cs156.frontiers.repositories.RosterStudentRepository;
import edu.ucsb.cs156.frontiers.repositories.UserRepository;
import edu.ucsb.cs156.frontiers.repositories.InstructorRepository;
import edu.ucsb.cs156.frontiers.repositories.AdminRepository;
import edu.ucsb.cs156.frontiers.services.CurrentUserService;
import edu.ucsb.cs156.frontiers.services.OrganizationLinkerService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@WebMvcTest(controllers = CoursesController.class)
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

        @MockitoBean
        private CourseStaffRepository courseStaffRepository;

        @MockitoBean
        private InstructorRepository instructorRepository;

        @MockitoBean
        private AdminRepository adminRepository;

        /**
         * Test that ROLE_ADMIN can create a course
         */
        @Test
        @WithMockUser(roles = { "ADMIN" })
        public void testPostCourse_byAdmin() throws Exception {

                User user = currentUserService.getCurrentUser().getUser();

                // arrange
                Course course = Course.builder()
                                .courseName("CS156")
                                .term("S25")
                                .school("UCSB")
                                .instructorEmail(user.getEmail())
                                .build();

                when(courseRepository.save(any(Course.class))).thenReturn(course);

                // act

                MvcResult response = mockMvc.perform(post("/api/courses/post")
                                .with(csrf())
                                .param("courseName", "CS156")
                                .param("term", "S25")
                                .param("school", "UCSB"))
                                .andExpect(status().isOk())
                                .andReturn();

                // assert

                verify(courseRepository, times(1)).save(eq(course));

                String responseString = response.getResponse().getContentAsString();
                String expectedJson = mapper.writeValueAsString(new InstructorCourseView(course));
                assertEquals(expectedJson, responseString);

        }

        /**
         * Test that ROLE_INSTRUCTOR can create a course
         */
        @Test
        @WithMockUser(roles = { "INSTRUCTOR" })
        public void testPostCourse_byInstructor() throws Exception {

                User user = currentUserService.getCurrentUser().getUser();

                // arrange
                Course course = Course.builder()
                                .courseName("CS156")
                                .term("S25")
                                .school("UCSB")
                                .instructorEmail(user.getEmail())
                                .build();

                when(courseRepository.save(any(Course.class))).thenReturn(course);

                // act

                MvcResult response = mockMvc.perform(post("/api/courses/post")
                                .with(csrf())
                                .param("courseName", "CS156")
                                .param("term", "S25")
                                .param("school", "UCSB"))
                                .andExpect(status().isOk())
                                .andReturn();

                // assert

                verify(courseRepository, times(1)).save(eq(course));

                String responseString = response.getResponse().getContentAsString();
                String expectedJson = mapper.writeValueAsString(new InstructorCourseView(course));
                assertEquals(expectedJson, responseString);

        }

        /**
         * Test the GET all endpoint for courses for admins
         */

        @Test
        @WithMockUser(roles = { "ADMIN" })
        public void testAllCourses_ROLE_ADMIN() throws Exception {

                // arrange
                Course course1 = Course.builder()
                                .courseName("CS156")
                                .term("S25")
                                .school("UCSB")
                                .build();

                InstructorCourseView courseView1 = new InstructorCourseView(course1);

                Course course2 = Course.builder()
                                .courseName("CS148")
                                .term("S25")
                                .school("UCSB")
                                .build();
                InstructorCourseView courseView2 = new InstructorCourseView(course2);

                when(courseRepository.findAll()).thenReturn(java.util.List.of(course1, course2));

                // act

                MvcResult response = mockMvc.perform(get("/api/courses/allForAdmins"))
                                .andExpect(status().isOk())
                                .andReturn();

                // assert

                String responseString = response.getResponse().getContentAsString();
                String expectedJson = mapper.writeValueAsString(java.util.List.of(courseView1, courseView2));
                assertEquals(expectedJson, responseString);
        }

        /**
         * Test the GET endpoint for courses for instructors
         */

        @Test
        @WithMockUser(roles = { "INSTRUCTOR" })
        public void testAllCourses_ROLE_INSTRUCTOR() throws Exception {

                User user = currentUserService.getCurrentUser().getUser();

                // arrange
                Course course1 = Course.builder()
                                .courseName("CS156")
                                .term("S25")
                                .school("UCSB")
                                .instructorEmail(user.getEmail())
                                .build();

                InstructorCourseView courseView1 = new InstructorCourseView(course1);

                when(courseRepository.findByInstructorEmail(eq(user.getEmail()))).thenReturn(java.util.List.of(course1));

                // act

                MvcResult response = mockMvc.perform(get("/api/courses/allForInstructors"))
                                .andExpect(status().isOk())
                                .andReturn();

                // assert

                String responseString = response.getResponse().getContentAsString();
                String expectedJson = mapper.writeValueAsString(java.util.List.of(courseView1));
                verify(courseRepository, times(1)).findByInstructorEmail(eq(user.getEmail()));
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
                                .instructorEmail(user.getEmail())
                                .id(1L)
                                .build();

                CourseStaff courseStaff1 = CourseStaff.builder().orgStatus(OrgStatus.PENDING).build();
                RosterStudent rs1 = RosterStudent.builder().orgStatus(OrgStatus.PENDING).build();

                course1.setCourseStaff(List.of(courseStaff1));
                course1.setRosterStudents(List.of(rs1));

                CourseStaff courseStaff1Updated = CourseStaff.builder().orgStatus(OrgStatus.JOINCOURSE).build();
                RosterStudent rs1Updated = RosterStudent.builder().orgStatus(OrgStatus.JOINCOURSE).build();
                Course course2 = Course.builder()
                                .courseName("CS156")
                                .orgName("ucsb-cs156-s25")
                                .term("S25")
                                .school("UCSB")
                                .instructorEmail(user.getEmail())
                                .installationId("1234")
                                .orgName("ucsb-cs156-s25")
                                .id(1L)
                                .build();

                course2.setCourseStaff(List.of(courseStaff1Updated));
                course2.setRosterStudents(List.of(rs1Updated));

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
                assertEquals("/instructor/courses?success=True&course=1", responseUrl);
        }

        @Test
        @WithMockUser(roles = { "INSTRUCTOR" })
        public void testLinkCourseSuccessfullyProfessorCreator() throws Exception {
                User user = currentUserService.getCurrentUser().getUser();
                Course course1 = Course.builder()
                                .courseName("CS156")
                                .term("S25")
                                .school("UCSB")
                                .instructorEmail(user.getEmail())
                                .courseStaff(List.of())
                                .rosterStudents(List.of())
                                .id(1L)
                                .build();
                Course course2 = Course.builder()
                                .courseName("CS156")
                                .orgName("ucsb-cs156-s25")
                                .term("S25")
                                .school("UCSB")
                                .instructorEmail(user.getEmail())
                                .installationId("1234")
                                .orgName("ucsb-cs156-s25")
                                .id(1L)
                                .courseStaff(List.of())
                                .rosterStudents(List.of())
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
                assertEquals("/instructor/courses?success=True&course=1", responseUrl);
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
        @WithMockUser(roles = { "INSTRUCTOR" })
        public void testNotCreator() throws Exception {
                User separateUser = User.builder().id(2L).email("separate@example.com").build();
                Course course1 = Course.builder()
                                .courseName("CS156")
                                .term("S25")
                                .school("UCSB")
                                .instructorEmail(separateUser.getEmail())
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
        public void testCourseLinkSuccessWhenAdminNotCreator() throws Exception {
                User user = currentUserService.getCurrentUser().getUser();
                Long userId = user.getId();
                User separateUser = User.builder().id(userId + 1L).email("separate@example.com").build();
                Course courseBefore = Course.builder()
                                .courseName("CS156")
                                .term("S25")
                                .school("UCSB")
                                .instructorEmail(separateUser.getEmail())
                                .courseStaff(List.of())
                                .rosterStudents(List.of())
                                .id(1L)
                                .build();
                Course courseAfter = Course.builder()
                                .courseName("CS156")
                                .orgName("ucsb-cs156-s25")
                                .term("S25")
                                .school("UCSB")
                                .instructorEmail(separateUser.getEmail())
                                .installationId("1234")
                                .orgName("ucsb-cs156-s25")
                                .courseStaff(List.of())
                                .rosterStudents(List.of())
                                .id(1L)
                                .build();

                doReturn(Optional.of(courseBefore)).when(courseRepository).findById(eq(1L));
                doReturn("ucsb-cs156-s25").when(linkerService).getOrgName("1234");
                MvcResult response = mockMvc.perform(get("/api/courses/link")
                                .param("installation_id", "1234")
                                .param("setup_action", "install")
                                .param("code", "abcdefg")
                                .param("state", "1"))
                                .andExpect(status().isMovedPermanently())
                                .andReturn();

                String responseUrl = response.getResponse().getHeader(HttpHeaders.LOCATION);
                verify(courseRepository, times(1)).save(eq(courseAfter));
                assertEquals("/instructor/courses?success=True&course=1", responseUrl);
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
                                .instructorEmail(user.getEmail())
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

        /**
         * Test the POST endpoint
         */
        @Test
        public void testListCoursesForCurrentUser() throws Exception {
                String email = "student@example.com";

                OAuth2User principal = Mockito.mock(OAuth2User.class);
                when(principal.getAttribute("email")).thenReturn(email);

                Authentication auth = new OAuth2AuthenticationToken(
                                principal,
                                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")),
                                "test-client");

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
                                                .with(authentication(auth)))
                                .andExpect(status().isOk())
                                .andReturn();

                Map<String, Object> expected = new LinkedHashMap<>();
                expected.put("id", course.getId());
                expected.put("installationId", course.getInstallationId());
                expected.put("orgName", course.getOrgName());
                expected.put("courseName", course.getCourseName());
                expected.put("term", course.getTerm());
                expected.put("school", course.getSchool());
                expected.put("studentStatus", new RosterStudentDTO(rs).orgStatus());
                expected.put("rosterStudentId", rs.getId());

                String expectedJson = mapper.writeValueAsString(List.of(expected));
                assertEquals(expectedJson, result.getResponse().getContentAsString());
        }

        @Test
        @WithMockUser(roles = { "USER" })
        public void testStudenIsStaffInCourse() throws Exception {
                // arrange
                User currentUser = User.builder()
                                .id(123L)
                                .email("user@example.org")
                                .build();

                Course course1 = Course.builder()
                                .id(1L)
                                .courseName("CS156")
                                .orgName("ucsb-cs156-s25")
                                .term("S25")
                                .school("UCSB")
                                .build();

                Course course2 = Course.builder()
                                .id(2L)
                                .courseName("CS24")
                                .orgName("ucsb-cs24-s25")
                                .term("S25")
                                .school("UCSB")
                                .build();

                CourseStaff cs1 = CourseStaff.builder()
                                .firstName("Chris")
                                .lastName("Gaucho")
                                .email("user@example.org")
                                .course(course1)
                                .user(currentUser)
                                .id(37L)
                                .build();

                CourseStaff cs2 = CourseStaff.builder()
                                .firstName("Chris")
                                .lastName("Gaucho")
                                .email("user@example.org")
                                .course(course2)
                                .user(currentUser)
                                .id(42L)
                                .build();

                StaffCoursesDTO staffCourse1 = new StaffCoursesDTO(
                                course1.getId(),
                                course1.getInstallationId(),
                                course1.getOrgName(),
                                course1.getCourseName(),
                                course1.getTerm(),
                                course1.getSchool(),
                                cs1.getOrgStatus(),
                                cs1.getId());

                StaffCoursesDTO staffCourse2 = new StaffCoursesDTO(
                                course2.getId(),
                                course2.getInstallationId(),
                                course2.getOrgName(),
                                course2.getCourseName(),
                                course2.getTerm(),
                                course2.getSchool(),
                                cs2.getOrgStatus(),
                                cs2.getId());

                when(courseStaffRepository.findAllByEmail("user@example.org"))
                                .thenReturn(List.of(cs1, cs2));

                when(courseRepository.findAllById(List.of(1L, 2L)))
                                .thenReturn(List.of(course1, course2));

                // act
                MvcResult response = mockMvc.perform(get("/api/courses/staffCourses")
                                .param("studentId", "123"))
                                .andExpect(status().isOk())
                                .andReturn();

                // assert
                String responseString = response.getResponse().getContentAsString();
                String expectedJson = mapper.writeValueAsString(List.of(staffCourse1, staffCourse2));
                assertEquals(expectedJson, responseString);
        }

        @Test
        @WithMockUser(roles = { "INSTRUCTOR" })
        public void testGetCourseById() throws Exception {
                // arrange
                User user = currentUserService.getCurrentUser().getUser();
                Course course = Course.builder()
                                .id(1L)
                                .courseName("CS156")
                                .orgName("ucsb-cs156-s25")
                                .term("S25")
                                .school("UCSB")
                                .instructorEmail(user.getEmail())
                                .build();

                CoursesController.InstructorCourseView courseView = new CoursesController.InstructorCourseView(course);

                when(courseRepository.findById(1L)).thenReturn(Optional.of(course));

                // act
                MvcResult response = mockMvc.perform(get("/api/courses/1"))
                                .andExpect(status().isOk())
                                .andReturn();

                // assert
                String responseString = response.getResponse().getContentAsString();
                String expectedJson = mapper.writeValueAsString(courseView);
                assertEquals(expectedJson, responseString);
        }

        @Test
        @WithMockUser(roles = { "INSTRUCTOR" })
        public void testGetCourseById_courseDoesNotExist() throws Exception {
               
                when(courseRepository.findById(1L)).thenReturn(Optional.empty());

                // act
                MvcResult response = mockMvc.perform(get("/api/courses/1"))
                                .andExpect(status().isNotFound())
                                .andReturn();

                // assert
                String responseString = response.getResponse().getContentAsString();
                Map<String, String> expectedMap = Map.of(
                                "type", "EntityNotFoundException",
                                "message", "Course with id 1 not found");
                Map<String, String> actualMap = mapper.readValue(responseString, new TypeReference<Map<String, String>>() {});
                assertEquals(expectedMap, actualMap);
        }

        @Test
        @WithMockUser(roles = { "INSTRUCTOR" })
        public void testGetCourseById_instructorCannotGetCourseCreatedBySomeoneElse() throws Exception {
                // arrange
                User user = currentUserService.getCurrentUser().getUser();
                User otherInstructorUser = User.builder()
                                .id(user.getId() + 1L)
                                .email("not_" + user.getEmail())
                                .build();

                Course course = Course.builder()
                                .id(1L)
                                .courseName("CS156")
                                .orgName("ucsb-cs156-s25")
                                .term("S25")
                                .school("UCSB")
                                .instructorEmail(otherInstructorUser.getEmail())
                                .build();

                when(courseRepository.findById(1L)).thenReturn(Optional.of(course));    
                
                 // act
                MvcResult response = mockMvc.perform(get("/api/courses/1"))
                                .andExpect(status().isNotFound())
                                .andReturn();

                // assert
                String responseString = response.getResponse().getContentAsString();
                Map<String, String> expectedMap = Map.of(
                                "type", "EntityNotFoundException",
                                "message", "Course with id 1 not found");
                Map<String, String> actualMap = mapper.readValue(responseString, new TypeReference<Map<String, String>>() {});
                assertEquals(expectedMap, actualMap);
        }
       
        @Test
        @WithMockUser(roles = { "ADMIN" })
        public void testGetCourseById_AdminCanGetCourseCreatedBySomeoneElse() throws Exception {
                // arrange
                User user = currentUserService.getCurrentUser().getUser();
                User otherInstructorUser = User.builder()
                                .id(user.getId() + 1L)
                                .email("not_" + user.getEmail())
                                .build();

                Course course = Course.builder()
                                .id(1L)
                                .courseName("CS156")
                                .orgName("ucsb-cs156-s25")
                                .term("S25")
                                .school("UCSB")
                                .instructorEmail(otherInstructorUser.getEmail())
                                .build();

                when(courseRepository.findById(1L)).thenReturn(Optional.of(course));    
                
                 // act
                MvcResult response = mockMvc.perform(get("/api/courses/1"))
                                .andExpect(status().isOk())
                                .andReturn();

        }

        @Test
        @WithInstructorCoursePermissions
        public void delete_not_found_returns_not_found() throws Exception {
                when(courseRepository.findById(eq(1L))).thenReturn(Optional.empty());
                MvcResult response = mockMvc.perform(delete("/api/courses")
                        .param("courseId", "1")
                        .with(csrf()))
                        .andExpect(status().isNotFound())
                        .andReturn();
                String responseString = response.getResponse().getContentAsString();
                Map<String, String> expectedMap = Map.of(
                        "type", "EntityNotFoundException",
                        "message", "Course with id 1 not found");
                String expectedJson = mapper.writeValueAsString(expectedMap);
                assertEquals(expectedJson, responseString);
                verify(courseRepository).findById(eq(1L));
                verifyNoMoreInteractions(courseRepository, linkerService, rosterStudentRepository);
        }

        @Test
        @WithInstructorCoursePermissions
        public void delete_success_returns_ok() throws Exception {
                Course course = Course.builder().id(1L).build();
                when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(course));
                MvcResult response = mockMvc.perform(delete("/api/courses")
                        .param("courseId", "1")
                        .with(csrf()))
                        .andExpect(status().isOk())
                        .andReturn();
                verify(linkerService).unenrollOrganization(eq(course));
                verify(courseRepository).findById(eq(1L));
                verify(courseRepository).delete(eq(course));
                verifyNoMoreInteractions(courseRepository, linkerService, rosterStudentRepository);
                String expectedJson = mapper.writeValueAsString(Map.of("message", "Course with id 1 deleted"));
                String responseString = response.getResponse().getContentAsString();
                assertEquals(expectedJson, responseString);
        }

        /**
         * Test that ROLE_ADMIN can update instructor email
         */
        @Test
        @WithMockUser(roles = { "ADMIN" })
        public void testUpdateInstructorEmail_byAdmin() throws Exception {
                User admin = currentUserService.getCurrentUser().getUser();
                Course course = Course.builder()
                                .id(1L)
                                .courseName("CS156")
                                .term("S25")
                                .school("UCSB")
                                .instructorEmail("old-instructor@example.com")
                                .build();

                when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(course));
                when(instructorRepository.existsByEmail(eq("new-instructor@example.com"))).thenReturn(true);
                when(adminRepository.existsByEmail(eq("new-instructor@example.com"))).thenReturn(false);
                
                Course updatedCourse = Course.builder()
                                .id(1L)
                                .courseName("CS156")
                                .term("S25")
                                .school("UCSB")
                                .instructorEmail("new-instructor@example.com")
                                .build();
                        
                when(courseRepository.save(any(Course.class))).thenReturn(updatedCourse);

                // act
                MvcResult response = mockMvc.perform(put("/api/courses/updateInstructor")
                                .with(csrf())
                                .param("courseId", "1")
                                .param("instructorEmail", "new-instructor@example.com"))
                                .andExpect(status().isOk())
                                .andReturn();

                // assert
                verify(courseRepository, times(1)).findById(eq(1L));
                verify(instructorRepository, times(1)).existsByEmail(eq("new-instructor@example.com"));
                verify(courseRepository, times(1)).save(any(Course.class));

                String responseString = response.getResponse().getContentAsString();
                String expectedJson = mapper.writeValueAsString(new InstructorCourseView(updatedCourse));
                assertEquals(expectedJson, responseString);
        }

        /**
         * Test that updateInstructorEmail fails when email doesn't exist in instructor or admin tables
         */
        @Test
        @WithMockUser(roles = { "ADMIN" })
        public void testUpdateInstructorEmail_emailNotFound() throws Exception {
                Course course = Course.builder()
                                .id(1L)
                                .courseName("CS156")
                                .term("S25")
                                .school("UCSB")
                                .instructorEmail("old-instructor@example.com")
                                .build();

                when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(course));
                when(instructorRepository.existsByEmail(eq("nonexistent@example.com"))).thenReturn(false);
                when(adminRepository.existsByEmail(eq("nonexistent@example.com"))).thenReturn(false);

                // act & assert
                mockMvc.perform(put("/api/courses/updateInstructor")
                                .with(csrf())
                                .param("courseId", "1")
                                .param("instructorEmail", "nonexistent@example.com"))
                                .andExpect(status().isBadRequest());

                verify(courseRepository, times(1)).findById(eq(1L));
                verify(instructorRepository, times(1)).existsByEmail(eq("nonexistent@example.com"));
                verify(adminRepository, times(1)).existsByEmail(eq("nonexistent@example.com"));
                verify(courseRepository, never()).save(any(Course.class));
        }

        /**
         * Test that updateInstructorEmail requires ADMIN role
         */
        @Test
        @WithMockUser(roles = { "INSTRUCTOR" })
        public void testUpdateInstructorEmail_requiresAdmin() throws Exception {
                // act & assert
                mockMvc.perform(put("/api/courses/updateInstructor")
                                .with(csrf())
                                .param("courseId", "1")
                                .param("instructorEmail", "new-instructor@example.com"))
                                .andExpect(status().isForbidden());
        }
}


