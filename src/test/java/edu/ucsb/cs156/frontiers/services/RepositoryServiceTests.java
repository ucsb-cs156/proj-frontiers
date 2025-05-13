package edu.ucsb.cs156.frontiers.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.entities.RosterStudent;
import edu.ucsb.cs156.frontiers.entities.User;
import edu.ucsb.cs156.frontiers.services.wiremock.WiremockService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureDataJpa;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

@RestClientTest(RepositoryService.class)
@AutoConfigureDataJpa
public class RepositoryServiceTests {
    @MockitoBean
    private JwtService jwtService;

    @Autowired
    private MockRestServiceServer mockRestServiceServer;

    @MockitoBean
    private WiremockService wiremockService;

    @Autowired
    RepositoryService repositoryService;

    @Autowired
    private ObjectMapper objectMapper;

    Course course = Course.builder().orgName("ucsb-cs156").installationId("1234").build();

    @BeforeEach
    public void setup() throws Exception{
        doReturn("real.installation.token").when(jwtService).getInstallationToken(eq(course));
    }

    @Test
    public void repo_already_exists_test() throws Exception{
        mockRestServiceServer
                .expect(requestTo("https://api.github.com/repos/ucsb-cs156/repo1-student1"))
                .andExpect(header("Authorization", "Bearer real.installation.token"))
                .andExpect(header("Accept", "application/vnd.github+json"))
                .andExpect(header("X-GitHub-Api-Version", "2022-11-28"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess());

        User user = User.builder().githubLogin("student1").build();
        RosterStudent student = RosterStudent.builder().user(user).build();

        repositoryService.createStudentRepository(course, student, "repo1", false);
        mockRestServiceServer.verify();
    }

    @Test
    public void successfully_creates_repo_public() throws Exception{
        mockRestServiceServer
                .expect(requestTo("https://api.github.com/repos/ucsb-cs156/repo1-student1"))
                .andExpect(header("Authorization", "Bearer real.installation.token"))
                .andExpect(header("Accept", "application/vnd.github+json"))
                .andExpect(header("X-GitHub-Api-Version", "2022-11-28"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withResourceNotFound());

        Map<String, Object> createBody =  new HashMap<>();
        createBody.put("name", "repo1-student1");
        createBody.put("private", false);
        String createBodyJson =  objectMapper.writeValueAsString(createBody);

        mockRestServiceServer
                .expect(requestTo("https://api.github.com/orgs/ucsb-cs156/repos"))
                .andExpect(header("Authorization", "Bearer real.installation.token"))
                .andExpect(header("Accept", "application/vnd.github+json"))
                .andExpect(header("X-GitHub-Api-Version", "2022-11-28"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json(createBodyJson))
                .andRespond(withSuccess());

        Map<String, Object> provisionBody =  new HashMap<>();
        provisionBody.put("permission", "admin");
        String provisionBodyJson =  objectMapper.writeValueAsString(provisionBody);
        mockRestServiceServer
                .expect(requestTo("https://api.github.com/repos/ucsb-cs156/repo1-student1/collaborators/student1"))
                .andExpect(header("Authorization", "Bearer real.installation.token"))
                .andExpect(header("Accept", "application/vnd.github+json"))
                .andExpect(header("X-GitHub-Api-Version", "2022-11-28"))
                .andExpect(method(HttpMethod.PUT))
                .andExpect(content().json(provisionBodyJson))
                .andRespond(withSuccess());

        User user = User.builder().githubLogin("student1").build();
        RosterStudent student = RosterStudent.builder().user(user).build();

        repositoryService.createStudentRepository(course, student, "repo1", false);

        mockRestServiceServer.verify();
    }

    @Test
    public void successfully_creates_repo_private() throws Exception{
        mockRestServiceServer
                .expect(requestTo("https://api.github.com/repos/ucsb-cs156/repo1-student1"))
                .andExpect(header("Authorization", "Bearer real.installation.token"))
                .andExpect(header("Accept", "application/vnd.github+json"))
                .andExpect(header("X-GitHub-Api-Version", "2022-11-28"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withResourceNotFound());

        Map<String, Object> createBody =  new HashMap<>();
        createBody.put("name", "repo1-student1");
        createBody.put("private", true);
        String createBodyJson =  objectMapper.writeValueAsString(createBody);

        mockRestServiceServer
                .expect(requestTo("https://api.github.com/orgs/ucsb-cs156/repos"))
                .andExpect(header("Authorization", "Bearer real.installation.token"))
                .andExpect(header("Accept", "application/vnd.github+json"))
                .andExpect(header("X-GitHub-Api-Version", "2022-11-28"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json(createBodyJson))
                .andRespond(withSuccess());

        Map<String, Object> provisionBody =  new HashMap<>();
        provisionBody.put("permission", "admin");
        String provisionBodyJson =  objectMapper.writeValueAsString(provisionBody);
        mockRestServiceServer
                .expect(requestTo("https://api.github.com/repos/ucsb-cs156/repo1-student1/collaborators/student1"))
                .andExpect(header("Authorization", "Bearer real.installation.token"))
                .andExpect(header("Accept", "application/vnd.github+json"))
                .andExpect(header("X-GitHub-Api-Version", "2022-11-28"))
                .andExpect(method(HttpMethod.PUT))
                .andExpect(content().json(provisionBodyJson))
                .andRespond(withSuccess());

        User user = User.builder().githubLogin("student1").build();
        RosterStudent student = RosterStudent.builder().user(user).build();

        repositoryService.createStudentRepository(course, student, "repo1", true);

        mockRestServiceServer.verify();
    }


    @Test
    public void exits_if_not_not_found() throws Exception{
        mockRestServiceServer
                .expect(requestTo("https://api.github.com/repos/ucsb-cs156/repo1-student1"))
                .andExpect(header("Authorization", "Bearer real.installation.token"))
                .andExpect(header("Accept", "application/vnd.github+json"))
                .andExpect(header("X-GitHub-Api-Version", "2022-11-28"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withForbiddenRequest());

        User user = User.builder().githubLogin("student1").build();
        RosterStudent student = RosterStudent.builder().user(user).build();

        repositoryService.createStudentRepository(course, student, "repo1", false);
        mockRestServiceServer.verify();
    }
}
