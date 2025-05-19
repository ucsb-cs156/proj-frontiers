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

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureDataJpa;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;

import edu.ucsb.cs156.frontiers.ControllerTestCase;
import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.entities.User;
import edu.ucsb.cs156.frontiers.errors.InvalidInstallationTypeException;
import edu.ucsb.cs156.frontiers.repositories.CourseRepository;
import edu.ucsb.cs156.frontiers.services.CurrentUserService;
import edu.ucsb.cs156.frontiers.services.OrganizationLinkerService;
import lombok.extern.slf4j.Slf4j;

import java.util.*; 
import edu.ucsb.cs156.frontiers.models.CurrentUser; 
import edu.ucsb.cs156.frontiers.entities.RosterStudent; 
import edu.ucsb.cs156.frontiers.enums.*; 

@Slf4j
@WebMvcTest(controllers = CoursesController.class)
@AutoConfigureDataJpa
public class CoursesControllerTests extends ControllerTestCase {

    @MockitoBean
    private CourseRepository courseRepository;

    @MockitoBean
    private CurrentUserService currentUserService;

    @MockitoBean
    private OrganizationLinkerService linkerService;

    /**
     * Test the POST endpoint
     */
    @Test
    @WithMockUser(roles = { "ADMIN" })
    public void testPostCourse() throws Exception {

        User user = User.builder()
                .email("cgaucho@example.org")
                .build();
        CurrentUser cUser = CurrentUser.builder()
                .user(user)
                .build(); 
        when(currentUserService.getCurrentUser()).thenReturn(cUser); 

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
    @WithMockUser(roles = {"ADMIN"})
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
    @WithMockUser(roles = {"ADMIN"})
    public void testLinkCourseSuccessfully() throws Exception {
        User user = User.builder()
                .email("cgaucho@example.org")
                .build();
        CurrentUser cUser = CurrentUser.builder()
                .user(user)
                .build(); 
        when(currentUserService.getCurrentUser()).thenReturn(cUser); 

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
        assertEquals("/admin/courses?success=True&course=1",  responseUrl);
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    public void testNoPerms() throws Exception {

        MvcResult response = mockMvc.perform(get("/api/courses/link")
                        .param("setup_action", "request")
                        .param("code", "abcdefg")
                        .param("state", "1"))
                .andExpect(status().isMovedPermanently())
                .andReturn();

        String responseUrl = response.getResponse().getHeader(HttpHeaders.LOCATION);
        assertEquals("/courses/nopermissions",  responseUrl);
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    public void testNotCreator() throws Exception {
        User user = User.builder()
                .email("cgaucho@example.org")
                .build();
        CurrentUser cUser = CurrentUser.builder()
                .user(user)
                .build(); 
        when(currentUserService.getCurrentUser()).thenReturn(cUser); 

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
    @WithMockUser(roles = {"ADMIN"})
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
    @WithMockUser(roles = {"ADMIN"})
    public void testNotOrganization() throws Exception {
        User user = User.builder()
                .email("cgaucho@example.org")
                .build();
        CurrentUser cUser = CurrentUser.builder()
                .user(user)
                .build(); 
        when(currentUserService.getCurrentUser()).thenReturn(cUser); 

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
                "message", "Invalid installation type: User. Frontiers can only be linked to organizations");
        String expectedJson = mapper.writeValueAsString(expectedMap);
        assertEquals(expectedJson, responseString);
    }

    @Test
    @WithMockUser(roles = { "USER" })
    public void testLookUpStudentCourseRoster() throws Exception {

        User user = User.builder()
                .email("cgaucho@example.org")
                .build();
        CurrentUser cUser = CurrentUser.builder()
                .user(user)
                .build(); 
        when(currentUserService.getCurrentUser()).thenReturn(cUser); 

        Course course = Course.builder()
                .id(1L)
                .courseName("CS156")
                .orgName("ucsb-cs156")
                .installationId(null)
                .term("F23")
                .school("Engineering")
                .build();

        Course course2 = Course.builder()
                .id(2L)
                .courseName("CS157")
                .orgName("ucsb-cs157")
                .installationId("1234")
                .term("F23")
                .school("Engineering")
                .build();

        RosterStudent rs1 = RosterStudent.builder()
                        .user(user)
                        .email("cgaucho@example.org")
                        .course(course)
                        .rosterStatus(RosterStatus.MANUAL)
                        .orgStatus(OrgStatus.NONE)
                        .build();

        RosterStudent rs2 = RosterStudent.builder()
                        .email("test@example.org")
                        .course(course)
                        .rosterStatus(RosterStatus.MANUAL)
                        .orgStatus(OrgStatus.NONE)
                        .build();

        course.setRosterStudents(List.of(rs1, rs2));
        course2.setRosterStudents(List.of(rs1));

        when(courseRepository.findAll()).thenReturn(List.of(course, course2));

        MvcResult response = mockMvc.perform(get("/api/courses/lookup"))
                .andExpect(status().isOk())
                .andReturn();

        String responseString = response.getResponse().getContentAsString();
        Map<String, Object> expectedMap = new HashMap<>();
        expectedMap.put("id", course.getId());
        expectedMap.put("orgName", course.getOrgName());
        expectedMap.put("courseName", course.getCourseName());
        expectedMap.put("term", course.getTerm());
        expectedMap.put("school", course.getSchool());
        expectedMap.put("installationId", course.getInstallationId()); 
        expectedMap.put("status", "NONE"); 

        Map<String, Object> expectedMap2 = new HashMap<>();
        expectedMap2.put("id", course2.getId());
        expectedMap2.put("orgName", course2.getOrgName());
        expectedMap2.put("courseName", course2.getCourseName());
        expectedMap2.put("term", course2.getTerm());
        expectedMap2.put("school", course2.getSchool());
        expectedMap2.put("installationId", course2.getInstallationId()); 
        expectedMap2.put("status", "NONE"); 

        String expectedJson = mapper.writeValueAsString(List.of(expectedMap, expectedMap2));
        assertEquals(expectedJson, responseString);
    }

}