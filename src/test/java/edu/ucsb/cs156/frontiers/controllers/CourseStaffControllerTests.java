package edu.ucsb.cs156.frontiers.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.ucsb.cs156.frontiers.entities.Job;
import edu.ucsb.cs156.frontiers.entities.User;
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
import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.entities.CourseStaff;
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
}
