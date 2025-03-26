package edu.ucsb.cs156.frontiers.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureDataJpa;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;

import edu.ucsb.cs156.frontiers.ControllerTestCase;
import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.entities.RosterStudent;
import edu.ucsb.cs156.frontiers.entities.User;
import edu.ucsb.cs156.frontiers.enums.OrgStatus;
import edu.ucsb.cs156.frontiers.enums.RosterStatus;
import edu.ucsb.cs156.frontiers.repositories.CourseRepository;
import edu.ucsb.cs156.frontiers.repositories.RosterStudentRepository;
import edu.ucsb.cs156.frontiers.services.CurrentUserService;
import lombok.extern.slf4j.Slf4j;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Slf4j
@WebMvcTest(controllers = RosterStudentsController.class)
@AutoConfigureDataJpa
public class RosterStudentsControllerTests extends ControllerTestCase {

        @MockitoBean
        private CourseRepository courseRepository;

        @MockitoBean
        private RosterStudentRepository rosterStudentRepository;

        @Autowired
        private CurrentUserService currentUserService;

        /**
         * Test the POST endpoint
         */
        @Test
        @WithMockUser(roles = { "ADMIN" })
        public void testPostRosterStudent() throws Exception {

                User user = currentUserService.getCurrentUser().getUser();

                // arrange
                Course course = Course.builder()
                                .id(1L)
                                .courseName("CS156")
                                .orgName("ucsb-cs156-s25")
                                .term("S25")
                                .school("UCSB")
                                .creator(user)
                                .build();

                RosterStudent rosterStudent = RosterStudent.builder()
                                .studentId("123456")
                                .firstName("Chris")
                                .lastName("Gaucho")
                                .email("cgaucho@example.org")
                                .course(course)
                                .rosterStatus(RosterStatus.MANUAL)
                                .orgStatus(OrgStatus.NONE)
                                .build();


                when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(course));
                when(rosterStudentRepository.save(any(RosterStudent.class))).thenReturn(rosterStudent);

                // act

                MvcResult response = mockMvc.perform(post("/api/rosterstudents/post")
                                .with(csrf())
                                .param("studentId", "123456")
                                .param("firstName", "Chris")
                                .param("lastName", "Gaucho")
                                .param("email", "cgaucho@example.org")
                                .param("course_id", "1"))
                                .andExpect(status().isOk())
                                .andReturn();

                // assert

                verify(courseRepository, times(1)).findById(eq(1L));
                verify(rosterStudentRepository, times(1)).save(eq(rosterStudent));

                String responseString = response.getResponse().getContentAsString();
                String expectedJson = mapper.writeValueAsString(rosterStudent);
                assertEquals(expectedJson, responseString);

        }

        /**
         * Test the GET endpoint
         */

        @Test
        @WithMockUser(roles = { "ADMIN" })
        public void testRosterStudentsByCourse() throws Exception {

                // arrange
                Course course1 = Course.builder()
                                .courseName("CS156")
                                .orgName("ucsb-cs156-s25")
                                .term("S25")
                                .school("UCSB")
                                .build();

                RosterStudent rs1 = RosterStudent.builder()
                                .firstName("Chris")
                                .lastName("Gaucho")
                                .studentId("123456")
                                .email("cgaucho@example.org")
                                .course(course1)
                                .rosterStatus(RosterStatus.MANUAL)
                                .orgStatus(OrgStatus.NONE)
                                .build();

                RosterStudent rs2 = RosterStudent.builder()
                                .firstName("Lauren")
                                .lastName("Sabado Tarde")
                                .studentId("234567")
                                .email("lsabado@example.org")
                                .course(course1)
                                .rosterStatus(RosterStatus.MANUAL)
                                .orgStatus(OrgStatus.NONE)
                                .build();

                when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(course1));
                when(rosterStudentRepository.findByCourseId(eq(1L))).thenReturn(java.util.List.of(rs1, rs2));

                // act

                MvcResult response = mockMvc.perform(get("/api/rosterstudents/course")
                                .param("course_id", "1"))
                                .andExpect(status().isOk())
                                .andReturn();

                // assert

                String responseString = response.getResponse().getContentAsString();
                String expectedJson = mapper.writeValueAsString(java.util.List.of(rs1, rs2));
                assertEquals(expectedJson, responseString);
        }
}
