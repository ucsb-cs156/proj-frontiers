package edu.ucsb.cs156.frontiers.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.entities.CourseApiKey;
import edu.ucsb.cs156.frontiers.entities.User;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

public class ApiKeyTokenTests {

  @Test
  public void test_api_key_assert() {
    User user = User.builder().email("test@example.com").build();
    Course course = Course.builder().id(1L).build();
    CourseApiKey courseApiKey =
        CourseApiKey.builder().id(1L).course(course).createdBy(user).build();

    assertThrows(
        AssertionError.class,
        () -> {
          new ApiKeyToken(courseApiKey, user, Set.of(new SimpleGrantedAuthority("ROLE_USER")));
        });

    assertDoesNotThrow(
        () -> {
          new ApiKeyToken(courseApiKey, user, Set.of(new SimpleGrantedAuthority("ROLE_API_KEY")));
        });

    ApiKeyToken testingToken =
        new ApiKeyToken(courseApiKey, user, Set.of(new SimpleGrantedAuthority("ROLE_API_KEY")));

    assertEquals("test@example.com", testingToken.getName());
    assertEquals(1L, testingToken.getCourseId());
    assertNull(testingToken.getCredentials());
    assertTrue(testingToken.isAuthenticated());
    assertEquals(user, testingToken.getPrincipal());
  }
}
