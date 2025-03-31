package edu.ucsb.cs156.frontiers.controllers;

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
import edu.ucsb.cs156.frontiers.entities.RosterStudent;
import edu.ucsb.cs156.frontiers.enums.OrgStatus;
import edu.ucsb.cs156.frontiers.enums.RosterStatus;
import edu.ucsb.cs156.frontiers.repositories.CourseRepository;
import edu.ucsb.cs156.frontiers.repositories.RosterStudentRepository;
import edu.ucsb.cs156.frontiers.services.CurrentUserService;
import edu.ucsb.cs156.frontiers.services.UpdateUserService;
import lombok.extern.slf4j.Slf4j;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.springframework.http.MediaType;

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

        @MockitoBean
        private UpdateUserService updateUserService;

        Course course1 = Course.builder()
                        .id(1L)
                        .courseName("CS156")
                        .orgName("ucsb-cs156-s25")
                        .term("S25")
                        .school("UCSB")
                        .build();

        RosterStudent rs1 = RosterStudent.builder()
                        .firstName("Chris")
                        .lastName("Gaucho")
                        .studentId("A123456")
                        .email("cgaucho@example.org")
                        .course(course1)
                        .rosterStatus(RosterStatus.MANUAL)
                        .orgStatus(OrgStatus.NONE)
                        .build();

        RosterStudent rs2 = RosterStudent.builder()
                        .id(2L)
                        .firstName("Lauren")
                        .lastName("Del Playa")
                        .studentId("A987654")
                        .email("ldelplaya@ucsb.edu")
                        .course(course1)
                        .rosterStatus(RosterStatus.ROSTER)
                        .orgStatus(OrgStatus.NONE)
                        .build();

        /**
         * Test the POST endpoint
         */
        @Test
        @WithMockUser(roles = { "ADMIN" })
        public void testPostRosterStudent() throws Exception {

                when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(course1));
                when(rosterStudentRepository.save(any(RosterStudent.class))).thenReturn(rs1);

                // act

                MvcResult response = mockMvc.perform(post("/api/rosterstudents/post")
                                .with(csrf())
                                .param("studentId", "A123456")
                                .param("firstName", "Chris")
                                .param("lastName", "Gaucho")
                                .param("email", "cgaucho@example.org")
                                .param("courseId", "1"))
                                .andExpect(status().isOk())
                                .andReturn();

                // assert

                verify(courseRepository, times(1)).findById(eq(1L));
                verify(rosterStudentRepository, times(1)).save(eq(rs1));

                String responseString = response.getResponse().getContentAsString();
                String expectedJson = mapper.writeValueAsString(rs1);
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
        public void test_AdminCannotPostRosterStudentForCourseThatDoesNotExist() throws Exception {
                when(courseRepository.findById(eq(1L))).thenReturn(Optional.empty());

                // act

                MvcResult response = mockMvc.perform(post("/api/rosterstudents/post")
                                .with(csrf())
                                .param("studentId", "A123456")
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
        public void testRosterStudentsByCourse() throws Exception {

                // arrange

                when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(course1));
                when(rosterStudentRepository.findByCourseId(eq(1L))).thenReturn(java.util.List.of(rs1, rs2));

                // act

                MvcResult response = mockMvc.perform(get("/api/rosterstudents/course")
                                .param("courseId", "1"))
                                .andExpect(status().isOk())
                                .andReturn();

                // assert

                String responseString = response.getResponse().getContentAsString();
                String expectedJson = mapper.writeValueAsString(java.util.List.of(rs1, rs2));
                assertEquals(expectedJson, responseString);
        }

        /** Test whether admin can get roster students for a non existing course */

        @WithMockUser(roles = { "ADMIN" })
        @Test
        public void admin_can_get_roster_students_for_a_non_existing_course() throws Exception {

                // arrange

                when(courseRepository.findById(eq(1L))).thenReturn(Optional.empty());

                // act

                MvcResult response = mockMvc.perform(get("/api/rosterstudents/course")
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

        /** Test whether admin can upload students */

        private final String sampleCSVContents = """
                        Enrl Cd,Perm #,Grade,Final Units,Student Last,Student First Middle,Quarter,Course ID,Section,Meeting Time(s) / Location(s),Email,ClassLevel,Major1,Major2,Date/Time,Pronoun

                        08235,A123456,,4.0,GAUCHO,CHRIS FAKE,F23,CMPSC156,0100,T R   2:00- 3:15 SH 1431     W    5:00- 5:50 PHELP 3525  W    6:00- 6:50 PHELP 3525  W    7:00- 7:50 PHELP 3525  ,cgaucho@ucsb.edu,SR,CMPSC,,9/27/2023 9:39:25 AM,
                        08250,A987654,,4.0,DEL PLAYA,LAUREN,F23,CMPSC156,0100,T R   2:00- 3:15 SH 1431     W    5:00- 5:50 PHELP 3525  W    6:00- 6:50 PHELP 3525  W    7:00- 7:50 PHELP 3525  ,ldelplaya@umail.ucsb.edu,SR,CMPSC,,9/27/2023 9:39:25 AM,She (She/Her/Hers)
                        08243,1234567,,4.0,TARDE,SABADO,F23,CMPSC156,0100,T R   2:00- 3:15 SH 1431     W    5:00- 5:50 PHELP 3525  W    6:00- 6:50 PHELP 3525  W    7:00- 7:50 PHELP 3525  ,sabadotarde@umail.ucsb.edu,SR,CMPSC,,9/27/2023 9:39:25 AM,He (He/Him/His)
                        """;

        @WithMockUser(roles = { "ADMIN" })
        @Test
        public void admin_can_upload_students_for_an_existing_course() throws Exception {

                // arrange

                RosterStudent rs1BeforeWithId = RosterStudent.builder()
                                .id(1L)
                                .firstName("Chris")
                                .lastName("Gaucho")
                                .studentId("A123456")
                                .email("cgaucho@ucsb.edu")
                                .course(course1)
                                .rosterStatus(RosterStatus.MANUAL)
                                .orgStatus(OrgStatus.NONE)
                                .build();

                RosterStudent rs1AfterWithId = RosterStudent.builder()
                                .id(1L)
                                .firstName("CHRIS FAKE")
                                .lastName("GAUCHO")
                                .studentId("A123456")
                                .email("cgaucho@ucsb.edu")
                                .course(course1)
                                .rosterStatus(RosterStatus.ROSTER)
                                .orgStatus(OrgStatus.NONE)
                                .build();

                RosterStudent rs2BeforeWithId = RosterStudent.builder()
                                .id(2L)
                                .firstName("Lauren")
                                .lastName("Del Playa")
                                .studentId("A987654")
                                .email("ldelplaya@umail.ucsb.edu")
                                .course(course1)
                                .rosterStatus(RosterStatus.ROSTER)
                                .orgStatus(OrgStatus.NONE)
                                .build();

                RosterStudent rs2AfterWithId = RosterStudent.builder()
                                .id(2L)
                                .course(course1)
                                .firstName("LAUREN")
                                .lastName("DEL PLAYA")
                                .email("ldelplaya@ucsb.edu")
                                .studentId("A987654")
                                .rosterStatus(RosterStatus.ROSTER)
                                .orgStatus(OrgStatus.NONE)
                                .build();

                RosterStudent rs3NoId = RosterStudent.builder()
                                .course(course1)
                                .firstName("SABADO")
                                .lastName("TARDE")
                                .email("sabadotarde@ucsb.edu")
                                .studentId("1234567")
                                .rosterStatus(RosterStatus.ROSTER)
                                .orgStatus(OrgStatus.NONE)
                                .build();

                RosterStudent rs3WithId = RosterStudent.builder()
                                .id(3L)
                                .course(course1)
                                .firstName("SABADO")
                                .lastName("TARDE")
                                .email("sabadotarde@ucsb.edu")
                                .studentId("1234567")
                                .rosterStatus(RosterStatus.ROSTER)
                                .orgStatus(OrgStatus.NONE)
                                .build();

                MockMultipartFile file = new MockMultipartFile(
                                "file",
                                "egrades.csv",
                                MediaType.TEXT_PLAIN_VALUE,
                                sampleCSVContents.getBytes());

                when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(course1));
                when(rosterStudentRepository.findByCourseIdAndStudentId(eq(1L), eq("A123456")))
                                .thenReturn(Optional.of(rs1BeforeWithId));
                when(rosterStudentRepository.findByCourseIdAndStudentId(eq(1L), eq("A987654")))
                                .thenReturn(Optional.of(rs2BeforeWithId));
                when(rosterStudentRepository.findByCourseIdAndStudentId(eq(1L), eq("1234567")))
                                .thenReturn(Optional.empty());

                when(rosterStudentRepository.save(eq(rs1AfterWithId))).thenReturn(rs1AfterWithId);
                when(rosterStudentRepository.save(eq(rs2AfterWithId))).thenReturn(rs2AfterWithId);
                when(rosterStudentRepository.save(eq(rs3NoId))).thenReturn(rs3WithId);

                // act

                MvcResult response = mockMvc
                                .perform(multipart("/api/rosterstudents/upload/egrades")
                                                .file(file)
                                                .param("courseId", "1")
                                                .with(csrf()))
                                .andExpect(status().isOk()).andReturn();

                // assert

                verify(courseRepository, atLeastOnce()).findById(eq(1L));
                verify(rosterStudentRepository, atLeastOnce()).findByCourseIdAndStudentId(eq(1L), eq("A123456"));
                verify(rosterStudentRepository, atLeastOnce()).findByCourseIdAndStudentId(eq(1L), eq("A987654"));
                verify(rosterStudentRepository, atLeastOnce()).findByCourseIdAndStudentId(eq(1L), eq("1234567"));
                verify(rosterStudentRepository, atLeastOnce()).save(eq(rs1AfterWithId));
                verify(rosterStudentRepository, atLeastOnce()).save(eq(rs2AfterWithId));
                verify(rosterStudentRepository, atLeastOnce()).save(eq(rs3NoId));

                String responseString = response.getResponse().getContentAsString();
                Map<String, String> expectedMap = Map.of(
                                "filename", "egrades.csv",
                                "message", "Inserted 1 new students, Updated 2 students");
                String expectedJson = mapper.writeValueAsString(expectedMap);
                assertEquals(expectedJson, responseString);

        }

        /** Test that you cannot upload a roster for a course that does not exist */

        @WithMockUser(roles = { "ADMIN" })
        @Test
        public void admin_cannot_upload_students_for_a_course_that_does_not_exist() throws Exception {

                // arrange

                MockMultipartFile file = new MockMultipartFile(
                                "file",
                                "egrades.csv",
                                MediaType.TEXT_PLAIN_VALUE,
                                sampleCSVContents.getBytes());

                when(courseRepository.findById(eq(1L))).thenReturn(Optional.empty());

                // act

                MvcResult response = mockMvc
                                .perform(multipart("/api/rosterstudents/upload/egrades")
                                                .file(file)
                                                .param("courseId", "1")
                                                .with(csrf()))
                                .andExpect(status().isNotFound()).andReturn();

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
