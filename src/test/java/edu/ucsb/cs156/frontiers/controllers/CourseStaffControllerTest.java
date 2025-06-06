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
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
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
import edu.ucsb.cs156.frontiers.entities.CourseStaff;
import edu.ucsb.cs156.frontiers.entities.User;
import edu.ucsb.cs156.frontiers.errors.InvalidInstallationTypeException;
import edu.ucsb.cs156.frontiers.repositories.CourseRepository;
import edu.ucsb.cs156.frontiers.repositories.CourseStaffRepository;
import edu.ucsb.cs156.frontiers.repositories.RosterStudentRepository;
import edu.ucsb.cs156.frontiers.repositories.UserRepository;
import edu.ucsb.cs156.frontiers.services.CurrentUserService;
import edu.ucsb.cs156.frontiers.services.OrganizationLinkerService;
import lombok.extern.slf4j.Slf4j;

import java.util.*; 
import edu.ucsb.cs156.frontiers.models.CurrentUser; 
import edu.ucsb.cs156.frontiers.entities.RosterStudent; 
import edu.ucsb.cs156.frontiers.enums.*; 

@Slf4j
@WebMvcTest(controllers = CourseStaffController.class)
@AutoConfigureDataJpa
public class CourseStaffControllerTest extends ControllerTestCase {

    @MockitoBean
    private CourseRepository courseRepository;

    @MockitoBean
    private RosterStudentRepository rosterStudentRepository;

    @MockitoBean
    private CourseStaffRepository courseStaffRepository;

    @MockitoBean
    private CurrentUserService currentUserService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private OrganizationLinkerService linkerService;

    /**
     * Test the POST endpoint
     */
        @Test
        @WithMockUser(roles = { "ADMIN" })
        public void testPostCourseStaff() throws Exception 
        {
                Course course = Course.builder()
                        .id(1L)
                        .courseName("CS156")
                        .orgName("ucsb-cs156")
                        .installationId(null)
                        .term("F23")
                        .school("Engineering")
                        .build();

                CourseStaff sr1 = CourseStaff.builder()
                        .email("cgaucho@example.org")
                        .course(course)
                        .orgStatus(OrgStatus.NONE)
                        .role("Teaching Assistant")
                        .build();

                when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(course));
                when(courseStaffRepository.save(any(CourseStaff.class))).thenReturn(sr1);
                when(courseStaffRepository.findAllByEmail("cgaucho@example.org")).thenReturn(List.of(sr1)); 

                // act

                MvcResult response = mockMvc.perform(post("/api/coursestaff/post")
                                .with(csrf())                    
                                .param("role", "Teaching Assistant")
                                .param("studentEmail", "cgaucho@example.org")
                                .param("courseId", "1"))
                                .andExpect(status().isOk())
                                .andReturn();

                // assert

                verify(courseRepository, times(1)).findById(eq(1L));
                verify(courseStaffRepository, times(1)).save(eq(sr1));

                String responseString = response.getResponse().getContentAsString();
                String expectedJson = mapper.writeValueAsString(sr1);
                assertEquals(expectedJson, responseString);
        }

        @Test
        @WithMockUser(roles = {"ADMIN"})
        public void testCourseLinkNotFound() throws Exception {
                doReturn(Optional.empty()).when(courseRepository).findById(eq(1L));
                MvcResult response = mockMvc.perform(post("/api/coursestaff/post")
                                .with(csrf())                    
                                .param("role", "Teaching Assistant")
                                .param("studentEmail", "cgaucho@example.org")
                                .param("courseId", "1"))
                        .andExpect(status().isNotFound())
                        .andReturn();
                String responseString = response.getResponse().getContentAsString();
                Map<String, String> expectedMap = Map.of(
                        "type", "EntityNotFoundException",
                        "message", "Course with id 1 not found");
                String expectedJson = mapper.writeValueAsString(expectedMap);
                assertEquals(expectedJson, responseString);
        }
}