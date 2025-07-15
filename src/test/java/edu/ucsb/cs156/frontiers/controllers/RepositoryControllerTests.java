package edu.ucsb.cs156.frontiers.controllers;


import com.fasterxml.jackson.databind.ObjectMapper;
import edu.ucsb.cs156.frontiers.ControllerTestCase;
import edu.ucsb.cs156.frontiers.annotations.WithInstructorCoursePermissions;
import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.entities.Job;
import edu.ucsb.cs156.frontiers.entities.User;
import edu.ucsb.cs156.frontiers.jobs.CreateStudentRepositoriesJob;
import edu.ucsb.cs156.frontiers.repositories.CourseRepository;
import edu.ucsb.cs156.frontiers.services.CurrentUserService;
import edu.ucsb.cs156.frontiers.services.RepositoryService;
import edu.ucsb.cs156.frontiers.services.jobs.JobService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureDataJpa;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
@WebMvcTest(controllers = RepositoryController.class)
public class RepositoryControllerTests extends ControllerTestCase {
    @MockitoBean
    private CourseRepository  courseRepository;

    @MockitoBean
    private JobService service;

    @MockitoBean
    private RepositoryService repositoryService;

    @Autowired
    private CurrentUserService  currentUserService;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser(roles = {"ADMIN"})
    public void not_the_creator() throws Exception {
        Course course = Course.builder().creator(User.builder().build()).build();
        doReturn(Optional.of(course)).when(courseRepository).findById(eq(2L));
        MvcResult response = mockMvc.perform(post("/api/repos/createRepos")
                .with(csrf())
                .param("courseId", "2")
                .param("repoPrefix", "repo1")
        ).andExpect(status().isForbidden())
        .andReturn();
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    public void not_registered_org() throws Exception {
        Course course = Course.builder().courseName("course").creator(currentUserService.getUser()).build();
        doReturn(Optional.of(course)).when(courseRepository).findById(eq(2L));
        MvcResult response = mockMvc.perform(post("/api/repos/createRepos")
                        .with(csrf())
                        .param("courseId", "2")
                        .param("repoPrefix", "repo1")
                ).andExpect(status().isBadRequest())
                .andReturn();
        Map<String, Object> json = responseToJson(response);
        assertEquals("NoLinkedOrganizationException", json.get("type"));
        assertEquals("No linked GitHub Organization to course. Please link a GitHub Organization first.", json.get("message"));
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    public void just_no_install_id() throws Exception {
        Course course = Course.builder().courseName("course").orgName("ucsb-cs156").creator(currentUserService.getUser()).build();
        doReturn(Optional.of(course)).when(courseRepository).findById(eq(2L));
        MvcResult response = mockMvc.perform(post("/api/repos/createRepos")
                        .with(csrf())
                        .param("courseId", "2")
                        .param("repoPrefix", "repo1")
                ).andExpect(status().isBadRequest())
                .andReturn();
        Map<String, Object> json = responseToJson(response);
        assertEquals("NoLinkedOrganizationException", json.get("type"));
        assertEquals("No linked GitHub Organization to course. Please link a GitHub Organization first.", json.get("message"));
    }

    @Test
    @WithInstructorCoursePermissions
    public void job_actually_fires_with_instructor() throws Exception {
        Course course = Course.builder().id(2L).orgName("ucsb-cs156").installationId("1234").courseName("course").creator(currentUserService.getUser()).build();
        doReturn(Optional.of(course)).when(courseRepository).findById(eq(2L));
        Job job = Job.builder().status("processing").build();
        doReturn(job).when(service).runAsJob(any(CreateStudentRepositoriesJob.class));
        MvcResult response = mockMvc.perform(post("/api/repos/createRepos")
                        .with(csrf())
                        .param("courseId", "2")
                        .param("repoPrefix", "repo1")
                ).andExpect(status().isOk())
                .andReturn();

        String expectedJson = objectMapper.writeValueAsString(job);
        String actualJson = response.getResponse().getContentAsString();
        assertEquals(expectedJson, actualJson);
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    public void job_actually_fires() throws Exception {
        Course course = Course.builder().id(2L).orgName("ucsb-cs156").installationId("1234").courseName("course").creator(currentUserService.getUser()).build();
        doReturn(Optional.of(course)).when(courseRepository).findById(eq(2L));
        Job job = Job.builder().status("processing").build();
        doReturn(job).when(service).runAsJob(any(CreateStudentRepositoriesJob.class));
        MvcResult response = mockMvc.perform(post("/api/repos/createRepos")
                        .with(csrf())
                        .param("courseId", "2")
                        .param("repoPrefix", "repo1")
                ).andExpect(status().isOk())
                .andReturn();

        String expectedJson = objectMapper.writeValueAsString(job);
        String actualJson = response.getResponse().getContentAsString();
        assertEquals(expectedJson, actualJson);
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    public void notFound() throws Exception {
        Course course = Course.builder().courseName("course").creator(currentUserService.getUser()).build();
        doReturn(Optional.empty()).when(courseRepository).findById(eq(2L));
        MvcResult response = mockMvc.perform(post("/api/repos/createRepos")
                        .with(csrf())
                        .param("courseId", "2")
                        .param("repoPrefix", "repo1")
                ).andExpect(status().isNotFound())
                .andReturn();
        Map<String, Object> json = responseToJson(response);
        assertEquals("EntityNotFoundException", json.get("type"));
        assertEquals("Course with id 2 not found", json.get("message"));
    }
}
