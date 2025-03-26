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
import edu.ucsb.cs156.frontiers.entities.User;
import edu.ucsb.cs156.frontiers.repositories.CourseRepository;
import edu.ucsb.cs156.frontiers.services.CurrentUserService;
import lombok.extern.slf4j.Slf4j;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Slf4j
@WebMvcTest(controllers = CoursesController.class)
@AutoConfigureDataJpa
public class CoursesControllerTests extends ControllerTestCase {

    @MockitoBean
    private CourseRepository courseRepository;

    @Autowired
    private CurrentUserService currentUserService;

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
}
