package edu.ucsb.cs156.frontiers.controllers;

import edu.ucsb.cs156.frontiers.ControllerTestCase;
import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.entities.RosterStudent;
import edu.ucsb.cs156.frontiers.entities.User;
import edu.ucsb.cs156.frontiers.enums.OrgStatus;
import edu.ucsb.cs156.frontiers.repositories.CourseRepository;
import edu.ucsb.cs156.frontiers.repositories.RosterStudentRepository;
import edu.ucsb.cs156.frontiers.repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = WebhookController.class)
public class WebhookControllerTests extends ControllerTestCase {

    @MockitoBean
    RosterStudentRepository rosterStudentRepository;

    @MockitoBean
    CourseRepository  courseRepository;

    @Test
    public void successfulWebhook() throws Exception {
        Course course = Course.builder().installationId("1234").build();
        RosterStudent student = RosterStudent.builder().githubLogin("testLogin").course(course).build();
        RosterStudent updated = RosterStudent.builder().githubLogin("testLogin").course(course).orgStatus(OrgStatus.MEMBER).build();

        doReturn(Optional.of(course)).when(courseRepository).findByInstallationId(contains("1234"));
        doReturn(Optional.of(student)).when(rosterStudentRepository).findByCourseAndGithubLogin(eq(course), contains("testLogin"));
        doReturn(updated).when(rosterStudentRepository).save(eq(updated));

        String sendBody = """
                {
                "action" : "member_added",
                "membership": {
                    "user": {
                        "login": "testLogin"
                    }
                },
                "installation":{
                    "id": "1234"
                }
                }
                """;

        MvcResult response = mockMvc.perform(post("/api/webhooks/github")
                .content(sendBody)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();
        verify(rosterStudentRepository, times(1)).findByCourseAndGithubLogin(eq(course), contains("testLogin"));
        verify(courseRepository, times(1)).findByInstallationId(contains("1234"));
        verify(rosterStudentRepository, times(1)).save(eq(updated));
        String actualBody = response.getResponse().getContentAsString();
        assertEquals(updated.toString(), actualBody);
    }

    @Test
    public void noStudent() throws Exception {
        Course course = Course.builder().installationId("1234").build();
        doReturn(Optional.of(course)).when(courseRepository).findByInstallationId(contains("1234"));
        doReturn(Optional.empty()).when(rosterStudentRepository).findByCourseAndGithubLogin(eq(course), contains("testLogin"));

        String sendBody = """
                {
                "action" : "member_added",
                "membership": {
                    "user": {
                        "login": "testLogin"
                    }
                },
                "installation":{
                    "id": "1234"
                }
                }
                """;

        MvcResult response = mockMvc.perform(post("/api/webhooks/github")
                        .content(sendBody)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();
        verify(rosterStudentRepository, times(1)).findByCourseAndGithubLogin(eq(course), contains("testLogin"));
        verify(courseRepository, times(1)).findByInstallationId(contains("1234"));
        verify(rosterStudentRepository, times(0)).save(any());
        String actualBody = response.getResponse().getContentAsString();
        assertEquals("success", actualBody);
    }

    @Test
    public void noCourse() throws Exception {
        doReturn(Optional.empty()).when(courseRepository).findByInstallationId(contains("1234"));

        String sendBody = """
                {
                "action" : "member_added",
                "membership": {
                    "user": {
                        "login": "testLogin"
                    }
                },
                "installation":{
                    "id": "1234"
                }
                }
                """;

        MvcResult response = mockMvc.perform(post("/api/webhooks/github")
                        .content(sendBody)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();
        verify(rosterStudentRepository, times(0)).findByCourseAndGithubLogin(any(), any());
        verify(courseRepository, times(1)).findByInstallationId(contains("1234"));
        verify(rosterStudentRepository, times(0)).save(any());
        String actualBody = response.getResponse().getContentAsString();
        assertEquals("success", actualBody);
    }

    @Test
    public void action_wrong() throws Exception {
        String sendBody = """
                {
                "action" : "member_invited",
                "membership": {
                    "user": {
                        "login": "testLogin"
                    }
                },
                "installation":{
                    "id": "1234"
                }
                }
                """;

        MvcResult response = mockMvc.perform(post("/api/webhooks/github")
                        .content(sendBody)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();
        verify(rosterStudentRepository, times(0)).findByCourseAndGithubLogin(any(), any());
        verify(courseRepository, times(0)).findByInstallationId(contains("1234"));
        verify(rosterStudentRepository, times(0)).save(any());
        String actualBody = response.getResponse().getContentAsString();
        assertEquals("success", actualBody);
    }

    @Test
    public void no_action() throws Exception {
        String sendBody = """
                {
                "membership": {
                    "user": {
                        "login": "testLogin"
                    }
                },
                "installation":{
                    "id": "1234"
                }
                }
                """;

        MvcResult response = mockMvc.perform(post("/api/webhooks/github")
                        .content(sendBody)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();
        verify(rosterStudentRepository, times(0)).findByCourseAndGithubLogin(any(), any());
        verify(courseRepository, times(0)).findByInstallationId(contains("1234"));
        verify(rosterStudentRepository, times(0)).save(any());
        String actualBody = response.getResponse().getContentAsString();
        assertEquals("success", actualBody);
    }
}
