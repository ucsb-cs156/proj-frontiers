package edu.ucsb.cs156.frontiers.security;

import edu.ucsb.cs156.frontiers.config.CourseSecurity;
import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.entities.User;
import edu.ucsb.cs156.frontiers.errors.EntityNotFoundException;
import edu.ucsb.cs156.frontiers.models.CurrentUser;
import edu.ucsb.cs156.frontiers.repositories.CourseRepository;
import edu.ucsb.cs156.frontiers.repositories.UserRepository;
import edu.ucsb.cs156.frontiers.services.CurrentUserService;
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


import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@ContextConfiguration
@NestedTestConfiguration(NestedTestConfiguration.EnclosingConfiguration.INHERIT)
@Import({CourseSecurity.class, DummyCourseSecurity.class})
@EnableMethodSecurity
public class CourseSecurityTests {

    @MockitoBean
    private CourseRepository courseRepository;

    @MockitoBean
    CurrentUserService currentUserService;

    @MockitoBean
    UserRepository userRepository;

    @Autowired
    DummyCourseSecurity DummyCourseSecurity;


    @Nested
    public class SuccessfulAdmin {
        @BeforeEach
        public void setup(){
            User user = User.builder().id(1L).email("admin@example.com").build();
            User user2 = User.builder().id(2L).email("instructor@example.com").build();
            Course testCourse = Course.builder().id(1L).instructorEmail(user2.getEmail()).build();
            when(currentUserService.getCurrentUser()).thenReturn(CurrentUser.builder().user(user).roles(Set.of(new SimpleGrantedAuthority("ROLE_ADMIN"))).build());
            when(courseRepository.findById(1L)).thenReturn(java.util.Optional.of(testCourse));
        }

        @Test
        @WithMockUser(setupBefore = TestExecutionEvent.TEST_EXECUTION, roles = {"ADMIN"})
        public void instructor_can_load_owned_course() {
            DummyCourseSecurity.loadCourse(1L);
        }
    }

    @Nested
    public class SuccessfulInstructor {
        @BeforeEach
        public void setup(){
            User user = User.builder().id(1L).email("instructor@example.com").build();
            Course testCourse = Course.builder().id(1L).instructorEmail(user.getEmail()).build();
            when(currentUserService.getCurrentUser()).thenReturn(CurrentUser.builder().user(user).roles(Set.of(new SimpleGrantedAuthority("ROLE_INSTRUCTOR"))).build());
            when(courseRepository.findById(1L)).thenReturn(java.util.Optional.of(testCourse));
        }

        @Test
        @WithMockUser(setupBefore = TestExecutionEvent.TEST_EXECUTION, roles = {"INSTRUCTOR"})
        public void instructor_can_load_owned_course() {
            DummyCourseSecurity.loadCourse(1L);
        }
    }

    @Nested
    public class UnsuccessfulInstructor {
        @BeforeEach
        public void setup(){
            User user = User.builder().id(1L).email("instructor1@example.com").build();
            User user2 = User.builder().id(2L).email("instructor2@example.com").build();
            Course testCourse = Course.builder().id(1L).instructorEmail(user2.getEmail()).build();
            when(currentUserService.getCurrentUser()).thenReturn(CurrentUser.builder().user(user).roles(Set.of(new SimpleGrantedAuthority("ROLE_INSTRUCTOR"))).build());
            when(courseRepository.findById(1L)).thenReturn(java.util.Optional.of(testCourse));
        }

        @Test
        @WithMockUser(setupBefore = TestExecutionEvent.TEST_EXECUTION, roles = {"INSTRUCTOR"})
        public void instructor_cant_load_non_owned_course() {
            assertThrows(AccessDeniedException.class, () -> DummyCourseSecurity.loadCourse(1L));
        }
    }

    @Nested
    public class NotFound {
        @BeforeEach
        public void setup(){
            User user = User.builder().id(1L).build();
            when(currentUserService.getCurrentUser()).thenReturn(CurrentUser.builder().user(user).roles(Set.of(new SimpleGrantedAuthority("ROLE_INSTRUCTOR"))).build());
            when(courseRepository.findById(1L)).thenReturn(Optional.empty());
        }

        @Test
        @WithMockUser(setupBefore = TestExecutionEvent.TEST_EXECUTION, roles = {"INSTRUCTOR"})
        public void null_on_null() {
            assertTrue(DummyCourseSecurity.nullTest(1L));
        }

    }
}
