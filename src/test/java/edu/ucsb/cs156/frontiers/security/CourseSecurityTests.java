package edu.ucsb.cs156.frontiers.security;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import edu.ucsb.cs156.frontiers.config.CourseSecurity;
import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.entities.CourseStaff;
import edu.ucsb.cs156.frontiers.entities.DownloadRequest;
import edu.ucsb.cs156.frontiers.entities.RosterStudent;
import edu.ucsb.cs156.frontiers.entities.User;
import edu.ucsb.cs156.frontiers.errors.EntityNotFoundException;
import edu.ucsb.cs156.frontiers.models.CurrentUser;
import edu.ucsb.cs156.frontiers.repositories.CourseRepository;
import edu.ucsb.cs156.frontiers.repositories.DownloadRequestRepository;
import edu.ucsb.cs156.frontiers.repositories.RosterStudentRepository;
import edu.ucsb.cs156.frontiers.repositories.UserRepository;
import edu.ucsb.cs156.frontiers.services.CurrentUserService;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.TestExecutionEvent;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.NestedTestConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/*
 * These tests are set up to test {@link CourseSecurity}.
 * To do so, they use {@link DummyCourseSecurity}, which contains the necessary fake methods to test
 * the security methods. These tests are set up differently from other tests in Frontiers due to the
 * way Spring Boot tests are loaded. Before the test starts, the security configuration is loaded.
 * As a result, because the mocks generally aren't set up until the test opens, any when() statements
 * usually won't run. To get around this, we define nested test classes with `@Nested` annotations.
 * This allows us to apply Mockito's BeforeEach to define mock behavior before the security context is loaded.
 * Additionally, the Role Hierarchy in the {@link SecurityConfig} class is loaded in via {@link DummyCourseSecurity}.
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration
@NestedTestConfiguration(NestedTestConfiguration.EnclosingConfiguration.INHERIT)
@Import({CourseSecurity.class, DummyCourseSecurity.class})
@EnableMethodSecurity
public class CourseSecurityTests {

  @MockitoBean private CourseRepository courseRepository;

  @MockitoBean CurrentUserService currentUserService;

  @MockitoBean UserRepository userRepository;

  @MockitoBean RosterStudentRepository rosterStudentRepository;

  @MockitoBean DownloadRequestRepository downloadRequestRepository;

  @Autowired DummyCourseSecurity DummyCourseSecurity;

  @Nested
  public class SuccessfulAdmin {
    @BeforeEach
    public void setup() {
      User user = User.builder().id(1L).email("admin@example.com").build();
      User user2 = User.builder().id(2L).email("instructor@example.com").build();
      Course testCourse =
          Course.builder().id(1L).instructorEmail(user2.getEmail()).courseStaff(List.of()).build();
      when(currentUserService.getCurrentUser())
          .thenReturn(
              CurrentUser.builder()
                  .user(user)
                  .roles(Set.of(new SimpleGrantedAuthority("ROLE_ADMIN")))
                  .build());
      when(courseRepository.findById(1L)).thenReturn(java.util.Optional.of(testCourse));
    }

    @Test
    @WithMockUser(
        setupBefore = TestExecutionEvent.TEST_EXECUTION,
        roles = {"ADMIN"})
    public void instructor_can_load_owned_course() {
      DummyCourseSecurity.loadCourse(1L);
    }
  }

  @Nested
  public class SuccessfulCourseStaff {
    @BeforeEach
    public void setup() {
      User user = User.builder().id(1L).email("coursestaff@example.com").build();
      CourseStaff courseStaff =
          CourseStaff.builder().id(1L).user(user).email("coursestaff@example.com").build();
      Course testCourse =
          Course.builder()
              .id(1L)
              .instructorEmail("alternateemail@ucsb.edu")
              .courseStaff(List.of(courseStaff))
              .build();
      when(currentUserService.getCurrentUser())
          .thenReturn(
              CurrentUser.builder()
                  .user(user)
                  .roles(Set.of(new SimpleGrantedAuthority("ROLE_USER")))
                  .build());
      when(courseRepository.findById(1L)).thenReturn(java.util.Optional.of(testCourse));
    }

    @Test
    @WithMockUser(
        setupBefore = TestExecutionEvent.TEST_EXECUTION,
        roles = {"INSTRUCTOR"})
    public void instructor_can_load_owned_course() {
      DummyCourseSecurity.loadCourse(1L);
    }
  }

  @Nested
  public class SuccessfulInstructor {
    @BeforeEach
    public void setup() {
      User user = User.builder().id(1L).email("instructor@example.com").build();
      Course testCourse =
          Course.builder().id(1L).instructorEmail(user.getEmail()).courseStaff(List.of()).build();
      when(currentUserService.getCurrentUser())
          .thenReturn(
              CurrentUser.builder()
                  .user(user)
                  .roles(Set.of(new SimpleGrantedAuthority("ROLE_INSTRUCTOR")))
                  .build());
      when(courseRepository.findById(1L)).thenReturn(java.util.Optional.of(testCourse));
    }

    @Test
    @WithMockUser(
        setupBefore = TestExecutionEvent.TEST_EXECUTION,
        roles = {"INSTRUCTOR"})
    public void instructor_can_load_owned_course() {
      DummyCourseSecurity.loadCourse(1L);
    }
  }

  @Nested
  public class UnsuccessfulInstructor {
    @BeforeEach
    public void setup() {
      User user = User.builder().id(1L).email("instructor1@example.com").build();
      User user2 = User.builder().id(2L).email("instructor2@example.com").build();
      CourseStaff courseStaff =
          CourseStaff.builder().id(1L).user(user).email("coursestaff@example.com").build();
      Course testCourse =
          Course.builder()
              .id(1L)
              .instructorEmail(user2.getEmail())
              .courseStaff(List.of(courseStaff))
              .build();
      when(currentUserService.getCurrentUser())
          .thenReturn(
              CurrentUser.builder()
                  .user(user)
                  .roles(Set.of(new SimpleGrantedAuthority("ROLE_INSTRUCTOR")))
                  .build());
      when(courseRepository.findById(1L)).thenReturn(java.util.Optional.of(testCourse));
    }

    @Test
    @WithMockUser(
        setupBefore = TestExecutionEvent.TEST_EXECUTION,
        roles = {"INSTRUCTOR"})
    public void instructor_cant_load_non_owned_course() {
      assertThrows(AccessDeniedException.class, () -> DummyCourseSecurity.loadCourse(1L));
    }
  }

  @Nested
  public class NotFound {
    @BeforeEach
    public void setup() {
      User user = User.builder().id(1L).build();
      when(currentUserService.getCurrentUser())
          .thenReturn(
              CurrentUser.builder()
                  .user(user)
                  .roles(Set.of(new SimpleGrantedAuthority("ROLE_INSTRUCTOR")))
                  .build());
      when(courseRepository.findById(1L)).thenReturn(Optional.empty());
    }

    @Test
    @WithMockUser(
        setupBefore = TestExecutionEvent.TEST_EXECUTION,
        roles = {"INSTRUCTOR"})
    public void null_on_null() {
      assertTrue(DummyCourseSecurity.nullTest(1L));
    }
  }

  @Nested
  public class SuccessfulAdminPerms {

    @BeforeEach
    public void setup() {
      User user = User.builder().id(1L).email("admin@example.com").build();
      User user2 = User.builder().id(2L).email("instructor@example.com").build();
      Course testCourse =
          Course.builder().id(1L).instructorEmail(user2.getEmail()).courseStaff(List.of()).build();
      when(currentUserService.getCurrentUser())
          .thenReturn(
              CurrentUser.builder()
                  .user(user)
                  .roles(Set.of(new SimpleGrantedAuthority("ROLE_ADMIN")))
                  .build());
      when(courseRepository.findById(1L)).thenReturn(java.util.Optional.of(testCourse));
    }

    @Test
    @WithMockUser(
        setupBefore = TestExecutionEvent.TEST_EXECUTION,
        roles = {"ADMIN"})
    public void instructor_can_load_owned_course() {
      DummyCourseSecurity.loadCourseInstructor(1L);
    }
  }

  @Nested
  public class SuccessfulInstructorPerms {

    @BeforeEach
    public void setup() {
      User user = User.builder().id(1L).email("instructor@example.com").build();
      Course testCourse =
          Course.builder().id(1L).instructorEmail(user.getEmail()).courseStaff(List.of()).build();
      when(currentUserService.getCurrentUser())
          .thenReturn(
              CurrentUser.builder()
                  .user(user)
                  .roles(Set.of(new SimpleGrantedAuthority("ROLE_INSTRUCTOR")))
                  .build());
      when(courseRepository.findById(1L)).thenReturn(java.util.Optional.of(testCourse));
    }

    @Test
    @WithMockUser(
        setupBefore = TestExecutionEvent.TEST_EXECUTION,
        roles = {"INSTRUCTOR"})
    public void instructor_can_load_owned_course() {
      DummyCourseSecurity.loadCourseInstructor(1L);
    }
  }

  @Nested
  public class UnsuccessfulInstructorPerms {

    @BeforeEach
    public void setup() {
      User user = User.builder().id(1L).email("instructor1@example.com").build();
      User user2 = User.builder().id(2L).email("instructor2@example.com").build();
      Course testCourse =
          Course.builder().id(1L).instructorEmail(user2.getEmail()).courseStaff(List.of()).build();
      when(currentUserService.getCurrentUser())
          .thenReturn(
              CurrentUser.builder()
                  .user(user)
                  .roles(Set.of(new SimpleGrantedAuthority("ROLE_INSTRUCTOR")))
                  .build());
      when(courseRepository.findById(1L)).thenReturn(java.util.Optional.of(testCourse));
    }

    @Test
    @WithMockUser(
        setupBefore = TestExecutionEvent.TEST_EXECUTION,
        roles = {"INSTRUCTOR"})
    public void instructor_cant_load_non_owned_course() {
      assertThrows(AccessDeniedException.class, () -> DummyCourseSecurity.loadCourseInstructor(1L));
    }
  }

  @Nested
  public class NotFoundInstructor {

    @BeforeEach
    public void setup() {
      User user = User.builder().id(1L).build();
      when(currentUserService.getCurrentUser())
          .thenReturn(
              CurrentUser.builder()
                  .user(user)
                  .roles(Set.of(new SimpleGrantedAuthority("ROLE_INSTRUCTOR")))
                  .build());
      when(courseRepository.findById(1L)).thenReturn(Optional.empty());
    }

    @Test
    @WithMockUser(
        setupBefore = TestExecutionEvent.TEST_EXECUTION,
        roles = {"INSTRUCTOR"})
    public void null_on_null() {
      assertTrue(DummyCourseSecurity.nullTestInstructor(1L));
    }
  }

  @Nested
  public class NotFoundRosterStudent {

    @BeforeEach
    public void setup() {
      User user = User.builder().id(1L).build();
      when(currentUserService.getCurrentUser())
          .thenReturn(
              CurrentUser.builder()
                  .user(user)
                  .roles(Set.of(new SimpleGrantedAuthority("ROLE_INSTRUCTOR")))
                  .build());
      when(rosterStudentRepository.findById(1L)).thenReturn(Optional.empty());
    }

    @Test
    @WithMockUser(
        setupBefore = TestExecutionEvent.TEST_EXECUTION,
        roles = {"INSTRUCTOR"})
    public void null_on_null() {
      assertThrows(EntityNotFoundException.class, () -> DummyCourseSecurity.loadRosterStudent(1L));
    }
  }

  @Nested
  public class CorrectPassRosterStudent {

    RosterStudent student;

    @BeforeEach
    public void setup() {
      User user = User.builder().id(1L).email("instructoremail2@ucsb.edu").build();
      when(currentUserService.getCurrentUser())
          .thenReturn(
              CurrentUser.builder()
                  .user(user)
                  .roles(Set.of(new SimpleGrantedAuthority("ROLE_INSTRUCTOR")))
                  .build());
      student =
          RosterStudent.builder()
              .id(1L)
              .course(
                  Course.builder()
                      .id(1L)
                      .instructorEmail("instructoremail@ucsb.edu")
                      .courseStaff(List.of())
                      .build())
              .build();
      when(rosterStudentRepository.findById(1L)).thenReturn(Optional.of(student));
    }

    @Test
    @WithMockUser(
        setupBefore = TestExecutionEvent.TEST_EXECUTION,
        roles = {"INSTRUCTOR"})
    public void returns_properly() {
      assertThrows(AccessDeniedException.class, () -> DummyCourseSecurity.loadRosterStudent(1L));
    }
  }

  @Nested
  public class FalseButExistsRosterStudent {

    RosterStudent student;

    @BeforeEach
    public void setup() {
      User user = User.builder().id(1L).email("instructoremail@ucsb.edu").build();
      when(currentUserService.getCurrentUser())
          .thenReturn(
              CurrentUser.builder()
                  .user(user)
                  .roles(Set.of(new SimpleGrantedAuthority("ROLE_INSTRUCTOR")))
                  .build());
      student =
          RosterStudent.builder()
              .id(1L)
              .course(
                  Course.builder()
                      .id(1L)
                      .instructorEmail("instructoremail@ucsb.edu")
                      .courseStaff(List.of())
                      .build())
              .build();
      when(rosterStudentRepository.findById(1L)).thenReturn(Optional.of(student));
    }

    @Test
    @WithMockUser(
        setupBefore = TestExecutionEvent.TEST_EXECUTION,
        roles = {"INSTRUCTOR"})
    public void returns_properly() {
      assertEquals(student, DummyCourseSecurity.loadRosterStudent(1L));
    }
  }

  @Nested
  public class NotFoundDownloadRequest {

    @BeforeEach
    public void setup() {
      User user = User.builder().id(1L).build();
      when(currentUserService.getCurrentUser())
          .thenReturn(
              CurrentUser.builder()
                  .user(user)
                  .roles(Set.of(new SimpleGrantedAuthority("ROLE_INSTRUCTOR")))
                  .build());
      when(downloadRequestRepository.findById(1L)).thenReturn(Optional.empty());
    }

    @Test
    @WithMockUser(
        setupBefore = TestExecutionEvent.TEST_EXECUTION,
        roles = {"INSTRUCTOR"})
    public void null_on_null() {
      assertThrows(
          EntityNotFoundException.class, () -> DummyCourseSecurity.loadDownloadRequest(1L));
    }
  }

  @Nested
  public class CorrectPassDownloadRequest {

    DownloadRequest downloadRequest;

    @BeforeEach
    public void setup() {
      User user = User.builder().id(1L).email("instructoremail2@ucsb.edu").build();
      when(currentUserService.getCurrentUser())
          .thenReturn(
              CurrentUser.builder()
                  .user(user)
                  .roles(Set.of(new SimpleGrantedAuthority("ROLE_INSTRUCTOR")))
                  .build());
      downloadRequest =
          DownloadRequest.builder()
              .id(1L)
              .course(
                  Course.builder()
                      .id(1L)
                      .instructorEmail("instructoremail@ucsb.edu")
                      .courseStaff(List.of())
                      .build())
              .build();
      when(downloadRequestRepository.findById(1L)).thenReturn(Optional.of(downloadRequest));
    }

    @Test
    @WithMockUser(
        setupBefore = TestExecutionEvent.TEST_EXECUTION,
        roles = {"INSTRUCTOR"})
    public void returns_properly() {
      assertThrows(AccessDeniedException.class, () -> DummyCourseSecurity.loadDownloadRequest(1L));
    }
  }

  @Nested
  public class FalseButExistsDownloadRequest {

    DownloadRequest downloadRequest;

    @BeforeEach
    public void setup() {
      User user = User.builder().id(1L).email("instructoremail@ucsb.edu").build();
      when(currentUserService.getCurrentUser())
          .thenReturn(
              CurrentUser.builder()
                  .user(user)
                  .roles(Set.of(new SimpleGrantedAuthority("ROLE_INSTRUCTOR")))
                  .build());
      downloadRequest =
          DownloadRequest.builder()
              .id(1L)
              .course(
                  Course.builder()
                      .id(1L)
                      .instructorEmail("instructoremail@ucsb.edu")
                      .courseStaff(List.of())
                      .build())
              .build();
      when(downloadRequestRepository.findById(1L)).thenReturn(Optional.of(downloadRequest));
    }

    @Test
    @WithMockUser(
        setupBefore = TestExecutionEvent.TEST_EXECUTION,
        roles = {"INSTRUCTOR"})
    public void returns_properly() {
      assertEquals(downloadRequest, DummyCourseSecurity.loadDownloadRequest(1L));
    }
  }
}
