package edu.ucsb.cs156.frontiers.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.ucsb.cs156.frontiers.ControllerTestCase;
import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.entities.CourseStaff;
import edu.ucsb.cs156.frontiers.entities.RosterStudent;
import edu.ucsb.cs156.frontiers.enums.OrgStatus;
import edu.ucsb.cs156.frontiers.repositories.CourseRepository;
import edu.ucsb.cs156.frontiers.repositories.CourseStaffRepository;
import edu.ucsb.cs156.frontiers.repositories.RosterStudentRepository;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;

@WebMvcTest(controllers = WebhookController.class)
@TestPropertySource(properties = "app.webhook.secret=test_webhook_secret_123")
public class WebhookControllerTests extends ControllerTestCase {

  @MockitoBean RosterStudentRepository rosterStudentRepository;

  @MockitoBean CourseRepository courseRepository;

  @MockitoBean CourseStaffRepository courseStaffRepository;

  private static final String TEST_SECRET = "test_webhook_secret_123";

  // Helper method to generate valid signatures for testing
  private String generateValidSignature(String payload, String secret) throws Exception {
    Mac mac = Mac.getInstance("HmacSHA256");
    SecretKeySpec secretKeySpec =
        new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    mac.init(secretKeySpec);
    byte[] signature = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
    return "sha256=" + bytesToHex(signature);
  }

  private String bytesToHex(byte[] bytes) {
    StringBuilder result = new StringBuilder();
    for (byte b : bytes) {
      result.append(String.format("%02x", b));
    }
    return result.toString();
  }

  @Test
  public void webhookWithoutSignature_returnsUnauthorized() throws Exception {
    String sendBody =
        """
                {
                "action" : "member_added",
                "membership": {
                    "role": "direct_member",
                    "user": {
                        "login": "testLogin"
                    }
                },
                "installation":{
                    "id": "1234"
                }
                }
                """;

    MvcResult response =
        mockMvc
            .perform(
                post("/api/webhooks/github")
                    .content(sendBody)
                    .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isUnauthorized())
            .andReturn();

    String actualBody = response.getResponse().getContentAsString();
    assertEquals("Unauthorized: Invalid signature", actualBody);

    verifyNoInteractions(rosterStudentRepository);
    verifyNoInteractions(courseRepository);
    verifyNoInteractions(courseStaffRepository);
  }

  @Test
  public void webhookWithInvalidSignature_returnsUnauthorized() throws Exception {
    String sendBody =
        """
                {
                "action" : "member_added",
                "membership": {
                    "role": "direct_member",
                    "user": {
                        "login": "testLogin"
                    }
                },
                "installation":{
                    "id": "1234"
                }
                }
                """;

    MvcResult response =
        mockMvc
            .perform(
                post("/api/webhooks/github")
                    .content(sendBody)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-Hub-Signature-256", "sha256=invalid_signature"))
            .andExpect(status().isUnauthorized())
            .andReturn();

    String actualBody = response.getResponse().getContentAsString();
    assertEquals("Unauthorized: Invalid signature", actualBody);

    verifyNoInteractions(rosterStudentRepository);
    verifyNoInteractions(courseRepository);
    verifyNoInteractions(courseStaffRepository);
  }

  @Test
  public void successfulWebhook_member() throws Exception {
    Course course = Course.builder().installationId("1234").build();
    RosterStudent student = RosterStudent.builder().githubLogin("testLogin").course(course).build();
    RosterStudent updated =
        RosterStudent.builder()
            .githubLogin("testLogin")
            .course(course)
            .orgStatus(OrgStatus.MEMBER)
            .build();

    doReturn(Optional.of(course)).when(courseRepository).findByInstallationId(contains("1234"));
    doReturn(Optional.of(student))
        .when(rosterStudentRepository)
        .findByCourseAndGithubLogin(eq(course), contains("testLogin"));
    doReturn(updated).when(rosterStudentRepository).save(eq(updated));

    String sendBody =
        """
                {
                "action" : "member_added",
                "membership": {
                    "role": "direct_member",
                    "user": {
                        "login": "testLogin"
                    }
                },
                "installation":{
                    "id": "1234"
                }
                }
                """;

    String signature = generateValidSignature(sendBody, TEST_SECRET);

    MvcResult response =
        mockMvc
            .perform(
                post("/api/webhooks/github")
                    .content(sendBody)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-Hub-Signature-256", signature))
            .andExpect(status().isOk())
            .andReturn();
    verify(rosterStudentRepository, times(1))
        .findByCourseAndGithubLogin(eq(course), contains("testLogin"));
    verify(courseRepository, times(1)).findByInstallationId(contains("1234"));
    verify(rosterStudentRepository, times(1)).save(eq(updated));
    String actualBody = response.getResponse().getContentAsString();
    assertEquals(updated.toString(), actualBody);
  }

  @Test
  public void successfulWebhook_admin() throws Exception {
    Course course = Course.builder().installationId("1234").build();
    RosterStudent student = RosterStudent.builder().githubLogin("testLogin").course(course).build();
    RosterStudent updated =
        RosterStudent.builder()
            .githubLogin("testLogin")
            .course(course)
            .orgStatus(OrgStatus.OWNER)
            .build();

    doReturn(Optional.of(course)).when(courseRepository).findByInstallationId(contains("1234"));
    doReturn(Optional.of(student))
        .when(rosterStudentRepository)
        .findByCourseAndGithubLogin(eq(course), contains("testLogin"));
    doReturn(updated).when(rosterStudentRepository).save(eq(updated));

    String sendBody =
        """
                {
                "action" : "member_added",
                "membership": {
                    "role": "admin",
                    "user": {
                        "login": "testLogin"
                    }
                },
                "installation":{
                    "id": "1234"
                }
                }
                """;

    String signature = generateValidSignature(sendBody, TEST_SECRET);

    MvcResult response =
        mockMvc
            .perform(
                post("/api/webhooks/github")
                    .content(sendBody)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-Hub-Signature-256", signature))
            .andExpect(status().isOk())
            .andReturn();
    verify(rosterStudentRepository, times(1))
        .findByCourseAndGithubLogin(eq(course), contains("testLogin"));
    verify(courseRepository, times(1)).findByInstallationId(contains("1234"));
    verify(rosterStudentRepository, times(1)).save(eq(updated));
    String actualBody = response.getResponse().getContentAsString();
    assertEquals(updated.toString(), actualBody);
  }

  @Test
  public void noStudent() throws Exception {
    Course course = Course.builder().installationId("1234").build();
    doReturn(Optional.of(course)).when(courseRepository).findByInstallationId(contains("1234"));
    doReturn(Optional.empty())
        .when(rosterStudentRepository)
        .findByCourseAndGithubLogin(eq(course), contains("testLogin"));

    String sendBody =
        """
                {
                "action" : "member_added",
                "membership": {
                    "role": "direct_member",
                    "user": {
                        "login": "testLogin"
                    }
                },
                "installation":{
                    "id": "1234"
                }
                }
                """;

    String signature = generateValidSignature(sendBody, TEST_SECRET);

    MvcResult response =
        mockMvc
            .perform(
                post("/api/webhooks/github")
                    .content(sendBody)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-Hub-Signature-256", signature))
            .andExpect(status().isOk())
            .andReturn();
    verify(rosterStudentRepository, times(1))
        .findByCourseAndGithubLogin(eq(course), contains("testLogin"));
    verify(courseRepository, times(1)).findByInstallationId(contains("1234"));
    verify(rosterStudentRepository, times(0)).save(any());
    String actualBody = response.getResponse().getContentAsString();
    assertEquals("success", actualBody);
  }

  @Test
  public void noCourse() throws Exception {
    doReturn(Optional.empty()).when(courseRepository).findByInstallationId(contains("1234"));

    String sendBody =
        """
                {
                "action" : "member_added",
                "membership": {
                    "role": "direct_member",
                    "user": {
                        "login": "testLogin"
                    }
                },
                "installation":{
                    "id": "1234"
                }
                }
                """;

    String signature = generateValidSignature(sendBody, TEST_SECRET);

    MvcResult response =
        mockMvc
            .perform(
                post("/api/webhooks/github")
                    .content(sendBody)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-Hub-Signature-256", signature))
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
    String sendBody =
        """
                {
                "action" : "member_removed",
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

    String signature = generateValidSignature(sendBody, TEST_SECRET);

    MvcResult response =
        mockMvc
            .perform(
                post("/api/webhooks/github")
                    .content(sendBody)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-Hub-Signature-256", signature))
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
    String sendBody =
        """
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

    String signature = generateValidSignature(sendBody, TEST_SECRET);

    MvcResult response =
        mockMvc
            .perform(
                post("/api/webhooks/github")
                    .content(sendBody)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-Hub-Signature-256", signature))
            .andExpect(status().isOk())
            .andReturn();
    verify(rosterStudentRepository, times(0)).findByCourseAndGithubLogin(any(), any());
    verify(courseRepository, times(0)).findByInstallationId(contains("1234"));
    verify(rosterStudentRepository, times(0)).save(any());
    String actualBody = response.getResponse().getContentAsString();
    assertEquals("success", actualBody);
  }

  @Test
  public void successfulWebhook_memberInvited() throws Exception {
    Course course = Course.builder().installationId("1234").build();
    RosterStudent student = RosterStudent.builder().githubLogin("testLogin").course(course).build();
    RosterStudent updated =
        RosterStudent.builder()
            .githubLogin("testLogin")
            .course(course)
            .orgStatus(OrgStatus.INVITED)
            .build();

    // Create argument captor to capture the actual student being saved
    ArgumentCaptor<RosterStudent> studentCaptor = ArgumentCaptor.forClass(RosterStudent.class);

    doReturn(Optional.of(course)).when(courseRepository).findByInstallationId(contains("1234"));
    doReturn(Optional.of(student))
        .when(rosterStudentRepository)
        .findByCourseAndGithubLogin(eq(course), contains("testLogin"));
    doReturn(updated).when(rosterStudentRepository).save(studentCaptor.capture());

    String sendBody =
        """
                {
                "action" : "member_invited",
                "user": {
                    "login": "testLogin"
                },
                "installation":{
                    "id": "1234"
                }
                }
                """;

    String signature = generateValidSignature(sendBody, TEST_SECRET);

    MvcResult response =
        mockMvc
            .perform(
                post("/api/webhooks/github")
                    .content(sendBody)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-Hub-Signature-256", signature))
            .andExpect(status().isOk())
            .andReturn();

    // Verify the methods were called
    verify(rosterStudentRepository, times(1))
        .findByCourseAndGithubLogin(eq(course), contains("testLogin"));
    verify(courseRepository, times(1)).findByInstallationId(contains("1234"));
    verify(rosterStudentRepository, times(1)).save(any(RosterStudent.class));

    // Verify that the status was set to INVITED
    assertEquals(OrgStatus.INVITED, studentCaptor.getValue().getOrgStatus());

    String actualBody = response.getResponse().getContentAsString();
    assertEquals(updated.toString(), actualBody);
  }

  @Test
  public void memberAdded_missingMembershipField() throws Exception {
    String sendBody =
        """
                {
                "action" : "member_added",
                "installation":{
                    "id": "1234"
                }
                }
                """;

    String signature = generateValidSignature(sendBody, TEST_SECRET);

    MvcResult response =
        mockMvc
            .perform(
                post("/api/webhooks/github")
                    .content(sendBody)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-Hub-Signature-256", signature))
            .andExpect(status().isOk())
            .andReturn();
    verify(rosterStudentRepository, times(0)).findByCourseAndGithubLogin(any(), any());
    verify(courseRepository, times(0)).findByInstallationId(any());
    verify(rosterStudentRepository, times(0)).save(any());
    String actualBody = response.getResponse().getContentAsString();
    assertEquals("success", actualBody);
  }

  @Test
  public void memberAdded_missingUserField() throws Exception {
    String sendBody =
        """
                {
                "action" : "member_added",
                "membership": {
                    "role": "direct_member"
                },
                "installation":{
                    "id": "1234"
                }
                }
                """;

    String signature = generateValidSignature(sendBody, TEST_SECRET);

    MvcResult response =
        mockMvc
            .perform(
                post("/api/webhooks/github")
                    .content(sendBody)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-Hub-Signature-256", signature))
            .andExpect(status().isOk())
            .andReturn();
    verify(rosterStudentRepository, times(0)).findByCourseAndGithubLogin(any(), any());
    verify(courseRepository, times(0)).findByInstallationId(any());
    verify(rosterStudentRepository, times(0)).save(any());
    String actualBody = response.getResponse().getContentAsString();
    assertEquals("success", actualBody);
  }

  @Test
  public void memberAdded_missingLoginField() throws Exception {
    String sendBody =
        """
                {
                "action" : "member_added",
                "membership": {
                    "role": "direct_member",
                    "user": {
                    }
                },
                "installation":{
                    "id": "1234"
                }
                }
                """;

    String signature = generateValidSignature(sendBody, TEST_SECRET);

    MvcResult response =
        mockMvc
            .perform(
                post("/api/webhooks/github")
                    .content(sendBody)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-Hub-Signature-256", signature))
            .andExpect(status().isOk())
            .andReturn();
    verify(rosterStudentRepository, times(0)).findByCourseAndGithubLogin(any(), any());
    verify(courseRepository, times(0)).findByInstallationId(any());
    verify(rosterStudentRepository, times(0)).save(any());
    String actualBody = response.getResponse().getContentAsString();
    assertEquals("success", actualBody);
  }

  @Test
  public void memberAdded_missingInstallationField() throws Exception {
    String sendBody =
        """
                {
                "action" : "member_added",
                "membership": {
                    "role": "direct_member",
                    "user": {
                        "login": "testLogin"
                    }
                }
                }
                """;

    String signature = generateValidSignature(sendBody, TEST_SECRET);

    MvcResult response =
        mockMvc
            .perform(
                post("/api/webhooks/github")
                    .content(sendBody)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-Hub-Signature-256", signature))
            .andExpect(status().isOk())
            .andReturn();
    verify(rosterStudentRepository, times(0)).findByCourseAndGithubLogin(any(), any());
    verify(courseRepository, times(0)).findByInstallationId(any());
    verify(rosterStudentRepository, times(0)).save(any());
    String actualBody = response.getResponse().getContentAsString();
    assertEquals("success", actualBody);
  }

  @Test
  public void memberAdded_missingInstallationIdField() throws Exception {
    String sendBody =
        """
                {
                "action" : "member_added",
                "membership": {
                    "role": "direct_member",
                    "user": {
                        "login": "testLogin"
                    }
                },
                "installation":{
                }
                }
                """;

    String signature = generateValidSignature(sendBody, TEST_SECRET);

    MvcResult response =
        mockMvc
            .perform(
                post("/api/webhooks/github")
                    .content(sendBody)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-Hub-Signature-256", signature))
            .andExpect(status().isOk())
            .andReturn();
    verify(rosterStudentRepository, times(0)).findByCourseAndGithubLogin(any(), any());
    verify(courseRepository, times(0)).findByInstallationId(any());
    verify(rosterStudentRepository, times(0)).save(any());
    String actualBody = response.getResponse().getContentAsString();
    assertEquals("success", actualBody);
  }

  @Test
  public void memberInvited_missingUserField() throws Exception {
    String sendBody =
        """
                {
                "action" : "member_invited",
                "installation":{
                    "id": "1234"
                }
                }
                """;

    String signature = generateValidSignature(sendBody, TEST_SECRET);

    MvcResult response =
        mockMvc
            .perform(
                post("/api/webhooks/github")
                    .content(sendBody)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-Hub-Signature-256", signature))
            .andExpect(status().isOk())
            .andReturn();
    verify(rosterStudentRepository, times(0)).findByCourseAndGithubLogin(any(), any());
    verify(courseRepository, times(0)).findByInstallationId(any());
    verify(rosterStudentRepository, times(0)).save(any());
    String actualBody = response.getResponse().getContentAsString();
    assertEquals("success", actualBody);
  }

  @Test
  public void memberInvited_missingLoginField() throws Exception {
    String sendBody =
        """
                {
                "action" : "member_invited",
                "user": {
                },
                "installation":{
                    "id": "1234"
                }
                }
                """;

    String signature = generateValidSignature(sendBody, TEST_SECRET);

    MvcResult response =
        mockMvc
            .perform(
                post("/api/webhooks/github")
                    .content(sendBody)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-Hub-Signature-256", signature))
            .andExpect(status().isOk())
            .andReturn();
    verify(rosterStudentRepository, times(0)).findByCourseAndGithubLogin(any(), any());
    verify(courseRepository, times(0)).findByInstallationId(any());
    verify(rosterStudentRepository, times(0)).save(any());
    String actualBody = response.getResponse().getContentAsString();
    assertEquals("success", actualBody);
  }

  @Test
  public void memberInvited_missingInstallationField() throws Exception {
    String sendBody =
        """
                {
                "action" : "member_invited",
                "user": {
                    "login": "testLogin"
                }
                }
                """;

    String signature = generateValidSignature(sendBody, TEST_SECRET);

    MvcResult response =
        mockMvc
            .perform(
                post("/api/webhooks/github")
                    .content(sendBody)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-Hub-Signature-256", signature))
            .andExpect(status().isOk())
            .andReturn();
    verify(rosterStudentRepository, times(0)).findByCourseAndGithubLogin(any(), any());
    verify(courseRepository, times(0)).findByInstallationId(any());
    verify(rosterStudentRepository, times(0)).save(any());
    String actualBody = response.getResponse().getContentAsString();
    assertEquals("success", actualBody);
  }

  @Test
  public void memberInvited_missingInstallationIdField() throws Exception {
    String sendBody =
        """
                {
                "action" : "member_invited",
                "user": {
                    "login": "testLogin"
                },
                "installation":{
                }
                }
                """;

    String signature = generateValidSignature(sendBody, TEST_SECRET);

    MvcResult response =
        mockMvc
            .perform(
                post("/api/webhooks/github")
                    .content(sendBody)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-Hub-Signature-256", signature))
            .andExpect(status().isOk())
            .andReturn();
    verify(rosterStudentRepository, times(0)).findByCourseAndGithubLogin(any(), any());
    verify(courseRepository, times(0)).findByInstallationId(any());
    verify(rosterStudentRepository, times(0)).save(any());
    String actualBody = response.getResponse().getContentAsString();
    assertEquals("success", actualBody);
  }

  @Test
  public void testUnrecognizedAction_withValidFields() throws Exception {
    String sendBody =
        """
                {
                "action" : "some_other_action",
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

    String signature = generateValidSignature(sendBody, TEST_SECRET);

    MvcResult response =
        mockMvc
            .perform(
                post("/api/webhooks/github")
                    .content(sendBody)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-Hub-Signature-256", signature))
            .andExpect(status().isOk())
            .andReturn();
    verify(rosterStudentRepository, times(0)).findByCourseAndGithubLogin(any(), any());
    verify(courseRepository, times(0)).findByInstallationId(any());
    verify(rosterStudentRepository, times(0)).save(any());
    String actualBody = response.getResponse().getContentAsString();
    assertEquals("success", actualBody);
  }

  @Test
  public void testNullGithubLoginAndInstallationId() throws Exception {
    // This test creates a situation where the action is recognized but the
    // extraction
    // of githubLogin and installationId fails in an unexpected way
    String sendBody =
        """
                {
                "action" : "member_added",
                "membership": {
                    "user": {
                        "not_login": "testLogin"
                    }
                },
                "installation":{
                    "not_id": "1234"
                }
                }
                """;

    String signature = generateValidSignature(sendBody, TEST_SECRET);

    MvcResult response =
        mockMvc
            .perform(
                post("/api/webhooks/github")
                    .content(sendBody)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-Hub-Signature-256", signature))
            .andExpect(status().isOk())
            .andReturn();
    verify(rosterStudentRepository, times(0)).findByCourseAndGithubLogin(any(), any());
    verify(courseRepository, times(0)).findByInstallationId(any());
    verify(rosterStudentRepository, times(0)).save(any());
    String actualBody = response.getResponse().getContentAsString();
    assertEquals("success", actualBody);
  }

  @Test
  public void testMemberRemoved_withValidFields() throws Exception {
    // This test creates a situation where we have a valid action that's not handled
    // but all fields are present, to test the specific branch where githubLogin and
    // installationId
    // are extracted but the action isn't one we process further
    String sendBody =
        """
                {
                "action" : "member_removed",
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

    String signature = generateValidSignature(sendBody, TEST_SECRET);

    MvcResult response =
        mockMvc
            .perform(
                post("/api/webhooks/github")
                    .content(sendBody)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-Hub-Signature-256", signature))
            .andExpect(status().isOk())
            .andReturn();
    verify(rosterStudentRepository, times(0)).findByCourseAndGithubLogin(any(), any());
    verify(courseRepository, times(0)).findByInstallationId(any());
    verify(rosterStudentRepository, times(0)).save(any());
    String actualBody = response.getResponse().getContentAsString();
    assertEquals("success", actualBody);
  }

  @Test
  public void courseStaffInvited() throws Exception {
    Course course = Course.builder().installationId("1234").build();
    CourseStaff staff = CourseStaff.builder().githubLogin("testLogin").course(course).build();
    CourseStaff updated =
        CourseStaff.builder()
            .githubLogin("testLogin")
            .course(course)
            .orgStatus(OrgStatus.INVITED)
            .build();

    doReturn(Optional.of(course)).when(courseRepository).findByInstallationId(contains("1234"));
    doReturn(Optional.of(staff))
        .when(courseStaffRepository)
        .findByCourseAndGithubLogin(eq(course), contains("testLogin"));
    String sendBody =
        """
                {
                "action" : "member_invited",
                "user": {
                    "login": "testLogin"
                },
                "installation":{
                    "id": "1234"
                }
                }
                """;

    String signature = generateValidSignature(sendBody, TEST_SECRET);

    MvcResult response =
        mockMvc
            .perform(
                post("/api/webhooks/github")
                    .content(sendBody)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-Hub-Signature-256", signature))
            .andExpect(status().isOk())
            .andReturn();

    verify(courseStaffRepository, times(1))
        .findByCourseAndGithubLogin(eq(course), contains("testLogin"));
    verify(courseRepository, times(1)).findByInstallationId(contains("1234"));
    verify(courseStaffRepository, times(1)).save(eq(updated));
    verifyNoMoreInteractions(courseStaffRepository, courseStaffRepository);
    verify(rosterStudentRepository, never()).save(any(RosterStudent.class));

    String actualBody = response.getResponse().getContentAsString();
    assertEquals(updated.toString(), actualBody);
  }

  @Test
  public void successfulWebhook_member_course_staff() throws Exception {
    Course course = Course.builder().installationId("1234").build();
    CourseStaff staff = CourseStaff.builder().githubLogin("testLogin").course(course).build();
    CourseStaff updated =
        CourseStaff.builder()
            .githubLogin("testLogin")
            .course(course)
            .orgStatus(OrgStatus.MEMBER)
            .build();

    doReturn(Optional.of(course)).when(courseRepository).findByInstallationId(contains("1234"));
    doReturn(Optional.of(staff))
        .when(courseStaffRepository)
        .findByCourseAndGithubLogin(eq(course), contains("testLogin"));
    doReturn(updated).when(courseStaffRepository).save(eq(updated));

    String sendBody =
        """
                {
                "action" : "member_added",
                "membership": {
                    "role": "direct_member",
                    "user": {
                        "login": "testLogin"
                    }
                },
                "installation":{
                    "id": "1234"
                }
                }
                """;

    String signature = generateValidSignature(sendBody, TEST_SECRET);

    MvcResult response =
        mockMvc
            .perform(
                post("/api/webhooks/github")
                    .content(sendBody)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-Hub-Signature-256", signature))
            .andExpect(status().isOk())
            .andReturn();
    verify(courseStaffRepository, times(1))
        .findByCourseAndGithubLogin(eq(course), contains("testLogin"));
    verify(courseRepository, times(1)).findByInstallationId(contains("1234"));
    verify(courseStaffRepository, times(1)).save(eq(updated));
    String actualBody = response.getResponse().getContentAsString();
    assertEquals(updated.toString(), actualBody);
  }

  @Test
  public void successfulWebhook_admin_course_staff() throws Exception {
    Course course = Course.builder().installationId("1234").build();
    CourseStaff staff = CourseStaff.builder().githubLogin("testLogin").course(course).build();
    CourseStaff updated =
        CourseStaff.builder()
            .githubLogin("testLogin")
            .course(course)
            .orgStatus(OrgStatus.OWNER)
            .build();

    doReturn(Optional.of(course)).when(courseRepository).findByInstallationId(contains("1234"));
    doReturn(Optional.of(staff))
        .when(courseStaffRepository)
        .findByCourseAndGithubLogin(eq(course), contains("testLogin"));
    doReturn(updated).when(courseStaffRepository).save(eq(updated));

    String sendBody =
        """
                {
                "action" : "member_added",
                "membership": {
                    "role": "admin",
                    "user": {
                        "login": "testLogin"
                    }
                },
                "installation":{
                    "id": "1234"
                }
                }
                """;

    String signature = generateValidSignature(sendBody, TEST_SECRET);

    MvcResult response =
        mockMvc
            .perform(
                post("/api/webhooks/github")
                    .content(sendBody)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-Hub-Signature-256", signature))
            .andExpect(status().isOk())
            .andReturn();
    verify(courseStaffRepository, times(1))
        .findByCourseAndGithubLogin(eq(course), contains("testLogin"));
    verify(courseRepository, times(1)).findByInstallationId(contains("1234"));
    verify(courseStaffRepository, times(1)).save(eq(updated));
    String actualBody = response.getResponse().getContentAsString();
    assertEquals(updated.toString(), actualBody);
  }

  @Test
  public void unsuccessfulWebhook_invalidSignature() throws Exception {

    String sendBody =
        """
                {
                "action" : "member_added",
                "membership": {
                    "role": "admin",
                    "user": {
                        "login": "testLogin"
                    }
                },
                "installation":{
                    "id": "1234"
                }
                }
                """;

    String invalid_signature = "INVALID SIGNATURE";

    MvcResult response =
        mockMvc
            .perform(
                post("/api/webhooks/github")
                    .content(sendBody)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-Hub-Signature-256", invalid_signature))
            .andExpect(status().isUnauthorized())
            .andReturn();

    String actualBody = response.getResponse().getContentAsString();
    assertEquals("Unauthorized: Invalid signature", actualBody);
  }

  @Test
  public void unsuccessfulWebhook_noXHubSignature() throws Exception {

    String sendBody = "{}";
    MvcResult response =
        mockMvc
            .perform(
                post("/api/webhooks/github")
                    .content(sendBody)
                    .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isUnauthorized())
            .andReturn();

    String actualBody = response.getResponse().getContentAsString();
    assertEquals("Unauthorized: Invalid signature", actualBody);
  }

  @Test
  public void unsuccessfulWebhook_badJSON() throws Exception {

    String sendBody = """
                INVALID JSON
                  """;

    String signature = generateValidSignature(sendBody, TEST_SECRET);

    MvcResult response =
        mockMvc
            .perform(
                post("/api/webhooks/github")
                    .content(sendBody)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-Hub-Signature-256", signature))
            .andExpect(status().isBadRequest())
            .andReturn();

    String actualBody = response.getResponse().getContentAsString();
    assertEquals("Invalid JSON", actualBody);
  }

  @Test
  public void uninstall_success_clears_installation_and_orgName() throws Exception {
    Course course = Course.builder().installationId("1234").orgName("ucsb-cs156-s25").build();

    ArgumentCaptor<Course> courseCaptor = ArgumentCaptor.forClass(Course.class);

    doReturn(Optional.of(course)).when(courseRepository).findByInstallationId(contains("1234"));
    doReturn(course).when(courseRepository).save(courseCaptor.capture());

    String sendBody =
        """
                {
                "action" : "deleted",
                "installation":{
                    "id": "1234"
                }
                }
                """;

    String signature = generateValidSignature(sendBody, TEST_SECRET);

    MvcResult response =
        mockMvc
            .perform(
                post("/api/webhooks/github")
                    .content(sendBody)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-Hub-Signature-256", signature))
            .andExpect(status().isOk())
            .andReturn();

    verify(courseRepository, times(1)).findByInstallationId(contains("1234"));
    verify(courseRepository, times(1)).save(any(Course.class));

    Course saved = courseCaptor.getValue();
    assertEquals(null, saved.getInstallationId());
    assertEquals(null, saved.getOrgName());

    String actualBody2 = response.getResponse().getContentAsString();
    assertEquals("success", actualBody2);
  }

  @Test
  public void uninstall_missingInstallationField_returns_success_noops() throws Exception {
    String sendBody =
        """
                {
                "action" : "deleted"
                }
                """;

    String signature = generateValidSignature(sendBody, TEST_SECRET);

    MvcResult response =
        mockMvc
            .perform(
                post("/api/webhooks/github")
                    .content(sendBody)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-Hub-Signature-256", signature))
            .andExpect(status().isOk())
            .andReturn();

    verify(courseRepository, times(0)).findByInstallationId(any());
    verify(courseRepository, times(0)).save(any());

    String actualBody3 = response.getResponse().getContentAsString();
    assertEquals("success", actualBody3);
  }

  @Test
  public void uninstall_missingInstallationIdField_returns_success_noops() throws Exception {
    String sendBody =
        """
                {
                "action" : "deleted",
                "installation":{
                }
                }
                """;

    String signature = generateValidSignature(sendBody, TEST_SECRET);

    MvcResult response =
        mockMvc
            .perform(
                post("/api/webhooks/github")
                    .content(sendBody)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-Hub-Signature-256", signature))
            .andExpect(status().isOk())
            .andReturn();

    verify(courseRepository, times(0)).findByInstallationId(any());
    verify(courseRepository, times(0)).save(any());

    String actualBody4 = response.getResponse().getContentAsString();
    assertEquals("success", actualBody4);
  }

  @Test
  public void uninstall_noCourseFound_returns_success() throws Exception {
    doReturn(Optional.empty()).when(courseRepository).findByInstallationId(contains("1234"));

    String sendBody =
        """
                {
                "action" : "deleted",
                "installation":{
                    "id": "1234"
                }
                }
                """;

    String signature = generateValidSignature(sendBody, TEST_SECRET);

    MvcResult response =
        mockMvc
            .perform(
                post("/api/webhooks/github")
                    .content(sendBody)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-Hub-Signature-256", signature))
            .andExpect(status().isOk())
            .andReturn();

    verify(courseRepository, times(1)).findByInstallationId(contains("1234"));
    verify(courseRepository, times(0)).save(any());

    String actualBody5 = response.getResponse().getContentAsString();
    assertEquals("success", actualBody5);
  }
}
